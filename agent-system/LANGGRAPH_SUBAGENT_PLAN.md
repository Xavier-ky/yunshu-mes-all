# 云枢小智 LangGraph 子 Agent 规划

> 版本：v1.0（规划稿）  
> 依据：`MES-GOLDEN-PATH.md`、`docs/workflow-golden-path.md`、现有 `agent-system` MVP 代码  
> 第一优先级：黄金流程第 1–6 步——订单确认、工单下达、齐套预留、甘特排产、工序派工、生产领料。

## 1. 目标与边界

云枢小智的目标不是把每个 MES 菜单包装成“聊天功能”，而是让它能围绕同一张工单，理解问题、收集事实、提出计划、请求审批并安全地推进流程。

核心业务主键为 `work_order_id`。一项 Agent 任务还必须绑定：

- `thread_id`：用户对话/连续上下文；
- `task_id`：一次可追踪的 Agent 任务；
- `trace_id`：一次图执行的链路标识；
- `user_id`、`role`、`data_scope`：权限和数据范围；
- `lifecycle_status`：工单在黄金流程中的业务状态。

### 1.1 强约束

1. Agent 不允许直连 MySQL 或自行拼接 SQL；所有业务数据和写操作必须经过受 JWT 保护的 Spring Boot MES API 工具层。
2. 读数据、生成建议、生成草稿可以自动执行；会改变库存、工单、排产、派工、质量或通知状态的动作必须经过策略节点和审批。
3. 不把每一个步骤都实现成 LLM Agent。身份解析、状态校验、权限判断、库存锁定、写入、审批恢复等必须是确定性 LangGraph 节点或工具。
4. 未具备真实 API、表字段和数据契约的能力只能登记为“预留 Agent”，不得进入默认路由，更不得伪造结果。

## 2. 推荐的 LangGraph 总图

```text
START
  → 身份/数据权限节点
  → 会话与工单上下文装配节点
  → 意图与任务路由（Supervisor）
  → 读取事实 / 制定计划 / 专业 Agent 或工具子图
  → 结果校验与风险评审
  → [需要写操作] 人工审批 Interrupt → 执行工具 → 回写与复核
  → 回答生成、流式事件、审计与检查点
  → END
```

### 2.1 必须是节点而非子 Agent 的能力

| 节点 | 作用 | 备注 |
|---|---|---|
| `auth_scope_guard` | 解析 JWT、角色、组织与数据权限 | 所有图入口必经 |
| `context_resolver` | 解析工单号、订单号、任务号、物料、产线等实体 | 统一写入图状态 |
| `lifecycle_guard` | 校验黄金流程前置状态 | 例如入库前必须 `QC_PASSED` |
| `tool_policy_guard` | 校验工具白名单、参数 Schema、读写风险 | 不交给模型决定 |
| `approval_interrupt` | 暂停、展示审批卡、恢复同一任务 | LangGraph checkpoint/interrupt |
| `result_verifier` | 校验数据来源、时间、口径、是否存在冲突 | 防止无依据结论 |
| `audit_checkpoint` | 保存节点事件、工具调用、状态快照 | 支持工作流程侧栏与恢复 |
| `answer_streamer` | 输出 Token 与任务状态事件 | 前端可展示“规划/查询/审批/完成” |

## 3. 子 Agent 分层

### 3.1 P0：排产黄金流程默认调用的核心 Agent

这些 Agent 是第一期真正需要接入 LangGraph 的能力。默认图可调用它们，但每次只按任务需要调用，不要求全量串行执行。

| 编号 | 子 Agent | 黄金步骤 | 主要职责 | 核心只读工具 | 写入/审批动作 | 默认状态 |
|---|---|---:|---|---|---|---|
| A0 | `PlanningSupervisorAgent` | 1–6 | 理解目标、拆任务、选择子图、汇总结论；不直接写业务数据 | 任务上下文、各 Agent 摘要 | 无 | 启用 |
| A1 | `OrderCommitmentAgent` | 1 | 校验订单可生产性、交期、产品与数量，识别订单确认风险 | 订单、订单行、产品、交期、客户优先级 | 订单确认草稿；正式确认需审批 | 启用 |
| A2 | `WorkOrderReleaseAgent` | 2 | 从已确认订单生成/校验工单，绑定产品、BOM、工艺路线与计划数量 | 订单、BOM、工艺路线、现有工单 | 创建/下达工单需审批 | 启用 |
| A3 | `KittingReservationAgent` | 3 | 逐项分析 BOM 齐套、缺料、替代料、可锁库存，给出齐套结论 | BOM、库存、批次、已锁量、到货计划 | 库存预留/释放需审批 | 启用 |
| A4 | `CapacitySchedulingAgent` | 4 | 基于工艺、产线、工位、日历、产能生成甘特排产方案与多方案比较 | 工单、工艺、产线、工位、产能、日历、任务 | 保存/发布排产方案需审批 | 启用 |
| A5 | `DispatchOrchestrationAgent` | 5 | 将排产任务转换为工序派工；校验工位、人员、班次、技能与负荷 | 生产任务、工序、工位、人员、班次、技能 | 自动派工/调整派工需审批；可由系统服务执行 | 启用 |
| A6 | `MaterialIssueAgent` | 6 | 根据已派工和预留，生成领料建议、拣货顺序、批次建议和异常说明 | 领料单、库存、批次、预留、工单、派工 | 创建/执行领料需仓库角色审批 | 启用 |
| A7 | `SchedulingRiskAgent` | 1–6 支撑 | 聚合缺料、产能、设备、人员、质量、安灯风险，解释延期与冲突 | A1–A6 的结构化结果、安灯、质量、设备摘要 | 无，仅输出风险和建议 | 启用 |
| A8 | `SchedulePlanReviewerAgent` | 4–6 支撑 | 审核排产/派工/领料方案是否满足生命周期、交期和资源约束 | 方案草稿、约束校验结果、风险摘要 | 无；拒绝或退回草稿 | 启用 |

