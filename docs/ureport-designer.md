# UReport3 报表设计器

云枢 MES 使用 [UReport3](https://gitee.com/haron_1_0/ureport3)（Jakarta / Spring Boot 3 兼容 fork）提供可视化报表设计与预览。

## 首次构建

```powershell
# 1. 构建 UReport3 并安装到本地 Maven 仓库
.\scripts\install-ureport3.ps1

# 2. 启动 mes-server（local profile）
cd backend\mes-server
$env:SPRING_PROFILES_ACTIVE='local'
.\mvnw.cmd spring-boot:run
```

源码 vendored 于 `third-party/ureport3/`。

## 访问地址

| 功能 | URL |
|------|-----|
| 可视化设计器（iframe） | `/app/analytics/report-designer` → `/ureport/designer?_u=mysql:...` |
| 报表预览 | `/ureport/preview?_u=mysql:报表文件名.ureport.xml` |
| Excel 导出（取数） | `/ureport/excel?_u=mysql:报表文件名.ureport.xml` |
| 报表文件 CRUD | `/api/ureportM/*` |
| 档案元数据导出 | `POST /api/ureportM/export` |

设计器页顶部提供**模板选择栏**，默认打开「通用表格」模板，无需手动从数据库打开文件。

### 嵌入布局说明

云枢将 UReport 设计器嵌入 iframe，并通过 `ureport-iframe-patch.js` 做以下适配：

- **打印竖线默认隐藏**（UReport 原版的 A4 右边界参考线 `.ureport-right-hr-for-print`）
- **左表格 + 右属性/数据源面板** 左右分栏，填满 iframe 高度
- 禁用属性面板拖拽，避免误操作导致面板飘移

### 模板字段中文化

数据类模板（产量/质量/工单）的 dataset 名、SQL 字段别名、Bean 输出字段均使用中文，设计器绑定格显示如 `产量数据.select(工单号)`；预览时状态码会翻译为中文（待生产、已下达等）。

预览示例：

```
/ureport/preview?_u=mysql:MES通用表格模板.ureport.xml
/ureport/preview?_u=mysql:产量统计报表.ureport.xml
/ureport/preview?_u=mysql:质量检验报告.ureport.xml
/ureport/preview?_u=mysql:生产工单打印模版.ureport.xml&id=1&code=WO-001
```

## 预置 MES 模板

启动时 `UreportTemplateInitializer` 将 `classpath:ureport-templates/*.ureport.xml` 同步到 `ureport_file_tbl`：

| 库中状态 | 行为 |
|----------|------|
| 文件名不存在 | 安装 classpath XML |
| 存在且 `content_` 为空 | **自动补写** classpath XML |
| 存在且 content 非空 | **不覆盖**（保护用户改稿）；仅当 `mes.ureport.force-template-upgrade=true` 时强制覆盖 |

| 文件名 | 用途 | 数据源 |
|--------|------|--------|
| `MES通用表格模板.ureport.xml` | 标题 + 表头 + 空白行，手工填内容 | 无 |
| `产量统计报表.ureport.xml` | 工单产量列表 | 内置数据源 SQL（dataset：`产量数据`） |
| `质量检验报告.ureport.xml` | IQC 来料检验列表 | 内置数据源 SQL（dataset：`质检数据`） |
| `生产工单打印模版.ureport.xml` | 工单打印单 | Spring Bean `mesReportBean`（dataset：`工单数据`） |

### 设计器 `_u` 参数

UReport 设计器读取 URL 参数 `_u` 自动加载报表：

```
/ureport/designer?_u=mysql:MES通用表格模板.ureport.xml
```

格式与预览一致：`mysql:` + 文件名（需 URL 编码）。无 `_u` 时加载内置空 4×3 模板。

### 只填内容即可

1. 打开 `/app/analytics/report-designer`，默认已是「通用表格」
2. 在格子里直接输入文字或调整样式
3. 点击工具栏「预览」查看效果
4. 保存后写入 `ureport_file_tbl`，预览 URL 不变

### 报表档案

设计器内「报表档案」抽屉支持搜索、新增、修改、删除、导出：

- **选中一条**点导出 → 打开 `/ureport/excel?_u=mysql:…`（按模板取数导出 Excel）
- **未选中**点导出 → `POST /api/ureportM/export` 下载档案元数据列表 xlsx

## 存储约定

- 表：`ureport_file_tbl`（`name_`, `content_`）
- UReport Provider 前缀：`mysql:`（云枢 MES 约定）
- Servlet 保存与 `/api/ureportM` CRUD 共用 `MySQLReportProvider` / `UreportCompatRepository`
- 模板源码：`backend/mes-server/src/main/resources/ureport-templates/`
- Seed **不再**插入空 content 的预置行；安装交给 Initializer

## Spring Bean 数据集

Bean 名：`mesReportBean`（`MesReportBean.java`）

| 方法 | 说明 |
|------|------|
| `getData` | 单条工单（参数 `id`） |
| `getChildData` | 子工单列表 |
| `getQc` | 条码 QC（`wm_barcode`） |

内置 SQL 数据源名称：**内置数据源**（`fan_mes`）。

## 配置项（application-local.yml）

```yaml
ureport:
  disableHttpSessionReportCache: false
  disableFileProvider: true   # 仅使用 MySQL Provider
  debug: false

# 可选：强制用 classpath XML 覆盖库中已有非空模板
# mes.ureport.force-template-upgrade: true
```

## 前端代理

Vite 已将 `/ureport` 代理至 `:8080`（见 `frontend/web-admin/vite.config.js`）。

## 自检 SQL

```sql
SELECT name_,
       CASE WHEN content_ IS NULL THEN 'NULL'
            WHEN LENGTH(content_)=0 THEN 'EMPTY'
            ELSE CONCAT('OK ', LENGTH(content_), 'B') END AS content_status
FROM ureport_file_tbl;
```

四预置模板均应为 `OK …B`。若仍为空：重启 backend（Initializer 会补写）。

## 回退

若 Servlet 不可用，可将路由改回 `designer.vue`（XML 工作台）。
