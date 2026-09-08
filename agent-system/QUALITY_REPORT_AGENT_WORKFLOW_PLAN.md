# 质量分析报告 Agent 工作流设计

## 1. 目标与边界

本设计为现有“质量总 Agent”增加一个**后续追问工作流**：当用户在同一主对话中输入“请帮我生成质量分析报告”“出一份今天的质量报告”“生成质量日报”等表达时，系统基于当前 MES 的实时只读数据，逐段生成一份可视化质量报告，并在结束时提供“导出 Word”操作。

现有“帮我分析今天的质量情况”五节点工作流保持原样：意图识别 → 质量事实 → 风险规则 → 本地 Qwen3-0.6B 微分析 → 外部模型总体分析。本报告工作流是它的独立续接能力，不写入任何 QC 业务记录，也不改变工单、检验或放行状态。

## 2. 触发与会话规则

### 触发语句

- 请帮我生成质量分析报告
- 生成今天的质量日报 / 质量周报
- 把刚才的质量分析整理成报告
- 导出一份质量分析报告

新增 `is_quality_report_request()`：必须同时包含质量语义（质量、质检、不良、缺陷、IQC/IPQC/OQC/RQC）与报告语义（报告、报表、日报、周报、导出、整理）。它优先于普通质量分析路由。

如果同一会话刚完成质量分析，报告界面会标识“基于刚才的分析继续生成”；但报告仍重新读取一次实时 MES 快照，避免使用过期的聊天文本。若用户直接发起报告请求，则提示“未找到上一轮质量分析，已按当前实时数据生成报告”，随后正常执行。

## 3. 真实数据范围

数据只来自现有 JWT 保护的 Spring Boot 质量只读门面，不允许模型自行拼接 SQL，也不允许前端上传或篡改报告数据。

| 报告内容 | 真实来源 | 用途 |
|---|---|---|
| 待检数、今日完成数、合格率、缺陷批次、待处置数 | `QcAnalyticsController.summary()` | 顶部 KPI 与风险概览 |
| 最近 1–30 天检验量、通过数、合格率 | `QcAnalyticsController.trend(days)` | 通过率/检验量趋势图 |
| 缺陷名称、数量、严重度 | `defectTop(limit)` | 缺陷 TOP 柱状图与表格 |
| CR / MAJ / MIN 数量 | `defectByLevel(days)` | 严重度分布图和风险判定 |
| IQC / PQC / OQC / RQC 指标 | `typeStats()` 与待检任务 | 检验环节结构图与待办队列 |
| 近期完成检验、待处置单据 | `recentFinished(limit)`、`pendingDisposition(limit)` | 报告明细表、行动清单 |

实施时扩展 `AgentQualityReadService.reportSnapshot(days)` 与 `/api/agent/read/quality/report-snapshot`。该门面只组合上述已经存在的 SQL-backed 统计接口，并添加 `queriedAt`、`windowDays`、`source`、`readOnly=true`；不会新增页面接口或写入质量表。

## 4. 报告子流程

`QualityManagementAgent` 仍是用户可见的业务 Agent。报告内部使用专门的 `QualityReportAgent` 与 `QualityReportRenderer`，它们属于质量 Agent 的内部能力，不改变现有 16 个 Agent 总览卡片数量。

```text
用户：生成质量分析报告
        │
        ▼
1. QualityManagementAgent / 报告意图与范围确认
        │  外部 API：只解释报告目的与时间窗口，不参与事实决定
        ▼
2. QualityManagementAgent / 质量报告事实快照
        │  JWT + Spring 只读门面 + fan_mes
        ▼
3. QualityReportAgent / 指标与风险核验
        │  固定规则：CR、MAJ、合格率、积压、待处置阈值
        ▼
4. QualityReportAgent / 本地 Qwen3-0.6B 细项解读
        │  仅接收压缩事实包，生成“趋势、缺陷、处置优先级”短解读
        ▼
5. QualityReportRenderer / 报告图、表与章节数据包
        │  不调用模型；将真实数值转为受控 ECharts 配置和表格
        ▼
6. MainAgent / 外部 API 最终报告撰写
        │  基于快照、规则结果、本地解读；受控提示词，流式输出
        ▼
7. 报告完成 + 是否导出 Word？
        │
        ├── 否：仅保留会话与报告快照
        └── 是：服务端从冻结快照生成 .docx 并下载
```

模型职责清晰分离：外部 API 共两次（报告意图、最终报告叙述）；本地 Qwen3-0.6B 一次（受事实约束的细项解读）；图表、KPI、阈值、排序、表格和导出数据均由确定性程序完成。外部模型不可决定质量结论的数值、不可写 MES、不可替代数据源。

## 5. 流式呈现设计

报告不是一次性大段文本，而是按以下顺序在主对话区逐步出现，每个阶段只创建一个稳定的 Agent 输出卡片：

1. `node_start`：质量报告 Agent 说明正在核验时间窗口和实时数据；
2. `node`：事实快照卡片（4 个 KPI 与数据查询时间）；
3. `node`：规则风险卡片（风险等级和触发依据）；
4. `node_delta/node_complete`：本地 Qwen3-0.6B 的一张细项解读卡；
5. `report_section_delta`：报告标题、执行摘要、趋势章节逐段流式显示；
6. `report_chart`：每张图的真实数据到达后立即淡入渲染；
7. `report_table`：缺陷 TOP 与待处置明细表；
8. `final_delta/final_complete`：外部 API 的“风险、行动建议、数据局限”总结；
9. `report_export_ready`：固定确认卡“是否导出 Word？”。

