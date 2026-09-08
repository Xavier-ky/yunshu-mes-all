# 云枢智造 MES — 接口覆盖完整分析

> 数据库 103 张业务表 | 已实现 58 张 | 未覆盖 45 张 | 分析日期 2026-07-09

---

## 一、接口现状总览

```
┌──────────────────────────────────────────────┐
│  fan_mes 数据库 ─ 103 张业务表（不含 Agent）    │
│                                              │
│  ████████████████████████░░░░░░░  58 已实现   │
│  ░░░░░░░░░░░░░░░░░░░░░░░░░░████  45 未覆盖   │
│                                              │
│  已实现中: 50 GET 全通 / 47 POST 通 / 3 POST  │
│  待重启修复（设备保养/点检）                     │
└──────────────────────────────────────────────┘
```

---

## 二、所有 45 张未覆盖表 — 按必要程度分级

### 第一节：必要级（影响核心业务流程闭环）— 3 张

| # | 表名 | 业务场景 | 缺少的影响 |
|---|------|---------|-----------|
| 1 | **`barcode_record`** | 条码生成/打印/查询 | 无法生成和打印产品条码标签，追溯链路断裂。条码规则和模板已配置，但无法产出实际条码记录 |
| 2 | **`sop_file`** | 工序标准作业指导书 | 操作工在「我的工位」页面无法查看 SOP，生产作业缺少标准化指引 |
| 3 | **`storage_location`** | 仓库库位管理 | 库存只能到仓库级别，无法管理到货架/库位。领料发料时无法指定具体储位 |

### 第二节：重要级（显著增强业务能力）— 5 张

| # | 表名 | 业务场景 | 价值 |
|---|------|---------|------|
| 4 | **`maintenance_task`** | 保养计划拆分为具体任务 | 保养计划 → 任务分配 → 执行确认的闭环 |
| 5 | **`barcode_application_rule`** | 条码应用规则配置 | 决定哪些物料/产品使用哪种条码规则，条码体系从"有配置"到"能运转" |
| 6 | **`product_spec`** | 产品规格参数定义 | 产品的电压/功率/尺寸等结构化属性，质检和 BOM 管理需要 |
| 7 | **`quality_standard` + `quality_standard_item`** | 质量检验标准定义 | 质检执行时对照标准逐项判定，当前质检记录是自由文本，缺少标准化 |
| 8 | **`factory_calendar`** | 工厂工作日历 | 排产、交期计算、OEE 统计依赖班次日历，当前只有班次定义无日历 |

### 第三节：增强级（提升专业性和数据完整性）— 12 张

| # | 表名 | 业务场景 |
|---|------|---------|
| 9 | `inspection_item` + `inspection_item_category` | 通用检验项目库，供质检任务引用 |
| 10 | `quality_record_item` | 检验记录明细，按检验项目逐项记录结果 |
| 11 | `defect_reason` | 缺陷原因分类库，结构化缺陷分析 |
| 12 | `product_process_state` | 产品在制状态追踪，实时知道每个 SN 处于哪道工序 |
| 13 | `package_binding` | 包装层级绑定（箱→托盘），支持多级包装追溯 |
| 14 | `production_report_detail` | 报工扩展明细，记录每道工序的详细产出 |
| 15 | `barcode_template_param` | 标签模板参数，打印时可动态填充 |
| 16 | `barcode_rule_segment` | 条码规则片段（前缀/日期/流水号等） |
| 17 | `label_print_log` | 标签打印日志，审计追溯 |
| 18 | `andon_notice` | 安灯通知记录（微信/钉钉/短信推送日志） |
| 19 | `andon_exception_config` | 安灯异常自动触发规则配置 |
| 20 | `andon_result` | 安灯处理结果归档 |

### 第四节：辅助级（IoT / 集成 / 日志 / 扩展）— 25 张

