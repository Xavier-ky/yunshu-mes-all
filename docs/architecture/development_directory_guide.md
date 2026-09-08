# 云枢智造 MES 开发目录总览

本文档是整个项目的开发总说明，面向项目组长、前端开发、后端开发、数据库开发、Agent 开发以及后续接手项目的 Codex/Agent。  
目标是：任何人只阅读本文档，就能理解本项目要开发什么、有哪些用户、有哪些页面、有哪些后端模块、数据库如何组织、各成员如何在同一框架下协作扩展。

## 1. 项目定位

本项目是面向电风扇制造组装行业的 MES 系统，产品名称暂定为“云枢智造 MES”。系统围绕电风扇从订单进入、生产计划、物料齐套、产线执行、质量检验、安灯异常、设备维修、成品入库、产品追溯到智能分析的完整业务闭环进行建设。

系统不是简单的课程演示系统，而应按可持续开发的商业化产品结构搭建。开发时应保证：

- 前端、后端、数据库、Agent 模块边界清晰；
- 多人开发时互不影响，成员可在自己的模块内扩展；
- 页面、接口、数据库命名统一；
- 系统支持 Web 管理端和微信小程序端；
- 后续能够接入更强大的智能 Agent；
- Windows 和 Mac 环境均可正常打开、开发和部署。

## 2. 技术栈

| 层级 | 技术 |
| --- | --- |
| Web 管理端 | Vue 3、Vue Router 4 |
| 微信端 | 微信小程序原生开发 |
| 后端 | Spring Boot、RESTful API |
| 数据库 | MySQL 8.0+ |
| 数据库迁移 | Flyway 约定目录，或手动执行 SQL |
| 安全 | 对称加密算法，前后端双端实现 |
| Agent | 后端 Agent 模块 + 大模型配置 + 工具调用 + 知识库 + 审批审计 |

## 3. 顶层目录结构

标准目录如下：

```text
MES/
  backend/
    mes-server/
      src/main/java/com/yunshu/mes/
      src/main/resources/db/migration/
      src/main/resources/db/seed/
      src/test/java/com/yunshu/mes/
  frontend/
    web-admin/
    wechat-miniprogram/
    shared/
  database/
    migrations/
    seeds/
    docs/
  docs/
    architecture/
    api/
    database/
    deployment/
    security/
  ops/
    docker/
    scripts/
    ci/
  security/
    crypto-spec/
    test-vectors/
  shared/
    contracts/
    schemas/
  tools/
```

### 3.1 目录职责

| 目录 | 职责 |
| --- | --- |
| `backend/mes-server` | Spring Boot 后端主服务，承载 RESTful API、业务逻辑、权限控制、Agent 服务 |
| `frontend/web-admin` | Vue 3 管理端，面向全部六类角色（管理人员、生产主管、仓库物料员、质检员、设备维修员、产线操作工人） |
| `frontend/wechat-miniprogram` | 微信小程序端，面向移动现场使用，如看板、追溯、异常、设备状态 |
| `frontend/shared` | Web 与小程序可共享的常量、字段枚举、接口类型说明 |
| `database/migrations` | 可独立执行的 MySQL 建表脚本 |
| `database/seeds` | 开发初始化种子数据 |
| `backend/mes-server/src/main/resources/db/migration` | Spring Boot/Flyway 数据库迁移脚本 |
| `docs/api` | RESTful API 文档 |
| `docs/database` | 数据库设计说明、数据字典、ER 图 |
| `docs/security` | 前后端加密规范、登录安全、接口签名等说明 |
| `security/crypto-spec` | 对称加密算法实现规范 |
| `security/test-vectors` | 前后端加密一致性测试样例 |
| `shared/contracts` | 前后端共享接口契约 |
| `shared/schemas` | JSON Schema、OpenAPI Schema 等共享结构 |

## 4. 用户角色设计

系统至少包含以下用户角色。所有页面与接口都应围绕角色权限进行控制。

| 角色 | 主要职责 | 典型页面 |
| --- | --- | --- |
| 管理人员 | 查看综合看板、报表、追溯、质量与生产分析；维护用户、角色、权限和系统参数 | 中控看板、报表分析、产品追溯、系统管理 |
| 生产主管 | 接收订单、创建工单、排产、齐套分析、派工，管理产线任务和现场生产异常 | 订单管理、工单管理、齐套分析、生产任务、派工管理、安灯事件 |
| 仓库物料员 | 管理库存、备料、领料、发料、退料、库存流水 | 仓库管理、库存批次、领料单、发料单、库存流水 |
| 质检员 | 首末件检验、巡检、成品检验、不良记录、返修处理 | 质检任务、质检记录、不良记录、返修单、质量放行 |
| 设备维修员 | 设备点检、报修处理、维修记录、保养任务 | 设备台账、点检记录、报修单、维修记录、保养任务 |
| 产线操作工人 | 接工、扫码、查看 SOP、报工、绑定关键物料、发起安灯 | 工位任务、工序作业、电子 SOP、生产报工、异常安灯 |

> 角色精简说明（方案 A）：本项目将角色精简为 6 类。原 PMC 计划员的订单、工单、排产、齐套、派工职责并入生产主管；原系统管理员的用户、角色、权限、参数维护职责并入管理人员权限集。Agent 管理员、知识维护员推迟到 Agent 阶段（Phase 5/6）再加。

权限原则：

