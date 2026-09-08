# 云枢智造 MES 全业务子 Agent 目录

> 版本：2026-07-16  
> 原则：每个 Agent 对应一个稳定业务责任，而不是一个页面或一张表。实时业务事实来自 MES 受控工具；RAG 只提供 SOP、制度和规则依据。任何写入均须复用既有领域服务，并按风险执行确认或审批。

## 1. 当前已包装且已运行的智能排产子图

```text
IntentRouting
  → OrderIntake
  → BomRoute
  → KittingRisk
  → KittingExecution
  → CapacityScheduling
  → DispatchExecution
  → SchedulingValidation
  → ProductionIssue
```

| Agent | 真实模块 / 服务 | 数据边界 | 状态 |
|---|---|---|---|
| `IntentRoutingAgent` | `llm_analysis.py` + Qdrant | 用户意图、已审批 SOP | 已运行 |
| `OrderIntakeAgent` | 订单中心 / 工单中心 | `customer_order`、`work_order` | 已运行 |
| `BomRouteAgent` | Agent 只读 BOM/路线快照 | BOM、工艺、工序 | 已运行 |
| `KittingRiskAgent` | Agent 只读齐套快照 | 库存可用量、短缺 | 已运行 |
| `KittingExecutionAgent` | 受控排产执行接口 | 锁料 | 已运行 |
| `CapacitySchedulingAgent` | 甘特排产 / 资源建议 | `production_task` | 已运行 |
| `DispatchExecutionAgent` | 派工同步 | `dispatch_task` | 已运行 |
| `SchedulingValidationAgent` | 工单/任务/派工写回结果 | 生命周期与数量 | 已运行 |
| `ProductionIssueAgent` | WMS 领料与消耗服务 | 领料单、库存流水、消耗明细 | 已运行 |

说明：锁料、排产、任务生成与派工同步当前由一个原子 MES 命令完成。四个相关 Agent 负责不同业务责任和结果投影，但不得拆成重复写库。

## 2. 第一优先级：把工单闭环从领料推进到成品入库

这组 Agent 与现有智能排产直接相连，且后端已有明确生命周期门禁，是下一阶段最值得实现的子图。

```text
ProductionIssueAgent
  → ProductionFeedbackAgent
  → IPQCQualityGateAgent
      ├─ QC_PASSED → FinishedGoodsReceiptAgent → TraceabilityAgent
      └─ QC_FAILED → QualityDispositionAgent / ReworkAgent（后续）
```

| Agent | 真实业务与触发 | 主要真实数据 | 写入与门禁 | 实施级别 |
|---|---|---|---|---|
| `ProductionFeedbackAgent` | 操作工完成一道派工或报工时 | `dispatch_task`、`production_task`、`pro_feedback`、`production_report` | 复用 `ProFeedbackService`；仅允许 `MATERIAL_ISSUED/IN_PROGRESS/QC_PENDING`，写入后推进 `IN_PROGRESS → QC_PENDING` | P1，优先实现 |
| `IPQCQualityGateAgent` | 报工后出现待过程检验任务时 | `qc_ipqc`、检验项、缺陷记录、工单 | 复用 `QcIpqcController`；合格推进 `QC_PASSED`，不合格推进 `QC_FAILED` | P1，优先实现 |
| `FinishedGoodsReceiptAgent` | `QC_PASSED` 后申请成品入库时 | `wm_product_recpt*`、库存、批次、工单 | 复用 `WmProductRecptService`；必须通过数量、上架和 `QC_PASSED` 门禁，成功后推进 `COMPLETED` | P1，优先实现 |
| `TraceabilityAgent` | 查询工单、批次、产品 SN 或完工后归档时 | 工单、领料、报工、检验、入库、`product_sn`、物料绑定 | 只读优先；当前追溯接口会触发同步，需增加无副作用 Agent 快照后再开放 | P1，先读后写 |
| `MaterialBindingAgent` | 现场扫码绑定产品 SN 与物料批次时 | `product_sn`、`product_material_binding`、`inventory_batch` | 需经现有物料绑定业务服务写入；必须校验工单、工序与批次状态 | P1/P2，视现场扫码流程启用 |
| `QualityDispositionAgent` | IPQC 不合格或不良记录产生时 | `qc_defect_record`、IPQC、返工/报废记录 | 必须明确返工、让步、报废的现有领域边界后才可写入 | P2，先只读建议 |

## 3. 仓储与物料流转子图

仓储不应只包装为一个“库存 Agent”，而应按入库、出库、退货、批次和库存健康拆分。

