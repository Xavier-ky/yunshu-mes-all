# MES launcher - silent background, no popup windows.

$ErrorActionPreference = "Stop"
$root = Split-Path -Parent $MyInvocation.MyCommand.Path
$logsDir = Join-Path $root "logs"
$null = New-Item -ItemType Directory -Force -Path $logsDir

. (Join-Path $root "scripts\cleanup-external-repos.ps1") -ProjectRoot $root | Out-Null

function Test-PortListening([int]$Port) {
    try {
        $c = Get-NetTCPConnection -LocalPort $Port -State Listen -ErrorAction Stop
        return $null -ne $c
    } catch {
        return $false
    }
}

function Wait-HttpOk([string]$Url, [int]$MaxSeconds = 180) {
    $deadline = (Get-Date).AddSeconds($MaxSeconds)
    while ((Get-Date) -lt $deadline) {
        try {
            $r = Invoke-WebRequest -Uri $Url -UseBasicParsing -TimeoutSec 5
            if ($r.StatusCode -ge 200 -and $r.StatusCode -lt 500) { return $true }
        } catch { }
        Start-Sleep -Seconds 3
    }
    return $false
}

function Test-QdrantIndexCorruption([string]$ComposeFile) {
    # Recover only for the known persistent-index corruption signatures. A
    # stopped Docker Desktop, slow startup, or ordinary network error must
    # never cause a knowledge-index replacement.
    try {
        $containerLog = (& docker compose -f $ComposeFile logs --tail 120 qdrant 2>&1 | Out-String)
        return $containerLog -match "Failed to deserialize|Failed to load local shard|gridstore::blob"
    } catch {
        return $false
    }
}

function Invoke-QdrantIndexRecovery {
    param(
        [string]$ComposeFile,
        [string]$StoragePath,
        [string]$ProjectRoot,
        [string]$AgentBackend,
        [string]$PythonExe
    )

    $dataRoot = Join-Path $ProjectRoot "agent-system\data"
    $resolvedStorage = [System.IO.Path]::GetFullPath($StoragePath)
    $resolvedDataRoot = [System.IO.Path]::GetFullPath($dataRoot)
    if (-not $resolvedStorage.StartsWith($resolvedDataRoot + [System.IO.Path]::DirectorySeparatorChar, [System.StringComparison]::OrdinalIgnoreCase)) {
        throw "Refusing Qdrant recovery outside project data directory: $resolvedStorage"
    }
    if (-not (Test-Path -LiteralPath $resolvedStorage)) {
        throw "Qdrant recovery storage directory is missing: $resolvedStorage"
    }

    $timestamp = Get-Date -Format "yyyyMMdd-HHmmss"
    $backupPath = Join-Path $dataRoot "qdrant-corrupt-$timestamp"
    $suffix = 1
    while (Test-Path -LiteralPath $backupPath) {
        $backupPath = Join-Path $dataRoot "qdrant-corrupt-$timestamp-$suffix"
        $suffix++
    }

    Write-Host "  Detected a damaged Qdrant index; preserving it at $backupPath" -ForegroundColor Yellow
    & docker compose -f $ComposeFile down
    if ($LASTEXITCODE -ne 0) { throw "Unable to stop the damaged Qdrant container." }

    # Move, never delete: a failed automatic recovery remains inspectable.
    Move-Item -LiteralPath $resolvedStorage -Destination $backupPath
    New-Item -ItemType Directory -Force -Path $resolvedStorage | Out-Null

    & docker compose -f $ComposeFile up -d
    if ($LASTEXITCODE -ne 0) { throw "Unable to start Qdrant after preserving its damaged index." }
    if (-not (Wait-HttpOk "http://127.0.0.1:6333/" 60)) {
        throw "Qdrant did not become ready after index recovery."
    }

    Push-Location $AgentBackend
    try {
        & $PythonExe scripts\init_qdrant_collection.py --verify
        if ($LASTEXITCODE -ne 0) { throw "Unable to initialize the recovered Qdrant collection." }
        & $PythonExe scripts\ingest_initial_knowledge.py
        if ($LASTEXITCODE -ne 0) { throw "Unable to re-import approved RAG knowledge." }
        & $PythonExe scripts\evaluate_knowledge.py
        if ($LASTEXITCODE -ne 0) { throw "Recovered Qdrant knowledge failed its retrieval regression suite." }
    } finally {
        Pop-Location
    }

    Write-Host "  Qdrant index recovered and approved knowledge re-imported." -ForegroundColor Green
}

