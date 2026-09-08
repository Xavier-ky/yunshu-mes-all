# 质量管理 Agent 工作流实施方案

> 状态：设计完成，尚未接入默认主对话或执行任何质量写入。  
> 目标：把现有 `QualityManagementAgent` 做成一条可审计、真实数据驱动、逐步流式可视化的质量分析工作流，而不是增加多个空壳子 Agent。

## 1. 设计结论

保留一个业务主体：`QualityManagementAgent`。它负责质量领域的所有事实读取与业务边界；内部分析节点不是新的独立子 Agent，不计入 16 个 Agent 数量。

```text
用户质量请求
  ↓
MainAgent / IntentRouting（外部 API + 规则白名单 + RAG）
  ↓
QualityManagementAgent
  ├─ 读取实时质量事实（MES）
  ├─ 确定性风险分级（规则，不用模型）
  ├─ 本地 Qwen3-0.6B：待检与积压微分析（真实 token stream）
  ├─ 本地 Qwen3-0.6B：缺陷/趋势或工单质量关卡微分析（按需）
  └─ 外部 API：最终质量结论与风险建议（流式）
  ↓
主对话总览 + 每一步 QualityManagementAgent 卡片 + 数据库审计记录
```

外部 API 固定只调用两次：一次用于开始的意图识别，一次用于最终总结。中间不让外部模型接触无边界原始数据；本地 Qwen3-0.6B 只处理压缩后的事实包，输出短结论。

## 2. 真正的数据来源与边界

实时业务事实始终来自 `fan_mes`，通过当前用户 JWT 转发到 Spring Boot；Qdrant RAG 只提供经审核的质量 SOP、检验规范和处置规则，不能替代实时事实，也不能驱动写入。

| 事实类别 | 现有真实接口/表来源 | 首版用途 |
|---|---|---|
| 全域待检队列 | `GET /api/mes/qc/pending/list`；聚合 IQC、外协 IQC、PQC/IPQC、OQC、RQC | 队列数量、检验类型、来源单据、工单、批次、老化风险 |
| 质量总览 | `GET /api/mes/qc/analytics/summary` | 待检数、当日完成、合格率、不良批次、待处置数 |
| 趋势与缺陷 | `GET /api/mes/qc/analytics/trend`、`/defect-top`、`/defect-by-level`、`/type-stats` | 近 7 天趋势、TOP 缺陷、CR/MAJ/MIN 风险、各检验类型负荷 |
| 待处置单据 | `GET /api/mes/qc/analytics/pending-disposition` | 需人工处置的 IQC/IPQC/OQC/RQC 单据 |
| 工单质量上下文 | 现有无副作用 `GET /api/agent/read/work-orders/{workOrderNo}/trace` | 领料、报工、质量、入库、批次、SN 的工单范围事实 |
| IPQC 老化预览 | `GET /api/agent/companion/quality-backlog` | 待检总量、超过 24/72 小时的数量和最久 5 条任务 |

### 2.1 首先补齐的 Agent 专用只读门面

不让 Python Agent 直接拼接页面 API，也不暴露会产生同步副作用的追溯接口。新增 Spring Boot `AgentQualityReadController` 后，由 `SpringBootClient` 白名单访问：

```text
GET /api/agent/read/quality/overview?days=7
GET /api/agent/read/work-orders/{workOrderNo}/quality
```

`quality/overview` 固化返回：质量概览、待检按类型统计、老化统计、近 7 天趋势、TOP 5 缺陷、严重等级分布、待处置清单（限制条数）。

`work-orders/{workOrderNo}/quality` 固化返回：工单生命周期、已完成/待检 IPQC、报工待检量、质量结论、相关产出批次及质量状态。两者均只读、带 JWT、带角色范围检查和 `X-Trace-Id`，失败时明确返回不可用而不是返回演示数据。

## 3. 质量工作流节点

### 节点 Q0：开始意图识别（外部 API）

触发语句覆盖“帮我分析质量情况”“质量任务是否积压”“今天有哪些不良”“某工单为什么质检未通过”“查看待检任务”等自然表达。

