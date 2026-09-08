# 生产数据闭环缺陷清单

> **2026-07-14 更新**：compat 主线（WM/QC/pro_feedback）已可演示黄金链，见 [`workflow-golden-path.md`](workflow-golden-path.md)。下表区分 **compat 已实现** / **native V2 待桥接** / **仍缺失**。

## Compat 主线（UI 实际走的路径）— 已实现

| 模块 | 表/API | 页面 | 说明 |
|------|--------|------|------|
| 计划 | `work_order` + `lifecycle_status` | 工单中心 | P0 状态机软启动 |
| 排产 | `production_task` → `dispatch_task` | 甘特/我的工位 | P0 自动派工同步 |
| 领料 | `wm_issue_header` + `StorageCoreService` | 出库领料 | 扣 `wm_material_stock` |
| 报工 | `pro_feedback` | 我的工位/报工 | execute 回写 dispatch |
| 质检 | `qc_ipqc` + `QcFinishWriteback` | 质检工作台 | finish → lifecycle |
| 入库 | `wm_product_recpt` | 成品入库 | P0 QC 门禁（lifecycle 非空时） |

## 已实现（152 项）

所有后端 CRUD 接口均通过 JDBC Repository → MockDataService 双轨模式运行，连接数据库时直写 MySQL，无数据库时自动降级为内存 mock。

| 模块 | 已实现数 | 详情 |
|------|---------|------|
| 工厂资源 | 20 | 车间、产线、工位、班次 全 CRUD |
| 生产执行 | 15 | 产品SN、报工、完工 全 CRUD |
| 计划工单 | 20 | 客户订单、工单、生产任务、派工 全 CRUD |
| 工艺流程 | 10 | 工序、工艺路线 全 CRUD |
| 产品物料 | 20 | 产品、物料、BOM、计量单位 全 CRUD |
| 条码应用 | 15 | 条码类型、规则、模板 全 CRUD |
| 质量管理 | 15 | 质检任务、不良、返修 全 CRUD |
| 设备管理 | 15 | 设备类别、设备、报修 全 CRUD |
| 仓储库存 | 11 | 仓库、批次、流水 全 CRUD |
| 安灯异常 | 15+ | 类型、原因 全 CRUD；**pro_andon 呼叫记录/配置 compat**；事件桥接；任务/分析页 |

---

## 缺失（29 项）

### 阻断级（无法完成生产闭环）

| # | 缺失功能 | 影响 | 涉及表 |
|---|---------|------|--------|
| 1 | ~~**安灯事件创建/关闭**~~ | ✅ compat `pro_andon_record` + 桥接 `andon_event`；操作工页可发起 | `pro_andon_record`, `andon_event` |
| 2 | **安灯任务分配/结果** | 任务页已建；分配/结果 API 部分可用 | `andon_task`, `andon_result` |
| 3 | ~~**领料/发料/退料**~~ | ✅ WM compat：`wm_issue_header` 已扣库存；native `material_issue` **废弃 UI** | `wm_issue_*` / `material_*` |
| 4 | **统一接口，service 未暴露** | 关键物料无法绑定到SN，影响追溯 | `product_material_binding` |

### 重要级（影响数据完整性）

| # | 缺失功能 | 影响 | 涉及表 |
|---|---------|------|--------|
| 5 | **质量放行** | 质检后无法正式放行/隔离/让步 | `quality_release` |
| 6 | **检验记录** | 无录入检验结果的标准接口 | `quality_record`, `quality_record_item` |
| 7 | **检验项目/标准** | 无定义检验项目的接口 | `inspection_item`, `quality_standard` |
| 8 | **BOM 明细管理** | 无法编辑 BOM 中的物料及用量 | `bom_item` |
| 9 | **产品工艺路线绑定** | 无法为产品指定工艺路线 | `product_route` |
| 10 | **工艺路线工序编排** | 无法编辑路线中的工序顺序 | `process_route_step` |
| 11 | **条码记录生成** | 无生成/打印/查询条码记录的接口 | `barcode_record` |
| 12 | **条码应用规则** | 无配置条码应用规则的接口 | `barcode_application_rule` |

### 次要级（后续迭代）

| # | 缺失功能 | 涉及表 |
|---|---------|--------|
| 13-18 | 设备保养计划/任务/点检/OEE/状态日志/维修记录 | `maintenance_plan`, `maintenance_task`, `device_inspection_record`, `device_oee_daily`, `device_status_log`, `repair_record` |
| 19-22 | 成品入库/库位/报废记录/齐套分析 | `finished_inbound`, `storage_location`, `scrap_record`, `kitting_analysis` |
| 23-29 | 安灯通知/配置/异常配置/产品规格/SOP/标签模板/条码包装绑定 | `andon_notice`, `andon_exception_config`, `product_spec`, `sop_file`, `barcode_template_param`, `package_binding` 等 |

---

## 说明

- 所有缺失项对应的**数据库表结构已完整建立**（Flyway V1+V2）。缺失的是后端 Service/Controller 层代码。
- Repository 层部分已有基础实现（如安灯事件的 `insert()`/`updateStatus()`），但 Service 未调用。
- 前端可按相同组件模式独立开发，后端补全后直接对接。