- 产线操作工人只能操作自己工位或自己任务；
- 仓库物料员只能处理仓储单据；
- 质检员只能创建和处理质量相关单据；
- 维修员只能处理设备点检、报修和维修；
- Agent 默认只能读取数据，高风险写操作必须审批；
- 管理人员负责用户、角色、权限和系统参数维护，但业务审批仍由对应业务负责人完成。

## 5. Web 管理端页面规划

Web 管理端路径建议统一放在 `frontend/web-admin/src/views/` 下。路由建议按业务域分组，不要把所有页面平铺。

### 5.1 登录与首页

| 页面 | 路由建议 | 必须功能 | 主要用户 |
| --- | --- | --- | --- |
| 登录页 | `/login` | 用户名密码登录、前端对称加密、记住登录状态、错误提示 | 所有用户 |
| 首页工作台 | `/dashboard/workbench` | 按角色显示待办、快捷入口、今日工单、安灯待处理、质检任务、设备故障 | 所有用户 |
| 中控看板 | `/dashboard/central` | 产量、工单达成率、直通率、不良率、设备 OEE、安灯数量、库存风险 | 管理人员 |
| 车间看板 | `/dashboard/workshop` | 按车间/产线展示生产任务、进度、质量异常、设备状态 | 管理人员、生产主管 |
| 产线看板 | `/dashboard/line` | 单产线工单、当前工序、产出、良品、不良、节拍、异常 | 生产主管、产线人员 |

首页工作台应按角色动态展示，不同用户登录后看到不同待办。例如质检员看到待检任务，维修员看到待维修设备，PMC 看到欠料工单。

### 5.2 系统管理模块

目录建议：

```text
frontend/web-admin/src/views/system/
```

| 页面 | 路由建议 | 必须功能 | 后端模块 |
| --- | --- | --- | --- |
| 用户管理 | `/system/users` | 新增、编辑、停用、重置密码、分配角色、按部门查询 | `auth/system` |
| 角色管理 | `/system/roles` | 角色 CRUD、分配菜单权限、分配接口权限 | `auth/system` |
| 权限管理 | `/system/permissions` | 维护菜单、按钮、API、数据权限 | `auth/system` |
| 部门管理 | `/system/departments` | 部门树、部门类型、启停用 | `auth/system` |
| 操作日志 | `/system/operation-logs` | 查询用户操作、业务对象、请求结果 | `auth/system` |
| 系统参数 | `/system/config` | 基础参数、业务参数、加密参数配置 | `auth/system` |

### 5.3 工厂资源模块

目录建议：

```text
frontend/web-admin/src/views/factory/
```

| 页面 | 路由建议 | 必须功能 | 后端模块 |
| --- | --- | --- | --- |
| 车间管理 | `/factory/workshops` | 车间 CRUD、状态维护 | `factory` |
| 产线管理 | `/factory/lines` | 产线 CRUD、绑定车间、额定产能 | `factory` |
| 工位管理 | `/factory/workstations` | 工位 CRUD、绑定产线、工位类型 | `factory` |
| 班次管理 | `/factory/shifts` | 班次时间、班次状态 | `factory` |
| 工厂日历 | `/factory/calendar` | 工作日、休息日、班次日历、排产依据 | `factory` |

工厂资源是排产、派工、报工、设备绑定的基础。其他模块只能引用这些主数据，不应重复维护车间和产线字段。

### 5.4 产品、物料、BOM 模块

目录建议：

```text
frontend/web-admin/src/views/master-data/
```

| 页面 | 路由建议 | 必须功能 | 后端模块 |
| --- | --- | --- | --- |
| 产品管理 | `/master/products` | 产品 CRUD、型号、类别、状态、规格维护 | `master-data` |
| 产品规格 | `/master/product-specs` | 扇叶直径、电机功率、颜色、能效等级等规格项 | `master-data` |
| 物料管理 | `/master/materials` | 电机、扇叶、外壳、螺丝、包装物料，是否关键物料 | `master-data` |
| BOM 管理 | `/master/boms` | BOM 版本、BOM 明细、单位用量、损耗率、启用状态 | `master-data` |
| 计量单位 | `/master/uom` | 单位维护、外部单位映射基础 | `master-data` |

关键要求：

- 产品和物料必须有唯一编码；
- BOM 必须支持版本；
- BOM 明细中必须标明关键追溯物料；
- 生产工单引用已发布 BOM，不能引用草稿 BOM。

### 5.5 工艺与 SOP 模块

目录建议：

```text
frontend/web-admin/src/views/process/
```

| 页面 | 路由建议 | 必须功能 | 后端模块 |
| --- | --- | --- | --- |
| 工序管理 | `/process/steps` | 工序 CRUD、工序类型、标准工时、计件单价 | `process` |
| 工艺路线 | `/process/routes` | 路线版本、路线工序排序、是否必过 | `process` |
| 产品工艺绑定 | `/process/product-routes` | 产品绑定默认工艺路线 | `process` |
| 电子 SOP | `/process/sops` | 上传图片、视频、PDF、文档，按工序绑定 | `process` |
| 工序不良原因 | `/process/defect-reasons` | 维护工序常见不良原因 | `process` |

工位平板和生产报工依赖工艺路线。后端必须校验产品是否按照路线顺序报工，避免跳工序、错工序。

### 5.6 条码应用模块

目录建议：

