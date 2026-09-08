# 云枢智造 MES — 系统状态文档

> 日期：2026-07-09 | 版本：v0.2.0

---

## 一、技术栈

| 层级 | 技术 | 版本 |
|------|------|------|
| 后端框架 | Spring Boot | 3.3.6 |
| JDK | OpenJDK | 17 |
| 构建工具 | Maven Wrapper | 3.x |
| 数据库 | MySQL | 8.0 |
| 数据库迁移 | Flyway | 10.x（双轨模式） |
| 前端框架 | Vue 3 + Vite | 3.5 / 6.x |
| 状态管理 | Pinia | 2.2 |
| 路由 | Vue Router | 4.5 |
| HTTP客户端 | Axios | 1.7 |
| 图表 | ECharts | 6.1 |
| JWT | jjwt | 0.12.6 |

---

## 二、数据库初始化方法

### 2.1 连接信息

```
Host:     localhost:3306
Database: fan_mes
User:     root
Password: 本机密码
Charset:  utf8mb4
```

### 2.2 表结构

系统共 **103 张**业务表（不含 Agent AI 平台 54 张表），通过 Flyway 版本化迁移脚本管理。

**迁移脚本位置：**

| 路径 | 内容 | 说明 |
|------|------|------|
| `backend/mes-server/src/main/resources/db/migration/` | V1-V4 主迁移 | 由 Flyway 自动执行 |
| `backend/mes-server/src/main/resources/db/seed/` | R__seed_basic_data.sql | 可重复执行的种子数据 |
| `database/migrations/` | 001-004 独立副本 | 手动执行备用 |

**Flyway 迁移版本：**

| 版本 | 文件 | 内容 |
|------|------|------|
| V1 | `V1__core_schema.sql` | 系统基础表（用户、角色、部门、权限、操作日志等） |
| V2 | `V2__mes_business_schema.sql` | MES 业务表（工厂、主数据、工艺、计划、生产、质量、库存、设备、安灯、条码等） |
| V3 | `V3__agent_schema.sql` | Agent AI 平台表 |
| V4 | `V4__ai_chat.sql` | AI 对话会话表 |

### 2.3 启动时数据库行为

配置在 `application-local.yml`：

```yaml
spring:
  datasource:
    url: jdbc:mysql://localhost:3306/fan_mes?createDatabaseIfNotExist=true
    username: root
    password: 051002sry
    hikari:
      maximum-pool-size: 5
      minimum-idle: 1
  flyway:
    enabled: true                          # 启用自动迁移
    locations:
      - classpath:db/migration             # 先执行版本化迁移
      - classpath:db/seed                  # 再执行可重复种子数据
    baseline-on-migrate: true              # 已有表时从 V5 开始基准
    baseline-version: 5
    validate-on-migrate: false             # 不校验已有表结构差异
```

**启动流程：**

```
应用启动
  ├── 连接 MySQL (localhost:3306)
  ├── 若 fan_mes 库不存在 → 自动创建
  ├── Flyway 检查 schema_version 表
  │   ├── 首次启动 → 执行 V1→V2→V3→V4
  │   └── 已迁移 → 跳过，显示 "up to date"
  ├── 执行 R__seed_basic_data.sql（可重复）
  └── 应用就绪 → 监听 8080
```

### 2.4 双轨数据访问模式

后端采用 **Repository → Service → Mock** 三层弹性架构：

```
Controller
  │
  ▼
Service (@Primary)          ← Spring 注入
  ├── try: JdbcTemplate 直连 MySQL
  │       成功 → 返回真实数据
  │
  └── catch (DataAccessException)
         ├── 读操作 → 回退 MockDataService（内存种子数据）
         └── 写操作 → 返回 "当前为 Mock 模式" 错误
```

此架构允许应用在 MySQL 不可用时仍能提供读取服务，但写入需要数据库连接正常。

### 2.5 种子数据

种子数据在 `R__seed_basic_data.sql` 中，包含：

