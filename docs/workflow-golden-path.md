# MES 黄金演示链（Golden Path）

> 唯一主线：Yunshu compat 层（`wm_*` / `qc_*` / `pro_feedback`），同一 `work_order_id` 串起 6 角色操作。

## 10 步流程矩阵

| 步骤 | 业务 | 角色 | 页面路由 | API | 主表 | 前置 lifecycle | 后置 lifecycle |
|------|------|------|----------|-----|------|----------------|----------------|
| 1 | 确认订单 | 生产主管 | `/app/planning/orders` | `GET/POST /api/planning/orders` | `customer_order` | — | — |
| 2 | 下达工单 | 生产主管 | `/app/planning/work-orders` | `/api/mes/pro/workorder` | `work_order` | DRAFT | RELEASED |
| 3 | 齐套预留 | 生产主管 | 工单/kitting | `POST /api/planning/kitting/{id}/reserve` | `kitting_analysis` | RELEASED | KITTING_OK |
| 4 | 甘特排产 | 生产主管 | `/app/planning/scheduling` | `POST /api/mes/pro/protask` | `production_task` | KITTING_OK | SCHEDULED |
| 5 | 工序派工 | 生产主管/系统 | 同上（自动） | `DispatchSyncService` | `dispatch_task` | SCHEDULED | SCHEDULED |
| 6 | 生产领料 | 仓库员 | `/app/inventory/outbound` | `PUT /api/mes/wm/issueheader/{id}` execute | `wm_issue_header` | RELEASED.. | MATERIAL_ISSUED |
| 7 | 现场报工 | 操作工 | `/app/my-work` | `pro_feedback` execute | `pro_feedback` | MATERIAL_ISSUED | QC_PENDING |
| 8 | 过程质检 | 质检员 | `/app/quality/workbench` | `PUT /api/mes/qc/ipqc` FINISHED | `qc_ipqc` | QC_PENDING | QC_PASSED |
| 9 | 成品入库 | 仓库员 | `/app/inventory/inbound` | `WmProductRecptService.execute` | `wm_product_recpt` | QC_PASSED | COMPLETED |
| 10 | 追溯 | 全员 | `/app/analytics/trace` | trace API | binding/SN/batch | COMPLETED | — |

## 关联键（验收 SQL）

```sql
-- 黄金工单 WO-GP-20260714 全链查询
SET @wo := (SELECT work_order_id FROM work_order WHERE work_order_no = 'WO-GP-20260714');

SELECT lifecycle_status, status, plan_qty, completed_qty FROM work_order WHERE work_order_id = @wo;
SELECT task_id, task_no, task_qty FROM production_task WHERE work_order_id = @wo;
SELECT dispatch_id, dispatch_no, operator_id, planned_qty, completed_qty FROM dispatch_task WHERE work_order_id = @wo;
SELECT issue_id, issue_code, status FROM wm_issue_header WHERE workorder_id = @wo;
SELECT record_id, feedback_code, status FROM pro_feedback WHERE workorder_id = @wo;
SELECT ipqc_id, ipqc_code, check_result, status FROM qc_ipqc WHERE workorder_id = @wo;
SELECT recpt_id, recpt_code, status FROM wm_product_recpt WHERE workorder_id = @wo;
```

## 配置项

| 配置 | 默认 | 说明 |
|------|------|------|
| `mes.workflow.enforce` | `false` | 为 true 时后端拦截越权报工等 |
| `mes.workflow.auto-dispatch` | `true` | 甘特保存 production_task 时自动生成 dispatch_task |

环境变量：`MES_WORKFLOW_ENFORCE`、`MES_WORKFLOW_AUTO_DISPATCH`

## 验收脚本

```powershell
# 冒烟（只读检查）
.\scripts\workflow-golden-path-test.ps1

# 端到端打通（领料→报工→质检→入库，同一 WO-GP-20260714）
.\scripts\workflow-golden-path-e2e.ps1 -ResetSeed
```

通过标准：`RELEASED → MATERIAL_ISSUED → QC_PENDING → QC_PASSED → COMPLETED`

1. `supervisor` / `123456` — 订单→工单→排产
2. `warehouse` / `123456` — 领料 execute
3. `worker` / `123456` — 我的工位→报工 execute
4. `quality` / `123456` — 质检工作台 IPQC finish ACCEPT
5. `warehouse` / `123456` — 成品入库 execute
6. 任意 — 追溯中心查 `WO-GP-20260714`

## 种子数据

独立故事脚本：`backend/mes-server/src/main/resources/db/seed/R__seed_story_10_golden_path.sql`  
不修改 story_01–09 已有行；黄金 WO 编号前缀 `WO-GP-*`。

## 断点桥接状态（P0 + 全链）

| 桥接 | 状态 | 实现位置 |
|------|------|----------|
| C2 甘特→派工 | ✅ | `DispatchSyncService` + `ProTaskService` |
| C5 质检→入库 | ✅ | `WorkOrderLifecycleService.validateProductRecpt` |
| B lifecycle 写回 | ✅ | issue/feedback/ipqc/recpt/kitting |
| C1 报工→production_report | ✅ | `ProductionReportSyncService` on feedback execute |
| C3 订单→工单 FK | ✅ | `WorkorderService.create` + `orderMatchesProduct` |
| C4 领料→wm_item_consume | ✅ | `IssueConsumeSyncService` on issue execute |
| C6 追溯 binding/SN | ✅ | story_10 SN+binding + `/api/traceability/work-order/{no}` |
| D 角色待办 + 上下文条 | ✅ | `/api/planning/workflow/todos` + `WorkflowContextBar.vue` |

## 流程 API

| 端点 | 说明 |
|------|------|
| `GET /api/planning/workflow/todos?role=` | 按角色返回待办工单（JWT 角色自动推断） |
| `GET /api/planning/workflow/pipeline/{woCode}` | 单工单全链摘要（任务/派工/领料/报工/质检/入库/消耗/SN） |
| `GET /api/traceability/work-order/{woCode}` | 追溯视角全链查询 |