```text
frontend/web-admin/src/views/barcode/
```

| 页面 | 路由建议 | 必须功能 | 后端模块 |
| --- | --- | --- | --- |
| 条码类型 | `/barcode/types` | 产品码、材料码、箱码、栈板码 | `barcode` |
| 条码规则 | `/barcode/rules` | 常量、日期、变量、流水号规则片段 | `barcode` |
| 标签模板 | `/barcode/templates` | 模板上传、模板参数维护 | `barcode` |
| 应用规则 | `/barcode/application-rules` | 产品/物料绑定条码规则和模板 | `barcode` |
| 条码生成 | `/barcode/generate` | 批量生成、导入、打印 | `barcode` |
| 包装绑定 | `/barcode/package-binding` | 产品码、箱码、栈板码层级绑定 | `barcode` |

条码是追溯入口，必须与 `product_sn`、`inventory_batch`、包装层级形成稳定关联。

### 5.7 订单、工单、排产模块

目录建议：

```text
frontend/web-admin/src/views/planning/
```

| 页面 | 路由建议 | 必须功能 | 后端模块 |
| --- | --- | --- | --- |
| 客户订单 | `/planning/orders` | 订单 CRUD、订单明细、交期、状态 | `planning` |
| 生产工单 | `/planning/work-orders` | 手工创建、导入、ERP 同步、绑定产品/BOM/工艺 | `planning` |
| 齐套分析 | `/planning/kitting-analysis` | 按工单计算物料需求、库存可用量、欠料数量 | `planning/inventory` |
| 欠料看板 | `/planning/shortage-board` | 欠料工单、欠料物料、预计到料、风险等级 | `planning` |
| 生产任务单 | `/planning/production-tasks` | 下达产线日计划、开工、暂停、结束 | `planning/production` |
| 生产派工单 | `/planning/dispatch-tasks` | 按工序、工位、人员派工 | `planning/production` |

优化建议：

- 工单创建后先进入齐套分析；
- 齐套不通过时不允许直接下达生产任务；
- 生产任务单是车间/产线层面的日计划；
- 派工单是工序/工位/人员层面的执行任务。

### 5.8 仓储库存模块

目录建议：

```text
frontend/web-admin/src/views/inventory/
```

| 页面 | 路由建议 | 必须功能 | 后端模块 |
| --- | --- | --- | --- |
| 仓库管理 | `/inventory/warehouses` | 原材料仓、成品仓、不良品仓 | `inventory` |
| 库位管理 | `/inventory/locations` | 仓库下库位维护 | `inventory` |
| 库存批次 | `/inventory/batches` | 批次号、物料、仓库、库位、可用量、质量状态 | `inventory` |
| 领料单 | `/inventory/requisitions` | 产线按工单发起领料 | `inventory` |
| 发料单 | `/inventory/issues` | 仓库按领料单发料，扣减批次库存 | `inventory` |
| 退料单 | `/inventory/returns` | 产线退料、数量回库 | `inventory` |
| 库存流水 | `/inventory/transactions` | 入库、发料、退料、调整、报废全记录 | `inventory` |
| 成品入库 | `/inventory/finished-inbound` | 质检合格后入成品仓 | `inventory/quality` |

库存相关动作必须生成 `inventory_transaction`，不得只修改库存批次数量。

### 5.9 生产执行模块

目录建议：

```text
frontend/web-admin/src/views/production/
```

| 页面 | 路由建议 | 必须功能 | 后端模块 |
| --- | --- | --- | --- |
| 生产任务看板 | `/production/tasks` | 按产线、日期、状态查看任务 | `production` |
| 工位作业台 | `/production/workstation` | 当前工位任务、接工、扫码、SOP、报工 | `production` |
| 产品 SN 管理 | `/production/product-sn` | 产品码生成、状态查询 | `production/barcode` |
| 关键物料绑定 | `/production/material-binding` | 产品 SN 绑定电机、扇叶、外壳等关键批次 | `production` |
| 生产报工 | `/production/reports` | 普通报工、不良报工、检测报工、设备计数报工 | `production` |
| 生产完工单 | `/production/completions` | 工单完工数量、不良数量、完工确认 | `production` |
| 计件工资 | `/production/piece-wage` | 工序单价、报工数量、工资记录 | `production` |

生产执行是系统核心。页面必须围绕“任务、工序、产品 SN、物料批次、报工”组织，而不是简单录入数量。

### 5.10 质量管理模块

目录建议：

```text
frontend/web-admin/src/views/quality/
```

| 页面 | 路由建议 | 必须功能 | 后端模块 |
| --- | --- | --- | --- |
| 检验项目分类 | `/quality/item-categories` | 检验分类维护 | `quality` |
| 检验项目 | `/quality/items` | 转速、风量、噪音、外观、绝缘等 | `quality` |
| 检验标准方案 | `/quality/standards` | 产品/客户/检验类型对应标准 | `quality` |
| 质检任务 | `/quality/tasks` | 待检、检验中、已完成 | `quality` |
| 首末件检验 | `/quality/first-last` | 首件、末件检验记录 | `quality` |
| 巡检单 | `/quality/patrol` | 生产过程定时巡检 | `quality` |
| 成品入库检验 | `/quality/finished-inbound` | 成品入库前抽检 | `quality` |
| 成品发货检验 | `/quality/shipping` | 出厂前检验报告 | `quality` |
| 质检记录 | `/quality/records` | 检验明细、实测值、判定结果 | `quality` |
| 不良记录 | `/quality/defects` | 不良现象、责任工序、严重程度 | `quality` |
| 返修单 | `/quality/rework-orders` | 返修、复检、报废 | `quality/production` |
| 质量放行 | `/quality/release` | 放行、让步、锁定 | `quality` |

