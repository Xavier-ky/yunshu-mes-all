# Golden path E2E: WO-GP-20260714 (PowerShell, ASCII-only)
param([switch]$ResetSeed)

$base = "http://localhost:8080"
$ErrorActionPreference = "Stop"
$GoldenWo = "WO-GP-20260714"

function Login($user, $pass) {
    $body = @{ username = $user; password = $pass } | ConvertTo-Json
    $r = Invoke-RestMethod -Uri "$base/api/auth/login" -Method Post -Body $body -ContentType "application/json"
    if (-not $r.data.token) { throw "Login failed: $user" }
    return $r.data.token
}

function AuthHeaders($token) {
    return @{ Authorization = "Bearer $token"; "Content-Type" = "application/json" }
}

function Assert-Lifecycle($token, $expected) {
    $h = AuthHeaders $token
    $uri = "$base/api/mes/pro/workorder/list?pageNum=1&pageSize=5&workorderCode=$GoldenWo"
    $list = Invoke-RestMethod -Uri $uri -Headers $h
    $wo = $list.rows | Where-Object { $_.workorderCode -eq $GoldenWo } | Select-Object -First 1
    if (-not $wo) { throw "Golden WO not found" }
    if ($wo.lifecycleStatus -ne $expected) {
        throw "Expected lifecycle=$expected got $($wo.lifecycleStatus)"
    }
    Write-Host "  lifecycle=$($wo.lifecycleStatus) OK"
}

function Assert-PipelineLinks($token) {
    $h = AuthHeaders $token
    $pipe = Invoke-RestMethod -Uri "$base/api/planning/workflow/pipeline/$GoldenWo" -Headers $h
    if (-not $pipe.data.workOrder) { throw "Pipeline missing workOrder" }
    if ($pipe.data.issues.Count -lt 1) { throw "Pipeline missing issues" }
    Write-Host "  pipeline links OK (issues=$($pipe.data.issues.Count))"
}

function Assert-ReportAndConsume($token, $workOrderId) {
    $h = AuthHeaders $token
    $trace = Invoke-RestMethod -Uri "$base/api/traceability/work-order/$GoldenWo" -Headers $h
    if ($trace.data.reports.Count -lt 1) { throw "Expected production_report after feedback" }
    if ($trace.data.consumes.Count -lt 1) { throw "Expected wm_item_consume after issue" }
    Write-Host "  reports=$($trace.data.reports.Count) consumes=$($trace.data.consumes.Count) OK"
}

if ($ResetSeed) {
    Write-Host "Reset seed and main WO state..."
    Get-Content "d:\ClaudeCode\MES\backend\mes-server\src\main\resources\db\seed\R__seed_story_10_golden_path.sql" | mysql -uroot -p051002sry fan_mes 2>$null
    mysql -uroot -p051002sry fan_mes -e "UPDATE work_order SET lifecycle_status='RELEASED', completed_qty=0 WHERE work_order_no='WO-GP-20260714'; UPDATE wm_issue_header SET status='APPROVED' WHERE issue_code='IS-GP-001'; UPDATE wm_product_recpt SET status='PREPARE' WHERE recpt_code='PR-GP-001'; UPDATE dispatch_task SET completed_qty=0, status='CREATED' WHERE dispatch_no='DT-GP-001'; DELETE FROM pro_feedback WHERE workorder_code='WO-GP-20260714' AND feedback_code LIKE 'FB-E2E-%'; DELETE FROM qc_ipqc WHERE ipqc_code LIKE 'IPQC-E2E-%'; DELETE FROM production_report_detail WHERE item_type='PRO_FEEDBACK' AND item_code LIKE 'FB-E2E-%'; DELETE FROM production_report WHERE report_no LIKE 'RPT-FB-E2E-%'; DELETE d FROM wm_item_consume_detail d JOIN wm_item_consume c ON c.record_id=d.record_id JOIN wm_issue_header h ON h.issue_id=CAST(c.attr1 AS UNSIGNED) WHERE h.issue_code='IS-GP-001'; DELETE l FROM wm_item_consume_line l JOIN wm_item_consume c ON c.record_id=l.record_id JOIN wm_issue_header h ON h.issue_id=CAST(c.attr1 AS UNSIGNED) WHERE h.issue_code='IS-GP-001'; DELETE c FROM wm_item_consume c JOIN wm_issue_header h ON h.issue_id=CAST(c.attr1 AS UNSIGNED) WHERE h.issue_code='IS-GP-001';" 2>$null
}

Write-Host "Step 1: issue execute"
$tokWh = Login "warehouse" "123456"
$hWh = AuthHeaders $tokWh
$issues = Invoke-RestMethod -Uri "$base/api/mes/wm/issueheader/list?pageNum=1&pageSize=10&issueCode=IS-GP-001" -Headers $hWh
$issueId = $issues.rows[0].issueId
Invoke-RestMethod -Uri "$base/api/mes/wm/issueheader/$issueId" -Method Put -Headers $hWh | Out-Null
Assert-Lifecycle $tokWh "MATERIAL_ISSUED"