### 3.2 P1：黄金流程后四步的执行闭环 Agent

这些 Agent 应与 P0 使用同一张状态图和同一 `work_order_id`，但第一期不必由排产问题自动调用。

| 编号 | 子 Agent | 黄金步骤 | 主要职责 | 写入风险 | 默认路由 |
|---|---|---:|---|---|---|
| B1 | `ShopfloorExecutionAgent` | 7 | 查询派工、指导开工/报工、汇总实际产量、工时和异常 | 提交报工：高 | 按操作工问题调用 |
| B2 | `MaterialBindingAgent` | 7 支撑 | 指导 SN/批次绑定，校验物料可追溯性 | 绑定写入：中高 | 按扫描/追溯问题调用 |
| B3 | `InProcessQualityAgent` | 8 | 组织 IPQC/PQC 待检、检查标准、判定与不合格处置建议 | 提交检验：高 | 按质检问题调用 |
| B4 | `FinishedGoodsReceiptAgent` | 9 | 校验 `QC_PASSED`、生成成品入库建议、上架策略 | 执行入库：高 | 按仓库问题调用 |
| B5 | `TraceabilityAgent` | 10 | 以工单/SN/批次反查领料、报工、质检、入库和绑定链路 | 只读 | 按追溯问题调用 |

### 3.3 P2：已规划但暂不默认调用的预留 Agent

以下能力覆盖项目已有或未来可能补齐的模块。没有真实工具与数据契约前，只在注册表中以 `reserved` 标记，不参与 Supervisor 默认候选集。

| 编号 | 预留子 Agent | 业务域 | 未来职责 | 触发条件 |
|---|---|---|---|---|
| C1 | `EquipmentCapacityAgent` | 设备 | 提供设备状态、OEE、停机、保养窗口给排产 | 排产资源不足或设备风险 |
| C2 | `MaintenanceDecisionAgent` | 设备 | 维修/保养建议、故障工单、停机影响评估 | 设备报警、安灯、故障趋势 |
| C3 | `WorkforceSkillAgent` | 人员 | 人员技能、资质、班次、负荷和替班建议 | 派工或缺员风险 |
| C4 | `ShiftCalendarAgent` | 人员/工厂 | 班次日历、假期、加班和产能时段 | 排产时间计算 |
| C5 | `ProcessRouteAgent` | 工艺 | 工艺路线版本、工序约束、标准工时、工装要求 | 工单下达、排产、工艺变更 |
| C6 | `MasterDataGovernanceAgent` | 主数据 | 产品、BOM、物料、单位、替代料、版本完整性检查 | 工单/BOM 数据异常 |
| C7 | `SupplierMaterialRiskAgent` | 供应链 | 到货承诺、供应风险、替代料与采购催料建议 | 齐套不足 |
| C8 | `InventoryOptimizationAgent` | 仓储 | 安全库存、呆滞料、批次优先级、库位优化 | 库存分析任务 |
| C9 | `AndonCoordinationAgent` | 安灯 | 创建、分派、升级、关闭安灯及影响范围评估 | 现场异常、风险升级 |
| C10 | `QualityAnalyticsAgent` | 质量 | 缺陷 Pareto、趋势、批次/工位关联、CAPA 建议 | 质量分析任务 |
| C11 | `DocumentKnowledgeAgent` | 知识库 | 检索 SOP、工艺规程、作业指导书、制度；返回可引用片段 | 需要规范依据 |
| C12 | `KPIAnalyticsAgent` | 数据分析 | 产量、交付、OEE、良率、库存等 KPI 分析与报告 | 管理驾驶舱问题 |
| C13 | `NotificationAgent` | 协同 | 将已审批的通知发送给角色、班组或负责人 | 经业务 Agent 发起 |
| C14 | `IntegrationAgent` | 集成 | ERP/PLM/WMS/设备数据接口状态与数据对账 | 外部数据异常 |
| C15 | `AccessGovernanceAgent` | 系统 | 解释菜单、角色、数据权限；不直接授予权限 | 系统管理问题 |
| C16 | `ContinuousImprovementAgent` | 改善 | 汇总异常、生成改善课题、跟踪措施效果 | 管理改善任务 |