质量模块必须与 `product_sn`、`work_order`、`process_step`、`defect_record` 关联，不能只保存孤立的质检结论。

### 5.11 安灯异常模块

目录建议：

```text
frontend/web-admin/src/views/andon/
```

| 页面 | 路由建议 | 必须功能 | 后端模块 |
| --- | --- | --- | --- |
| 安灯类型 | `/andon/types` | 生产、质量、设备、缺料等类型 | `andon` |
| 异常原因 | `/andon/reasons` | 常用异常原因维护 | `andon` |
| 异常通知配置 | `/andon/config` | 按类型、产线配置通知处理人 | `andon` |
| 安灯事件 | `/andon/events` | 查询异常、状态、关联工单/设备/产品 | `andon` |
| 处理任务 | `/andon/tasks` | 分派、接收、处理、转派 | `andon` |
| 处理结果 | `/andon/results` | 处理措施、关闭、复核 | `andon` |
| 异常分析 | `/andon/analytics` | 异常次数、响应时长、关闭时长、原因占比 | `andon/reporting` |

安灯必须形成闭环：发起、通知、分派、处理、关闭、统计。

### 5.12 设备管理模块

目录建议：

```text
frontend/web-admin/src/views/equipment/
```

| 页面 | 路由建议 | 必须功能 | 后端模块 |
| --- | --- | --- | --- |
| 设备类别 | `/equipment/categories` | 设备分类维护 | `equipment` |
| 设备制造商 | `/equipment/manufacturers` | 厂商和售后联系方式 | `equipment` |
| 故障原因 | `/equipment/fault-causes` | 按设备类别维护故障原因 | `equipment` |
| 设备台账 | `/equipment/devices` | 设备编码、位置、产线、工位、状态 | `equipment` |
| 设备状态 | `/equipment/status` | 正常、故障、维修中、停用 | `equipment` |
| 保养计划 | `/equipment/maintenance-plans` | 定期保养规则 | `equipment` |
| 保养任务 | `/equipment/maintenance-tasks` | 保养执行和回传 | `equipment` |
| 设备点检 | `/equipment/inspections` | 点检项目、点检记录 | `equipment` |
| 报修任务 | `/equipment/repair-orders` | 发起报修、分配维修人 | `equipment` |
| 维修记录 | `/equipment/repair-records` | 维修方案、结果、完成时间 | `equipment` |
| 设备 OEE | `/equipment/oee` | 开动率、性能、合格率、OEE | `equipment/reporting` |
| 能源分析 | `/equipment/energy` | 能耗、单位产品能耗、异常能耗 | `equipment/reporting` |

设备模块与安灯、生产报工、OEE、能源分析关联。设备异常可以触发安灯，也可以被 Agent 用于维修建议。

### 5.13 追溯模块

目录建议：

```text
frontend/web-admin/src/views/traceability/
```

| 页面 | 路由建议 | 必须功能 | 后端模块 |
| --- | --- | --- | --- |
| 产品追溯 | `/traceability/product` | 输入产品码，查订单、工单、工序、物料、质检、异常 | `traceability` |
| 物料批次追溯 | `/traceability/material-batch` | 输入物料批次，查库存流水、使用产品、关联工单 | `traceability` |
| 工单追溯 | `/traceability/work-order` | 查工单投入、产出、报工、质检、安灯、入库 | `traceability` |
| 质量问题追溯 | `/traceability/quality` | 从不良记录反查产品、物料、设备、工序 | `traceability/quality` |
| 追溯查询日志 | `/traceability/query-logs` | 查询谁在何时查询了哪些追溯对象 | `traceability` |

追溯链路必须以数据库真实关联为依据，不允许前端拼假数据。

### 5.14 报表分析模块

目录建议：

```text
frontend/web-admin/src/views/reporting/
```

| 页面 | 路由建议 | 必须功能 | 后端模块 |
| --- | --- | --- | --- |
| 报表定义 | `/reporting/definitions` | 维护报表名称、类型、查询条件 | `reporting` |
| 产量报表 | `/reporting/output` | 按日/周/月/产线/产品统计产量 | `reporting` |
| 生产实时信息 | `/reporting/realtime-production` | 工单投入、产出、不良、进度 | `reporting` |
| 不良查询 | `/reporting/defects` | 按时间、班组、原因、点位统计 | `reporting/quality` |
| 车间时段报表 | `/reporting/workshop-period` | 车间日/周/月生产情况 | `reporting` |
| 关键物料追溯报表 | `/reporting/material-trace` | 制令单、同批产品、同批物料 | `reporting/traceability` |
| OEE 报表 | `/reporting/oee` | 设备/产线 OEE 趋势 | `reporting/equipment` |
| 能耗报表 | `/reporting/energy` | 能耗与产量分析 | `reporting/equipment` |

报表查询结果可通过后端聚合实时生成，也可以读取 `dashboard_kpi_snapshot` 快照。

### 5.15 接口集成模块

目录建议：

```text
frontend/web-admin/src/views/integration/
```

