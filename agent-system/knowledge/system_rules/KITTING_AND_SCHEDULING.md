# 齐套预留、排产与派工规则

> 权威来源：`backend/mes-server/src/main/java/com/yunshu/mes/planning/controller/KittingController.java`、`backend/mes-server/src/main/java/com/yunshu/mes/planning/compat/service/DispatchSyncService.java`  
> 业务来源：`docs/workflow-golden-path.md`；版本 0.1；生效日期 2026-07-15。

## 齐套分析

对工单执行 `GET /api/planning/kitting/{workOrderId}`，以 `work_order_id` 为主键汇总 BOM 需求、库存与缺料明细。齐套分析回答的是“理论需求是否有可用物料”，不能替代实时库存、批次或锁定量查询。

## 库存预留

生产主管在齐套分析后可执行 reserve 锁定库存，成功后工单进入 `KITTING_OK`；release 会释放对应预留。预留和释放会影响库存可用性，属于需要审批和后端幂等保护的账实操作。

## 齐套不足时的处理

齐套不足时，Agent 应列出缺料物料与缺口，建议仓库/供应链处理并生成“待人工决策”的排产建议。不得把缺料工单描述为已经具备可执行物料条件，也不得绕过库存预留直接承诺按期完成。

## 排产到派工

甘特排产创建或调整 `production_task`，记录产线、工位、工序、数量和时间。若 `mes.workflow.auto-dispatch=true`，任务保存会经 `DispatchSyncService` 自动生成或补齐 `dispatch_task`，并推进工单至 `SCHEDULED`。

Agent 必须在排产后读取真实派工结果：自动派工未生效或人员/工位信息缺失时，只能给出缺口与处理建议，不能假设派工已经完成。