function Start-BackgroundProcess {
    param(
        [string]$Name,
        [string]$WorkDir,
        [string]$FilePath,
        [string[]]$ArgumentList = @(),
        [hashtable]$Env = @{}
    )

    $outLog = Join-Path $logsDir "$Name.out.log"
    $errLog = Join-Path $logsDir "$Name.err.log"
    $pidFile = Join-Path $logsDir "$Name.pid"

    if (Test-Path $pidFile) {
        $oldPid = Get-Content $pidFile -ErrorAction SilentlyContinue
        if ($oldPid -and (Get-Process -Id $oldPid -ErrorAction SilentlyContinue)) {
            Write-Host "[$Name] already running (PID $oldPid)" -ForegroundColor DarkGray
            return [int]$oldPid
        }
    }

    foreach ($key in $Env.Keys) {
        Set-Item -Path "env:$key" -Value $Env[$key]
    }

    $proc = Start-Process -FilePath $FilePath -ArgumentList $ArgumentList `
        -WorkingDirectory $WorkDir -WindowStyle Hidden -PassThru `
        -RedirectStandardOutput $outLog -RedirectStandardError $errLog

    Set-Content -Path $pidFile -Value $proc.Id -Encoding ascii
    Write-Host "[$Name] started (PID $($proc.Id)), logs: logs\$Name.out.log" -ForegroundColor Green
    return $proc.Id
}

Write-Host "========================================" -ForegroundColor Cyan
Write-Host "Yunshu MES silent startup" -ForegroundColor Cyan
Write-Host "========================================" -ForegroundColor Cyan

$backDir = Join-Path $root "backend\mes-server"
$frontDir = Join-Path $root "frontend\web-admin"
$agentDir = Join-Path $root "agent-system\backend"
$faceAuthDir = Join-Path $root "face-auth"
$ragCompose = Join-Path $root "agent-system\docker-compose.rag.yml"
$ragStorage = Join-Path $root "agent-system\data\qdrant"
$agentPython = "D:\Anaconda2024.10\envs\pytorch\python.exe"
if (-not (Test-Path $agentPython)) { $agentPython = "python" }