- 6 个部门（管理、生产、仓储、质量、设备等）
- 7 种角色（MANAGER / PROD_SUPERVISOR / WAREHOUSE_CLERK / QUALITY_INSPECTOR / EQUIPMENT_MAINTAINER / LINE_OPERATOR / TESTER）
- 12 个用户（admin, supervisor, warehouse, quality, repair, worker, tester 及各中文名副本）
- 3 个车间、3 条产线、8 个工位
- 3 个产品、5 种物料、1 个 BOM（含 5 项物料明细）
- 5 个工序、2 条工艺路线
- 2 个客户订单、3 个工单
- 3 个仓库、5 个库存批次
- 2 台设备
- 安灯类型 3 种、安灯原因 6 种
- 条码规则 3 条、条码类型 3 种

### 2.6 测试账号

| 用户名 | 密码 | 角色 | 用途 |
|--------|------|------|------|
| admin | admin123 | MANAGER | 系统管理员 |
| supervisor | 123456 | PROD_SUPERVISOR | 生产主管 |
| warehouse | 123456 | WAREHOUSE_CLERK | 仓库物料员 |
| quality | 123456 | QUALITY_INSPECTOR | 质检员 |
| repair | 123456 | EQUIPMENT_MAINTAINER | 设备维修员 |
| worker | 123456 | LINE_OPERATOR | 产线操作工人 |
| tester | 123456 | TESTER | 全模块测试 |

JWT 鉴权已开启（`JWT_ENFORCE: true`），除 `/api/auth/login` 和 `/api/health` 外所有接口需要 `Authorization: Bearer <token>`。

---

## 三、已实现的接口清单（58 张表）

### 3.1 系统管理（4 张表）

| 接口 | 方法 | 路径 |
|------|------|------|
| 用户 CRUD | GET/POST/PUT/DELETE | `/api/system/users` |
| 角色只读 | GET | `/api/system/roles` |
| 部门 CRUD | GET/POST/PUT/DELETE | `/api/system/departments` |
| 操作日志 | GET | `/api/system/logs` |

### 3.2 工厂资源（4 张表）

| 接口 | 方法 | 路径 |
|------|------|------|
| 车间 CRUD | ALL | `/api/factory/workshops` |
| 产线 CRUD | ALL | `/api/factory/lines` |
| **产线监控总览** | GET | `/api/factory/lines/monitor/summary` |
| **产线监控详情** | GET | `/api/factory/lines/{lineId}/monitor` |
| 工位 CRUD | ALL | `/api/factory/workstations` |
| 班次 CRUD | ALL | `/api/factory/shifts` |

### 3.3 主数据（6 张表）

| 接口 | 方法 | 路径 |
|------|------|------|
| 产品 CRUD | ALL | `/api/master-data/products` |
| 物料 CRUD | ALL | `/api/master-data/materials` |
| BOM CRUD + 明细 | ALL | `/api/master-data/boms` |
| 产品-路线绑定 | GET/POST | `/api/master-data/products/{id}/route` |
| 计量单位 CRUD | ALL | `/api/master-data/uoms` |

### 3.4 工艺流程（3 张表）

| 接口 | 方法 | 路径 |
|------|------|------|
| 工序 CRUD | ALL | `/api/process/steps` |
| 工艺路线 CRUD | ALL | `/api/process/routes` |
| 路线工序编排 | ALL | `/api/process/routes/{id}/steps` |

### 3.5 计划排程（7 张表）

| 接口 | 方法 | 路径 |
|------|------|------|
| 客户订单 CRUD | ALL | `/api/planning/orders` |
| 工单 CRUD | ALL | `/api/planning/work-orders` |
| 生产任务 CRUD | ALL | `/api/planning/production-tasks` |
| 派工单 CRUD | ALL | `/api/planning/dispatch-tasks` |
| 齐套分析 | GET | `/api/planning/kitting/{workOrderId}` |
| 库存锁定 | POST | `/api/planning/kitting/{id}/reserve` |
| 锁定释放 | POST | `/api/planning/kitting/{id}/release` |
| 欠料列表 | GET | `/api/planning/material-shortages` |
| 工单取消 | POST | `/api/planning/work-orders/{id}/cancel` |
| 任务日志 | GET/POST | `/api/planning/task-logs` |

### 3.6 生产执行（5 张表）

