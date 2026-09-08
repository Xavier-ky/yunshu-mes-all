# 工单、批次、SN 与全过程追溯规则

> 权威来源：`traceability/controller/ReverseTraceController.java`、`traceability/service/WorkOrderTraceSyncService.java`、`inventory/compat/service/WmProductRecptService.java`  
> 适用范围：黄金流程第 10 步（正向与反向追溯）；版本 `0.1`；生效日期 `2026-07-15`。

## 追溯主键与查询入口

全流程追溯以 `work_order.work_order_id` 为主键，`work_order_no` 仅用作用户输入和展示。工单追溯入口为：

```text
GET /api/traceability/work-order/{workOrderNo}
```

该接口先解析工单，再调用 `WorkOrderTraceSyncService.syncFromWorkOrder(workOrderId)` 补齐已完成入库后的派生追溯数据，随后返回真实单据集合。批次反查入口为 `GET /api/traceability/batch/{batchNo}`；物料影响范围入口为 `GET /api/traceability/material/{materialId}`。

## 工单追溯实际串联的数据库

工单追溯结果由下列真实关系组成：

```text
work_order
  → wm_issue_header → wm_item_consume
  → pro_feedback → production_report
  → qc_ipqc
  → wm_product_recpt → wm_product_recpt_line
  → pro_card / wm_product_produce / inventory_batch
  → product_sn → product_material_binding → inventory_batch / material
```

工单接口会返回领料单、报工单、生产报告、IPQC、成品入库单、流转卡、物料消耗、产品 SN 和物料绑定。物料绑定通过 `product_material_binding.sn_id → product_sn.work_order_id` 与 `inventory_batch.batch_id` 关联，因此可以回答“某批物料影响了哪些 SN”，前提是系统中已经存在真实 SN 与绑定记录。

## 结论边界

追溯接口是读取实时数据库的唯一依据。RAG 可以说明应查哪些单据和关联路径，但不能从流程规则推断某工单已产生 SN、已使用某批次或已完成入库。若查询结果为空，Agent 应明确说“当前系统未查到记录”，而不是用种子数据或示例数据补全答案。

## 下游 Agent 使用

质量异常、客户投诉、批次隔离或完工核对都应先调用工单/批次追溯工具，再用本规则解释关系。涉及批次冻结、返工、报废、库存调整或单据写入时，必须进入后续受审批的工具流程，不能由检索结果直接执行。
