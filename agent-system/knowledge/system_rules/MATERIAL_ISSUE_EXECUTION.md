# 生产领料执行与物料消耗同步规则

> 权威来源：`inventory/compat/controller/WmIssueHeaderController.java`、`inventory/compat/service/WmIssueService.java`、`inventory/compat/service/StorageCoreService.java`、`inventory/workflow/IssueConsumeSyncService.java`、`planning/workflow/WorkOrderLifecycleService.java`  
> 数据库来源：`V8__wm_ktg_p0_documents.sql`；适用范围：黄金流程第 6 步（生产领料）；版本 `0.1`；生效日期 `2026-07-15`。

## 主键、单据与入口

领料单使用 `wm_issue_header.issue_id`，并通过 `workorder_id`（展示时可使用 `workorder_code`）关联 `work_order`。单据的明细层级是：

```text
work_order_id
  → wm_issue_header.issue_id
  → wm_issue_line.line_id
  → wm_issue_detail.detail_id（实际库存、库位、批次）
```

创建入口为 `POST /api/mes/wm/issueheader`，也可通过 `POST /api/mes/wm/issueheader/fromWorkOrder/{workorderId}` 按工单与 BOM 建立领料单及行。执行入口是 `PUT /api/mes/wm/issueheader/{issueId}`；数量校验入口是 `GET /api/mes/wm/issueheader/checkQuantity/{issueId}`。

## 领料前置门禁

创建或执行领料时，若工单已维护生命周期，只允许 `RELEASED`、`KITTING_OK` 或 `SCHEDULED`。按工单生成时会复用尚未完成或取消的既有领料单；新单据初始状态为 `PREPARE`，并按 `work_order_bom` 生成领料行。

执行前必须同时满足：领料单存在、单据状态为 `APPROVED`、工单生命周期允许领料、以及每一行的拣货数量与领料数量一致。只有草稿状态单据可删除。

## 执行后的真实写入

`StorageCoreService.processIssue(issueId)` 以 `wm_issue_detail` 为执行依据，选择并回写实际的 `wm_material_stock`、仓库、库位和批次信息，然后完成实际出库。领料单头状态更新为 `FINISHED`。

随后 `IssueConsumeSyncService` 以 `wm_issue_header` 和已执行的行/明细生成消耗凭证：`wm_item_consume → wm_item_consume_line → wm_item_consume_detail`。它使用 `wm_item_consume.attr1 = issue_id` 且状态为 `FINISHED` 做幂等判断，因此同一领料单不应被 Agent 重复记成新的消耗。

成功执行后，工单生命周期推进为 `MATERIAL_ISSUED`，下游可以进入现场报工。

## Agent 使用边界

齐套分析、可用库存和锁定库存都是实时事实，必须通过受控 MES 工具重新查询。Agent 不能仅依据本规则承诺“可领料”，也不能跳过 `APPROVED`、数量校验或后端库存处理直接改变工单状态。
