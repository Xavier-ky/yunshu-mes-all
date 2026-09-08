# 专业 Agent 编排层

本目录包含两类 LangGraph 图，二者都不直接访问 MES 业务表：

| 图 | 当前状态 | 职责 |
|---|---|---|
| `SchedulingOrchestrationSidecar` | 旁路、可单独验证 | 计划、审批暂停/恢复和已完成事件的审计观察 |
| `SchedulingExecutionGraph` | **已接管确认后的智能排产执行编排** | 按既有顺序委托订单、BOM、齐套、排产、派工、校验与领料 Agent |

## 已接管的智能排产图

当前表单确认后的入口 `OrderIntakeService.create_order()` 会进入
`SchedulingExecutionGraph`。图节点为：

```text
OrderIntake
  → BOM/Route
  → KittingRisk
  → [LOW] KittingExecution → Capacity → Dispatch → Validation → ProductionIssue
  → [非 LOW] Finalize blocked
  → Finalize
```

每个节点调用的仍是已有的子 Agent 包装与 Spring Boot 领域服务：

- 订单确认/工单生成仍由原订单中心边界完成；
- 锁料、任务生成、甘特排产和派工仍由原子排产事务完成一次；
- 生产领料仍由原 WMS 领料执行边界完成；
- 前端 API 响应、`workflow_events` 顺序、会话消息和流式展示协议不变。

图在每个节点将脱敏的进度投影保存到既有
`agent_graph_checkpoint`、`agent_runtime_state`，以 `thread_id`、用户和
`trace_id` 关联。审计存储不可用不会伪造业务回滚：结果会标为
`persistence_status=DEGRADED`，而既有 MES 事务结果仍如实返回。

现有确认表单仍是本版本的人在回路边界：`confirmed=false` 会在进入图前按原逻辑拒绝；只有已确认请求进入执行图。将来可在不重写业务节点的前提下，把这一边界进一步升级为 LangGraph 原生 `interrupt()` / `Command(resume=...)`。
# Agent orchestration and foundation layers

The production scheduling graph remains the only active business execution
graph.  The adjacent `app.foundation` package is intentionally sidecar-only:
it provides owner-scoped memory records, a governed capability catalogue and
fact-only run reflection without changing scheduling, quality, report or MES
transaction behavior.  See `app/foundation/README.md` for its safety boundary.
