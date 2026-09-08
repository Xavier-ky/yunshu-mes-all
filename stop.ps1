# Stop background MES services started by start.ps1

$ErrorActionPreference = "SilentlyContinue"
$root = Split-Path -Parent $MyInvocation.MyCommand.Path
$logsDir = Join-Path $root "logs"

foreach ($name in @("backend", "frontend", "agent", "face-auth")) {
    $pidFile = Join-Path $logsDir "$name.pid"
    if (-not (Test-Path $pidFile)) { continue }
    if ($name -eq "face-auth") {
        # Tell the watchdog not to restart its worker during an intentional stop.
        Set-Content -LiteralPath (Join-Path $logsDir "face-auth.stop") -Value "stop" -Encoding ascii
        $workerPidFile = Join-Path $logsDir "face-auth.worker.pid"
        if (Test-Path $workerPidFile) {
            $workerProcessId = Get-Content $workerPidFile
            if (Get-Process -Id $workerProcessId -ErrorAction SilentlyContinue) {
                Stop-Process -Id $workerProcessId -Force
                Write-Host "Stopped face-auth worker (PID $workerProcessId)" -ForegroundColor Yellow
            }
            Remove-Item $workerPidFile -Force
        }
    }
    $processId = Get-Content $pidFile
    $proc = Get-Process -Id $processId -ErrorAction SilentlyContinue
    if ($proc) {
        Stop-Process -Id $processId -Force
        Write-Host "Stopped $name (PID $processId)" -ForegroundColor Yellow
    }
    Remove-Item $pidFile -Force
}

Write-Host "Done." -ForegroundColor Green