| 页面 | 路由建议 | 必须功能 | 后端模块 |
| --- | --- | --- | --- |
| 外部系统 | `/integration/systems` | ERP、WMS、QMS、设备系统配置 | `integration` |
| API 接口定义 | `/integration/endpoints` | 接口路径、方法、方向、认证方式 | `integration` |
| API 调用日志 | `/integration/api-logs` | 请求、响应、耗时、错误信息 | `integration` |
| 同步日志 | `/integration/sync-logs` | 工单同步、生产结果回传、设备采集同步 | `integration` |
| ERP 工单映射 | `/integration/erp-work-orders` | ERP 工单号与 MES 工单关系 | `integration/planning` |
| 计量单位映射 | `/integration/unit-mapping` | 外部单位与 MES 单位映射 | `integration/master-data` |
| 设备对接配置 | `/integration/device-config` | OPC-UA、Modbus、MQTT、HTTP 信号映射 | `integration/equipment` |

接口集成模块由专人维护，其他模块只调用封装好的服务，不直接读写外部系统。

### 5.16 Agent 智能中心

目录建议：

```text
frontend/web-admin/src/views/agent/
```

| 页面 | 路由建议 | 必须功能 | 后端模块 |
| --- | --- | --- | --- |
| Agent 列表 | `/agent/profiles` | 配置生产计划、齐套、质量、设备、追溯等 Agent | `agent` |
| Agent 版本 | `/agent/versions` | 版本发布、回滚、发布说明 | `agent` |
| 模型配置 | `/agent/models` | 大模型服务商、模型、温度、token 限制 | `agent` |
| 提示词管理 | `/agent/prompts` | 系统提示词、任务提示词、版本管理 | `agent` |
| 工具管理 | `/agent/tools` | 可调用工具、输入输出 schema、风险等级 | `agent` |
| 工具权限 | `/agent/tool-permissions` | Agent 可用工具、是否需要审批 | `agent/security` |
| 会话记录 | `/agent/sessions` | 用户对话、消息、附件、上下文 | `agent` |
| Agent 任务 | `/agent/tasks` | 分析任务、追溯任务、诊断任务、执行状态 | `agent` |
| 知识空间 | `/agent/knowledge-spaces` | 工艺、质量、设备、报告知识空间 | `agent/knowledge` |
| 知识文档 | `/agent/documents` | 上传 SOP、维修手册、质量标准、接口文档 | `agent/knowledge` |
| 向量索引 | `/agent/embeddings` | 切片、向量ID、索引状态 | `agent/knowledge` |
| 长期记忆 | `/agent/memory` | Agent 记忆、标签、业务对象关联 | `agent/memory` |
| 案例库 | `/agent/cases` | 历史质量问题、维修案例、处理步骤 | `agent/memory` |
| 事件订阅 | `/agent/event-subscriptions` | 订阅安灯、质检失败、设备故障、欠料事件 | `agent/workflow` |
| 工作流 | `/agent/workflows` | 多 Agent 协作流程设计 | `agent/workflow` |
| 建议与预警 | `/agent/recommendations` | Agent 生成建议、依据、风险预警 | `agent` |
| 审批中心 | `/agent/approvals` | 高风险工具调用审批 | `agent/security` |
| 安全策略 | `/agent/policies` | 工具限制、数据范围、输出规则 | `agent/security` |
| 评估用例 | `/agent/evaluations` | Agent 场景评测、得分、问题记录 | `agent/evaluation` |
| 成本统计 | `/agent/costs` | token 用量、模型成本 | `agent/ops` |
| 审计日志 | `/agent/audit-logs` | Agent 调用了什么工具、影响了什么业务对象 | `agent/security` |

Agent 的开发原则：

- 第一阶段只做只读分析，不自动修改业务数据；
- 高风险写操作必须经过 `agent_approval_request`；
- Agent 输出重要建议必须保存依据；
- Agent 不直接绕过权限访问数据；
- Agent 与业务对象通过 `biz_object_type + biz_object_id` 松耦合关联。

## 6. 微信小程序页面规划

微信小程序主要服务现场轻量化操作和管理层移动查看。目录建议：

```text
frontend/wechat-miniprogram/pages/
```

| 页面 | 路径建议 | 必须功能 | 主要用户 |
| --- | --- | --- | --- |
| 登录/绑定 | `/pages/login/index` | 微信身份绑定系统用户、对称加密登录 | 所有移动端用户 |
| 首页 | `/pages/home/index` | 快捷入口、待办、今日任务、异常提醒 | 所有用户 |
| 实时看板 | `/pages/dashboard/index` | 产量、质量、安灯、设备状态 | 管理人员、生产主管 |
| 生产分析 | `/pages/analysis/production` | 产量趋势、良率、不良趋势 | 管理人员 |
| 产品追溯 | `/pages/trace/product` | 扫码产品码查询生产过程 | 管理人员、质检员、产线人员 |
| 工位任务 | `/pages/production/tasks` | 当前工位任务、开工、暂停、结束 | 产线操作工人 |
| 工序作业 | `/pages/production/operation` | SOP、扫码、物料绑定、报工 | 产线操作工人 |
| 异常安灯 | `/pages/andon/create` | 发起生产/非生产安灯 | 产线操作工人 |
| 异常处理 | `/pages/andon/handle` | 查看待处理、确认、处理、关闭 | 管理人员、维修员 |
| 设备状态 | `/pages/equipment/status` | 查看设备状态、故障信息 | 维修员、管理人员 |
| 设备报修 | `/pages/equipment/repair` | 发起报修、查看维修进度 | 产线人员、维修员 |
| 我的 | `/pages/mine/index` | 用户信息、角色、退出登录 | 所有用户 |