| 类别 | 表名 | 说明 |
|------|------|------|
| 设备IoT | `device_count_config`, `equipment_count_record`, `device_oee_daily`, `device_integration_config`, `device_manufacturer` | 设备联网采集、OEE 计算，需硬件对接 |
| 能耗 | `energy_meter`, `energy_reading` | 能源数据采集，需 IoT 接入 |
| 故障 | `fault_cause` | 故障原因知识库 |
| 集成 | `erp_work_order_map`, `api_unit_mapping`, `sync_log` | ERP/MES 双向数据同步 |
| 点检 | `device_inspection_item` | 点检项目模板，已有点检记录但缺项目定义 |
| 报表 | `dashboard_config`, `dashboard_kpi_snapshot`, `report_run_log` | 用户自定义仪表板和报表调度 |
| 工资 | `piece_wage_record`, `piece_wage_rule` | 计件工资核算 |
| 日志 | `api_call_log`, `mobile_operation_log`, `task_operation_log`, `trace_query_log` | 各类审计日志 |
| 配置 | `api_endpoint` | 接口端点元数据，swagger 替代方案 |
| 移动端 | `wechat_user_binding` | 微信小程序用户绑定 |

---

## 三、必要级的业务影响分析

### 3.1 `barcode_record` — 条码缺失的影响链

```
当前状态：
  条码类型 ✓ → 条码规则 ✓ → 条码模板 ✓ → 条码记录 ✗ → 打印 ✗ → 扫码追溯 ✗
  
修复后：
  产品 SN 创建 → 条码规则匹配 → 生成 barcode_record → 打印标签 → 贴标 → 
  工位扫码 → 自动报工 / 物料绑定
```

**涉及接口设计：**
- `POST /api/barcode/records/generate` — 为产品 SN 生成条码记录
- `GET /api/barcode/records/{id}` — 查询条码记录
- `GET /api/barcode/records?snId=` — 按 SN 查询关联条码

### 3.2 `sop_file` — 操作工现场缺指引

```
当前：操作工看到派工任务 → 只有工序名称 → 凭经验操作
  
修复后：
  操作工看到派工任务 → 点击工序 → 弹出 SOP 文档（图片/PDF/视频）→ 按标准作业
```

**涉及接口设计：**
- `GET /api/sop/files?stepId=` — 按工序查询 SOP 文件列表
- `POST /api/sop/files` — 上传 SOP 文件
- `DELETE /api/sop/files/{id}` — 删除

### 3.3 `storage_location` — 仓库精确定位缺失

```
当前：发料时只知道"原材料仓"，物料员需要自己找位置
  
修复后：
  批次入库 → 分配库位（A-01-03）→ 领料单自动推荐最近库位 → 物料员按库位拣货
```

**涉及接口设计：**
- `GET /api/inventory/storage-locations?warehouseId=` — 按仓库查库位
- `POST /api/inventory/storage-locations` — 创建库位
- `PUT /api/inventory/batches/{id}/location` — 移动批次到指定库位

---

## 四、推荐实施路线

| 阶段 | 范围 | 表数 | 预计工作量 |
|------|------|------|-----------|
| **P0-必要** | barcode_record, sop_file, storage_location | 3 | 1-2 天 |
| **P1-重要** | maintenance_task, barcode_application_rule, product_spec, quality_standard(+item), factory_calendar | 6 | 2-3 天 |
| **P2-增强** | inspection_item, quality_record_item, defect_reason, product_process_state, package_binding, barcode_template_param, barcode_rule_segment, label_print_log, andon_notice, andon_exception_config, andon_result | 12 | 3-5 天 |
| **P3-辅助** | 其余 IoT/集成/日志/扩展表 | 24 | 按需迭代 |

---

## 五、本轮调优成果

| 指标 | 数值 |
|------|------|
| 后端修复项 | **17 项**（Schema 不匹配 ×7、VO 缺字段 ×3、DTO 校验 ×3、配置 ×1、类型转换 ×1、前端 ×2） |
| 前端改造页 | **30 页**（Dashboard 首页 7 角色 + 管理 5 模块 + 业务 18 模块） |
| GET 接口健康度 | **47/47 (100%)** |
| 主要业务闭环 | **订单→工单→派工→报工→安灯→质检→入库 全部跑通** |