- 调用：DeepSeek/Qwen/GLM 中当前选中的外部模型一次。
- 输入：用户原话 + 已审核质量 SOP 的少量 RAG 片段。
- 输出：严格 JSON，只允许 `quality_overview`、`quality_backlog`、`quality_defect_analysis`、`quality_work_order`、`quality_disposition_draft`、`other` 六类意图。
- 守卫：JSON 与意图白名单校验失败时，由关键词/工单号规则回退；绝不让模型直接决定质量结论或写操作。
- 页面：先显示 `QualityManagementAgent` 的“质量请求识别”卡片；结构化返回通过前不显示模型原始文本。

### 节点 Q1：读取真实质量事实（MES）

由现有 `QualityManagementAgent` 调用新增只读门面：

- 总览、积压、缺陷分析：读取 `quality/overview`；
- 指定工单：额外读取 `work-orders/{workOrderNo}/quality`；
- 读取结果进入不可变 `quality_fact_pack`，包含 `queried_at`、`trace_id`、数据来源、筛选条件和精简后的数值/列表；
- 任何读取失败：展示“实时质量数据不可用”的节点卡片并停止后续模型分析，禁止模型猜测。

### 节点 Q2：确定性风险分级（规则）

本节点不调用模型。规则基于 `quality_fact_pack`，初版建议：

| 条件 | 风险 |
|---|---|
| 存在 CR 缺陷；或工单明确为 `QC_FAILED` | `HIGH`，阻止后续放行类建议 |
| 老化待检超过 72 小时；或 MAJ 缺陷/待处置达到阈值 | `MEDIUM`，要求质量人员优先复核 |
| 待检存在但无 CR/MAJ 且无严重老化 | `LOW`，显示常规跟进建议 |
| 数据不全、角色无权限、实时源不可用 | `UNKNOWN`，只说明数据边界 |

阈值必须配置化并存储版本；模型不能修改阈值。

### 节点 Q3：本地 Qwen3-0.6B 微分析（真实流式）

使用已经可用的 `D:\ClaudeCode\MES\agent-system\mymodel\models\Qwen3-0.6B` 和 `local_qwen.stream_generate()`。本节点默认不使用微调模型 Qwen3L，也不调用外部 API。

为避免 0.6B 模型被大表格淹没，只输入压缩事实包，限制每次输出 120～180 个中文字符，禁止 Markdown、禁止自行补充数字。

1. **队列与积压分析（必跑）**
   - 输入：待检总数、类型分布、24/72 小时老化、最久任务、规则风险级别。
   - 输出：`质量队列判断：…；优先级：…；原因：…`。
2. **缺陷/趋势分析（按需）**
   - 仅在用户询问异常/趋势，或事实包存在 CR/MAJ/不良批次时运行。
   - 输入：7 日趋势、TOP 缺陷、严重等级分布、待处置数。
   - 输出：`缺陷关注点：…；变化：…；建议复核：…`。
3. **工单质量关卡分析（指定工单时运行）**
   - 输入：工单生命周期、报工待检量、IPQC 状态、质量/批次事实。
   - 输出：`工单质量关卡：…；阻断项：…；可执行下一步：…`。

`TextIteratorStreamer` 的 token delta 直接作为 SSE `quality_local_delta` 事件推送，前端沿用现有平滑写入器；每个微分析对应一个 `QualityManagementAgent` 消息卡片和独立模型来源标记“本地 Qwen3-0.6B”。首次加载模型较慢时，应显示“本地质量分析模型初始化中”，而不是让界面无响应。

### 节点 Q4：最终质量总结与风险建议（外部 API）

- 调用：当前选择的外部 API 一次；推荐 DeepSeek 为默认。
- 输入：已验证的 `quality_fact_pack`、规则风险分级、Q3 本地分析结果、角色范围内 RAG 质量 SOP 片段。
- 明确禁止：编造缺陷数量、检验结论、质量放行、批次或工单状态；不得执行写入；RAG 与本地模型输出均不能覆盖事实包。
- 输出格式：

```text
质量结论：...
实时依据：...
风险等级：...
建议优先级：...
下一步与责任边界：...
规则依据：...
```

实现 `stream_chat_completion()` 解析 OpenAI 兼容 SSE，真正把最终 API token 推到前端。若供应商不支持流式，则后端标记 `fallback_visual_stream`，由现有前端平滑写入器呈现，不伪称为 API token stream。

