# 云枢智造 MES — AI 协作指南

> 最后更新：2026-07-10

---

## 1. 项目架构

```
MES/
├── backend/mes-server/        # Spring Boot 3.3.6 + JDK 17 + Maven
│   └── src/main/java/com/yunshu/mes/
│       ├── common/            # ApiResponse, exception, enums
│       ├── system/            # User, Role, Department, OperationLog
│       ├── factory/           # Workshop, Line, Workstation, Shift
│       ├── masterdata/        # Product, Material, BOM, BOM Item, ProductRoute
│       ├── process/           # ProcessStep, ProcessRoute, RouteStep
│       ├── planning/          # Order, WorkOrder, ProductionTask, DispatchTask, Kitting
│       ├── production/        # ProductSN, Report, Completion, MaterialBinding
│       ├── quality/           # QualityTask, Defect, Rework, Record, Release
│       ├── inventory/         # Warehouse, Batch, Transaction, Requisition, Issue, Return
│       ├── equipment/         # Device, Repair, Maintenance, Inspection
│       ├── andon/             # AndonType, Reason, Event, Task, Notice
│       ├── barcode/           # Type, Rule, Template
│       ├── traceability/      # Reverse trace, SN lookup
│       ├── reporting/         # Report definitions
│       ├── dashboard/         # Dashboard summary
│       ├── integration/       # External systems
│       └── agent/             # AI chat (future)
├── frontend/web-admin/        # Vue 3 + Vite + Pinia + Vue Router
│   └── src/
│       ├── api/               # Axios modules: system.js, planning.js, factory.js, etc.
│       ├── components/        # CrudManager.vue (reusable CRUD), IconGlyph
│       ├── layouts/           # NewAdminLayout.vue (primary), AdminLayout.vue (legacy)
│       ├── router/            # index.js — all routes with allowedRoles
│       ├── stores/            # app.js — Pinia store
│       ├── styles/            # theme.css, main.css, chrome.css, admin-glass.css
│       └── views/             # See section 2
├── database/migrations/       # Standalone SQL (dual-track with Flyway)
├── docs/                      # role-views.md, phase1-archive.md, crud-test-report.md
└── start.cmd                  # Interactive control panel
```

---

## 2. 前端页面清单

### Admin (MANAGER) — 5 pages
| Route | Component | Type |
|-------|-----------|------|
| `/app` | DashboardHome.vue | Role-specific dashboard |
| `/app/system/users` | UserManage.vue | Left-list + right-detail + edit toggle |
| `/app/system/departments` | DepartmentManage.vue | Same pattern |
| `/app/system/logs` | SystemLogs.vue | Operation log table |

### Production Supervisor (PROD_SUPERVISOR) — 16 pages
| Route | Component | Pattern |
|-------|-----------|--------|
| Dashboard, Orders, WorkOrders, Tasks, Dispatch, Kitting, Workshops, Lines, Stations, Shifts, Products, Materials, BOMs, Routes, Reports, Trace, Andon | Various | Mixed |

### Other roles — 12 pages
Warehouse: Dashboard, Batches, Requisitions, Issues, Returns, Warehouses, Andon
Quality: QualityManage (Records+Release), Andon
Equipment: DeviceManage, Maintenance, LineMonitor, Andon
Operator: Dashboard, OperatorWork, Andon

---

## 3. 后端代码规范

### Repository 模式
```java
@Repository
public class XxxRepository {
    private final ObjectProvider<JdbcTemplate> jdbcTemplateProvider;
    // JdbcTemplate CRUD methods
    private JdbcTemplate requireJdbc() {
        JdbcTemplate j = jdbcTemplateProvider.getIfAvailable();
        if (j == null) throw new DataAccessResourceFailureException("no jdbc");
        return j;
    }
}
```

