# 工单生命周期与业务门禁规则

> 权威来源：`backend/mes-server/src/main/java/com/yunshu/mes/planning/workflow/WorkOrderLifecycleService.java`  
> 数据库来源：`backend/mes-server/src/main/resources/db/migration/V28__work_order_lifecycle.sql`  
> 适用范围：云枢智造 MES 十步黄金流程；版本 0.1；生效日期 2026-07-15。

## 生命周期主线

```text
DRAFT → RELEASED → KITTING_OK → SCHEDULED → MATERIAL_ISSUED
  → IN_PROGRESS → QC_PENDING → QC_PASSED → COMPLETED
                              └→ QC_FAILED
```

所有步骤以 `work_order_id` 关联。`work_order_no` 仅供用户输入、展示和追溯查询使用。

## 领料门禁

生产领料仅允许工单处于 `RELEASED`、`KITTING_OK` 或 `SCHEDULED`。领料执行由 `WmIssueService` 校验数量并同步 `wm_item_consume`，成功后推进为 `MATERIAL_ISSUED`。

## 报工门禁

现场报工仅允许工单处于 `MATERIAL_ISSUED`、`IN_PROGRESS` 或 `QC_PENDING`。报工执行会回写派工完成量、同步 `production_report`，并推进到 `QC_PENDING`。

## 质检与入库门禁

IPQC 判定 `ACCEPT` 后，工单进入 `QC_PASSED`；其他判定进入 `QC_FAILED`。成品入库只允许 `QC_PASSED` 或已经 `COMPLETED` 的工单执行。`QC_FAILED` 不能直接入库，必须由生产主管/质量人员处理异常、返工或复检后，再由后端生命周期校验决定是否允许进入下一步。

## 角色待办

- 仓库员：处理领料，以及 `QC_PASSED` 后的成品入库。
- 操作工：处理 `MATERIAL_ISSUED` / `IN_PROGRESS` 后的现场报工。
- 质检员：处理 `QC_PENDING` 的过程质检。
- 生产主管或管理人员：处理早期计划阶段与 `QC_FAILED` 异常。

## Agent 使用规则

Agent 必须通过流程快照和业务 API 读取当前状态，并以后端校验结果为最终准则。对于存量 `lifecycle_status` 为 `null` 的历史工单，后端兼容策略可能不拦截；Agent 必须明确标记“生命周期未建档”，不能把它视为已验证的正常路径。