## 4. P0 排产子图设计

### 4.1 排产前六步的标准路径

```text
订单上下文
  → A1 OrderCommitmentAgent
  → A2 WorkOrderReleaseAgent
  → A3 KittingReservationAgent
  → [C1 设备能力 / C3 人员技能 / C4 班次日历：有数据后并行读取]
  → A4 CapacitySchedulingAgent
  → A8 SchedulePlanReviewerAgent
  → 审批（发布排产）
  → A5 DispatchOrchestrationAgent
  → 审批（确认派工；可配置自动派工）
  → A6 MaterialIssueAgent
  → 审批（仓库执行领料）
  → A7 SchedulingRiskAgent 汇总与回复
```

### 4.2 重要分支

| 条件 | 图的行为 |
|---|---|
| 订单未确认或产品/BOM/工艺缺失 | 停在 A1/A2，返回缺失项，不创建工单 |
| 齐套不足 | A3 输出缺料明细；转 C7（未来）或生成催料建议；不能发布依赖缺料的方案 |
| 产线、设备或人员能力不足 | A4 请求资源数据；生成备选甘特方案；不可直接强行排产 |
| 排产方案违反交期或生命周期 | A8 退回 A4，要求重算或人工决策 |
| 派工缺少合格人员 | A5 返回人员/技能缺口；未来调用 C3/C4 |
| 领料与预留不一致、批次受限 | A6 阻止执行，返回仓库处理清单 |
| 任一写操作未获批 | `approval_interrupt` 保存 checkpoint，等待恢复，不重新执行已经完成的读取节点 |

## 5. 工具契约与审批矩阵

### 5.1 P0 最小工具包

| Agent | 建议工具 | 类型 |
|---|---|---|
| A1 | `get_order`、`get_order_items`、`validate_order_confirm`、`draft_order_confirm` | 读 / 草稿 |
| A2 | `get_work_order`、`get_bom_version`、`get_process_route`、`draft_work_order`、`release_work_order` | 读 / 写 |
| A3 | `analyze_kitting`、`get_stock_by_material`、`get_reserved_stock`、`reserve_materials`、`release_reservation` | 读 / 写 |
| A4 | `get_capacity_calendar`、`get_production_tasks`、`simulate_schedule`、`save_schedule_draft`、`publish_schedule` | 读 / 写 |
| A5 | `get_dispatch_candidates`、`get_operator_skill`、`sync_dispatch_draft`、`confirm_dispatch` | 读 / 写 |
| A6 | `get_issueable_stock`、`get_issue_document`、`draft_material_issue`、`execute_material_issue` | 读 / 写 |
| A7/A8 | `get_workflow_pipeline`、`get_andon_summary`、`validate_schedule_constraints` | 只读 |

### 5.2 审批规则

| 动作等级 | 示例 | 策略 |
|---|---|---|
| L0：只读 | 查询工单、库存、排产、风险 | 自动执行，保留审计 |
| L1：生成草稿 | 生成工单草稿、排产建议、领料建议 | 自动生成，明确标记“未执行” |
| L2：影响计划 | 确认订单、下达工单、发布排产、调整派工 | 生产主管或授权角色审批 |
| L3：影响账实 | 锁定/释放库存、执行领料、提交报工、提交质检、执行入库 | 业务角色审批 + 后端生命周期校验 |
| L4：外部影响 | 通知、安灯升级、外部系统同步 | 业务审批；必要时二次确认 |

## 6. LangGraph 状态模型（建议）

```python
class MesAgentState(TypedDict):
    thread_id: str
    task_id: str
    trace_id: str
    user: dict                 # user_id, role, data_scope
    request: str
    entities: dict             # work_order_id/no, order_id/no, task_id, material, line...
    lifecycle: dict            # current status and allowed next transitions
    intent: str
    plan: list[dict]
    agent_results: dict        # agent_name -> structured result
    tool_calls: list[dict]
    risk_items: list[dict]
    approval: dict | None
    answer: str
    events: list[dict]         # streamed planning/tool/approval events
    errors: list[dict]
```

状态中的业务结果必须是结构化对象，最终回复只是它的可读视图；不能把事实只留在模型自然语言中。

## 7. 现有 agent-system 的迁移映射

