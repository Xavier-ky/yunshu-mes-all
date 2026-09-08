# 成品入库、完工与追溯同步规则

> 权威来源：`inventory/compat/controller/WmProductRecptController.java`、`inventory/compat/service/WmProductRecptService.java`、`inventory/compat/service/StorageCoreService.java`、`traceability/service/WorkOrderTraceSyncService.java`、`planning/workflow/WorkOrderLifecycleService.java`  
> 数据库来源：`V9__wm_ktg_p0_sales.sql`；适用范围：黄金流程第 9 步（成品入库）；版本 `0.1`；生效日期 `2026-07-15`。

## 单据主链与入口

成品入库使用三层单据：

```text
work_order_id
  → wm_product_recpt.recpt_id
  → wm_product_recpt_line.line_id
  → wm_product_recpt_detail.detail_id（成品仓、库位、批次、数量）
```

入口包括 `POST /api/mes/wm/productrecpt/fromWorkOrder/{workorderId}`（按工单创建/复用入库单）、`PUT /api/mes/wm/productrecpt/{recptId}`（执行入库），以及 `GET /api/mes/wm/productrecpt/checkQuantity/{recptId}`（数量校验）。新建单据默认 `PREPARE`；草稿外的单据不可删除。

## 创建、上架与执行门禁

按工单创建时，后端先调用 `validateProductRecpt(workOrderId)`；已经维护生命周期的工单必须处于 `QC_PASSED` 或 `COMPLETED`。服务会复用未完成/未取消的既有入库单；若入库行存在，则补齐上架明细。

执行前必须存在入库行与入库明细，入库行的 `quantity_recived` 必须与其明细数量之和一致，且工单必须通过质量门禁。`StorageCoreService.processProductRecpt(recptId)` 负责按明细完成实际成品入库；只有之后单头状态才更新为 `FINISHED`。

## 完工与追溯写入

入库执行成功后，工单推进到 `COMPLETED`，并调用 `WorkOrderTraceSyncService.syncFromWorkOrder(workOrderId)`。同步服务从已完成的入库行读取产品和批次，补齐或更新：

- `pro_card` 与 `pro_card_process`：工单流转卡和已完成派工工序；
- `wm_product_produce` 与 `wm_product_produce_line`：产出凭证；
- `inventory_batch`：成品批次（若该批次尚不存在）。

若没有已完成的入库行，追溯同步会直接返回。因此 Agent 不能因为工单显示完成就断言某个成品批次、流转卡或 SN 必然已经存在，必须读取追溯接口的真实结果。

## Agent 使用边界

成品库存、上架库位、实际入库数量和批次均是实时事实。RAG 只用于解释门禁和流程；任何“可以入库”或“已经入库”的结论都应由受 JWT 保护的 MES 工具确认。