小程序不应承载复杂配置功能，复杂主数据维护放在 Web 管理端。小程序只保留高频现场操作。

## 7. 后端模块规划

Spring Boot 后端建议按业务域拆包，目录建议如下：

```text
backend/mes-server/src/main/java/com/yunshu/mes/
  MesApplication.java
  common/
    api/
    exception/
    response/
    pagination/
    validation/
    enums/
    audit/
  config/
  security/
    auth/
    crypto/
    permission/
  system/
  factory/
  masterdata/
  process/
  barcode/
  planning/
  inventory/
  production/
  quality/
  andon/
  equipment/
  traceability/
  reporting/
  integration/
  agent/
```

每个业务模块内部建议统一：

```text
module/
  controller/
  service/
  service/impl/
  repository/ 或 mapper/
  entity/
  dto/
  vo/
  converter/
  enums/
```

协作要求：

- 每个成员只在自己模块下新增业务代码；
- 公共响应、异常、分页、加密、权限等统一放 `common` 和 `security`；
- 不允许模块之间互相直接访问对方数据库 mapper，应通过 service 接口调用；
- 跨模块操作必须有明确服务边界，例如生产报工触发质检任务，应由 `production` 调用 `quality` 的服务接口；
- Agent 调用业务能力时应通过工具服务，不直接绕过业务 service。

## 8. RESTful API 设计原则

接口路径建议按业务域分组：

```text
/api/system/**
/api/factory/**
/api/master-data/**
/api/process/**
/api/barcode/**
/api/planning/**
/api/inventory/**
/api/production/**
/api/quality/**
/api/andon/**
/api/equipment/**
/api/traceability/**
/api/reporting/**
/api/integration/**
/api/agent/**
```

接口返回统一结构：

```json
{
  "code": "SUCCESS",
  "message": "ok",
  "data": {},
  "traceId": "..."
}
```

分页返回统一结构：

```json
{
  "records": [],
  "pageNo": 1,
  "pageSize": 20,
  "total": 100
}
```

接口命名原则：

- 查询列表：`GET /api/{module}/{resources}`
- 查询详情：`GET /api/{module}/{resources}/{id}`
- 新增：`POST /api/{module}/{resources}`
- 修改：`PUT /api/{module}/{resources}/{id}`
- 删除：`DELETE /api/{module}/{resources}/{id}`
- 状态流转：`POST /api/{module}/{resources}/{id}/actions/{action}`
- 导出：`POST /api/{module}/{resources}/export`
- Agent 工具类接口：`POST /api/agent/tools/{toolCode}/execute`

## 9. 对称加密安全设计要求

项目明确要求前后端双端实现对称加密。目录分工如下：

```text
security/crypto-spec/
security/test-vectors/
backend/mes-server/src/main/java/com/yunshu/mes/security/crypto/
frontend/web-admin/src/security/
frontend/wechat-miniprogram/utils/crypto/
```

必须制定统一规范：

- 算法名称，例如 AES-GCM 或 AES-CBC + HMAC；
- 密钥长度；
- IV/Nonce 生成方式；
- 加密字段范围；
- 请求时间戳和重放保护；
- 前端加密输出格式；
- 后端解密异常格式；
- 测试向量，保证 Web、小程序、后端加解密结果一致。

建议优先加密：

- 登录密码；
- 高风险业务请求；
- Agent 工具调用审批信息；
- 外部系统密钥引用不明文入库。

## 10. 数据库设计目录与分组

数据库脚本已经放在项目中：

```text
database/migrations/001_core_schema.sql
database/migrations/002_mes_business_schema.sql
database/migrations/003_agent_schema.sql
database/seeds/001_seed_basic_data.sql
```

Spring Boot/Flyway 版本：

```text
backend/mes-server/src/main/resources/db/migration/V1__core_schema.sql
backend/mes-server/src/main/resources/db/migration/V2__mes_business_schema.sql
backend/mes-server/src/main/resources/db/migration/V3__agent_schema.sql
backend/mes-server/src/main/resources/db/seed/R__seed_basic_data.sql
```

### 10.1 `001_core_schema.sql`

基础主数据和配置表，包含：

- `sys_department`
- `sys_user`
- `sys_role`
- `sys_permission`
- `sys_user_role`
- `sys_role_permission`
- `sys_operation_log`
- `workshop`
- `production_line`
- `workstation`
- `factory_shift`
- `factory_calendar`
- `uom`
- `product`
- `product_spec`
- `material`
- `bom`
- `bom_item`
- `process_step`
- `process_route`
- `process_route_step`
- `product_route`
- `sop_file`
- `defect_reason`
- `barcode_type`
- `barcode_rule`
- `barcode_rule_segment`
- `barcode_template`
- `barcode_template_param`
- `barcode_application_rule`
- `barcode_record`
- `label_print_log`
- `package_binding`

这些表是所有业务的基础。开发时优先完成基础 CRUD。

### 10.2 `002_mes_business_schema.sql`

MES 核心业务表，包含：