| 接口 | 方法 | 路径 |
|------|------|------|
| 产品SN CRUD | ALL | `/api/production/product-sns` |
| 报工 CRUD | ALL | `/api/production/reports` |
| 完工 CRUD | ALL | `/api/production/completions` |
| SN 批量生成 | POST | `/api/production/completion-ops/product-sns/generate` |
| 成品入库 | POST | `/api/production/completion-ops/finished-inbound` |
| 物料绑定 | GET/POST | `/api/production/binding-ops/material-bindings` |

### 3.7 质量管理（7 张表）

| 接口 | 方法 | 路径 |
|------|------|------|
| 质检任务 CRUD | ALL | `/api/quality/tasks` |
| 缺陷记录 CRUD | ALL | `/api/quality/defects` |
| 返修单 CRUD | ALL | `/api/quality/rework-orders` |
| 检验记录 CRUD | ALL | `/api/quality/records` |
| 质量放行 CRUD | ALL | `/api/quality/releases` |
| 返修行为 | POST | `/api/quality/rework-records` |
| 报废记录 | POST | `/api/quality/scraps` |

### 3.8 仓储库存（6 张表）

| 接口 | 方法 | 路径 |
|------|------|------|
| 仓库 CRUD | ALL | `/api/inventory/warehouses` |
| 批次 CRUD | ALL | `/api/inventory/batches` |
| 交易流水 | GET | `/api/inventory/transactions` |
| 领料单 CRUD | ALL | `/api/inventory/requisitions` |
| 发料单 CRUD | ALL | `/api/inventory/issues` |
| 退料单 CRUD | ALL | `/api/inventory/returns` |

### 3.9 设备管理（6 张表）

| 接口 | 方法 | 路径 |
|------|------|------|
| 设备类别 CRUD | ALL | `/api/equipment/categories` |
| 设备台账 CRUD | ALL | `/api/equipment/devices` |
| 报修单 CRUD | ALL | `/api/equipment/repair-orders` |
| 维修记录 | GET/POST | `/api/equipment/repair-records` |
| 设备状态日志 | GET/POST | `/api/equipment/device-status-logs/{deviceId}` |
| 保养计划 CRUD | GET/POST/DELETE | `/api/equipment/maintenance-plans` |
| 点检记录 CRUD | GET/POST/DELETE | `/api/equipment/inspections` |

### 3.10 安灯中心（5 张表）

| 接口 | 方法 | 路径 |
|------|------|------|
| 安灯类型 CRUD | ALL | `/api/andon/types` |
| 安灯原因 CRUD | ALL | `/api/andon/reasons` |
| 事件列表 | GET | `/api/andon/events` |
| 事件创建 | POST | `/api/andon/events/create` |
| 事件关闭 | PUT | `/api/andon/events/{id}/close` |
| 任务分配 | POST | `/api/andon/events/{id}/assign` |
| 任务结果 | POST | `/api/andon/events/{id}/result` |

### 3.11 条码管理（3 张表）

| 接口 | 方法 | 路径 |
|------|------|------|
| 条码类型 CRUD | ALL | `/api/barcode/types` |
| 条码规则 CRUD | ALL | `/api/barcode/rules` |
| 条码模板 CRUD | ALL | `/api/barcode/templates` |

### 3.12 追溯 / 报表 / 集成 / 仪表板

| 接口 | 方法 | 路径 |
|------|------|------|
| 产品追溯 | GET | `/api/traceability/product?code=` |
| 批次反追溯 | GET | `/api/traceability/batch/{batchNo}` |
| 物料追溯 | GET | `/api/traceability/material/{materialId}` |
| 报表定义 | GET | `/api/reporting/definitions` |
| 外部系统 CRUD | ALL | `/api/integration/systems` |
| 仪表板摘要 | GET | `/api/dashboard/summary` |

---

## 四、数据库已设计但尚未实现的接口

以下表结构已通过 Flyway 创建，但缺少对应的后端 Controller / Service / Repository。

### 4.1 必要级（阻碍核心流程 — 3 张）