| 现有 MVP | 目标演进 |
|---|---|
| `MainAgent` | 演进为 `PlanningSupervisorAgent`，只负责路由、计划与汇总 |
| `ScheduleAgent` | 拆分为 A1/A2/A3/A4/A5，避免“排产”成为大而不可审计的黑盒 |
| `WarehouseAgent` | 拆分为 A3 齐套预留、A6 领料、C8 库存优化 |
| `ProductionAgent` | 演进为 B1 现场执行与 B2 物料绑定 |
| `QualityAgent` | 演进为 B3 过程质检与 C10 质量分析 |
| `EquipmentAgent` | 保留为 C1/C2；没有真实设备能力数据前不进默认排产路由 |
| `AndonAgent` | 演进为 C9，作为风险升级能力，不替代排产主线 |
| `AnalyticsAgent` | 演进为 A7 风险分析、A8 方案评审、C12 KPI 分析 |
| `SystemAgent` | 收敛为 C15 权限/系统解释，不参与业务写操作 |
| 硬编码 `workflow.py` | 替换为 `StateGraph` + 子图 + 工具策略层 + checkpoint |
| Mock `tools/registry.py` | 逐步替换为 Spring Boot 受控 API 工具；Mock 仅用于开发测试并明确标记来源 |

## 8. 推荐目录（目标态）

```text
agent-system/backend/app/
├── langgraph/
│   ├── state.py                 # MesAgentState 与 reducer
│   ├── main_graph.py            # 总图与入口路由
│   ├── graphs/
│   │   ├── scheduling_graph.py  # P0：步骤 1–6
│   │   ├── execution_graph.py   # P1：步骤 7–9
│   │   └── trace_graph.py       # P1：步骤 10
│   ├── nodes/                   # 权限、实体解析、审批、校验、流式、审计
│   └── persistence/             # checkpointer 与长期记忆适配
├── agents/
│   ├── planning/                # A0–A8
│   ├── execution/               # B1–B5
│   └── reserved/                # C1–C16，不默认注册到路由
├── tools/
│   ├── planning/
│   ├── warehouse/
│   ├── production/
│   ├── quality/
│   └── policy.py
├── integrations/
│   └── spring_boot_client.py    # 唯一 MES 业务数据入口
├── storage/
│   ├── checkpoint_store.py
│   ├── task_store.py
│   └── audit_store.py
└── evals/
    └── golden_path/             # 按 WO-GP-* 回归评测
```

## 9. 分期落地建议

### 第一阶段：可解释的排产只读 Agent

- 落地 A0、A3、A4、A7、A8 的只读版。
- 接入真实 `work_order`、`kitting_analysis`、`production_task`、`dispatch_task` 和黄金流程 pipeline API。
- 输出“为什么不能排/能否按期交付/缺什么资源/备选方案”，不写任何业务数据。

### 第二阶段：排产草稿与审批

- 落地 A1、A2、A5、A6 的草稿工具。
- 所有写操作进入审批卡；使用 checkpoint 保留任务状态并可恢复。
- 在前端 Agent 流程侧栏显示每个节点、工具、结果摘要和审批状态。

### 第三阶段：执行闭环

- 接入 B1–B5，使同一 `work_order_id` 从领料到报工、质检、入库、追溯完整闭环。
- 用黄金工单 `WO-GP-20260714` 建立端到端评测集。

### 第四阶段：资源与改善能力

- 逐项接入 C1–C16；仅当设备、人员、主数据等模块有真实 API、权限和验收样例时，才加入默认路由。

## 10. 实现核对：十步流程、数据库与 Agent 边界

以下映射已经对照 `MES-GOLDEN-PATH.md`、`docs/workflow-golden-path.md`、Spring Boot 的 Controller / Service 以及迁移 SQL。它定义的是 **Agent 可以理解和编排的真实事实源**，不是一份另起炉灶的流程设想。

### 10.1 业务脊柱与事实快照

主关联链为：

```text
customer_order / customer_order_item
  └─ order_id
     └─ work_order (work_order_id, work_order_no, lifecycle_status)
        ├─ kitting_analysis
        ├─ production_task (task_id)
        │  └─ dispatch_task (dispatch_id, task_id, operator_id)
        ├─ wm_issue_header / line / detail → wm_item_consume
        ├─ pro_feedback → production_report
        ├─ qc_ipqc
        ├─ wm_product_recpt / line / detail
        └─ product_sn → product_material_binding → inventory_batch
```

`work_order_id` 是跨 Agent、跨步骤、跨角色的唯一业务脊柱；`work_order_no` 仅作为用户输入、展示和追溯查询键。进入图后的 `entity_resolver` 必须将工单号解析为工单 ID，并把 ID 固化在 `MesAgentState.entities`。任何工具结果都必须附带 `source`、`queried_at`、`work_order_id` 和必要的单据 ID，禁止仅把结果写进自然语言回复。

### 10.2 十步实现映射

