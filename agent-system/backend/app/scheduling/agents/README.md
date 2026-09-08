# 智能排产子 Agent 包装层

本目录将智能排产流程中的每个业务责任包装为独立 Agent 模块，同时复用既有的 MES 领域服务。包装层不直接操作数据库，也不改变 Spring Boot API、事务边界或前端 `workflow_events` 协议。

## 当前节点

| Agent | 模块 | 真实边界 |
|---|---|---|
| `IntentRoutingAgent` | `intent_routing.py` | 受控模型意图识别与角色范围 RAG 规则依据 |
| `OrderIntakeAgent` | `order_intake.py` | 订单校验、创建、确认和工单生成 |
| `BomRouteAgent` | `bom_route.py` | MES BOM 与工艺路线快照 |
| `KittingRiskAgent` | `kitting_risk.py` | 齐套、短缺和风险判断 |
| `KittingExecutionAgent` | `kitting_execution.py` | 调用原子排产写入命令一次 |
| `CapacitySchedulingAgent` | `capacity_scheduling.py` | 排产辅助建议读取 |
| `DispatchExecutionAgent` | `dispatch_execution.py` | 派工写入结果投影 |
| `SchedulingValidationAgent` | `scheduling_validation.py` | 工单、任务、派工写回结果投影 |
| `ProductionIssueAgent` | `production_issue.py` | 生产领料、出库与消耗追溯 |

## 关键约束

1. 新 Agent 继承 `BaseSchedulingAgent`，在自己的模块中声明唯一 `agent_name`。
2. 共享数据只通过 `SchedulingAgentState` 传递；它是内存态，不是数据库实体。
3. 所有实时事实必须继续调用已有 MES 服务或受控只读工具；RAG 只能提供 SOP 解释依据。
4. 写操作必须复用现有领域服务，禁止在包装层直接访问 MySQL。
5. 若新节点需要展示，补充 `events.py`、`agents/registry.py` 和前端 Agent 名称映射；若不参与默认排产路径，只注册即可。
6. 当前 `workflow.py` 仍是顺序编排。未来迁移到 LangGraph 时，每个本目录模块可直接作为一个 `StateGraph` 节点或子图实现。

## 特别说明：原子排产命令

`KittingExecutionAgent` 调用的现有 MES 命令会原子完成锁料、任务生成、排产与派工同步。为保证当前数据库语义不变，`CapacitySchedulingAgent`、`DispatchExecutionAgent` 和 `SchedulingValidationAgent` 只分别包装其职责与结果，不重复发起写操作。