| Agent | 对应模块 | 主要数据 | 权限与动作 | 实施级别 |
|---|---|---|---|---|
| `InventoryHealthAgent` | 库存现有量、批次、欠料预警 | `wm_material_stock`、`inventory_batch`、`material_shortage` | 只读：呆滞、临期、低库存、锁定异常、批次质量状态 | P1，需专用只读快照 |
| `MaterialReceiptAgent` | 物料入库 | `wm_item_recpt*`、批次、库存流水 | 创建/确认/执行入库必须仓库人员确认 | P2 |
| `WarehouseIssueAgent` | 非生产领料的出库作业 | `wm_issue_*`、库存流水 | 与 `ProductionIssueAgent` 区分：后者只负责工单生产领料 | P2 |
| `WarehouseReturnAgent` | 生产退料、销售退货、供应商退货 | `wm_rt_issue*`、`wm_rt_sales*`、`wm_rt_vendor*` | 建议、单据草稿、执行均需要仓库审批 | P2 |
| `BatchTraceAgent` | 批次正反向查询 | `inventory_batch`、物料绑定、产品 SN | 只读：影响范围、隔离建议、可追溯产品清单 | P1，和 TraceabilityAgent 共享快照 |
| `BarcodePackagingAgent` | 条码、装箱、包装层级 | 条码配置、`wm_package*`、产品 SN | 可辅助生成草稿；打印、装箱确认属于受控写入 | P2 |
| `ShipmentReadinessAgent` | 销售出库前核验 | `wm_product_sales*`、库存、质量/包装状态 | 只读核验优先；销售出库必须经仓库确认 | P3 |

## 4. 质量管理子图

质量 Agent 应按检验类型和处置责任拆分，不能让模型自行判定合格或不合格。

| Agent | 对应模块 | 职责 | 写入边界 | 实施级别 |
|---|---|---|---|---|
| `IncomingQualityAgent` | `qc_iqc*` | 来料待检、抽样与检验模板匹配 | 结果由质检员确认 | P2 |
| `IPQCQualityGateAgent` | `qc_ipqc*` | 过程检验与工单质量门禁 | P1，见第 2 节 | P1 |
| `OutgoingQualityAgent` | `qc_oqc*` | 出货前检验与放行建议 | 结果由质检员确认 | P2 |
| `ReturnQualityAgent` | `qc_rqc*` | 退料/退货检验与处置建议 | 结果由质检员确认 | P3 |
| `DefectAnalysisAgent` | 缺陷、检验结果、质量分析中心 | 不良 Pareto、批次/工序异常、风险解释 | 只读分析 + RAG 质量规范 | P1 |
| `QualityStandardAgent` | 模板、检验项、缺陷字典、SIP/SOP | 标准版本推荐、覆盖缺口识别 | 变更必须管理人员/质量负责人审批 | P3 |

## 5. 现场生产与异常响应子图

| Agent | 对应模块 | 真实数据与职责 | 写入边界 | 实施级别 |
|---|---|---|---|---|
| `ProductionProgressAgent` | 我的工位、报工、生产任务 | 派工完成率、节拍、在制进度、待报工事项 | 只读优先；报工委托 `ProductionFeedbackAgent` | P1 |
| `AndonTriageAgent` | 安灯看板、安灯类型/原因 | 根据安灯类型、产线、工位、持续时间生成响应建议和影响范围 | 只读分析 + 可创建安灯草稿 | P1 |
| `AndonResponseAgent` | 安灯任务与处置 | 分派、处理记录、关闭前检查 | 创建/分派/关闭必须责任人确认 | P2 |
| `LineBalanceAgent` | 产线监控、任务、派工、节拍 | 识别工位拥堵、任务等待和产线失衡 | 仅建议；需要可靠工时/节拍数据 | P3 |
| `WorkstationGuidanceAgent` | SOP、工序、工位、设备/工装 | 向操作工解释当前任务、SOP 和注意事项 | 只读；适合接入 RAG + 当前派工快照 | P2 |

## 6. 设备保障子图

| Agent | 对应模块 | 真实数据与职责 | 写入边界 | 实施级别 |
|---|---|---|---|---|
| `EquipmentHealthAgent` | 设备工作台、设备台账、OEE | 停机设备、故障设备、待点检/保养/维修与 OEE 概览 | 只读；已有聚合工作台接口 | P1 |
| `MaintenancePlanningAgent` | 点检、保养计划与记录 | 逾期点检/保养、计划冲突、建议排程 | 创建或调整计划需设备主管确认 | P2 |
| `RepairDispatchAgent` | 报修、维修任务、维修记录 | 故障影响、维修优先级、推荐处理人/顺序 | 创建报修/派工/完工必须设备人员确认 | P2 |
| `OeeAnalysisAgent` | OEE 与设备分析 | 可用率、故障趋势、维护效果解释 | 只读分析；不能替代真实 OEE 口径 | P2 |

