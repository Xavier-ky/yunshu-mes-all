$ErrorActionPreference = "Stop"

$python = "D:\Anaconda2024.10\envs\pytorch\python.exe"
if (-not (Test-Path $python)) {
  $python = "python"
}

Write-Host "Starting Yunshu companion API on http://127.0.0.1:8090" -ForegroundColor Cyan
& $python -m uvicorn app.main:app --host 127.0.0.1 --port 8090
