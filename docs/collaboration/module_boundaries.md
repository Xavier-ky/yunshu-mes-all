# 模块边界

本项目按业务域拆分。成员开发时应优先在自己的业务域目录内新增文件，减少公共文件冲突。

## 后端目录边界

每个后端模块固定结构：

```text
controller/
service/
repository/
entity/
dto/
vo/
converter/
enums/
```

规则：

- `controller` 只处理 HTTP 入参、权限和响应，不写业务流程。
- `service` 写业务编排，是跨模块调用的唯一入口。
- `repository` 只负责数据访问。
- `entity` 对应数据库表结构。
- `dto` 表示请求参数。
- `vo` 表示响应数据。
- `converter` 负责 entity/dto/vo 转换。
- `enums` 放本模块枚举。

禁止：

- 一个模块的 controller 直接调用另一个模块的 repository。
- 在业务模块里直接改 `common` 的统一响应结构。
- 在业务模块里写全局配置。

## 前端目录边界

前端公共目录：

```text
frontend/web-admin/src/components
frontend/web-admin/src/layouts
frontend/web-admin/src/router
frontend/web-admin/src/stores
frontend/web-admin/src/api
frontend/web-admin/src/utils
frontend/web-admin/src/styles
frontend/web-admin/src/constants
frontend/web-admin/src/types
```

业务页面目录：

```text
frontend/web-admin/src/views/system
frontend/web-admin/src/views/factory
frontend/web-admin/src/views/master-data
frontend/web-admin/src/views/process
frontend/web-admin/src/views/barcode
frontend/web-admin/src/views/planning
frontend/web-admin/src/views/inventory
frontend/web-admin/src/views/production
frontend/web-admin/src/views/quality
frontend/web-admin/src/views/andon
frontend/web-admin/src/views/equipment
frontend/web-admin/src/views/traceability
frontend/web-admin/src/views/reporting
frontend/web-admin/src/views/integration
frontend/web-admin/src/views/agent
```

规则：

- 模块页面优先只改自己的 `views/<domain>`。
- 模块接口优先新增到 `src/api/<domain>.js`。
- 模块常量可新增到 `src/constants/<domain>.js`。
- 如果需要新增菜单路由，先在模块页完成，再由项目组长集中更新 router 和菜单。

## 数据库边界

- 历史 migration 不改，只新增。
- migration 命名使用 `V{number}__{module}_{change}.sql`。
- 一个 migration 尽量只服务一个业务域。
- 种子数据按模块集中放在 `db/seed` 或独立 seed 文件中。

## 公共能力变更规则

修改以下目录前必须说明原因：

```text
backend/mes-server/src/main/java/com/yunshu/mes/common
backend/mes-server/src/main/java/com/yunshu/mes/config
backend/mes-server/src/main/java/com/yunshu/mes/security
frontend/web-admin/src/router
frontend/web-admin/src/layouts
frontend/web-admin/src/stores
frontend/web-admin/src/styles
```