### Controller 模式（新增的简单 CRUD）
```java
@RestController
@RequestMapping("/api/module")
public class XxxController {
    private final JdbcTemplate jdbc;
    @GetMapping public ApiResponse<List<Map<String,Object>>> list(HttpServletRequest req) { ... }
    @PostMapping public ApiResponse<Long> create(@RequestBody Map<String,Object> body, HttpServletRequest req) { ... }
    @PutMapping("/{id}") public ApiResponse<Void> update(@PathVariable Long id, @RequestBody Map<String,Object> body, HttpServletRequest req) { ... }
    @DeleteMapping("/{id}") public ApiResponse<Void> delete(@PathVariable Long id, HttpServletRequest req) { ... }
}
```

### 关键注意事项
- **不要用 `.fail(null)` 或 `.fail("msg", "msg", null)`** — 必须用 `.fail(code, msg, request)` 或 `.fail(code, msg, traceId)`
- **新增 Controller 避免与已有路径冲突** — 已有 `/api/production` 时，新功能用 `/api/production/completion-ops` 等独立路径
- **外键字段发送 ID 值**，不要发送 name 字符串
- **数据库列名以 SQL  schema 为准**，不要假设 Java 驼峰名

---

## 4. 前端代码规范

### 统一 CRUD 页面模式
所有列表类 CRUD 页面统一采用：标题+搜索栏水平对齐 → 左列表(380px)+右详情(弹性宽度)+编辑 toggle 开关

```vue
<template>
  <div class="x-root">
    <div class="x-top"><div class="x-hero"><h2>Title</h2><p>Desc</p></div>
      <div class="x-bar"><!-- search + add button --></div>
    </div>
    <div class="x-main">
      <div class="x-left"><!-- list of items --></div>
      <div class="x-right"><!-- detail panel with edit toggle --></div>
    </div>
  </div>
</template>
```

### 下拉选项必须设置深色字体
```css
select, select option { color: #1a1a1a; background: #fff; }
input, select { background: #fafafa; color: #1a1a1a; }
```

### API 调用统一使用 request 模块
```js
import { request } from "@/api/request";
// Always unwrap: const data = (await request.get("/path"))?.data || [];
```

### JWT Token 自动携带
`request.js` 拦截器已自动在请求头添加 `Authorization: Bearer <token>`。

---

## 5. 数据库

- 本地 MySQL: `localhost:3306/fan_mes`, root/`051002sry`（见 `application-local.yml`）
- 163 张表（含 Agent 平台 54 张），核心业务表约 70 张
- Flyway 已禁用（`application-local.yml` 中 `enabled: false`）
- 种子数据见 `docs/seed-data.md`

---

## 6. 启动与运维

```cmd
start.cmd    # 交互控制台
  [1] Start All (Backend :8080 + Frontend :5173, frontend waits 30s)
  [2] Restart Backend Only
  [3] Restart Frontend Only
  [4] Health Check
  [0] Exit
```

### 测试账号

主账号与别名均可登录（别名由 `R__seed_story_06_calendar_system.sql` 写入）：

| User | 别名 | Pass | Role |
|------|------|------|------|
| admin | — | admin123 | MANAGER |
| supervisor01 | supervisor | 123456 | PROD_SUPERVISOR |
| warehouse01 | warehouse | 123456 | WAREHOUSE_CLERK |
| qc01 | quality | 123456 | QUALITY_INSPECTOR |
| repair01 | repair | 123456 | EQUIPMENT_MAINTAINER |
| worker01 | worker | 123456 | LINE_OPERATOR |
| tester | — | 123456 | TESTER (all modules) |

### JWT 鉴权
- `application.yml` 中 `JWT_ENFORCE: true`（已开启）
- `/api/auth/login` 和 `/api/health` 免鉴权
- 其余 `/api/*` 需要 `Authorization: Bearer <token>`

---

## 7. 关键文档

| 文档 | 内容 |
|------|------|
| `docs/role-views.md` | 6 角色视图分配方案 |
| `docs/phase1-archive.md` | Phase 1 完成度报告 |
| `docs/crud-test-report.md` | 全局 CRUD 压测报告（100%通过） |
| `docs/data-gaps.md` | 29 项后端缺失清单 |
| `docs/seed-data.md` | 种子数据 SQL 脚本 |
| `docs/ureport-designer.md` | UReport3 报表设计器集成与首次构建 |