- 订单与工单：`customer_order`、`customer_order_item`、`work_order`
- 齐套与派工：`kitting_analysis`、`material_shortage`、`production_task`、`dispatch_task`
- 仓储库存：`warehouse`、`storage_location`、`inventory_batch`、`material_requisition`、`material_issue`、`inventory_transaction`
- 生产执行：`product_sn`、`product_process_state`、`production_report`、`product_material_binding`、`production_completion`、`piece_wage_record`
- 质量管理：`inspection_item`、`quality_standard`、`quality_task`、`quality_record`、`defect_record`、`rework_order`
- 安灯异常：`andon_type`、`andon_reason`、`andon_event`、`andon_notice`、`andon_task`、`andon_result`
- 设备管理：`device_category`、`device`、`maintenance_plan`、`device_inspection_record`、`repair_order`、`repair_record`、`device_oee_daily`、`energy_reading`
- 报表看板：`dashboard_config`、`dashboard_kpi_snapshot`、`report_definition`、`report_run_log`
- 接口集成：`external_system`、`api_endpoint`、`api_call_log`、`sync_log`、`erp_work_order_map`

### 10.3 `003_agent_schema.sql`

Agent 平台表，包含：

- Agent 配置：`agent_profile`、`agent_version`、`agent_capability`
- 模型与提示词：`llm_provider`、`llm_model`、`agent_model_config`、`agent_prompt_template`、`agent_prompt_version`
- 工具与权限：`agent_connector`、`agent_tool`、`agent_tool_permission`
- 会话与任务：`agent_session`、`agent_message`、`agent_task`、`agent_plan`、`agent_action`
- 工具调用与审批：`agent_tool_call`、`agent_approval_request`、`agent_action_result`
- 知识库：`knowledge_space`、`knowledge_source`、`knowledge_document`、`knowledge_chunk`、`knowledge_embedding`
- 检索：`agent_retrieval_query`、`agent_retrieval_result`
- 记忆与案例：`agent_memory`、`agent_case`
- 事件与工作流：`agent_event_subscription`、`agent_event_inbox`、`agent_workflow`
- 建议与风险：`agent_recommendation`、`agent_risk_alert`、`agent_decision_record`
- 安全与治理：`agent_policy`、`agent_guardrail_rule`、`agent_evaluation_run`、`agent_feedback`、`agent_cost_usage`、`agent_audit_log`

### 10.4 数据库协作规则

- 已发布迁移文件不要随意修改，新增字段应新建 `V4__xxx.sql`；
- 开发阶段如果确实需要重构，可以在团队确认后统一重建库；
- 不同成员新增表必须放在对应业务域；
- Agent 相关表统一使用 `agent_`、`knowledge_`、`llm_` 前缀；
- 业务日志、接口日志、Agent 日志后期考虑按时间归档；
- 所有关键业务单据必须有唯一业务编号，如 `work_order_no`、`report_no`、`andon_no`。

## 11. 开发阶段划分

### 第一阶段：基础框架与主数据

目标：项目能启动，用户能登录，基础数据能维护。

开发内容：

- Spring Boot 基础工程；
- Vue3 基础工程；
- 登录、权限、用户角色；
- 产品、物料、BOM、工艺、SOP；
- 车间、产线、工位；
- 数据库迁移跑通。

### 第二阶段：生产主闭环

目标：从订单到工单、派工、发料、报工能够跑通。

开发内容：

- 订单；
- 工单；
- 齐套分析；
- 生产任务单；
- 派工单；
- 库存批次；
- 领料发料；
- 产品 SN；
- 关键物料绑定；
- 生产报工。

### 第三阶段：质量、安灯、设备闭环

目标：生产异常、质检不良、设备维修能闭环。

开发内容：

- 质检任务；
- 质检记录；
- 不良记录；
- 返修单；
- 安灯事件；
- 安灯处理；
- 设备台账；
- 点检；
- 报修维修。

### 第四阶段：报表、追溯、小程序

目标：管理端能看数据，移动端能现场操作。

开发内容：

- 产品追溯；
- 物料批次追溯；
- 工单追溯；
- 产量报表；
- 不良报表；
- 车间看板；
- 小程序任务、追溯、安灯、设备状态。

### 第五阶段：Agent MVP

目标：Agent 能基于真实数据进行只读问答和分析。

开发内容：

- Agent 配置；
- Agent 会话；
- 知识库导入；
- 工单查询工具；
- 欠料查询工具；
- 产品追溯工具；
- 质量不良查询工具；
- 设备维修查询工具。

### 第六阶段：Agent 高级能力

目标：Agent 能主动预警、生成建议、走审批后调用写操作。

开发内容：

- 事件订阅；
- 多 Agent 工作流；
- 风险预警；
- 推荐建议；
- 高风险动作审批；
- 审计日志；
- Agent 评估与成本统计。

## 12. 多人协作规范

项目组长先搭建整体框架，其他成员按模块扩展。必须遵守以下规则：

### 12.1 模块边界

每个成员负责一个或多个模块，例如：

- 成员 A：基础主数据、工艺、BOM；
- 成员 B：订单、工单、排产、派工；
- 成员 C：仓储、库存、领料发料；
- 成员 D：生产执行、报工、条码；
- 成员 E：质量、返修；
- 成员 F：安灯、设备；
- 成员 G：Agent、知识库、工具调用。

成员只能在自己模块目录下扩展，公共能力必须提 PR 或经组长确认。