| 步骤 | 已实现的业务入口 / 串联 | 主表与关键关联 | 生命周期或真实写回 | 建议负责 Agent 与工具等级 |
|---|---|---|---|---|
| 1. 确认订单 | `/api/planning/orders`；页面 `/app/planning/orders` | `customer_order`、`customer_order_item`，通过 `order_id` 进入工单 | 订单确认本身不应由模型越权提交 | A1 订单履约：L0 查询、L1 草稿、L2 主管确认 |
| 2. 下达工单 | `/api/mes/pro/workorder`；页面 `/app/planning/work-orders` | `work_order`，可关联 `work_order_bom` | 从 DRAFT 进入 RELEASED；工单创建须保留订单、产品、数量、交期事实 | A2 工单下达：L0/L1；真实发布为 L2 |
| 3. 齐套预留 | `GET /api/planning/kitting/{workOrderId}`；`POST .../reserve` 或 `.../release` | `kitting_analysis`，BOM / 库存数据；均以 `work_order_id` 汇总 | 预留成功推进 KITTING_OK；释放预留是账实影响操作 | A3 齐套预留：分析 L0，预留/释放 L3 |
| 4. 甘特排产 | `/api/mes/pro/protask`；甘特读取 `listGanttTaskList`，页面 `/app/planning/scheduling` | `production_task(task_id, work_order_id)`，含产线、工位、工序、计划量、时间 | 保存任务后可推进 SCHEDULED | A4 产能排产：L0 仿真、L1 草稿、发布 L2 |
| 5. 工序派工 | `DispatchSyncService.syncFromProductionTask(taskId)` | `dispatch_task(dispatch_id, task_id, work_order_id, station_id, operator_id, planned_qty)` | 当 `mes.workflow.auto-dispatch=true`，排产任务保存会自动补建派工并推进 SCHEDULED | A5 派工编排：默认只解释和校验；人工调整为 L2 |
| 6. 生产领料 | 领料单创建/执行；执行入口为 `PUT /api/mes/wm/issueheader/{id}` | `wm_issue_header` / `wm_issue_line` / `wm_issue_detail`，外键字段为 `workorder_id` | 执行时校验数量，写入 `wm_item_consume`，工单推进 MATERIAL_ISSUED | A6 领料：查询/草稿 L0/L1，执行 L3 |
| 7. 现场报工 | `POST /api/mes/pro/feedback` 创建，`PUT /api/mes/pro/feedback/execute/{recordId}` 执行；页面 `/app/my-work` | `pro_feedback(workorder_id)`，回写 `dispatch_task.completed_qty` 与 `production_report` | 执行前校验阶段；依次推进 IN_PROGRESS、QC_PENDING | B1 现场执行：查询 L0，提交/执行 L3 |
| 8. 过程质检 | `PUT /api/mes/qc/ipqc`，单据状态 FINISHED；页面 `/app/quality/workbench` | `qc_ipqc(workorder_id)`、`qc_ipqc_line` | ACCEPT → QC_PASSED；其他结果 → QC_FAILED | B3 过程质检：分析 L0，提交结果 L3 |
| 9. 成品入库 | `POST /api/mes/wm/productrecpt/fromWorkOrder/{workorderId}` 创建；`PUT /api/mes/wm/productrecpt/{recptId}` 执行 | `wm_product_recpt` / line / detail，外键字段为 `workorder_id` | 仅 QC_PASSED / COMPLETED 可执行；执行后 COMPLETED，并同步追溯数据 | B4 成品入库：草稿 L1，执行 L3 |
| 10. 追溯查询 | `GET /api/traceability/work-order/{workOrderNo}`；页面 `/app/analytics/trace` | 汇总上述单据，并读取 `product_sn`、`product_material_binding`、`inventory_batch` | 查询会补同步该工单追溯数据；不改变业务单据状态 | B5 追溯闭环：L0 只读 |

### 10.3 现有生命周期是图的确定性守门人

不能让 LLM 自行判断“是否可以执行”。`WorkOrderLifecycleService` 已经给出可复用的后端事实门禁，LangGraph 只负责在调用前解释、在调用后刷新：

```text
DRAFT → RELEASED → KITTING_OK → SCHEDULED → MATERIAL_ISSUED
  → IN_PROGRESS → QC_PENDING → QC_PASSED → COMPLETED
                              └→ QC_FAILED（退回主管处理）
```

- 领料只允许 `RELEASED`、`KITTING_OK`、`SCHEDULED`。
- 报工只允许 `MATERIAL_ISSUED`、`IN_PROGRESS`、`QC_PENDING`。
- 成品入库只允许 `QC_PASSED` 或已完成状态。
- `QC_FAILED` 必须转 A7 风险诊断和 A0/生产主管，不得自动重新入库或跨越质量门禁。
- 存量工单的 `lifecycle_status` 可能为 `null`，后端当前兼容性策略是不拦截。Agent 必须在结果中标记为“历史工单 / 生命周期未建档”，只可给建议，不得把它当作已验证的正常路径。

`GET /api/planning/workflow/pipeline/{key}` 是首选工单事实快照；`GET /api/planning/workflow/todos?role=` 是角色待办事实源。图中的 `lifecycle_guard` 应读取它们，并仍以写工具返回的后端校验结果为最终准则。

## 11. 从真实接口到受控 Agent 工具

### 11.1 可立即纳入只读工具的接口

第一期不必新建数据库表，也不应让 Python 服务直连 MySQL。应先让 `SpringBootClient` 以当前用户 JWT 调用下列受限资源，并把响应标准化为结构化事实：

