# 云枢智造 MES — 黄金演示链（页面操作指南）

> 本文说明 **10 步生产主线** 在系统中的**具体页面**如何完成。  
> 技术细节（API、表结构、lifecycle 写回）见 [`docs/workflow-golden-path.md`](docs/workflow-golden-path.md)。

**脊柱**：所有步骤共用同一个 `work_order_id`（工单 ID）。  
**演示工单**：`WO-GP-20260714`（订单 `CO-GP-20260714`，产品 FS40-A × 100 台）  
**本地前端**：http://localhost:5173（登录后路径以 `/app/...` 开头）

---

## 一、10 步总览

| 步骤 | 业务 | 角色 | 侧栏入口 | 页面路由 | 关键操作 |
|------|------|------|----------|----------|----------|
| 1 | 确认订单 | 生产主管 | 计划调度 → **订单中心** | `/app/planning/orders` | 新建/确认客户订单 |
| 2 | 下达工单 | 生产主管 | 计划调度 → **工单中心** | `/app/planning/work-orders` | 从订单创建生产工单 |
| 3 | 齐套预留 | 生产主管 | 计划调度 → **工单中心**（齐套 API） | `/app/planning/work-orders` | 分析齐套 → 锁定库存 |
| 4 | 甘特排产 | 生产主管 | 计划调度 → **排产派工** | `/app/planning/scheduling` | 甘特编辑排产任务 |
| 5 | 工序派工 | 系统/主管 | 计划调度 → **排产派工** | `/app/planning/scheduling` | 保存排产后自动生成派工 |
| 6 | 生产领料 | 仓库员 | 仓储物流 → **出库作业** | `/app/inventory/outbound?tab=issue` | 领料单 → 执行领出 |
| 7 | 现场报工 | 操作工 | 生产执行 → **我的工位** | `/app/my-work` | 选派工 → 生产报工 |
| 8 | 过程质检 | 质检员 | 质量管理 → **检验工作台** | `/app/quality/workbench` | 过程检 PQC → 判定 ACCEPT |
| 9 | 成品入库 | 仓库员 | 仓储物流 → **入库作业** | `/app/inventory/inbound?tab=product-recpt` | 成品入库单 → 执行入库 |
| 10 | 追溯查询 | 全员 | 数据分析 → **追溯中心** | `/app/analytics/trace` | 产品 SN / 批次 / 工单全链 |

**lifecycle 状态变化（演示链验收标准）：**

```
RELEASED → MATERIAL_ISSUED → QC_PENDING → QC_PASSED → COMPLETED
```

（完整路径含齐套/排产：`RELEASED → KITTING_OK → SCHEDULED → … → COMPLETED`）

---

## 二、分步详解（页面 + 组件 + 操作）

### 步骤 1 · 确认订单

| 项 | 说明 |
|----|------|
| **登录账号** | `supervisor` / `123456`（或 `supervisor01`，角色：生产主管） |
| **侧栏** | **计划调度** → **订单中心** |
| **路由** | `/app/planning/orders` |
| **前端组件** | `frontend/web-admin/src/yunshu-ui/planning/orders/index.vue` |
| **做什么** | 创建客户订单，维护订单行（产品、数量、交期），确认状态为可生产 |
| **落库表** | `customer_order`、`customer_order_item` |
| **演示数据** | 订单号 `CO-GP-20260714` |

---

### 步骤 2 · 下达工单

| 项 | 说明 |
|----|------|
| **角色** | 生产主管 |
| **侧栏** | **计划调度** → **工单中心** |
| **路由** | `/app/planning/work-orders` |
| **前端组件** | `frontend/web-admin/src/yunshu-ui/pro/workorder/index.vue` |
| **做什么** | 新建生产工单，关联订单（`order_id`）、产品、BOM、工艺路线、计划数量 |
| **界面提示** | 页顶有 **「流程待办」** 和 **「工单流程上下文条」**（lifecycle + 下一步跳转） |
| **落库表** | `work_order`（创建后 `lifecycle_status = RELEASED`） |
| **演示数据** | 工单号 `WO-GP-20260714` |

