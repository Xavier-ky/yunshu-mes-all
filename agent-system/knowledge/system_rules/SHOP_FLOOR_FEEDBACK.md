# 现场报工、任务进度与生产报告同步规则

> 权威来源：`production/compat/controller/ProFeedbackController.java`、`production/compat/service/ProFeedbackService.java`、`production/workflow/ProductionReportSyncService.java`、`planning/repository/DispatchTaskRepository.java`、`planning/workflow/WorkOrderLifecycleService.java`  
> 数据库来源：`V13__pro_feedback_wm_consume_produce.sql`；适用范围：黄金流程第 7 步（现场报工）；版本 `0.1`；生效日期 `2026-07-15`。

## 主键与调用入口

报工单存储在 `pro_feedback`，主键为 `record_id`，通过 `workorder_id` 关联工单；页面可传 `taskId` 关联现场任务。接口为：

- `POST /api/mes/pro/feedback`：创建报工，默认状态 `PREPARE`。
- `PUT /api/mes/pro/feedback/execute/{recordId}`：执行报工。
- `GET /api/mes/pro/feedback/list`：查询真实报工记录。

报工的 `taskId` 不是固定只表示一种表主键：后端先将其按 `dispatch_task.dispatch_id` 解析，找不到时才按 `dispatch_task.task_id`（即 `production_task.task_id`）解析。Agent 必须以实际查询结果确认关联关系。

## 执行门禁与进度回写

若工单已维护生命周期，只有 `MATERIAL_ISSUED`、`IN_PROGRESS` 或 `QC_PENDING` 可以执行报工。执行时，合格数量会回写已解析的 `dispatch_task.completed_qty`；同时按实际关联更新 `production_task.completed_qty` 并把排产任务置为 `RUNNING`。

工单随后依次推进到 `IN_PROGRESS` 与 `QC_PENDING`。报工单更新为 `FINISHED`，并将 `quantity_uncheck` 记录为待检数量；这意味着后续 IPQC 仍是完成制造闭环的必要步骤。

## 生产报告同步

执行报工后，`ProductionReportSyncService` 会尝试创建 `production_report` 和一条 `production_report_detail`。明细以 `item_type='PRO_FEEDBACK'`、`item_value=record_id` 标记来源，避免同一报工重复同步。

该同步是 fail-safe：同步异常不会回滚已成功执行的报工。因此“报工已完成”不等同于“生产报告一定已生成”；Agent 要回答具体单据时，必须读取真实 `production_report`，不能从规则推断记录必然存在。

## 下游连接

`QC_PENDING` 表示应进入过程质检。质量人员完成 IPQC 后，才会把该工单推进为 `QC_PASSED` 或 `QC_FAILED`；未通过时不得把报工数量直接解释为可成品入库数量。