| # | 表名 | 业务场景 | 缺失影响 |
|---|------|---------|---------|
| 1 | **`barcode_record`** | 条码生成/打印/查询 | 产品SN创建后无法生成条码记录，标签打印和扫码追溯链路中断 |
| 2 | **`sop_file`** | SOP 标准作业指导书 | 操作工在「我的工位」看不到工序 SOP，缺少标准化作业指引 |
| 3 | **`storage_location`** | 仓库库位管理 | 库存只能定位到仓库级，无法按货架/库位精准拣货 |

### 4.2 重要级（显著增强业务能力 — 6 张）

| # | 表名 | 业务场景 | 缺失影响 |
|---|------|---------|---------|
| 4 | **`maintenance_task`** | 保养计划 → 保养任务分解 | 只能创建保养计划，无法拆分为具体执行任务 |
| 5 | **`barcode_application_rule`** | 条码应用规则（产品/物料→条码规则映射） | 无法配置哪种产品使用哪种条码规则 |
| 6 | **`product_spec`** | 产品规格参数（电压、功率、尺寸等） | 产品无结构化技术参数 |
| 7 | **`quality_standard`** | 质量检验标准定义 | 质检时无标准可对照，只能自由文本记录 |
| 8 | **`quality_standard_item`** | 检验标准检测项 | 标准下的具体检测项目无法逐项判定 |
| 9 | **`factory_calendar`** | 工厂工作日历 | 排产计算和 OEE 统计缺少日历基准 |

### 4.3 增强级（提升专业化程度 — 12 张）

| # | 表名 | 用途 |
|---|------|------|
| 10 | `inspection_item` | 通用检验项目库 |
| 11 | `inspection_item_category` | 检验项目分类 |
| 12 | `quality_record_item` | 检验记录明细（逐项记录） |
| 13 | `defect_reason` | 缺陷原因分类库 |
| 14 | `product_process_state` | 产品在制状态追踪 |
| 15 | `package_binding` | 包装层级绑定（箱→托盘） |
| 16 | `production_report_detail` | 报工扩展明细 |
| 17 | `barcode_template_param` | 标签模板参数 |
| 18 | `barcode_rule_segment` | 条码规则片段 |
| 19 | `label_print_log` | 标签打印日志 |
| 20 | `andon_notice` | 安灯通知记录 |
| 21 | `andon_exception_config` | 安灯异常自动触发规则 |

### 4.4 辅助级（IoT / 集成 / 扩展 — 24 张）

| 类别 | 表名 |
|------|------|
| 设备IoT | `device_count_config`, `equipment_count_record`, `device_oee_daily`, `device_integration_config`, `device_manufacturer` |
| 能耗 | `energy_meter`, `energy_reading` |
| 故障库 | `fault_cause` |
| 点检 | `device_inspection_item` |
| 集成 | `erp_work_order_map`, `api_unit_mapping`, `sync_log` |
| 报表 | `dashboard_config`, `dashboard_kpi_snapshot`, `report_run_log` |
| 工资 | `piece_wage_record`, `piece_wage_rule` |
| 日志 | `api_call_log`, `mobile_operation_log`, `task_operation_log`, `trace_query_log` |
| 配置 | `api_endpoint` |
| 移动端 | `wechat_user_binding` |

---

## 五、启动指南

### 5.1 前置条件

- JDK 17+
- Node.js 18+
- MySQL 8.0 运行中
- 端口 8080、5173 可用

### 5.2 启动命令

```cmd
# 方式一：使用控制面板
start.cmd
  [1] Start All    — 启动后端 (:8080) + 前端 (:5173)
  [2] Restart Backend  — 仅重启后端
  [3] Restart Frontend — 仅重启前端
  [4] Health Check  — 检查各服务状态

# 方式二：手动启动
cd backend\mes-server
.\mvnw.cmd spring-boot:run          # 后端

cd frontend\web-admin
npm run dev                          # 前端
```

### 5.3 访问地址

- 前端：http://127.0.0.1:5173/app
- 后端 API：http://127.0.0.1:8080/api
- 健康检查：http://127.0.0.1:8080/api/health

### 5.4 配置文件

| 文件 | 用途 |
|------|------|
| `application.yml` | 通用配置（端口、CORS、JWT） |
| `application-local.yml` | 本地开发（MySQL 连接、Flyway、Agent LLM） |
| `application-dev.yml` | 开发环境（环境变量驱动） |
