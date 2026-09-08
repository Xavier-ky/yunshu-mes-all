# 电风扇 MES 数据库落地方案

本目录是项目开发用的数据库设计，不是课程报告稿。

## 技术假设

- 数据库：MySQL 8.0+
- 存储引擎：InnoDB
- 字符集：utf8mb4
- 迁移方式：可以手动执行，也可以接入 Flyway、Liquibase 或后端框架自带 migration
- 主键策略：当前使用 `BIGINT UNSIGNED AUTO_INCREMENT`，后续如果上分布式服务，可替换为雪花 ID
- Agent 向量：MySQL 只保存 `vector_store_id` 和 `vector_id`，实际向量建议放向量数据库或支持向量检索的组件

## 文件说明

```text
database/
  migrations/
    001_core_schema.sql       -- 基础主数据：组织权限、车间产线、产品物料、BOM、工艺、条码
    002_mes_business_schema.sql -- 业务交易：订单、工单、库存、报工、质检、安灯、设备、接口、看板
    003_agent_schema.sql      -- Agent 平台：配置、会话、任务、工具调用、知识库、记忆、审批、审计
  seeds/
    001_seed_basic_data.sql   -- 开发种子数据
  docs/
    implementation_plan.md    -- 实施建议
```

## 执行顺序

手动执行：

```bash
mysql -u root -p < database/migrations/001_core_schema.sql
mysql -u root -p < database/migrations/002_mes_business_schema.sql
mysql -u root -p < database/migrations/003_agent_schema.sql
mysql -u root -p < database/seeds/001_seed_basic_data.sql
```

Spring Boot / Flyway 执行：

```text
backend/mes-server/src/main/resources/db/migration/V1__core_schema.sql
backend/mes-server/src/main/resources/db/migration/V2__mes_business_schema.sql
backend/mes-server/src/main/resources/db/migration/V3__agent_schema.sql
backend/mes-server/src/main/resources/db/seed/R__seed_basic_data.sql
```

两套 SQL 文件内容必须保持同步。提交前运行：

```bash
./ops/scripts/verify-local.sh
```

Windows PowerShell：

```powershell
.\ops\scripts\verify-local.ps1
```

## 当前脚手架约定

- 默认本地后端使用 `dev` profile，Flyway 默认关闭，可先不连 MySQL 启动 Mock API。
- `system` 模块的 Mock 用户/角色编码与 `001_seed_basic_data.sql` 保持一致，例如 `MANAGER`、`PROD_SUPERVISOR`、`qc01`。
- 后续接入真实 JDBC repository 时，优先复用现有 `UserVO` / `RoleVO` 字段，不在前端重复维护第二套角色编码。

## 第一阶段建议先开发的核心闭环

真实开发时不要一开始把所有功能都做满。建议第一阶段只实现这条闭环：

1. 用户、角色、部门
2. 产品、物料、BOM、工艺路线、SOP
3. 客户订单、生产工单、齐套分析、生产任务单、派工单
4. 仓库、库存批次、领料单、发料单、库存流水
5. 产品 SN、关键物料绑定、生产报工
6. 质检任务、质检记录、不良记录、返修单
7. 安灯事件、安灯任务、安灯处理结果
8. 设备台账、设备报修、维修记录
9. 追溯查询接口

第二阶段再开发条码模板、包装层级、OEE、能源分析、计件工资、微信小程序、复杂报表。

Agent 可以与第一阶段并行做 MVP，但先只开放只读工具：

- 查询工单
- 查询欠料
- 查询产品追溯
- 查询质量不良
- 查询设备维修记录

高风险写操作，如更新工单状态、关闭安灯、创建维修单，必须走 `agent_approval_request`。

## 关键设计原则

- 核心业务表使用强外键，保证数据一致性。
- Agent 与业务数据使用 `biz_object_type + biz_object_id` 松耦合关联，避免 Agent 表结构强依赖每一张业务表。
- 日志类、消息类、采集类表后续应按时间分区或归档。
- 密码、API Key、模型 Key 不直接明文入库，使用 hash 或 `secret_ref`。
- 所有状态字段先使用 `VARCHAR`，便于开发早期快速迭代；业务稳定后可引入字典表或枚举约束。