---

### 步骤 3 · 齐套预留

| 项 | 说明 |
|----|------|
| **角色** | 生产主管 |
| **侧栏** | **计划调度** → **工单中心**（路由 `/app/planning/kitting` 会重定向到工单中心） |
| **路由** | `/app/planning/work-orders` |
| **做什么** | 对工单做 BOM 齐套分析，确认物料够则 **锁定库存（reserve）** |
| **后端 API** | `GET /api/planning/kitting/{workOrderId}` · `POST .../reserve` |
| **UI 说明** | 齐套后端已打通；可复用组件 `frontend/web-admin/src/components/planning/WorkOrderKittingPanel.vue`（分析 / 锁定 / 释放）。当前侧栏无独立「齐套」页，演示数据种子中 `WO-GP-A` 已处于 `KITTING_OK` 快照 |
| **落库表** | `kitting_analysis`；lifecycle → **`KITTING_OK`** |

---

### 步骤 4 · 甘特排产

| 项 | 说明 |
|----|------|
| **角色** | 生产主管 |
| **侧栏** | **计划调度** → **排产派工** |
| **路由** | `/app/planning/scheduling` |
| **甘特编辑** | `/app/planning/scheduling/ganttedit`（排产页点编辑按钮进入） |
| **前端组件** | `yunshu-ui/pro/schedule/index.vue` · `ganttedit.vue` · `ganttx.vue` |
| **做什么** | 为工单创建/调整 `production_task`：产线、工位、工序、数量、时间 |
| **落库表** | `production_task` |
| **演示数据** | 任务号 `PT-GP-001` |
| **lifecycle** | 可推进至 **`SCHEDULED`** |

**辅助页面**：**计划调度 → 生产进度**（`/app/planning/progress`）可树形查看工单下任务进度，不负责排产编辑。

---

### 步骤 5 · 工序派工

| 项 | 说明 |
|----|------|
| **角色** | **系统自动**（默认 `mes.workflow.auto-dispatch=true`）；主管在排产页保存任务时触发 |
| **页面** | 仍在 **排产派工** `/app/planning/scheduling`（无需单独菜单） |
| **做什么** | 排产保存 → 自动生成 `dispatch_task`，指定工位、操作工、计划数量 |
| **后端** | `DispatchSyncService` + `ProTaskService` |
| **落库表** | `dispatch_task` |
| **演示数据** | 派工单 `DT-GP-001` → 工位 ST-01、操作工 worker01 |

操作工在 **我的工位** 看到的待办列表，即来自这里的派工数据。

---

### 步骤 6 · 生产领料（出库）

| 项 | 说明 |
|----|------|
| **登录账号** | `warehouse` / `123456`（仓库员） |
| **侧栏** | **仓储物流** → **出库作业** |
| **路由** | `/app/inventory/outbound` → Tab **「生产领料」**（`?tab=issue`） |
| **等价路由** | `/app/inventory/issue`（自动重定向到上述 Tab） |
| **前端组件** | `yunshu-ui/pro/wm/outbound/index.vue` → `yunshu-ui/pro/wm/issue/index.vue` |
| **做什么** | ① 新建/打开领料单（关联工单）→ ② 维护领料行 → ③ 执行拣货 → ④ **执行领出** |
| **关键按钮** | 「执行拣货」「提交执行」「**执行领出**」 |
| **落库表** | `wm_issue_header` / `wm_issue_line` / `wm_issue_detail`；同步 `wm_item_consume` |
| **lifecycle** | → **`MATERIAL_ISSUED`** |
| **演示数据** | 领料单 `IS-GP-001` |

页顶 **工单流程上下文条** 可通过 URL 参数 `?wo=WO-GP-20260714` 聚焦演示工单。

---

### 步骤 7 · 现场报工