## 4. 页面呈现与数据库审计

主对话不默认打开执行轨迹。质量流程的节点卡片按顺序在主对话展示：

1. `QualityManagementAgent`：质量请求识别（外部 API，校验后的摘要）；
2. `QualityManagementAgent`：实时质量数据已读取（事实来源、时间、筛选范围）；
3. `QualityManagementAgent`：风险规则判定；
4. `QualityManagementAgent`：本地 Qwen 队列/缺陷/工单微分析（真实流式，按需 1～3 张卡片）；
5. `云枢小智`：外部 API 最终质量总结（真实流式）。

每个可见节点都使用已有会话持久化写入；额外保存：`agent_name`、`trace_id`、节点序号、事实来源名称、查询时间、模型类型/版本、提示词版本、RAG 引用 ID、风险规则版本、失败/回退来源。保存“精简事实包或其哈希”，不无约束复制整张质量表到对话记录。

## 5. 写入策略：分两期

### 第一期（本次质量流程首发）

完全只读：允许分析、总结、生成建议；不创建或完成 IQC/IPQC/OQC/RQC，不修改缺陷、报工、产出、工单生命周期、成品入库或库存。

### 第二期（用户确认后才做）

只允许质量角色在明确选择了具体单据后调用既有 Spring Boot 领域接口。Agent 只能生成草稿和确认卡：

- IPQC 结束前必须校验检验数量 = 合格 + 不合格；
- `ACCEPT` 才允许推进工单到 `QC_PASSED`，否则进入 `QC_FAILED`；
- 质量完成会回写报工待检、产出明细、质量状态，可能进一步触发成品入库草稿；
- OQC/RQC/IQC 的不同来源单据存在不同回写逻辑，不能用一个泛化“质量通过”按钮处理；
- 无权限、过期单据、数量不一致、状态非 `PREPARE` 均必须阻断。

因此，质量 Agent 永远不能自动“放行”“报废”“完成质检”或“入库”。

## 6. 异常与降级

| 故障 | 行为 |
|---|---|
| 外部意图 API 失败/格式非法 | 规则识别回退，显示来源为规则，不停止真实数据读取 |
| Qdrant 不可用 | 继续，只省略 SOP 依据并标注 |
| 质量事实读取失败 | 阻断后续分析，显示真实数据不可用，不生成结论 |
| 本地 Qwen 不可用或超时 | 跳过 Q3，保留事实与规则，最终总结注明本地微分析未完成 |
| 外部最终 API 失败 | 用事实包 + 规则模板生成最终摘要，标注“规则总结” |
| SSE 断连 | 已持久化完成节点可从会话恢复；未完成节点显示中断而非伪造完成 |

## 7. 实施顺序与验收

1. 增加两个 Agent 专用质量只读门面、Python 白名单和 `QualityManagementAgent` 事实包映射；先验证 JWT、角色、零写入。
2. 新建 `quality_workflow` 编排层与质量意图外部 API 分析器；接入现有会话/工作流事件持久化。
3. 接入本地 Qwen3-0.6B 的受限微分析和真实 SSE；预热、超时和不可用降级可测。
4. 增加外部最终总结 SSE、RAG 引用和严格事实提示词。
5. 前端将质量节点卡片按顺序流式渲染，右侧执行轨迹只在用户手动打开时展示。
6. 最后才设计确认卡与质量写入门面，单独做事务、权限和回归验证。

### 最小验收集

- 正常质量总览：所有数字可回读至 MES 接口，零写入；
- 待检积压：能显示 24/72 小时老化、最久任务和本地 Qwen 优先级；
- CR/MAJ 缺陷：规则风险为 HIGH/MEDIUM，外部总结不淡化风险；
- 指定工单：只读取该工单质量上下文，不混入其他工单；
- 无待检：模型明确“当前事实包未显示待检积压”，不得虚构异常；
- 无外部 API、本地模型、RAG 三种降级路径：来源标记正确；
- 不同角色/无 JWT：401/403 或字段范围受限；
- 全流程消息可从数据库会话恢复，包含节点顺序和模型来源；
- 所有首发测试后 `qc_*`、`pro_feedback`、`wm_product_produce*`、`work_order`、库存表行数与关键状态不变。