前端扩展通用 SSE 消费器以识别 `report_section_delta`、`report_chart`、`report_table` 和 `report_export_ready`。`AgentHome.vue` 新增 `QualityReportCard.vue`：同一报告对象以 `report_id` 聚合，不会因一个 token 或一张图而生成多余对话框。图表使用项目已有 ECharts，在画布容器可见后 `nextTick` 初始化；数据到达后执行一次 220–320ms 的淡入缩放动画。聊天文本继续使用已验证的平滑 writer，确保“上一卡片结束后才开始下一卡片”。

## 6. 图、文、表版式

报告卡片按手机和桌面自适应，默认包含：

- 标题区：`质量分析报告`、统计窗口、生成时间、数据来源与“实时快照”标识；
- KPI：待检任务、今日完成、累计合格率、待处置数量；
- 图 1：检验量与合格率双轴趋势图；
- 图 2：缺陷 TOP 横向柱状图（颜色按 CR / MAJ / MIN 分级）；
- 图 3：缺陷严重度环形图；
- 图 4：按 IQC / PQC / OQC / RQC 的待检队列或完成概览；
- 表 1：缺陷 TOP（排名、缺陷名称、数量、等级）；
- 表 2：待处置/近期检验明细（类型、单号、对象、结果、状态、时间）；
- 文本：执行摘要、趋势解读、风险依据、建议行动、数据局限。

当某组实时数据为空时，保留标题并显示“当前窗口无可用记录”，不使用模拟数据，不画误导性零值图。

## 7. 持久化、审计与 Word 导出

### 报告快照

为了让“确认导出”与对话中的内容完全一致，生成阶段创建一个只读、不可变的报告快照。优先新增独立 `agent_report_artifact` 表（而不是扩展质量业务表）：

- `report_id`：UUID，公开引用；
- `session_id`、`user_id`、`agent_id`：会话与所有权；
- `report_type`：`QUALITY_ANALYSIS`；
- `status`：`GENERATING/READY/EXPORTED/FAILED`；
- `window_days`、`queried_at`；
- `snapshot_json`：事实、规则结果、图表规格、文本章节、数据源版本；
- `created_at`、`exported_at`、`expires_at`。

每一张可见 Agent 卡片仍按既有机制写入 `agent_message` 的版本化 metadata envelope；报告完整事件链和模型来源写入 trace。报告快照不保存模型密钥、JWT 或原始敏感字段。数据库实际以当前项目的 Flyway-disabled 部署方式通过幂等初始化/受控迁移创建，并先检查表是否存在。

### 导出接口

`POST /api/agent/quality/reports/{report_id}/export`：

- 必须携带当前用户的 MES JWT；
- 服务端验证 `report_id` 属于当前用户且状态为 `READY`；
- 只从服务器保存的 `snapshot_json` 读取数据，拒绝浏览器上传的图表/文本；
- 使用现有 Spring Boot `poi-ooxml 5.2.5` 实现为首选：生成中文 Word 标题、KPI 表、四张 PNG 图、数据表和行动建议；
- 图像由 Agent 服务根据同一快照确定性生成，或由 Spring 端统一绘制，不能依赖浏览器截屏；
- 返回 `application/vnd.openxmlformats-officedocument.wordprocessingml.document`，前端以 Blob 下载为 `质量分析报告_YYYYMMDD.docx`；
- 成功后写 `EXPORTED` 状态和导出审计事件。导出失败不会丢失已完成报告，用户可重试。

工程已有的 `/api/agent/chat/weekly-report` 是旧页面的 501 Stub，不能替换本流程；质量报告导出使用独立、用户隔离的 Agent API。

## 8. 受控提示词与安全规则

外部最终报告提示词将明确要求：仅使用编号事实包；数字必须原样引用；无法判断时写“数据不足”；必须输出“事实、推断、建议”三种不同语义；不得声称已处理缺陷、已关闭任务或已执行放行。每段输出会保留 provider、prompt version、source（`llm_api/local_qwen/rule_fallback`）以供审计。

本地 Qwen 提示词限制为三项：趋势变化、缺陷优先级、待处置建议，且每一项必须引用事实包内数值。任何模型不可调用写接口；报告流程的 Spring 访问仅使用 `GET /api/agent/read/quality/report-snapshot`。

## 9. 实施顺序与验收

1. 扩展 Spring 质量只读门面，加入报告快照数据并用真实 `fan_mes` 查询验证字段；
2. 新增 FastAPI `quality/report_workflow.py`、报告快照存储、路由识别与 SSE 事件；
3. 新增 ECharts 报告卡片与导出确认卡，保持既有质量卡片/平滑输出逻辑不变；
4. 实现服务端 Word 生成与下载接口；
5. 用质量角色的 JWT 做端到端测试：完整 SSE 顺序、每个图真实数值、报告快照落库、拒绝跨用户导出、Word 可打开且包含图/文/表；
6. 回归“帮我分析今天的质量情况”现有五节点流程，确保输出事件、数据库写入和主对话显示不回退。

验收重点：报告中任意 KPI、图表数据点、表格行都能在相同 `report_id` 的服务器快照中追溯；没有数据时明确显示空态；一次报告只出现一个连续报告卡片而不是大量碎片卡片；导出的 Word 与界面报告使用同一份冻结数据。