| 工具名 | MES API | 服务对象 | 供哪些 Agent 使用 |
|---|---|---|---|
| `get_workflow_pipeline` | `GET /api/planning/workflow/pipeline/{workOrderNo}` | 生命周期、当前节点、下一动作 | A0、A2–A8、B1–B5 |
| `get_role_todos` | `GET /api/planning/workflow/todos?role=` | 当前角色待办工单 | A0、任务历史侧栏 |
| `get_kitting_analysis` | `GET /api/planning/kitting/{workOrderId}` | 齐套率、缺料明细 | A3、A4、A7 |
| `get_work_order_tasks` | `GET /api/mes/pro/protask/listTaskListByWorkorder?workorderId=` | 工序、工位、计划任务 | A4、A5、A7、B1 |
| `get_gantt_snapshot` | `GET /api/mes/pro/protask/listGanttTaskList` | 排产时间线 | A4、A7、A8 |
| `get_work_order_trace` | `GET /api/traceability/work-order/{workOrderNo}` | 领料、报工、质检、入库、SN、批次全链 | A7、B5、C10 |
| `get_recent_traceable_work_orders` | `GET /api/traceability/recent-work-orders` | 可追溯工单清单 | B5、历史任务列表 |

现有 `agent-system/backend/app/integrations/spring_boot_client.py` 的 allowlist 仅包含 dashboard、订单外的工单列表、库存、安灯、设备、质量等通用资源，尚未包含上表；`tools/registry.py` 和 `graph/workflow.py` 仍返回 Mock 数据。因此在这些资源加入 allowlist 且被真实工具替换前，不得把当前 `ScheduleAgent` 的输出标为真实 MES 结论。

### 11.2 写入必须经“草稿 / 审批 / 领域服务”三层

现有 compat API 是 UI 兼容入口，不适合将任意请求体直接暴露给模型。建议在 Spring Boot 新增面向 Agent 的窄门面（例如 `/api/agent/operations/...`），其内部调用既有领域服务，绝不由 FastAPI 拼 SQL 或绕开 Service：

| Agent 操作 | Agent 门面语义 | 复用的既有业务服务 / 接口 | 必填控制 |
|---|---|---|---|
| 齐套预留 | `preview_reservation` / `reserve_after_approval` | KittingController 的分析、预留、释放逻辑 | 工单、BOM、库存版本；L3 审批与幂等键 |
| 发布排产 | `simulate_schedule` / `publish_schedule` | ProTaskService + `DispatchSyncService` | 产能校验、任务差异摘要、L2 审批 |
| 调整派工 | `preview_dispatch_change` / `confirm_dispatch_change` | `dispatch_task` 领域服务 | 工位/人员授权、冲突检测、L2 审批 |
| 领料执行 | `draft_issue_from_work_order` / `execute_issue_after_approval` | WmIssueService 的创建、数量校验、execute | 生命周期、批次/库存版本、L3 审批、幂等键 |
| 报工执行 | `draft_feedback` / `execute_feedback_after_approval` | ProFeedbackService.execute | 只允许对应操作工与派工；数量范围、L3 审批 |
| 质检结果 | `draft_ipqc_result` / `submit_ipqc_after_approval` | QcIpqcController 的 FINISHED 写回 | 质检角色、检验模板/合格量校验、L3 审批 |
| 成品入库 | `draft_product_receipt` / `execute_receipt_after_approval` | WmProductRecptService 的创建、execute | QC 门禁、数量校验、L3 审批、幂等键 |

每个执行工具应接收 `task_id`、`thread_id`、`approval_id`、`idempotency_key`、`expected_lifecycle`、`expected_updated_at/version`；返回 `business_document_id`、`work_order_id`、`before/after_lifecycle`、`affected_rows`、`trace_id`。这样即使 LangGraph 从 checkpoint 恢复，也不会重复领料、重复报工或重复入库。

## 12. 子 Agent 如何串成真实图

### 12.1 排产前六步子图

```text
resolve_order_or_work_order
  → load_pipeline_snapshot
  → A1/A2（仅按用户意图进入；草稿优先）
  → A3 get_kitting_analysis
  → [欠料] A7 风险解释 → approval / 结束
  → A4 get_gantt_snapshot + 生成可解释方案
  → A8 规则复核（交期、工序、生命周期、冲突）
  → [通过] approval_interrupt（发布排产）
  → publish_schedule
  → refresh pipeline + dispatch result
  → A5 派工校验
  → [需要领料] A6 草稿 → approval_interrupt → execute_issue
  → refresh pipeline → answer / audit_checkpoint
```

其中排产任务保存后的 `DispatchSyncService` 可能自动创建 `dispatch_task`（受 `mes.workflow.auto-dispatch` 配置控制）。A5 不能假设“所有排产一定已有派工”：必须读取实际派工结果；未自动派工时只输出缺口与操作建议，不自创派工记录。

### 12.2 执行闭环子图

