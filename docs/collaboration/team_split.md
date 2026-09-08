# 四人分工建议

本分工适合课程项目或项目制开发：项目组长承担约 50% 的主干和公共能力，其余三位成员各自负责独立业务域，尽量不互相改文件。

## 角色 1：项目组长

建议负责人：你。

职责：

- 总体架构、模块边界和合并把关。
- 后端公共能力：统一响应、异常、配置、安全、基础权限。
- 前端公共能力：布局、路由、request、store、样式规范。
- 数据库 migration 编号和 Flyway 接入。
- 系统管理、Agent 中心、工作台大屏和最终集成。

主要目录：

```text
backend/mes-server/src/main/java/com/yunshu/mes/common
backend/mes-server/src/main/java/com/yunshu/mes/config
backend/mes-server/src/main/java/com/yunshu/mes/security
backend/mes-server/src/main/java/com/yunshu/mes/system
backend/mes-server/src/main/java/com/yunshu/mes/dashboard
backend/mes-server/src/main/java/com/yunshu/mes/agent
frontend/web-admin/src/layouts
frontend/web-admin/src/router
frontend/web-admin/src/api
frontend/web-admin/src/stores
frontend/web-admin/src/styles
frontend/web-admin/src/views/system
frontend/web-admin/src/views/dashboard
frontend/web-admin/src/views/agent
```

## 角色 2：成员 A

建议方向：基础资料和工艺建模。

主要模块：

```text
backend/mes-server/src/main/java/com/yunshu/mes/factory
backend/mes-server/src/main/java/com/yunshu/mes/masterdata
backend/mes-server/src/main/java/com/yunshu/mes/process
backend/mes-server/src/main/java/com/yunshu/mes/barcode
frontend/web-admin/src/views/factory
frontend/web-admin/src/views/master-data
frontend/web-admin/src/views/process
frontend/web-admin/src/views/barcode
```

交付重点：

- 工厂、车间、产线、工位基础数据。
- 产品、物料、BOM。
- 工艺路线、工序、SOP。
- 条码规则和基础生成页面。

## 角色 3：成员 B

建议方向：计划、库存和生产执行。

主要模块：

```text
backend/mes-server/src/main/java/com/yunshu/mes/planning
backend/mes-server/src/main/java/com/yunshu/mes/inventory
backend/mes-server/src/main/java/com/yunshu/mes/production
backend/mes-server/src/main/java/com/yunshu/mes/andon
frontend/web-admin/src/views/planning
frontend/web-admin/src/views/inventory
frontend/web-admin/src/views/production
frontend/web-admin/src/views/andon
```

交付重点：

- 订单、工单、齐套、派工。
- 仓库、库存、领退料、出入库。
- 报工、SN、物料绑定。
- 安灯呼叫、响应和处理闭环。

## 角色 4：成员 C

建议方向：质量、设备、追溯和报表。

主要模块：

```text
backend/mes-server/src/main/java/com/yunshu/mes/quality
backend/mes-server/src/main/java/com/yunshu/mes/equipment
backend/mes-server/src/main/java/com/yunshu/mes/traceability
backend/mes-server/src/main/java/com/yunshu/mes/reporting
backend/mes-server/src/main/java/com/yunshu/mes/integration
frontend/web-admin/src/views/quality
frontend/web-admin/src/views/equipment
frontend/web-admin/src/views/traceability
frontend/web-admin/src/views/reporting
frontend/web-admin/src/views/integration
```

交付重点：

- 质检任务、缺陷、不良、返工、放行。
- 设备台账、点检、维修、OEE。
- 产品追溯查询。
- 报表分析和外部系统接口占位。

## 协作原则

- 每个人先完成自己模块内的 controller/service/dto/vo/entity 页面骨架。
- 公共接口和跨模块依赖由项目组长统一抽象。
- 数据库新增表必须先写清楚模块归属，再由项目组长统一安排 migration 版本。
