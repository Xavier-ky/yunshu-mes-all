# 智能排产总 Agent：已落地运行链路

> 版本：1.0（2026-07-16）  
> 适用角色：`MANAGER`、`PROD_SUPERVISOR`、`TESTER`；涉及领料时还受仓储领域权限约束。  
> 范围：从订单中心最新订单确认，到 BOM/齐套、原子排产、派工和生产领料，为现场作业准备真实业务数据。

## 1. 触发与订单确认

当用户表达“我有一个新订单，帮我智能排产”“帮我安排一笔新订单生产计划”等同类意图时，`IntentRoutingAgent` 先做受控意图识别，并检索角色范围内的排产规则。

随后 `OrderIntakeAgent` 不再默认创建一笔虚拟订单。它读取订单中心列表中的第一条记录；该列表的稳定排序是 `created_at DESC, order_id DESC`，即用户在订单中心看到的最新订单。自动填入确认卡的字段包括订单号、客户、产品、数量和交付日期。

确认时必须重新从订单中心读取该 `order_id` 的真实记录，不能相信前端可编辑字段。服务还会检查产品主数据、既有工单关系和当前权限，避免同一订单重复生成工单。只有通过用户确认，才会确认订单并生成工单。

## 2. 排产链路中的九个责任节点

| 顺序 | Agent | 真实职责 | 主要数据/接口边界 |
|---:|---|---|---|
| 1 | `IntentRoutingAgent` | 识别排产意图，给出基于 SOP 的解释 | 角色范围 RAG；不写业务数据 |
| 2 | `OrderIntakeAgent` | 读取最新订单、重新校验、确认订单并生成工单 | `/api/planning/orders`、`/api/planning/orders/{id}`、`/api/planning/work-orders` |
| 3 | `BomRouteAgent` | 读取已发布 BOM、物料和工艺路线快照 | `GET /api/agent/read/work-orders/{id}/bom-route` |
| 4 | `KittingRiskAgent` | 读取齐套、可用库存和欠料风险 | `GET /api/agent/read/work-orders/{id}/kitting` |
| 5 | `KittingExecutionAgent` | 仅在齐套风险为 LOW 时调用原子排产命令 | `POST /api/agent/scheduling/work-orders/{id}/execute` |
| 6 | `CapacitySchedulingAgent` | 展示设备、人员等排程建议与排产结果 | 原子排产结果/受控排程建议；不重复写入 |
| 7 | `DispatchExecutionAgent` | 展示原子命令同步生成的派工结果 | `dispatch_task` 结果投影；不重复派工 |
| 8 | `SchedulingValidationAgent` | 核验工单、生产任务和派工是否已写回 | `work_order`、`production_task`、`dispatch_task` 结果投影 |
| 9 | `ProductionIssueAgent` | 执行生产领料、出库与物料消耗同步 | `POST /api/agent/production-issue/work-orders/{id}/execute` |

确认订单后的执行现已由 `SchedulingExecutionGraph`（LangGraph `StateGraph`）编排：每个节点继续调用同一批 Agent 包装和原领域服务，齐套风险通过图的条件边决定是否进入原子排产。图不会直接写 MES 业务表；它仅将每个节点的可序列化进度投影保存为用户归属的 Agent 检查点。节点间共享的 `SchedulingAgentState` 仍是一次调用的内存状态，不是数据库实体。

## 3. 真实业务主链与状态门禁

```text
customer_order / customer_order_item
  → work_order
  → BOM 与工艺路线只读快照
  → 齐套分析
  → 原子锁料、任务生成、排产与派工同步
  → production_task / dispatch_task
  → 生产领料、出库、物料消耗
  → 进入现场作业
```

只有齐套风险为 LOW，流程才进入原子排产与后续领料。欠料、状态不合法、权限不足、产品失效、重复工单或领域服务失败时，流程必须在相应节点展示“blocked/失败”原因，不能跳过门禁。

原子排产命令是既有 MES 事务边界：锁料、任务生成、甘特排产和派工同步由原有领域服务完成。`CapacitySchedulingAgent`、`DispatchExecutionAgent` 与 `SchedulingValidationAgent` 只负责解释与结果核验，不能再次发起相同写入。

## 4. 模型与 RAG 在排产中的位置

- 首次意图识别可调用外部模型，输入为用户请求和已审核的排产规则；其结果只决定交互引导，不直接创建/修改业务数据。
- 工作流完成时可调用外部模型，对已生成的订单、工单、齐套、排产、派工和领料结果做总结与风险建议。
- RAG 只提供生命周期、齐套、派工、领料等稳定规则，不能提供任何“当前库存足够”“该订单已排产”等实时结论。
- 所有真实数量、状态、任务和派工结果以 Spring Boot 返回结果为准。

## 5. 典型问题的正确处理

| 用户问题 | Agent 必须先做什么 | 可引用的 RAG | 不可做的事 |
|---|---|---|---|
| “帮我排最新订单” | 读取订单中心第一条，并在确认时复读订单 | 订单/工单与排产 SOP | 依据旧聊天内容猜订单 |
| “为什么没排产成功？” | 查看 BOM、齐套和工作流节点状态 | 齐套与状态门禁规则 | 假设库存充足或自动绕过欠料 |
| “排产后为什么没派工？” | 查询任务与派工写回结果 | 排产到派工同步规则 | 再次重复生成派工 |
| “现在可以开工吗？” | 核验领料执行与工单生命周期 | 领料、现场作业规则 | 仅凭模型文案宣布可开工 |

## 6. 审计与安全边界

- 每一步展示的事件卡可用于解释执行过程；用户会话/消息单独记录在 Agent 审计存储，不进入共享 RAG。
- 所有调用携带用户 JWT 和 `trace_id`；Agent 无权提升角色或跨越数据范围。
- RAG、模型、前端卡片均不能替代 Spring Boot 的事务、数量校验和状态机。
