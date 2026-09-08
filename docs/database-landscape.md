# fan_mes 数据库梳理（2026-07-14，V29 删表后）

> 目标：说清「库里有什么」「主线走哪张表」「V29 删了哪些表」。

---

## 0. V29 清理结果（2026-07-14 已执行）

| 指标 | 删前 | 删后 |
|------|------|------|
| 表总数 | ~283 | **224** |
| 领料重复 | `material_issue` + `wm_issue` | 仅 `wm_issue_*` |
| 质检重复 | `quality_task` + `qc_ipqc` | 仅 `qc_*` |
| 设备重复 | `device` + `dv_machinery` | 仅 `dv_*` |
| 条码重复 | `barcode_*` + `wm_barcode_*` | 仅 `wm_barcode_*` |

**迁移文件：** `backend/mes-server/src/main/resources/db/migration/V29__drop_legacy_native_tables.sql`  
**备份：** `backup_fan_mes_before_v29.sql`（删表前 mysqldump）

### 已删除表（59 张）

**Stub / 无引用（18）：**  
`energy_meter`, `energy_reading`, `wechat_user_binding`, `mobile_operation_log`, `piece_wage_rule`, `piece_wage_record`, `sop_file`, `defect_reason`, `storage_location`, `dashboard_config`, `dashboard_kpi_snapshot`, `report_run_log`, `sys_permission`, `sys_role_permission`, `product_process_state`, `quality_record_item`, `ai_chat_session`, `ai_chat_message`

**Native 仓储/生产（8）：**  
`material_requisition`, `material_requisition_item`, `material_issue`, `material_issue_item`, `material_return`, `inventory_transaction`, `finished_inbound`, `production_completion`

**Native 质检（12）：**  
`quality_task`, `quality_record`, `quality_release`, `inspection_item_category`, `inspection_item`, `quality_standard`, `quality_standard_item`, `defect_record`, `rework_order`, `rework_record`, `scrap_record`

**Native 设备（14）：**  
`device_category`, `device_manufacturer`, `fault_cause`, `device`, `device_status_log`, `maintenance_plan`, `maintenance_task`, `device_inspection_item`, `device_inspection_record`, `repair_order`, `repair_record`, `device_count_config`, `equipment_count_record`, `device_oee_daily`

**Native 条码（9）：**  
`barcode_type`, `barcode_rule`, `barcode_rule_segment`, `barcode_template`, `barcode_template_param`, `barcode_application_rule`, `barcode_record`, `label_print_log`, `package_binding`

**代码同步退役：** Native Controller/Repository（inventory/production/quality/equipment/barcode）；Dashboard/Agent 改读 `qc_ipqc` / `qc_defect_record` / `pro_feedback` / `dv_machinery`。

---

## 1. 一张图：单库 + Compat 唯一主线

所有数据都在 **同一个 MySQL 库 `fan_mes`**（当前 **224 张表**）。  
Native V2 重复表已在 V29 删除；**黄金 10 步只走 Compat + 计划表**。

```
                    ┌─────────────────────────────────────┐
                    │         fan_mes（单库，224 表）       │
                    └─────────────────────────────────────┘
                                      │
          ┌───────────────────────────┼───────────────────────────┐
          ▼                           ▼                           ▼
   【计划 / 主数据】            【Compat 主线 ★】              【保留但非主线】
   customer_order              wm_issue_header                 agent_* (56+)
   work_order                  pro_feedback                    wm_product_sales*
   production_task             qc_ipqc                         md_* / pro_route*
   dispatch_task               wm_product_recpt                product/material/bom（桥接）
   product / material          wm_material_stock               andon_event（compat 同步）
                               pro_card
```

**黄金 10 步只认 Compat 主线 + 计划表**，关联键统一为 **`work_order.work_order_id`**（compat 表里列名多为 `workorder_id`，语义相同）。

| 步骤 | 应使用的表 |
|------|-----------|
| 1–2 订单/工单 | `customer_order`, `work_order` |
| 3–5 齐套/排产/派工 | `kitting_analysis`, `production_task`, `dispatch_task` |
| 6 领料 | `wm_issue_header` / `wm_issue_line` |
| 7 报工 | `pro_feedback` → 同步 `production_report` |
| 8 质检 | `qc_ipqc` |
| 9 成品入库 | `wm_product_recpt` |
| 10 追溯 | 上表 + `pro_card` + 可选 `product_sn` / `product_material_binding` |

---

## 2. 关联键速查（串链用）

| 表 | 工单外键列名 | 说明 |
|----|-------------|------|
| `production_task`, `dispatch_task`, `production_report`, `product_sn` | `work_order_id` | 下划线风格 |
| `wm_issue_header`, `pro_feedback`, `qc_ipqc`, `wm_product_recpt`, `pro_card`, `wm_item_consume` | `workorder_id` | compat 风格 |

**验收一条工单是否串起来：**

```sql
SET @wo := (SELECT work_order_id FROM work_order WHERE work_order_no = '你的工单号');

SELECT 'work_order' src, lifecycle_status val FROM work_order WHERE work_order_id = @wo
UNION ALL SELECT 'tasks', COUNT(*) FROM production_task WHERE work_order_id = @wo
UNION ALL SELECT 'dispatch', COUNT(*) FROM dispatch_task WHERE work_order_id = @wo
UNION ALL SELECT 'wm_issue', COUNT(*) FROM wm_issue_header WHERE workorder_id = @wo
UNION ALL SELECT 'feedback', COUNT(*) FROM pro_feedback WHERE workorder_id = @wo
UNION ALL SELECT 'ipqc', COUNT(*) FROM qc_ipqc WHERE workorder_id = @wo
UNION ALL SELECT 'recpt', COUNT(*) FROM wm_product_recpt WHERE workorder_id = @wo;
```

