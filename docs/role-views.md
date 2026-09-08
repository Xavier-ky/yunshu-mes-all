# 云枢智造 MES — 角色视图分配方案

## 角色定义（6 类）

| 角色编码 | 角色名称 | 演示账号 | 部门 |
|---------|---------|---------|------|
| MANAGER | 管理人员 | admin | 管理部 |
| PROD_SUPERVISOR | 生产主管 | supervisor01 | 生产部 |
| WAREHOUSE_CLERK | 仓库物料员 | warehouse01 | 仓储部 |
| LINE_OPERATOR | 产线操作工人 | worker01 | 生产部 |
| QUALITY_INSPECTOR | 质检员 | qc01 | 质量部 |
| EQUIPMENT_MAINTAINER | 设备维修员 | repair01 | 设备部 |

---

## 一、管理人员 (MANAGER)

**职责**：看板总览、报表分析、追溯查询、用户/角色/权限维护

| 视图 | 核心数据 | 页面类型 |
|------|---------|---------|
| 生产驾驶舱 | KPI 快照 + 工单进度 + 产线状态 + 安灯事件 | 仪表盘首页 |
| 产线实时监控 | `production_task` / `dispatch_task` 实时状态 | 可视化大屏 |
| 产量报表 | `production_report` 按日/周/月汇总 | 报表页 |
| 质量报表 | `quality_record` / `defect_record` 统计 | 报表页 |
| OEE 报表 | `device_oee_daily` 趋势图 | 报表页 |
| 产品追溯 | `product_sn` → `product_material_binding` → `inventory_batch` 全链路 | 追溯查询 |
| 安灯呼叫记录 | `pro_andon_record` 搜索/处置 + 安灯设置（yunshu-ui 1:1） | `/app/andon` |
| 安灯类型/原因/任务/分析 | `andon_type` / `andon_reason` / `andon_task` / 统计 | `/app/andon/types` 等 |
| 用户管理 | `sys_user` CRUD | 管理页 |
| 角色权限 | `sys_role` + `sys_permission` 矩阵 | 管理页 |
| 系统日志 | `sys_operation_log` 审计 | 管理页 |

---

## 二、生产主管 (PROD_SUPERVISOR)

**职责**：订单→工单→齐套→排产→派工→现场管理

| 视图 | 核心数据 | 页面类型 |
|------|---------|---------|
| 生产驾驶舱 | 今日工单进度 + 齐套缺口 + 产线负荷 | 仪表盘首页 |
| **计划调度（3 页）** | 订单中心 → 工单中心 → 排产派工（Stepper 三步，齐套内嵌工单 Tab） | |
| 订单中心 | `customer_order` / `customer_order_item` CRUD + 生成工单 | `/app/planning/orders` |
| 工单中心 | `work_order` 创建/下达/齐套/进度 | `/app/planning/work-orders` |
| 排产派工 | `production_task` → `dispatch_task` + 产线甘特 | `/app/planning/scheduling` |
| **生产管理** | | |
| 产品/物料/BOM | `product` / `material` / `bom` | 主数据 CRUD |
| 车间/产线/工位/班次 | 工厂模型 | 列表页 |
| 工艺路线 | `process_route` + `route_step` | 列表页 |

---

## 三、仓库物料员 (WAREHOUSE_CLERK)

**职责**：备料、领料、发料、退料、库存批次

| 视图 | 核心数据 | 页面类型 |
|------|---------|---------|
| 仓库工作台 | 待处理领料单数 + 欠料预警 + 库存概览 | 仪表盘首页 |
| 领料单处理 | `material_requisition` → `material_issue` | 流程页 |
| 发料管理 | `material_issue` + `material_issue_item`（批次指定） | 操作页 |
| 库存批次 | `inventory_batch`（可用/锁定/质检状态） | 列表+筛选 |
| 欠料预警 | `material_shortage`（缺料数量 + 预计到货时间） | 预警列表 |
| 库存流水 | `inventory_transaction` | 审计日志 |
| 退料处理 | `material_return` | 操作页 |