## 7. 计划、主数据与资源治理子图

| Agent | 对应模块 | 职责 | 写入边界 | 实施级别 |
|---|---|---|---|---|
| `OrderCommitmentAgent` | 客户订单、交期、工单 | 交付承诺风险、订单状态解释、交期变更影响 | 交期/订单变更需业务确认 | P2 |
| `MasterDataGovernanceAgent` | 产品、物料、BOM、供应商、客户 | 主数据缺失、重复、启停状态和引用影响检查 | 不自动修改主数据 | P2 |
| `BomChangeImpactAgent` | BOM、工单 BOM、库存、在制 | BOM 变更对未完工工单、齐套和追溯的影响 | 工程/计划审批后才可生效 | P3 |
| `ProcessRouteGovernanceAgent` | 工艺路线、工序、SOP | 路线完整性、工序资源覆盖、SOP 版本一致性 | 不自动发布路线 | P2 |
| `ResourceCalendarAgent` | 班次、班组、日历、节假日、工位资源 | 可用工时与排产约束解释 | 日历和班次变更须主管确认 | P2 |
| `PersonnelCapabilityAgent` | 工位人员配置、班组 | 人员/技能/班次匹配建议 | 当前缺少可靠技能、考勤与负荷事实，不默认启用 | P3 / 预留 |

## 8. 管理、分析、集成与安全子图

| Agent | 对应模块 | 职责 | 写入边界 | 实施级别 |
|---|---|---|---|---|
| `OperationsInsightAgent` | 生产驾驶舱、计划/质量/库存/安灯汇总 | 每日经营摘要、跨域风险归因与建议 | 只读 + RAG 规则依据 | P1 |
| `ReportingAgent` | 报表中心、图表、打印 | 指标解释、报表取数范围、打印任务草稿 | 报表模板发布需管理人员确认 | P2 |
| `IntegrationHealthAgent` | 集成中心、同步日志 | 接口失败、积压、重试建议、数据延迟 | 重试/补偿必须人工确认 | P2 |
| `KnowledgeGovernanceAgent` | Qdrant、知识源、审核元数据 | RAG 文档候选、版本、权限、评测与过期提醒 | 导入、发布、删除知识均须审批 | P2 |
| `AuditSecurityAgent` | 操作日志、登录日志、角色权限 | 异常操作、越权风险、审计摘要 | 只读；绝不自动改权限或用户 | P2 |

## 9. 实施优先级与最小新增工具

### 9.1 推荐下一批：生产执行闭环（4 个）

1. `ProductionFeedbackAgent`
2. `IPQCQualityGateAgent`
3. `FinishedGoodsReceiptAgent`
4. `TraceabilityAgent`

需要新增的受控工具边界：

- 生产执行只读快照：派工、已报工、合格/不良数量、待检状态；
- 报工草稿/执行门面：绑定操作工、派工和数量校验；
- IPQC 待检/执行门面：模板、检验项、缺陷与生命周期门禁；
- 成品入库准备/执行门面：入库明细、上架数量、质量状态、库存流水；
- 无副作用追溯快照：禁止调用会触发同步写入的现有查询接口。

### 9.2 第二批：现场响应与资源保障（5 个）

`InventoryHealthAgent`、`AndonTriageAgent`、`EquipmentHealthAgent`、`DefectAnalysisAgent`、`OperationsInsightAgent`。

这一批可以先只读，不触碰业务写入，适合作为云枢小智的日常问答和预警能力。

### 9.3 第三批：需审批的业务执行与治理能力

仓储收发退、设备保养维修、客户交期变更、主数据/BOM/工艺变更、集成重试、权限治理等。先提供分析与草稿，待审批机制和完整工具契约稳定后再开放写入。

## 10. 不应直接包装为自动执行 Agent 的能力

- 人员排班/技能分配：尚缺可靠的技能、出勤、工时和产能事实；
- 采购与供应商交期承诺：当前项目有供应商主数据，但没有完整采购订单闭环；
- 自动质量判定、自动放行、自动报废：必须由质量人员确认；
- 自动关闭安灯、自动调整库存、自动修改 BOM/工艺、自动重试外部集成：均属于高风险写入。

## 11. 与 LangGraph 的对应关系

当前已包装的 `app/scheduling/agents/` 是兼容现有业务的顺序工作流。未来每个本目录 Agent 都可作为一个 `StateGraph` 节点；跨域闭环可由主协调 Agent 根据生命周期、角色、工具权限和审批状态路由。此目录中的优先级不意味着一次性全部启用，而是按“真实数据 → 只读快照 → 草稿 → 审批写入 → 评测”的顺序逐步上线。
