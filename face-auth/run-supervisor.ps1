param(
    [Parameter(Mandatory = $true)]
    [string]$PythonExe,
    [Parameter(Mandatory = $true)]
    [string]$LogDirectory
)

# Keeps only the optional local face-auth service alive. It deliberately does
# not start or restart MES, Agent, Docker, or Qdrant services.
$ErrorActionPreference = "Stop"
$serviceRoot = Split-Path -Parent $MyInvocation.MyCommand.Path
$stopFile = Join-Path $LogDirectory "face-auth.stop"
$workerPidFile = Join-Path $LogDirectory "face-auth.worker.pid"
$workerOutLog = Join-Path $LogDirectory "face-auth.out.log"
$workerErrLog = Join-Path $LogDirectory "face-auth.err.log"
$supervisorLog = Join-Path $LogDirectory "face-auth.supervisor.log"

function Write-SupervisorLog([string]$Message) {
    $line = "{0} {1}" -f (Get-Date -Format "yyyy-MM-dd HH:mm:ss"), $Message
    Add-Content -LiteralPath $supervisorLog -Value $line -Encoding utf8
}

Remove-Item -LiteralPath $stopFile -Force -ErrorAction SilentlyContinue
Write-SupervisorLog "Face-auth supervisor started."

try {
    while (-not (Test-Path -LiteralPath $stopFile)) {
        $env:TF_CPP_MIN_LOG_LEVEL = "2"
        $worker = Start-Process -FilePath $PythonExe `
            -ArgumentList @("-m", "uvicorn", "app.main:app", "--host", "127.0.0.1", "--port", "8091") `
            -WorkingDirectory $serviceRoot -WindowStyle Hidden -PassThru `
            -RedirectStandardOutput $workerOutLog -RedirectStandardError $workerErrLog
        Set-Content -LiteralPath $workerPidFile -Value $worker.Id -Encoding ascii
        Write-SupervisorLog "Face-auth worker started (PID $($worker.Id))."

        $worker.WaitForExit()
        $exitCode = $worker.ExitCode
        Remove-Item -LiteralPath $workerPidFile -Force -ErrorAction SilentlyContinue

        if (Test-Path -LiteralPath $stopFile) {
            Write-SupervisorLog "Face-auth worker stopped intentionally (exit $exitCode)."
            break
        }

        Write-SupervisorLog "Face-auth worker exited unexpectedly (exit $exitCode); restarting in 3 seconds."
        Start-Sleep -Seconds 3
    }
} finally {
    Remove-Item -LiteralPath $workerPidFile -Force -ErrorAction SilentlyContinue
    Write-SupervisorLog "Face-auth supervisor stopped."
}