Write-Host "Step 2: feedback execute"
$tokW = Login "worker" "123456"
$hW = AuthHeaders $tokW
$disp = Invoke-RestMethod -Uri "$base/api/planning/dispatch-tasks" -Headers $hW
$dt = $disp.data | Where-Object { $_.workOrderNo -eq $GoldenWo } | Select-Object -First 1
$fbCode = "FB-E2E-" + [DateTimeOffset]::UtcNow.ToUnixTimeMilliseconds()
$fbBody = @{
    feedbackType = "SELF"
    feedbackCode = $fbCode
    workstationId = $dt.stationId
    workstationCode = $dt.stationCode
    workstationName = $dt.stationName
    workorderId = $dt.workOrderId
    workorderCode = $dt.workOrderNo
    processId = $dt.stepId
    processCode = $dt.stepCode
    processName = $dt.stepName
    taskId = $dt.dispatchId
    taskCode = $dt.dispatchNo
    itemId = $dt.productId
    itemCode = $dt.productCode
    itemName = $dt.productName
    quantityFeedback = 5
    quantityQualified = 5
    quantityUnquanlified = 0
    userName = "worker"
    nickName = "worker"
    feedbackChannel = "PC"
    status = "PREPARE"
} | ConvertTo-Json
$recordId = (Invoke-RestMethod -Uri "$base/api/mes/pro/feedback" -Method Post -Body $fbBody -Headers $hW).data
Invoke-RestMethod -Uri "$base/api/mes/pro/feedback/execute/$recordId" -Method Put -Headers $hW | Out-Null
Assert-Lifecycle $tokW "QC_PENDING"

Write-Host "Step 3: IPQC finish"
$tokQ = Login "quality" "123456"
$hQ = AuthHeaders $tokQ
$pending = Invoke-RestMethod -Uri "$base/api/mes/qc/pending/list?qcType=PQC" -Headers $hQ
$pqc = $pending.rows | Where-Object { $_.workOrderCode -eq $GoldenWo -and $_.sourceDocType -eq "FEEDBACK" } | Select-Object -First 1
$ipqcCode = "IPQC-E2E-" + [DateTimeOffset]::UtcNow.ToUnixTimeMilliseconds()
$createBody = @{
    ipqcCode = $ipqcCode
    ipqcName = "E2E IPQC"
    ipqcType = "PQC"
    templateId = 2
    sourceDocId = $pqc.sourceDocId
    sourceDocType = "FEEDBACK"
    sourceDocCode = $pqc.sourceDocCode
    workorderId = $pqc.workOrderId
    workorderCode = $pqc.workOrderCode
    workstationId = $pqc.workstationId
    itemId = $pqc.itemId
    itemCode = $pqc.itemCode
    itemName = $pqc.itemName
    quantityCheck = 5
    quantityQualified = 5
    quantityUnqualified = 0
    status = "PREPARE"
    inspector = "qc01"
} | ConvertTo-Json -Depth 5
$ipqcId = (Invoke-RestMethod -Uri "$base/api/mes/qc/ipqc" -Method Post -Body $createBody -Headers $hQ).data
$finishBody = @{
    ipqcId = $ipqcId
    ipqcCode = $ipqcCode
    ipqcName = "E2E IPQC"
    ipqcType = "PQC"
    templateId = 2
    sourceDocId = $pqc.sourceDocId
    sourceDocType = "FEEDBACK"
    sourceDocCode = $pqc.sourceDocCode
    workorderId = $pqc.workOrderId
    workorderCode = $pqc.workOrderCode
    workstationId = $pqc.workstationId
    itemId = $pqc.itemId
    itemCode = $pqc.itemCode
    itemName = $pqc.itemName
    quantityCheck = 5
    quantityQualified = 5
    quantityUnqualified = 0
    checkResult = "ACCEPT"
    status = "FINISHED"
    inspector = "qc01"
} | ConvertTo-Json -Depth 5
Invoke-RestMethod -Uri "$base/api/mes/qc/ipqc" -Method Put -Body $finishBody -Headers $hQ | Out-Null
Assert-Lifecycle $tokQ "QC_PASSED"

Write-Host "Step 4: product recpt execute"
$recpts = Invoke-RestMethod -Uri "$base/api/mes/wm/productrecpt/list?pageNum=1&pageSize=10&recptCode=PR-GP-001" -Headers $hWh
$recptId = $recpts.rows[0].recptId
Invoke-RestMethod -Uri "$base/api/mes/wm/productrecpt/$recptId" -Method Put -Headers $hWh | Out-Null
Assert-Lifecycle $tokWh "COMPLETED"

Write-Host "Step 5: pipeline + trace links"
Assert-PipelineLinks $tokWh
Assert-ReportAndConsume $tokWh $dt.workOrderId

Write-Host "Golden path E2E PASSED"