---

## 8. yunshu-ui 页面移植标准工作流（每次必按此执行）

> 目标：保留 Yunshu **黑色侧栏 + 顶栏**，内容区 **1:1 还原业务页面**；数据走 `fan_mes` + `compat` 适配层。**仅对照工程内已有** [`frontend/web-admin/src/yunshu-ui/`](frontend/web-admin/src/yunshu-ui/) 与 compat 后端实现。
>
> **硬性规则：** 禁止在项目根目录或工作区内创建、下载、解压任何外部参考仓库；禁止从工程外拷贝源码进仓库。启动脚本会自动删除误入的外部目录。

### 8.1 移植前确认（5 分钟）

| 项 | 说明 |
|----|------|
| 源页面 | 参考 UI 路径，如 `views/mes/pro/workorder/index.vue` |
| 目标路由 | Yunshu 路径，如 `/app/planning/work-orders` |
| 依赖 API | 列出 `@/api/mes/...` 全部端点 |
| 依赖组件 | Pagination、DictTag、Treeselect、Gantt、dhtmlx 等 |
| 数据库 | 需扩展 `fan_mes` 哪些表/字段，是否需种子数据 |

### 8.2 阶段 A — 路由与壳层（先做）

1. **`router/index.js`**：懒加载 `@/yunshu-ui/...`，`meta: { yunshuUi: true, allowedRoles: [...] }`
2. **`NewAdminLayout.vue`**：已有 `yunshuUi` → 隐藏 page-head、`tc-content--yunshu-ui`、外包 `YunshuUiHost`
3. **侧栏**：`constants/nav-modules.js` 指向新路由
4. **禁止**把参考源原路径（`/mes/pro/...`）写进 Yunshu 路由

### 8.3 阶段 B — 前端拷贝与 Vue3 改造

**目录规范**（全部落在 Yunshu 工程内）：

```
frontend/web-admin/src/
├── yunshu-ui/
│   ├── api/mes/...              # baseURL 走 @/yunshu-ui/api/request.js
│   ├── components/              # Pagination、DictTag、RightToolbar、Select 等
│   ├── compat/legacyElement.js
│   ├── lib/                     # UMD 兼容层（如 dhtmlxgantt.js）
│   ├── plugins/                 # modal、tab
│   ├── pro/                     # 业务页面
│   ├── styles/yunshu-ui-content.scss  # 仅 .yunshu-ui-root 内生效
│   └── utils/
├── components/yunshu-ui/YunshuUiHost.vue
└── styles/yunshu-ui.css         # 布局 / 表格 / 分页统一样式
```

**拷贝后必改 checklist：**

- [ ] `@/api/mes/` → `@/yunshu-ui/api/mes/`
- [ ] `slot-scope` → `#default="scope"`
- [ ] Pagination：`:page` + `@update:page`、`:limit` + `@update:limit`
- [ ] 删除 `Vue.set`，直接赋值
- [ ] 路由跳转改为 `/app/...`
- [ ] 列表根节点 `class="app-container"`
- [ ] 表格：`class="yunshu-data-table"` + `stripe` + `border`
- [ ] 列：`min-width` + `show-overflow-tooltip`
- [ ] 操作列：`class-name="col-actions"` + `<div class="yunshu-row-actions">`

**禁止：**

- 全局 import 参考源的 `index.scss` / `sidebar.scss`
- 把移植页嵌进 Yunshu 原生 CRUD 壳（除非用户明确要求）

### 8.4 阶段 C — 运行时（YunshuUiHost）

一次性注册（新页面通常**不用再改**）：

- Element Plus **`size="default"`**
- Pagination、RightToolbar、DictTag、dict、`$modal`、`$tab`
- `hasPermi`、`legacyElement`（`el-icon-*` 兼容）
- 样式：`element-plus.css`、`yunshu-ui-content.scss`、`yunshu-compat.scss`、**`yunshu-ui.css`**

