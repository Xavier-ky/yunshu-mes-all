# P0 Agent 持久化基座

完成日期：2026-07-15

## 已落库的治理对象

`scripts/bootstrap_agent_p0.py` 会在本地 `fan_mes` 中幂等执行以下初始化：

- 将 `mes_knowledge_v1` 的 13 份已审核知识文档、130 个向量点映射到 `knowledge_space`、`knowledge_source`、`knowledge_document`、`knowledge_chunk` 与 `knowledge_embedding`；
- 登记八个 P0 Agent：统一编排、订单承诺、工单下达、排产、齐套、派工、领料与风险审阅；
- 登记六个只读工具契约：知识检索、工单流程、齐套、生产任务、派工、工单/SN 追溯；
- 写入 31 条 Agent-工具权限、4 项策略、6 条守卫规则、版本化系统提示词和 30 条评测用例；
- 登记本地 Qwen3、Qwen3L 与 BGE embedding 模型元数据。

当前工具仅是受治理的契约和权限登记；下一阶段必须通过 JWT 保护的 Spring Boot API 实现这些工具，不能用 Agent 直连 MES 业务表。

## 可恢复状态

`V32__agent_p0_persistence.sql` 新增：

- `agent_graph_checkpoint`：保存 `thread_id`、图版本、状态快照、审批中断状态及关联会话/任务；
- `agent_runtime_state`：保存 trace 和确认单，使 FastAPI 重启后仍可查询和继续处理。

前者将作为 LangGraph checkpointer 的持久化契约；后者已接入现有 `memory_store.py`，数据库不可用时才回退到进程内存。

## 部署与验证

本地 Flyway 关闭时，按顺序执行：

```powershell
cd D:\ClaudeCode\MES\agent-system\backend
D:\Anaconda2024.10\envs\pytorch\python.exe scripts\bootstrap_agent_p0.py
D:\Anaconda2024.10\envs\pytorch\python.exe scripts\verify_agent_p0_foundation.py
```

验证脚本会跨进程读取 trace、确认单和 `INTERRUPTED` checkpoint，然后自动删除自己的临时测试行。

## 真实只读工具（P0）

Spring Boot 的专用门面是 `/api/agent/read/*`，而不是复用页面 API：现有齐套 `GET` 会落分析记录，现有追溯 `GET` 会触发同步，二者都不符合 Agent 的“只读事实”要求。

FastAPI 仅通过 `POST /api/agent/read-tools/{tool_name}` 调用这些受控事实源：

- `query_work_order_pipeline`
- `query_kitting_snapshot`
- `query_production_task_snapshot`
- `query_dispatch_snapshot`
- `query_work_order_trace`
- `search_knowledge`

它会校验工具名与参数、转发调用者 JWT、保留 trace，并返回 `source`、`queried_at` 与 `read_only`。可重复验收：

```powershell
cd D:\ClaudeCode\MES\agent-system\backend
D:\Anaconda2024.10\envs\pytorch\python.exe scripts\verify_agent_read_tools.py --password 123456
```

该脚本断言匿名请求为 401、五个实时工具可读取真实种子工单，并验证齐套快照前后 `kitting_analysis` 和 `material_shortage` 行数不变。操作工读取齐套库存明细被拒绝为 403。

当前数据库的 `work_order` 没有工厂归属字段，因此 P0 已严格实施 JWT/角色范围，但不能伪称已实现工厂级隔离。多工厂上线前需先补充工单—工厂的权威关联与过滤契约。

## 安全边界

- Qdrant 只保存可检索向量与载荷；MySQL 保存知识治理、审计和运行态权威映射。
- 动态事实必须来自 JWT 保护的 MES 只读 API，不允许模型编造或从 RAG 推断库存、任务、质量与排产状态。
- P0 禁止直连 SQL、未登记工具和自主写入；派工、领料、状态变更、质量放行、入库等后续动作必须走“审批中断 → 获批 → checkpoint 恢复”。
