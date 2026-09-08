# 云枢智造 MES 后端

Spring Boot RESTful API 服务，当前阶段提供标准工程骨架、统一响应、全局异常、Flyway 数据库迁移接入和少量 mock API。

## 启动

```bash
cd backend/mes-server
mvn spring-boot:run
```

默认读取 `application-dev.yml`，`FLYWAY_ENABLED` 默认为 `false`，便于未准备 MySQL 时先启动 mock API 骨架。

需要连接 MySQL 并执行 Flyway 时可启用 `local` profile：

```bash
cp src/main/resources/application-local.example.yml src/main/resources/application-local.yml
DB_URL='jdbc:mysql://localhost:3306/fan_mes?createDatabaseIfNotExist=true&serverTimezone=Asia/Shanghai' \
DB_USERNAME=root \
DB_PASSWORD=your_password \
SPRING_PROFILES_ACTIVE=local \
mvn spring-boot:run
```

## 示例接口

- `GET /api/health`
- `GET /api/system/users`
- `GET /api/system/roles`
- `GET /api/dashboard/summary`

## 模块协作

业务域按 `system/factory/masterdata/process/barcode/planning/inventory/production/quality/andon/equipment/traceability/reporting/integration/agent` 拆包。每个模块继续按 `controller/service/repository/entity/dto/vo/converter/enums` 扩展。

`application-local.yml` 属于个人本地配置，默认不提交。团队共享模板使用 `application-local.example.yml`。