| 项 | 说明 |
|----|------|
| **登录账号** | `worker` / `123456`（产线操作工） |
| **侧栏** | **生产执行** → **我的工位** |
| **路由** | `/app/my-work` |
| **前端组件** | `yunshu-ui/pro/operator/index.vue` |
| **子组件** | 左侧 `TaskQueueAside`（派工队列）· 中间 `TaskWorkbench` · 报工抽屉 `ReportDrawer.vue` |
| **做什么** | ① 左侧选派工任务 → ② 点「开工」（可选）→ ③ 点「**生产报工**」→ 填合格数 → **提交报工**（自动 execute） |
| **可选** | 「物料绑定」抽屉 `BindMaterialDrawer.vue`：扫 SN + 批次，写入追溯绑定 |
| **落库表** | `pro_feedback`；同步 `production_report`、`dispatch_task.completed_qty` |
| **lifecycle** | → **`QC_PENDING`** |
| **页顶** | 选中任务后显示该工单的 lifecycle 上下文条 |

---

### 步骤 8 · 过程质检（IPQC / PQC）

| 项 | 说明 |
|----|------|
| **登录账号** | `quality` / `123456`（质检员） |
| **侧栏** | **质量管理** → **检验工作台** |
| **路由** | `/app/quality/workbench` |
| **过程检子页** | `/app/quality/workbench/exec/pqc`（从待检列表点「检验」进入） |
| **前端组件** | `yunshu-ui/qc/workbench/index.vue` · `yunshu-ui/qc/pendinginspect/pqc.vue` |
| **做什么** | 左侧待检列表选 **来源为报工（FEEDBACK）** 的 PQC → 填写检验数量 → 结果选 **ACCEPT** → 完成 |
| **落库表** | `qc_ipqc` |
| **lifecycle** | → **`QC_PASSED`**（不合格则为 `QC_FAILED`） |

**相关页（查记录，非主线操作）**：**质量分析中心** `/app/quality/analytics` 可查看 IQC/IPQC/OQC 历史记录。

---

### 步骤 9 · 成品入库

| 项 | 说明 |
|----|------|
| **角色** | 仓库员 |
| **侧栏** | **仓储物流** → **入库作业** |
| **路由** | `/app/inventory/inbound` → Tab **「产品入库」**（`?tab=product-recpt`） |
| **等价路由** | `/app/inventory/product-recpt` |
| **前端组件** | `yunshu-ui/pro/wm/inbound/index.vue` → `yunshu-ui/pro/wm/productrecpt/index.vue` |
| **做什么** | ① 打开成品入库单（关联工单）→ ② 维护入库行/上架明细 → ③ **执行入库** |
| **关键按钮** | 「执行上架」「提交执行」「**执行入库**」 |
| **门禁** | 工单须已 **`QC_PASSED`**，否则后端拒绝入库 |
| **落库表** | `wm_product_recpt` / line / detail |
| **lifecycle** | → **`COMPLETED`** |
| **演示数据** | 入库单 `PR-GP-001` |

---

### 步骤 10 · 追溯查询

| 项 | 说明 |
|----|------|
| **角色** | 全员（主管 / 质检 / 仓库 / 操作工 / 管理员均可） |
| **侧栏** | **数据分析** → **追溯中心** |
| **路由** | `/app/analytics/trace` |
| **Tab** | **产品追溯**（`?tab=product`）· **批次追溯**（`?tab=batch`） |
| **前端组件** | `yunshu-ui/analytics/hub/trace/index.vue` → `procard/index.vue` · `qc/batchtrace/index.vue` |
| **查什么** | 产品 SN、物料批次、工单全链（领料 / 报工 / 质检 / 入库 / 绑定） |
| **演示 SN** | `SN-GP-20260714-001`（绑定电机批次 `BATCH-MOTOR-202607`） |
| **工单全链 API** | `GET /api/traceability/work-order/WO-GP-20260714` |
| **落库表** | `product_sn` · `product_material_binding` · `inventory_batch` 等 |

---

## 三、角色与侧栏对照