---

## 四、产线操作工人 (LINE_OPERATOR)

**职责**：扫码→看SOP→物料绑定→报工→发起安灯

| 视图 | 核心数据 | 页面类型 |
|------|---------|---------|
| 我的工位 | 当前派工任务 + SOP 指引 + 报工入口 | 核心工作页 |
| 扫码报工 | `production_report`（良品/不良品 + 数量） | 扫码+表单 |
| SOP 指引 | `sop_file`（工序对应的图片/视频/文档） | 多媒体展示 |
| 物料绑定 | `product_material_binding`（SN + 批次绑定扫描） | 扫码操作 |
| 发起安灯 | `andon_event`（选择类型/原因 → 提交） | 快捷表单 |
| 我的计件 | `piece_wage_record`（当日/当周工资） | 统计卡片 |

---

## 五、质检员 (QUALITY_INSPECTOR)

**职责**：质量态势分析 → 待检任务 → 检验执行 → 不良记录 → 标准配置

| 视图 | 路由 | 说明 |
|------|------|------|
| 质量分析中心 | `/app/quality/analytics` | **主打页**：KPI + 趋势图 + 类型分布 + 不良 TOP + 待检快捷表 |
| 检验工作台 | `/app/quality/workbench` | 左待检队列 + 右检验执行（IQC/IPQC/OQC/RQC） |
| 检验记录 | 分析/工作台内 Drawer | 合并原来料/过程/出货/退料 4 个列表页 |
| 标准配置 | 分析/工作台内 Drawer | 合并检测模板 / 检测项 / 常见缺陷 |
| 批次追溯 | `/app/analytics/batch-trace` | 从质量板块外链，不再重复侧栏入口 |

旧路由（`/app/quality/pending`、`/app/quality/iqc` 等）自动 redirect 至上述页面。

---

## 六、设备维修员 (EQUIPMENT_MAINTAINER)

**职责**：设备台账→点检→保养→报修→维修→OEE

| 视图 | 核心数据 | 页面类型 |
|------|---------|---------|
| 设备工作台 | 待点检/待保养/待维修数量 + OEE 概览 | 仪表盘首页 |
| 设备台账 | `device`（按产线/工位/状态筛选） | 列表页 |
| 点检管理 | `device_inspection_record`（按计划执行） | 任务+记录 |
| 保养计划 | `maintenance_plan` → `maintenance_task` | 日历视图 |
| 报修工单 | `repair_order`（故障原因 + 描述 + 状态） | 工单流 |
| 维修记录 | `repair_record` | 归档查询 |
| OEE 分析 | `device_oee_daily`（时间利用率/性能/质量） | 趋势图表 |

---

## 开发优先级

1. **管理人员** — 先行开发，作为系统骨架
2. **生产主管** — 核心业务流程（订单→工单→派工）
3. **产线操作工人** — 现场高频操作（报工+安灯）
4. **质检员** — 质量闭环
5. **仓库物料员** — 物料流转
6. **设备维修员** — 设备保障

---

## 权限实现说明（运行时）

| 层级 | 控制方式 | 配置位置 |
|------|---------|---------|
| 用户 → 角色 | 每用户仅 1 个角色（`sys_user_role`） | 权限与组织 → 用户 |
| 侧栏菜单 | `role_code` → 导航模块 | `frontend/web-admin/src/constants/role-access.js` |
| 页面路由 | 同上，路由守卫 `canRoleAccessPath` | `role-access.js` + `router/index.js` |
| 操作按钮 | DB `sys_role_menu` perms + 角色前缀 | 登录响应 `permissions` + `checkPermi` |

登录接口 `/api/auth/login` 返回 `roleCode` 与 `permissions`；刷新权限可调用 `/api/auth/me`。
