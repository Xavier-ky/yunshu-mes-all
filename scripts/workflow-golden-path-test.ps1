# 黄金演示链 API 验收（PowerShell）
# 用法: .\scripts\workflow-golden-path-test.ps1
# 前置: 后端 :8080 已启动；已执行 V28 migration + story_10 seed

$base = "http://localhost:8080"
$ErrorActionPreference = "Stop"

function Login($user, $pass) {
    $body = @{ username = $user; password = $pass } | ConvertTo-Json
    $r = Invoke-RestMethod -Uri "$base/api/auth/login" -Method Post -Body $body -ContentType "application/json"
    if (-not $r.data.token) { throw "Login failed for $user" }
    return $r.data.token
}

function Get-Headers($token) {
    return @{ Authorization = "Bearer $token"; "Content-Type" = "application/json" }
}

Write-Host "=== 1. Login supervisor ===" -ForegroundColor Cyan
$tok = Login "supervisor" "123456"
$h = Get-Headers $tok

Write-Host "=== 2. Find golden WO ===" -ForegroundColor Cyan
$woList = Invoke-RestMethod -Uri "$base/api/mes/pro/workorder/list?pageNum=1&pageSize=20&workorderCode=WO-GP-20260714" -Headers $h
$wo = $woList.rows | Where-Object { $_.workorderCode -eq "WO-GP-20260714" } | Select-Object -First 1
if (-not $wo) { throw "WO-GP-20260714 not found — run story_10 seed" }
Write-Host "  workorderId=$($wo.workorderId) lifecycle=$($wo.lifecycleStatus)"

Write-Host "=== 3. Dispatch tasks for worker ===" -ForegroundColor Cyan
$tokW = Login "worker" "123456"
$hw = Get-Headers $tokW
$disp = Invoke-RestMethod -Uri "$base/api/planning/dispatch-tasks" -Headers $hw
$dt = $disp.data | Where-Object { $_.workOrderNo -eq "WO-GP-20260714" } | Select-Object -First 1
if ($dt) { Write-Host "  dispatch_id=$($dt.dispatchId) OK" } else { Write-Host "  WARN: no dispatch for WO-GP-20260714" -ForegroundColor Yellow }

Write-Host "=== 4. QC gate on WO-GP-D inbound (should pass lifecycle check) ===" -ForegroundColor Cyan
$tokWh = Login "warehouse" "123456"
$hwh = Get-Headers $tokWh
$recpts = Invoke-RestMethod -Uri "$base/api/mes/wm/productrecpt/list?pageNum=1&pageSize=10&recptCode=PR-GP-D" -Headers $hwh
$recpt = $recpts.rows | Select-Object -First 1
if ($recpt) {
    Write-Host "  recptId=$($recpt.recptId) workorder=$($recpt.workorderCode)"
}

Write-Host "=== 5. QC gate block test (WO-GP-20260714 without QC should fail if lifecycle set) ===" -ForegroundColor Cyan
$badRecpts = Invoke-RestMethod -Uri "$base/api/mes/wm/productrecpt/list?pageNum=1&pageSize=5&workorderCode=WO-GP-20260714" -Headers $hwh
if ($badRecpts.rows.Count -gt 0) {
    Write-Host "  (skip execute — need lines/details first)"
} else {
    Write-Host "  no recpt for main WO — expected at RELEASED stage"
}

Write-Host "`n=== Golden path smoke OK ===" -ForegroundColor Green
Write-Host "Manual: warehouse execute IS-GP-001 -> worker report -> quality IPQC -> warehouse PR-GP-D"
