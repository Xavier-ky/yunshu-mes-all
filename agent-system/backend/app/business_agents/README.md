# 独立业务子 Agent 包装层

本目录是排产工作流之外的业务 Agent 包装层。每个模块都可以被未来的 LangGraph 节点或主协调 Agent 单独调用，但目前**没有接入默认主对话与智能排产工作流**。

## 已包装的业务 Agent

| Agent | 实时事实来源 | 当前状态 | 未来受控写入 |
|---|---|---|---|
| `ProductionExecutionAgent` | 工单流水、生产任务、派工快照 | 可单独只读 | 报工草案/执行 |
| `QualityManagementAgent` | 待检任务、质量指标、工单追溯快照 | 可单独只读 | 检验结论、处置草案 |
| `WarehouseLogisticsAgent` | 库存批次、工单齐套快照 | 可单独只读 | 来料、退料、成品入库 |
| `TraceabilityAgent` | 无副作用工单追溯快照 | 可单独只读 | 无 |
| `EquipmentMaintenanceAgent` | 设备工作台摘要与待办 | 可单独只读 | 保养/维修草案与执行 |
| `AndonResponseAgent` | 安灯事件 | 可单独只读 | 指派/关闭 |
| `OperationsInsightAgent` | 经营看板、今日生产、质量待办 | 可单独只读 | 无 |

## 真实数据约束

1. `BaseBusinessAgent` 仅使用 `SpringBootClient` 的 GET 白名单，并携带当前用户的 MES JWT；实时事实最终来自 `fan_mes` 数据库。
2. 读取失败必须抛出错误，禁止回退为演示数据、写死结论或模型猜测。
3. `BusinessAgentState` 是一次调用的内存状态，绝不是数据库实体或工作流持久化状态。
4. 目录中的 `future_write_actions` 只是未来接口契约；当前没有任何写入实现。后续必须复用 Spring Boot 领域服务、角色权限、数量/状态校验与人工确认。
5. RAG 仅可附加 SOP、检验标准和处置规范解释，不能替代上述实时事实。

## 单独调用示例

```python
from app.business_agents import ProductionExecutionAgent
from app.business_agents.base import BusinessAgentState

result = ProductionExecutionAgent().run(
    BusinessAgentState(
        principal=principal,
        trace_id="agent-read-001",
        work_order_id=65,
        work_order_no="WO-20260701",
    )
)
```

这只读取真实数据，不会写入任何订单、工单、库存、质检或设备记录。