路由带 `meta.yunshuUi: true` 即自动生效。

### 8.5 阶段 D — 后端 compat

**包路径**：`backend/.../{module}/compat/`

**约定：**

- URL 与参考 API 一致：`/api/mes/pro/workorder/list` 等
- 响应：`MesApiResponse.table()` / `MesApiResponse.ok()`（**禁止** `.fail(null)`）
- Java 字段用 API 驼峰，SQL 用 Yunshu 列名
- 工位 JOIN：`workstation.station_id`（非 `workstation_id`）
- 类名使用 `Compat` 前缀（如 `CompatProcessController`）；避免外部框架命名

**步骤：** 读参考 Controller → JdbcTemplate Repository 实现同等语义 → JWT curl 验证 → 复杂接口（甘特等）**对齐原版逻辑**，不擅自改参数。

### 8.6 阶段 E — 数据库与种子

1. 缺列 → `db/migration/Vn__*.sql`
2. 演示数据 → seed 脚本；甘特页需 **工单 + dispatch_task / production_task（含 start_time）**

### 8.7 阶段 F — 特殊依赖

| 依赖 | 做法 |
|------|------|
| dhtmlx-gantt@7 | `@/yunshu-ui/lib/dhtmlxgantt.js`；**勿** vite alias 到 `.js`；`optimizeDeps.exclude` |
| vue3-treeselect | 用 `@zanmato/vue3-treeselect`，禁用 Vue2 版 |
| sass | `npm i -D sass-embedded` |
| Vite 504/白屏 | 删 `node_modules/.vite`，`npm run dev -- --force` |

### 8.8 阶段 G — UI 规范（yunshu-ui.css）

- [ ] flex 填满视口，分页 `margin-top: auto` 贴底
- [ ] 甘特 `clamp(260px, 34vh, 420px)`，不写死像素高度
- [ ] 表头/单元格单行 + 省略号 + tooltip
- [ ] 操作按钮 `yunshu-row-actions` 横排不换行
- [ ] 侧栏/顶栏未被内容区样式污染

### 8.9 阶段 H — 验收清单

```text
[ ] npm run build 通过
[ ] 角色登录后可打开（如 supervisor/123456）
[ ] 列表 / 搜索 / 分页 / 弹窗 CRUD 正常
[ ] Network 无 404/500，控制台无红屏
[ ] 与参考原页对比：按钮、列、弹窗字段无缺失
```

**已验收参考：**

- `/app/planning/work-orders` — 工单中心
- `/app/planning/scheduling` — 排产派工（甘特）
- `/app/planning/scheduling/ganttedit` — 甘特编辑

### 8.10 踩坑速查表

| 问题 | 原因 | 正确做法 |
|------|------|----------|
| 白屏 / `export named 'gantt'` | dhtmlx UMD | `yunshu-ui/lib/dhtmlxgantt.js` |
| CSS 路径错误 | alias 到 .js | 去掉 alias，完整 package 路径 |
| 504 Optimize Dep | Vite 缓存 | 清 `.vite` + `--force` |
| 侧栏变黑 | 全局 SCSS 泄漏 | 仅 `yunshu-ui-content.scss` |
| 甘特无数据 | API 必填 workorderId | 对齐筛选 → project+task |
| `$refs` 报错 | created 过早 | `$nextTick` + `?.` |
| 表头/内容换行 | 无 min-width | `yunshu-data-table` + nowrap |
| 分页悬空 | 未 flex 填满 | `yunshu-ui.css` |
| 字体偏小 | Element small | YunshuUiHost `default` |

### 8.11 单次移植典型 touch 文件

| 层 | 新增/修改 |
|----|-----------|
| 页面 | `yunshu-ui/pro/<module>/` |
| API | `yunshu-ui/api/mes/` |
| 样式 | 优先只改 `styles/yunshu-ui.css` |
| 路由 | `router/index.js` + `nav-modules.js` |
| 后端 | `*/compat/*` |
| 库表 | `db/migration/` + seed |