```text
MATERIAL_ISSUED
  → B1 查询本人 dispatch_task / 生成报工草稿
  → approval_interrupt → execute_feedback
  → refresh pipeline (QC_PENDING)
  → B3 查询/提交 IPQC
  ├─ QC_FAILED → A7 风险单 + 主管待办（停止）
  └─ QC_PASSED → B4 入库草稿 → approval_interrupt → execute_receipt
       → refresh pipeline (COMPLETED) → B5 trace_work_order
```

`WorkflowPipelineService` 已根据角色定义待办：仓库处理领料和质检通过后的入库，操作工处理领料后的报工，质检员处理 `QC_PENDING`，生产主管/经理处理早期状态和 `QC_FAILED`。`authorization_node` 必须使用 JWT 中的实际角色与数据范围，不能信任前端传入的角色字符串。

### 12.3 现有写回的处理原则

- 领料执行会同步 `wm_item_consume`；不能再由 Agent 额外写一笔消耗记录。
- 报工执行会回写 `dispatch_task.completed_qty` 并调用 `ProductionReportSyncService`；后者是同步增强，Agent 应读取回写结果并在失败时标记“报表待补同步”，不能把报表同步失败伪装为报工失败。
- 入库执行会推进 COMPLETED 并调用追溯同步；B5 应在执行后重新读取追溯接口，而非在模型上下文中推断 SN/批次已生成。

## 13. 对现有 agent-system 的具体改造顺序

1. **先做真实只读。** 用 LangGraph `StateGraph` 替换 `workflow.py` 的关键词分支；`SpringBootClient` 增加第 11.1 节的 allowlist 和 JSON Schema 校验；逐个替换 `tools/registry.py` 的 Mock 工具。入口必须注入并传递当前 JWT、`trace_id` 与数据范围。
2. **再做排产草稿。** 先完成 A3/A4/A7/A8：给出齐套、甘特、风险、备选方案和引用的数据快照；不写业务表。
3. **把审批与幂等落在写入之前。** 使用 LangGraph interrupt/checkpointer 保存审批卡，不把审批状态仅存在浏览器；通过受控 Agent 门面调用既有领域服务。
4. **最后开启执行写操作。** 按 A6 → B1 → B3 → B4 顺序接入，每一步后强制刷新 pipeline；B5 只在 COMPLETED 或用户明确追溯时运行。
5. **建立真实回归集。** 用 `WO-GP-*` 种子与现有 `scripts/workflow-golden-path-e2e.ps1` / `workflow-golden-path-test.ps1` 覆盖正常、欠料、QC_FAILED、重复审批恢复四条路径；断言单据数量、生命周期和追溯结果，而不是只断言模型文本。

## 14. RAG 知识底座：先建，但不替代实时工具

### 14.1 三种数据必须分层

一个专业 MES Agent 不能只有“向量数据库”。它至少有三类完全不同的数据，查询路径、更新权限和可信度都不同：

| 数据层 | 解决的问题 | 本项目对应内容 | 数据来源与更新方式 | 能否作为执行依据 |
|---|---|---|---|---|
| **实时业务事实层（Tools）** | “这张工单现在怎么样、库存还有多少、质检是否通过？” | `work_order`、`production_task`、`dispatch_task`、库存、领料、报工、IPQC、入库 | Spring Boot 受 JWT 保护的 API，即时读取 | 可以；以后由后端事务与生命周期最终裁决 |
| **RAG 知识层（Knowledge）** | “齐套不足怎么处理、这个工序为何需要质检、排产冲突应遵循什么规则？” | SOP、十步黄金流程、工艺说明、质量规范、设备故障手册、制度、FAQ | 发布审核后的文档版本；异步切分、向量化、索引 | 只能提供解释、建议和规则依据；不能取代当前业务事实 |
| **状态与记忆层（State / Memory）** | “这次任务进行到哪、谁审批过、用户偏好是什么？” | LangGraph checkpoint、Agent 任务/步骤/审批记录、用户偏好 | 任务执行与受控人工维护 | 不能充当业务账实或公共制度来源 |

因此，答案是：**应该先建立 RAG 知识底座，但不应先把“实时 MES 数据库”做成 RAG 数据库。** 工单、库存、状态等高时效数据必须继续通过工具查询；RAG 是小智的“知识脑”，工具是它的“眼睛和手”。

### 14.2 第一版 RAG 的边界与知识目录

第一版只收录高价值、相对稳定、可注明版本与责任人的知识，先不把整库数据、聊天记录、操作日志和动态报表倒入向量库。

| 知识域 | 第一批资料 | 核心元数据 | 主要服务 Agent |
|---|---|---|---|
| 黄金流程与角色 SOP | `MES-GOLDEN-PATH.md`、`docs/workflow-golden-path.md`、角色操作说明 | `domain=workflow`、`step`、`role`、`version`、`effective_date`、`owner` | A0、A1–A8、B1–B5 |
| 生命周期与业务规则 | 生命周期状态图、领料/报工/质检/入库门禁说明、审批规则 | `domain=rule`、`lifecycle_status`、`authority=system`、`source_path` | A0、A7、A8、B3、B4 |
| 工艺与质量 | 工艺路线说明、检验规范、缺陷处置 SOP、返工规范 | `domain=process/quality`、`product_id/product_code`、`process_id`、`revision` | A4、B3、C5、C10 |
| 仓储与物料 | BOM 解释文档、齐套/领料/批次管理规范、异常处理 SOP | `domain=warehouse`、`material_code`、`warehouse_scope`、`revision` | A3、A6、C7、C8 |
| 设备与现场 | 点检、维修、换型和安灯处置手册 | `domain=equipment/andon`、`device_type`、`line_id`、`revision` | C1、C2、C9 |