if (-not (Wait-HttpOk "http://127.0.0.1:6333/" 3)) {
    if (-not (Get-Command docker -ErrorAction SilentlyContinue)) {
        Write-Host "[1/4] Qdrant needs Docker Desktop, but docker was not found." -ForegroundColor Red
        exit 1
    }
    Write-Host "[1/4] Qdrant :6333" -ForegroundColor Yellow
    & docker compose -f $ragCompose up -d
    if ($LASTEXITCODE -ne 0) {
        Write-Host "Qdrant failed. Start Docker Desktop and retry. See $ragCompose" -ForegroundColor Red
        exit 1
    }
    if (-not (Wait-HttpOk "http://127.0.0.1:6333/" 60)) {
        if (Test-QdrantIndexCorruption $ragCompose) {
            try {
                Invoke-QdrantIndexRecovery -ComposeFile $ragCompose -StoragePath $ragStorage `
                    -ProjectRoot $root -AgentBackend $agentDir -PythonExe $agentPython
            } catch {
                Write-Host "Qdrant automatic recovery failed: $($_.Exception.Message)" -ForegroundColor Red
                exit 1
            }
        } else {
            Write-Host "Qdrant did not become ready on :6333. Docker or its startup may be unavailable; the existing index was left untouched." -ForegroundColor Red
            exit 1
        }
    }
    Write-Host "  Qdrant ready: http://127.0.0.1:6333/dashboard/" -ForegroundColor Green
} else {
    Write-Host "[1/4] Qdrant :6333 already listening" -ForegroundColor DarkGray
}

if (-not (Test-PortListening 8080)) {
    Write-Host "[2/4] Backend :8080" -ForegroundColor Yellow
    Start-BackgroundProcess -Name "backend" -WorkDir $backDir `
        -FilePath "cmd.exe" -ArgumentList @("/c", "mvnw.cmd", "spring-boot:run") `
        -Env @{ SPRING_PROFILES_ACTIVE = "local" }
    if (-not (Wait-HttpOk "http://127.0.0.1:8080/api/health")) {
        Write-Host "Backend failed. See logs\backend.err.log" -ForegroundColor Red
        exit 1
    }
    Write-Host "  Backend ready: http://127.0.0.1:8080/api/health" -ForegroundColor Green
} else {
    Write-Host "[2/4] Backend :8080 already listening" -ForegroundColor DarkGray
}

if (-not (Test-PortListening 8090)) {
    Write-Host "[3/5] AI companion :8090" -ForegroundColor Yellow
    Start-BackgroundProcess -Name "agent" -WorkDir $agentDir -FilePath $agentPython `
        -ArgumentList @("-m", "uvicorn", "app.main:app", "--host", "127.0.0.1", "--port", "8090")
} else {
    Write-Host "[3/5] AI companion :8090 already listening" -ForegroundColor DarkGray
}

if ((Test-Path (Join-Path $faceAuthDir "app\main.py")) -and -not (Test-PortListening 8091)) {
    Write-Host "[4/5] Face verification :8091" -ForegroundColor Yellow
    Remove-Item -LiteralPath (Join-Path $logsDir "face-auth.stop") -Force -ErrorAction SilentlyContinue
    Start-BackgroundProcess -Name "face-auth" -WorkDir $faceAuthDir -FilePath "powershell.exe" `
        -ArgumentList @("-NoProfile", "-ExecutionPolicy", "Bypass", "-File", (Join-Path $faceAuthDir "run-supervisor.ps1"), "-PythonExe", $agentPython, "-LogDirectory", $logsDir) `
        -Env @{ TF_CPP_MIN_LOG_LEVEL = "2" }
    if (-not (Wait-HttpOk "http://127.0.0.1:8091/api/face/health" 90)) {
        Write-Host "Face verification service failed. See logs\face-auth.err.log" -ForegroundColor Red
        exit 1
    }
    Write-Host "  Face verification ready: http://127.0.0.1:8091/api/face/health" -ForegroundColor Green
} elseif (Test-PortListening 8091) {
    Write-Host "[4/5] Face verification :8091 already listening" -ForegroundColor DarkGray
}

if (-not (Test-PortListening 5173)) {
    Write-Host "[5/5] Frontend :5173" -ForegroundColor Yellow
    Start-BackgroundProcess -Name "frontend" -WorkDir $frontDir `
        -FilePath "cmd.exe" -ArgumentList @("/c", "npm", "run", "dev", "--", "--host", "127.0.0.1", "--port", "5173")
    if (-not (Wait-HttpOk "http://127.0.0.1:5173")) {
        Write-Host "Frontend failed. See logs\frontend.err.log" -ForegroundColor Red
        exit 1
    }
    Write-Host "  Frontend ready: http://127.0.0.1:5173" -ForegroundColor Green
} else {
    Write-Host "[5/5] Frontend :5173 already listening" -ForegroundColor DarkGray
}

Write-Host ""
Write-Host "All services running in background." -ForegroundColor Green
Write-Host "  Portal : http://127.0.0.1:5173" -ForegroundColor White
Write-Host "  API    : http://127.0.0.1:8080" -ForegroundColor White
Write-Host "  Face   : http://127.0.0.1:8091/api/face/health" -ForegroundColor White
Write-Host "  Logs   : $logsDir" -ForegroundColor White
Write-Host "  Stop   : powershell -File stop.ps1" -ForegroundColor DarkGray
