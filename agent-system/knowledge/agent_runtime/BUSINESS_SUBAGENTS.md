# 云枢小智业务子 Agent 清单与真实数据边界

> 版本：1.0（2026-07-16）  
> 说明：下表区分“已参与默认工作流”和“已独立包装、可被后续编排调用”。独立包装不等于已自动执行或已具有写入能力。

## 1. 已参与智能排产主链的子 Agent

| Agent | 业务边界 | 默认链路状态 | 数据/写入原则 |
|---|---|---|---|
| `IntentRoutingAgent` | 意图识别、规则解释 | 已参与排产和质量工作流 | 使用受控模型与角色 RAG；不直接写业务数据 |
| `OrderIntakeAgent` | 订单校验、确认、工单生成 | 已参与排产 | 复用订单中心与工单中心接口；确认前复读最新订单 |
| `BomRouteAgent` | BOM 与工艺路线快照 | 已参与排产 | 仅从 MES 读取 |
| `KittingRiskAgent` | 齐套、短缺和物料风险 | 已参与排产 | 仅从 MES 读取 |
| `KittingExecutionAgent` | 原子锁料、任务生成、排产派工同步 | 已参与排产 | 仅调用原有原子命令一次 |
| `CapacitySchedulingAgent` | 设备、人员等辅助排程建议 | 已参与排产 | 展示建议和执行结果，不重复写入 |
| `DispatchExecutionAgent` | 派工写回结果投影 | 已参与排产 | 读取/解释派工同步结果 |
| `SchedulingValidationAgent` | 工单、任务、派工一致性核验 | 已参与排产 | 读取/解释写回状态 |
| `ProductionIssueAgent` | 生产领料、出库与物料消耗 | 已参与排产 | 复用受控生产领料执行接口 |

## 2. 已独立包装的七个业务 Agent

这些 Agent 均通过 `SpringBootClient` 的 GET 白名单访问真实 MES 数据，并转发当前用户 JWT 与 `trace_id`。读取失败必须抛错，不能返回演示数据；`BusinessAgentState` 仅是单次内存输入。

| Agent | 真实只读事实 | 需要的关键上下文 | 当前写入状态 |
|---|---|---|---|
| `ProductionExecutionAgent` | 工单流水、生产任务、派工快照 | `work_order_id`；工单流水可附带 `work_order_no` | 未来报工草案/执行契约，尚未接入 |
| `QualityManagementAgent` | 质量概览；可附加工单质量追溯 | 可传统计天数；工单追溯需要 `work_order_no` | 已被质量分析/报告工作流作为只读事实节点使用；质量写入尚未由该包装层实现 |
| `WarehouseLogisticsAgent` | 库存批次、工单齐套快照 | 工单齐套需要 `work_order_id` | 未来来料、退料、成品入库契约，尚未接入 |
| `TraceabilityAgent` | 领料、报工、质量、入库、批次与 SN 工单追溯快照 | `work_order_no` | 无写入能力 |
| `EquipmentMaintenanceAgent` | 设备工作台摘要和待办 | 可传筛选参数 | 未来保养/维修草案与执行契约，尚未接入 |
| `AndonResponseAgent` | 安灯事件 | 可传筛选参数 | 未来指派/关闭契约，尚未接入 |
| `OperationsInsightAgent` | 经营看板、今日生产、质量待办 | 可传筛选参数 | 无写入能力 |

## 3. 编排时必须遵守的共性规则

1. 先确定需要的实时事实，再调用对应 Agent；不能只凭 RAG 或模型回答当前业务状态。
2. 非默认工作流的独立 Agent 可以作为 LangGraph 后续节点接入，但必须先定义触发条件、输入标识、权限、输出卡片和失败分支。
3. `future_write_actions` 只是接口设计方向，当前不能被当成真实能力调用。
4. 需要新写动作时，应复用 Spring Boot 领域服务、事务和人工确认，而非从 Agent 直接操作 MySQL。
5. UI 中的 Agent 总览服务于可视化，不等同于该 Agent 已加入默认主对话或具有自动执行权限。

## 4. 任务到 Agent 的推荐路由

| 任务类型 | 首选 Agent | 必须辅以的依据 |
|---|---|---|
| 新订单智能排产 | 九节点排产链 | 订单、BOM、齐套、排产/派工、领料实时接口 |
| 今日质量分析 | `QualityManagementAgent` | 质量概览实时门面 + 质量规则 |
| 质量报告/导出 | `QualityManagementAgent` + `QualityReportAgent` | 新鲜报告快照 + 报告审计存储 |
| 现场任务与派工进度 | `ProductionExecutionAgent` | 真实 `work_order_id` / `work_order_no` |
| 库存、批次、齐套 | `WarehouseLogisticsAgent` | 当前库存/批次和工单上下文 |
| 工单全链追溯 | `TraceabilityAgent` | 真实 `work_order_no` |
| 设备待办与停机影响 | `EquipmentMaintenanceAgent` | 设备工作台实时数据 |
| 安灯响应 | `AndonResponseAgent` | 安灯事件实时数据和后续人工确认 |
| 管理看板、生产/质量待办 | `OperationsInsightAgent` | 当前看板及待办实时数据 |