### 12.2 前端协作

- 每个业务域一个 views 子目录；
- 每个页面只调用本模块 API，不直接拼接其他模块逻辑；
- 公共组件放 `frontend/web-admin/src/components`；
- 枚举和接口类型放 `frontend/shared`；
- 路由按业务域懒加载；
- 页面权限由后端返回菜单和按钮权限控制。

### 12.3 后端协作

- 每个模块有独立 controller、service、repository、entity、dto、vo；
- 跨模块调用只能通过 service 接口；
- 不允许 controller 直接调用 mapper；
- 不允许 Agent 绕过 service 直接写业务表；
- 所有状态流转必须封装为明确方法；
- 复杂业务必须写事务。

### 12.4 数据库协作

- 不直接修改别人负责的表；
- 新字段先在文档中说明用途；
- 新迁移文件按版本递增；
- 表名、字段名使用英文小写下划线；
- 金额、数量使用 DECIMAL；
- 时间字段使用 DATETIME(3)；
- 逻辑删除字段统一 `is_deleted`；
- 乐观锁字段统一 `version`。

### 12.5 Agent 协作

- Agent 工具先注册到 `agent_tool`；
- 工具输入输出要有 schema；
- 高风险工具必须配置审批；
- Agent 回答不能只给结论，重要建议必须保存依据；
- Agent 知识库文档要能追溯来源；
- Agent 产生的业务影响必须写审计日志。

## 13. 页面与数据库对应关系

| 页面域 | 主要数据库表 |
| --- | --- |
| 系统管理 | `sys_user`、`sys_role`、`sys_permission`、`sys_operation_log` |
| 工厂资源 | `workshop`、`production_line`、`workstation`、`factory_shift`、`factory_calendar` |
| 产品物料 | `product`、`product_spec`、`material`、`bom`、`bom_item` |
| 工艺 SOP | `process_step`、`process_route`、`process_route_step`、`product_route`、`sop_file` |
| 条码应用 | `barcode_type`、`barcode_rule`、`barcode_template`、`barcode_record`、`package_binding` |
| 订单工单 | `customer_order`、`customer_order_item`、`work_order` |
| 齐套派工 | `kitting_analysis`、`material_shortage`、`production_task`、`dispatch_task` |
| 仓储库存 | `warehouse`、`storage_location`、`inventory_batch`、`material_requisition`、`material_issue`、`inventory_transaction` |
| 生产执行 | `product_sn`、`product_process_state`、`production_report`、`product_material_binding` |
| 质量检验 | `inspection_item`、`quality_standard`、`quality_task`、`quality_record`、`defect_record`、`rework_order` |
| 安灯异常 | `andon_type`、`andon_reason`、`andon_event`、`andon_notice`、`andon_task`、`andon_result` |
| 设备运维 | `device_category`、`device`、`maintenance_plan`、`device_inspection_record`、`repair_order`、`repair_record` |
| 报表看板 | `dashboard_config`、`dashboard_kpi_snapshot`、`report_definition`、`report_run_log` |
| 接口集成 | `external_system`、`api_endpoint`、`api_call_log`、`sync_log`、`erp_work_order_map` |
| Agent | `agent_profile`、`agent_session`、`agent_task`、`agent_tool_call`、`knowledge_document`、`agent_recommendation`、`agent_audit_log` |

## 14. 给后续 Codex/Agent 的开发指令

后续任何 Codex 或 Agent 接手项目时，必须先阅读本文档，再阅读：

```text
MES/README.md
MES/database/README.md
MES/database/docs/implementation_plan.md
MES/database/migrations/
```

开发时必须遵守：

- 不随意改变顶层目录结构；
- 不把所有代码写在一个模块；
- 新功能必须归属到明确业务域；
- 先查已有数据库表，再决定是否新增表；
- 新页面必须补充路由、权限、API、数据库映射；
- 修改数据库必须新增迁移脚本；
- Agent 写操作必须审批；
- 保持 Windows 和 Mac 双系统可开发。

## 15. 当前优先级

当前脚手架已经完成：

1. Spring Boot 工程、统一响应、`traceId`、健康检查；
2. Vue3 管理端、模块路由、侧边栏菜单、各业务域骨架页；
3. MySQL 三份 migration 与一份 seed，且 standalone / Flyway 双轨同步；
4. `dashboard` 与 `system` Mock API，前端可优先展示 API 数据并在失败时回退骨架数据；
5. `ops/scripts/verify-local.ps1` / `verify-local.sh` 本地验证脚本。

当前最优先的实际开发路线：

1. 以 `system` 为样板，把 Mock service 替换为 JDBC repository，但不改 VO 契约；
2. 跑通 MySQL + Flyway 本地库，验证 seed 用户/角色与页面展示一致；
3. 复制 `system` 模式到 `masterdata`、`factory`、`planning`；
4. 再逐步扩展库存、生产报工、质量、安灯、设备和 Agent。

本地启动顺序：

```bash
cd backend/mes-server && mvn spring-boot:run
cd frontend/web-admin && npm run dev
```

默认访问：

```text
http://127.0.0.1:5173/dashboard/workbench
http://127.0.0.1:5173/system/users
```

只要保持这个结构，项目组长可以先搭好总框架，其他成员可以独立在对应模块中开发，互不干扰，同时最终能够整合成一个统一、可扩展、可维护的 MES 产品。