---

## 3. 当前数据实况（本机 fan_mes 快照）

### 3.1 表量级（主线相关，V29 后）

| 表 | 行数 | 角色 |
|----|------|------|
| `work_order` | 18 | 工单 hub |
| `wm_issue_header` | 9 | compat 领料 |
| `pro_feedback` | 28 | compat 报工 |
| `qc_ipqc` | 21 | compat 质检 |
| `wm_product_recpt` | 9 | compat 成品入库 |
| `wm_transaction` | 14 | compat 库存流水 |
| `pro_card` | 3 | 流转卡/追溯 |
| `product_sn` | 82 | SN（多来自种子，非主线必需） |

### 3.2 工单 lifecycle 分布

| lifecycle_status | 数量 | 说明 |
|------------------|------|------|
| `NULL` | 12 | **story_01–07 旧种子**，未跑 V28 状态机 |
| `COMPLETED` | 2 | `WO-GP-20260714`、`WO20260714185838` |
| 其他（KITTING_OK … QC_PASSED） | 4 | `WO-GP-A/B/C/D` 演示链 |

→ **乱点 1**：一半工单没有 `lifecycle_status`，和 compat 主线状态机无关，是历史演示数据。

### 3.3 推荐保留的「干净主线」样本

| 工单号 | 用途 | lifecycle |
|--------|------|-----------|
| `WO-GP-20260714` | 种子脚本标准黄金链 | COMPLETED |
| `WO-GP-A` ~ `WO-GP-D` | 分阶段演示（齐套→领料→质检→入库） | 各阶段 |
| `WO20260714185838` | 你手工跑通的 100 台完整链 | COMPLETED |

### 3.4 测试产生的「脏数据」特征

以 `WO20260714185838` 为例（功能已打通，但数据冗余）：

- 领料 2 张（1 张 `CANCELED` + 1 张 `FINISHED`）
- 报工 4 条（4 个工序任务各报一次）
- IPQC 5 条（含 PREPARE 草稿）
- 入库 2 张（`PR7595`、`PR79256` 均 FINISHED）
- `completed_qty` 仍为 0（汇总字段未回写）

→ **乱点 2**：重复操作留下的草稿/取消单，不影响追溯，但列表看起来多。

### 3.5 孤儿单据（无工单 FK）

| 类型 | 数量 | 示例 |
|------|------|------|
| `wm_issue_header` | 1 | `IS-DEMO-001`，workorder_id NULL |
| `wm_product_recpt` | 2 | `PR-DEMO-001/002`，workorder_id NULL |

→ **乱点 3**：早期 story/demo 单据，与任何工单无关。

---

## 4. 表从哪来（Flyway 分层）

| 批次 | 迁移前缀 | 内容 |
|------|---------|------|
| V1–V4 | 核心 + Agent | 原生 MES + AI 表 |
| V5–V6 | planning compat | 工单/任务 compat 字段 |
| V7–V10 | wm_compat | 仓储 compat 全套 `wm_*` |
| V11–V12 | md/pro | 主数据、工艺路线 |
| V13 | pro_feedback | 报工 + consume/produce |
| V15–V18 | qc_compat | 质检 compat |
| V28 | lifecycle | `work_order.lifecycle_status` |
| **V29** | **drop legacy** | **删除 59 张 Native/stub 表** |
| R__seed_story_* | 种子故事 | 01–11 分模块演示数据（已去除对已删表的 INSERT） |

种子脚本**可重复追加**（R__ 前缀），多次跑 E2E / 手工测试会让单据变多，这是预期行为。

---

## 5. 整理策略（三档，按需选）

### 档 A — 只梳理认知（零风险）

- 以后所有操作只走 [workflow-golden-path.md](./workflow-golden-path.md) 里的 compat 表
- 用 `scripts/db-health-check.sql` 定期看工单链是否完整
- 追溯中心用 **工单追溯** Tab，查 `WO-GP-*` 或你的完工工单

### 档 B — 轻量清理（可逆，删 demo 孤儿）

1. 删除 `workorder_id IS NULL` 的 demo 领料/入库（`IS-DEMO-*`, `PR-DEMO-*`）
2. 标记或软删 `status=CANCELED` 的测试领料单
3. **不要动** `WO-GP-*` 和 `WO20260714185838`  unless 你要重跑 E2E

### 档 C — 重置为干净黄金环境

```powershell
# 重跑种子 + E2E（会按脚本重建 WO-GP-20260714 全链）
.\scripts\workflow-golden-path-e2e.ps1 -ResetSeed
```

适合：演示前、验收前、数据太乱想从零开始。

**不建议**：直接 `DROP DATABASE fan_mes`（224 表 + 权限/menu 种子都要重建）。

---

## 6. 后续收敛建议（工程侧）

| 优先级 | 项 | 说明 |
|--------|-----|------|
| P1 | 统一 FK 列名 | 长期可考虑 migration 把 `workorder_id` 统一为 `work_order_id`（工作量大，非必须） |
| P1 | 旧工单补 lifecycle | 对 story_01–07 的 12 条 NULL 工单批量设 `RELEASED` 或标记 `is_demo=1` |
| P2 | 回写 `completed_qty` | 入库 execute 时汇总工单完工数 |
| P3 | 合并重复入库/领料 | 业务规则：同一 WO 只允许一张有效 FINISHED 入库单 |

---

## 7. 相关文档

| 文档 | 内容 |
|------|------|
| [workflow-golden-path.md](./workflow-golden-path.md) | 10 步主线表/API |
| [data-gaps.md](./data-gaps.md) | compat vs native 实现状态 |
| [core-database-design.md](./core-database-design.md) | 45 张核心表设计 |
| [seed-data.md](./seed-data.md) | 种子脚本说明 |