对于 `BOM`、有效工艺路线、现有库存、当前设备状态等内容，RAG 可以存“解释和操作规范”，但活的版本、数量与可用性仍必须通过 MES 工具查实。

### 14.3 目标 RAG 数据模型与检索链路

建议将原文、元数据与向量索引分开保存，不让向量库承担文档治理：

```text
原始文件 / Markdown / PDF
  → rag_document（文档编号、来源、责任人、版本、生效/失效时间、权限域、校验哈希）
  → rag_chunk（chunk_id、document_id、标题层级、文本、页码/段落、结构化元数据）
  → 向量索引（chunk_id、embedding、可过滤元数据）

用户问题
  → intent_router
  ├─ 实时问题：MES Tool 查询
  ├─ 规则/SOP 问题：RAG 混合检索 + 重排
  └─ 混合问题：先 Tool 获取当前事实，再以事实实体过滤 RAG，最后综合回答
```

每个 chunk 至少保存：`document_id`、`title_path`、`source_path/url`、`page_or_anchor`、`domain`、`role_scope`、`factory_scope`、`product_code`、`process_code`、`lifecycle_status`、`version`、`effective_from`、`effective_to`、`owner`、`review_status`、`content_hash`。回答必须输出可点击或可定位的知识引用；检索到已失效、未审核或无权限的内容时不得进入模型上下文。

检索采用“关键词 / 元数据过滤 + 语义检索 + 重排 + 引用校验”的混合链路。不要一开始就上 GraphRAG：现有 MES 的工单关系已经由关系数据库和 API 表达，先把文档质量、元数据、权限和评测做好，只有跨产品—工艺—设备的知识关联确实检索不足时才评估图检索。

### 14.4 RAG MVP 的正确实施顺序

1. **知识治理清单。** 为每份首批文档指定责任人、有效版本、权限域和更新方式；未审核资料不入库。
2. **文档解析与可追溯切分。** 保留标题层级、页码/段落锚点，不按固定字数粗暴切碎流程和表格。
3. **索引与检索服务。** 建立 `rag_document`、`rag_chunk` 元数据表和独立向量索引；向量存储选型以本地部署、中文检索、元数据过滤、备份和权限隔离为验收条件，而不是先绑定某个产品。
4. **先做 RAG 工具，不把文档直接塞进 Prompt。** 对 LangGraph 暴露 `search_knowledge(query, filters)`，返回片段、来源、版本、置信度；由 `knowledge_guard` 过滤无效内容。
5. **建立评测集后再接主图。** 先准备至少 30–50 个有标准来源的 SOP / 生命周期 / 质量处置问题，验收命中来源、引用正确性、拒答能力和权限隔离，再让 A0 路由到 RAG。
6. **再与实时 Tools 融合。** 例如“WO-GP-xxx 为什么不能入库？”必须同时读取该工单的实时 `QC_PENDING/QC_PASSED` 状态，并检索“成品入库门禁”规则；任一侧缺失时明确告知用户，不能编造结论。

### 14.5 与 LangGraph 的第一个专业节点

第一张图应增加 `knowledge_router`，而不是让所有 Agent 无差别检索：

```text
authenticate → entity_resolver → intent_router
  ├─ knowledge_router → search_knowledge → citation_guard
  ├─ mes_tool_router → real MES tools → fact_guard
  └─ hybrid_merge → answer_with_sources → audit_checkpoint
```

共享制度和 SOP 默认只读，禁止模型将用户对话写回知识库；用户偏好、任务摘要则进入用户/线程隔离的 Memory。LangGraph 的线程状态、长期记忆与 RAG 语料库必须使用不同 namespace 和写权限，避免把一次聊天中的错误内容沉淀为全局规则。

## 15. 当前结论

第一期不应直接启用二十多个 Agent。应以 **A0–A8** 建立“排产黄金流程图”，其中 A1–A6 对应前六步，A7/A8 负责风险与确定性复核；B1–B5 共享同一 `work_order_id` 与生命周期图，形成后四步闭环。设备、人员、工艺和供应链 Agent 作为可插拔资源输入，先注册、后在具备真实 API、权限和验收样例时接入。

最关键的落地事实是：MES 端已有真实的生命周期门禁和部分跨表同步，Agent 的职责是读取事实、生成方案、申请审批、调用受控领域能力并复核结果；**不是**直连数据库或替代既有业务服务。这样既能用上当前已经串通的十步链路，也不会让模型破坏库存、质量和追溯的一致性。
