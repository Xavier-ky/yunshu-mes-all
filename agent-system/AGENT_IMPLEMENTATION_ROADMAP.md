# 云枢小智：真实 Agent 实施路线图

## 当前基线

已具备：受治理的 RAG、Qdrant/MySQL 知识审计映射、P0 Agent 注册与只读权限、策略守卫、评测样例、跨重启的运行态与 checkpoint。

尚未具备：这些工具契约的真实 JWT 调用实现，以及基于真实工具/RAG 的 LangGraph 编排。因此当前硬编码 `workflow.py` 不能作为生产 Agent。

## 实施顺序

### 1. P0 真实只读工具（下一步，必须先完成）

目标：让小智能回答“这张工单现在怎样、为什么不能排产、齐套是否满足、派工到了哪里、如何追溯”，且每个动态结论都有实时来源。

首批工具及唯一事实源：

| 工具 | Spring Boot 事实源 | 输出必须包含 |
|---|---|---|
| `get_workflow_pipeline` | `GET /api/planning/workflow/pipeline/{workOrderNo}` | `work_order_id`、生命周期、当前/下一节点、查询时间 |
| `get_kitting_analysis` | `GET /api/planning/kitting/{workOrderId}` | 齐套率、缺料明细、锁料状态 |
| `get_work_order_tasks` | `GET /api/mes/pro/protask/listTaskListByWorkorder` | `task_id`、工序/工位、计划与完成数量 |
| `get_dispatch_snapshot` | 现有派工查询接口（核对后固定） | `dispatch_id`、`task_id`、执行人、计划/完成数量 |
| `get_work_order_trace` | `GET /api/traceability/work-order/{workOrderNo}` | 批次、SN、领料/报工/质检/入库链路 |
| `search_knowledge` | 现有 FastAPI `/api/rag/search` | 引用、版本、来源与检索时间 |

约束：FastAPI 只透传当前用户 JWT；Python 不直连 MES 业务表；接口白名单、参数 Schema、角色/工厂范围和响应标准化必须一次完成。

验收：用真实种子工单分别验证“工单概览、齐套、任务/派工、追溯”四类问题；无 JWT、越权角色、未知工具与非法参数均应被拒绝。

### 2. 最小 LangGraph：只读问答图

目标：替换关键词硬编码工作流，不做写操作。

```text
authenticate
  → entity_resolver（工单号 / SN → 真实 ID）
  → intent_router
     ├─ knowledge_query → RAG → citation_guard
     ├─ realtime_query → read_tools → fact_guard
     └─ hybrid_query → read_tools + RAG → evidence_merge
  → response_stream → audit + checkpoint
```

关键规则：

- 先解析并固定 `work_order_id`，不把展示编号当内部主键；
- 工单状态、库存、齐套、任务、派工、质量等动态结论只能从工具来；
- SOP、门禁解释必须带 RAG 引用；
- 实时工具失败时明确告知“未取得事实”，不允许模型补全；
- 每一次工具调用、引用和路由决策写入审计记录与 checkpoint。

验收：30 条已有评测用例中，路由、工具选择、引用和拒答符合预期；SSE 输出保持已有小精灵的平滑流式体验。

### 3. 排产分析 Agent（仅建议与草稿）

目标：先完成排产前六步的“解释与方案生成”，不改变任何业务数据。

输入：订单/工单、生命周期、齐套、任务、甘特快照、规则知识。

输出：可执行的排产建议、风险项、缺料与冲突原因、依赖的实时数据和规则引用。任何“发布排产、生成/调整派工、领料”只能生成草稿操作卡。

验收：正常、欠料、缺工位/产能冲突、历史工单生命周期缺失四种场景；模型不得虚构排产结果。

### 4. 审批中断与受控写入门面

目标：把草稿安全地转成真实操作，不把 compat Controller 或 SQL 直接交给模型。

Spring Boot 新增窄门面，例如 `/api/agent/operations/*`，内部复用领域服务。每个写操作要求：审批号、线程号、幂等键、预期生命周期/版本，并返回前后状态与业务单据 ID。

接入顺序：齐套预留/释放 → 发布排产与派工 → 领料 → 报工 → IPQC → 成品入库。每步执行后重新读取 pipeline；失败或状态变化则停在 checkpoint，不自动重试写入。

### 5. 可观测、评测与上线控制

上线前补齐：

- Agent 任务、计划、工具调用、审批、检索引用与模型消耗的统一审计；
- 30–50 条回归用例扩展为真实工单的端到端评测；
- 角色/工厂数据隔离、工具超时/降级、提示注入与越权测试；
- 灰度开关：先仅对 `TESTER` 和 `MANAGER` 开放真实 Agent，再扩展至生产主管。

## 不应提前做的事

- 不增加更多子 Agent；现有 P0 角色足够，先让它们拥有真实工具。
- 不把动态订单、库存、任务、质量记录灌入向量库。
- 不让模型直接调用 SQL、兼容写接口或自动执行高风险操作。
- 不在真实只读闭环与评测完成前开放“自动排产/自动领料”。