| 角色 | 测试账号 | 密码 | 本流程涉及的侧栏板块 |
|------|----------|------|----------------------|
| 生产主管 | `supervisor` | `123456` | 计划调度（订单 / 工单 / 排产 / 进度） |
| 仓库员 | `warehouse` | `123456` | 仓储物流（出库 / 入库） |
| 操作工 | `worker` | `123456` | 生产执行（我的工位） |
| 质检员 | `quality` | `123456` | 质量管理（检验工作台） |
| 管理员 | `admin` | `admin123` | 以上均可访问 |
| 全能测试 | `tester` | `123456` | 以上均可访问 |

侧栏配置来源：`frontend/web-admin/src/constants/nav-modules.js`

---

## 四、按角色走一遍（推荐演示顺序）

### 生产主管（步骤 1–5）

1. **订单中心** — 确认 `CO-GP-20260714`
2. **工单中心** — 查看/创建 `WO-GP-20260714`，页顶「流程待办」点选工单
3. **工单中心** — 齐套分析并 reserve（或跳过，演示种子已备）
4. **排产派工** — 打开甘特，确认 `PT-GP-001` 任务
5. 保存排产 → 系统自动生成 `DT-GP-001` 派工

### 仓库员（步骤 6）

6. **出库作业 → 生产领料** — 打开 `IS-GP-001` → **执行领出**

### 操作工（步骤 7）

7. **我的工位** — 选 `DT-GP-001` → **生产报工** → 提交（可选：物料绑定 SN）

### 质检员（步骤 8）

8. **检验工作台** — 待检 PQC（来源报工）→ **ACCEPT** 完成

### 仓库员（步骤 9）

9. **入库作业 → 产品入库** — 打开 `PR-GP-001` → **执行入库**

### 任意角色（步骤 10）

10. **追溯中心** — 查 `SN-GP-20260714-001` 或批次 `BATCH-MOTOR-202607`

---

## 五、自动化验收

无需手工点页面，可用脚本验证 API 全链：

```powershell
# 端到端（领料 → 报工 → 质检 → 入库 + pipeline/追溯断言）
.\scripts\workflow-golden-path-e2e.ps1 -ResetSeed

# 只读冒烟
.\scripts\workflow-golden-path-test.ps1
```

---

## 六、页面路由速查表

| 路由 | 菜单名称 | Vue 组件路径 |
|------|----------|--------------|
| `/app/planning/orders` | 订单中心 | `yunshu-ui/planning/orders/index.vue` |
| `/app/planning/work-orders` | 工单中心 | `yunshu-ui/pro/workorder/index.vue` |
| `/app/planning/scheduling` | 排产派工 | `yunshu-ui/pro/schedule/index.vue` |
| `/app/planning/scheduling/ganttedit` | 甘特排产 | `yunshu-ui/pro/schedule/ganttedit.vue` |
| `/app/planning/progress` | 生产进度 | `yunshu-ui/pro/schedule/progress.vue` |
| `/app/inventory/outbound?tab=issue` | 出库 → 生产领料 | `yunshu-ui/pro/wm/issue/index.vue` |
| `/app/my-work` | 我的工位 | `yunshu-ui/pro/operator/index.vue` |
| `/app/quality/workbench` | 检验工作台 | `yunshu-ui/qc/workbench/index.vue` |
| `/app/inventory/inbound?tab=product-recpt` | 入库 → 产品入库 | `yunshu-ui/pro/wm/productrecpt/index.vue` |
| `/app/analytics/trace` | 追溯中心 | `yunshu-ui/analytics/hub/trace/index.vue` |

---

## 七、相关文档

| 文档 | 内容 |
|------|------|
| [`docs/workflow-golden-path.md`](docs/workflow-golden-path.md) | API 矩阵、lifecycle、桥接实现、SQL 验收 |
| [`AGENTS.md`](AGENTS.md) | 项目架构、账号、启动方式 |
| [`docs/seed-data.md`](docs/seed-data.md) | 种子数据说明 |

---

*最后更新：2026-07-14 · 对应黄金演示链 story_10（`R__seed_story_10_golden_path.sql`）*
