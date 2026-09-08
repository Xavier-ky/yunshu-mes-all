from __future__ import annotations

from dataclasses import dataclass, field
from typing import Any, Callable


@dataclass
class AgentDef:
    agent_name: str
    business_module: str
    description: str
    allowed_tools: list[str] = field(default_factory=list)
    allowed_api_paths: list[str] = field(default_factory=list)
    read_permissions: list[str] = field(default_factory=list)
    write_permissions: list[str] = field(default_factory=list)
    require_confirmation_actions: list[str] = field(default_factory=list)
    risk_level: str = "low"


AGENT_REGISTRY: dict[str, AgentDef] = {
    "MainAgent": AgentDef(
        agent_name="MainAgent",
        business_module="supervisor",
        description="主协调 Agent：意图识别、任务拆解、子 Agent 调度",
        read_permissions=["*"],
        write_permissions=[],
        risk_level="low",
    ),
    "OverviewAgent": AgentDef(
        agent_name="OverviewAgent",
        business_module="系统总览",
        description="系统总览与 KPI 汇总",
        allowed_tools=["query_system_overview"],
        read_permissions=["overview", "dashboard"],
        write_permissions=[],
    ),
    "ScheduleAgent": AgentDef(
        agent_name="ScheduleAgent",
        business_module="计划调度",
        description="计划、工单、齐套、排产",
        allowed_tools=["query_schedule_plan", "query_work_order", "query_material_kitting"],
        read_permissions=["planning", "work_orders"],
        write_permissions=["plan_adjust_apply"],
        require_confirmation_actions=["adjust_work_order", "release_plan"],
        risk_level="medium",
    ),
    "OrderIntakeAgent": AgentDef(
        agent_name="OrderIntakeAgent",
        business_module="订单与排产准备",
        description="接收新订单，校验产品主数据，并在用户表单确认后创建 CREATED 状态订单。",
        allowed_tools=["list_order_intake_products", "create_scheduling_order"],
        read_permissions=["master_data.products"],
        write_permissions=["planning.orders.create"],
        require_confirmation_actions=["create_scheduling_order"],
        risk_level="medium",
    ),
    "IntentRoutingAgent": AgentDef(
        agent_name="IntentRoutingAgent",
        business_module="智能排产入口",
        description="识别排产请求，并使用受控模型分析与角色范围内的 RAG 规则提供解释。",
        allowed_tools=["analyze_scheduling_intent", "search_approved_knowledge"],
        read_permissions=["knowledge.scheduling"],
        write_permissions=[],
        risk_level="low",
    ),
    "BomRouteAgent": AgentDef(
        agent_name="BomRouteAgent",
        business_module="主数据与工艺",
        description="读取工单对应的已发布 BOM、物料清单和工艺路线。",
        allowed_tools=["agent_bom_route_snapshot"],
        read_permissions=["planning.bom_route"],
        write_permissions=[],
        risk_level="low",
    ),
    "KittingRiskAgent": AgentDef(
        agent_name="KittingRiskAgent",
        business_module="物料齐套",
        description="根据受控 MES 快照识别齐套、短缺和物料风险等级。",
        allowed_tools=["agent_kitting_snapshot"],
        read_permissions=["planning.kitting"],
        write_permissions=[],
        risk_level="low",
    ),
    "KittingExecutionAgent": AgentDef(
        agent_name="KittingExecutionAgent",
        business_module="物料锁定",
        description="在齐套通过后调用原有原子排产命令中的库存锁定环节。",
        allowed_tools=["execute_autonomous_scheduling"],
        read_permissions=["planning.kitting"],
        write_permissions=["inventory.reserve"],
        require_confirmation_actions=["create_scheduling_order"],
        risk_level="high",
    ),
    "CapacitySchedulingAgent": AgentDef(
        agent_name="CapacitySchedulingAgent",
        business_module="计划调度",
        description="复用原有甘特排产命令，并读取设备、人员等辅助排程建议。",
        allowed_tools=["execute_autonomous_scheduling", "scheduling_advisory"],
        read_permissions=["planning.capacity"],
        write_permissions=["planning.schedule"],
        require_confirmation_actions=["create_scheduling_order"],
        risk_level="high",
    ),
    "DispatchExecutionAgent": AgentDef(
        agent_name="DispatchExecutionAgent",
        business_module="现场派工",
        description="确认原子排产命令已同步生成现场派工记录。",
        allowed_tools=["execute_autonomous_scheduling"],
        read_permissions=["planning.dispatch"],
        write_permissions=["planning.dispatch"],
        require_confirmation_actions=["create_scheduling_order"],
        risk_level="high",
    ),
    "SchedulingValidationAgent": AgentDef(
        agent_name="SchedulingValidationAgent",
        business_module="计划校验",
        description="核验工单、生产任务和派工记录的写回状态。",
        allowed_tools=["scheduling_execution_result"],
        read_permissions=["planning.schedule", "planning.dispatch"],
        write_permissions=[],
        risk_level="low",
    ),
    "ProductionIssueAgent": AgentDef(
        agent_name="ProductionIssueAgent",
        business_module="生产领料",
        description="调用原有 WMS 领料、出库和物料消耗追溯写入边界。",
        allowed_tools=["execute_production_issue"],
        read_permissions=["inventory.issue"],
        write_permissions=["inventory.issue", "inventory.consume"],
        require_confirmation_actions=["create_scheduling_order"],
        risk_level="high",
    ),
    "WarehouseAgent": AgentDef(
        agent_name="WarehouseAgent",
        business_module="仓储管理",
        description="库存、批次、发料",
        allowed_tools=["query_inventory", "query_material_kitting", "confirm_material_issue"],
        read_permissions=["inventory"],
        write_permissions=["issue_apply"],
        require_confirmation_actions=["confirm_material_issue"],
        risk_level="high",
    ),
    "ProductionAgent": AgentDef(
        agent_name="ProductionAgent",
        business_module="生产管理",
        description="生产进度、报工、SN",
        allowed_tools=["query_production_progress", "submit_production_report"],
        read_permissions=["production"],
        write_permissions=["report_apply"],
        require_confirmation_actions=["submit_production_report"],
        risk_level="high",
    ),
    "QualityAgent": AgentDef(
        agent_name="QualityAgent",
        business_module="质量管理",
        description="质检任务与不良分析",
        allowed_tools=["query_quality_record", "submit_quality_result"],
        read_permissions=["quality"],
        write_permissions=["quality_apply"],
        require_confirmation_actions=["submit_quality_result"],
        risk_level="high",
    ),
    "AndonAgent": AgentDef(
        agent_name="AndonAgent",
        business_module="安灯中心",
        description="安灯事件创建与处理",
        allowed_tools=["query_andon_events", "create_andon_event"],
        read_permissions=["andon"],
        write_permissions=["andon_create"],
        require_confirmation_actions=["create_andon_event"],
        risk_level="medium",
    ),
    "EquipmentAgent": AgentDef(
        agent_name="EquipmentAgent",
        business_module="设备管理",
        description="设备状态与保养",
        allowed_tools=["query_equipment_status"],
        read_permissions=["equipment"],
        write_permissions=["repair_apply"],
        require_confirmation_actions=["confirm_maintenance"],
        risk_level="medium",
    ),
    "SystemAgent": AgentDef(
        agent_name="SystemAgent",
        business_module="系统管理",
        description="用户角色权限（第一版只读）",
        allowed_tools=[],
        read_permissions=["system"],
        write_permissions=[],
        risk_level="low",
    ),
    "AnalyticsAgent": AgentDef(
        agent_name="AnalyticsAgent",
        business_module="分析集成",
        description="报表分析与趋势",
        allowed_tools=["query_report_summary"],
        read_permissions=["reports", "trace"],
        write_permissions=[],
        risk_level="low",
    ),
    "ProductionExecutionAgent": AgentDef(
        agent_name="ProductionExecutionAgent",
        business_module="生产执行",
        description="已独立包装；仅读取真实工单、生产任务与派工快照，尚未接入工作流。",
        allowed_tools=["agent_work_order_pipeline", "agent_task_snapshot", "agent_dispatch_snapshot"],
        read_permissions=["production.tasks", "production.dispatch"],
        write_permissions=[],
        risk_level="medium",
    ),
    "QualityManagementAgent": AgentDef(
        agent_name="QualityManagementAgent",
        business_module="质量管理",
        description="已独立包装；仅读取真实待检任务、质量指标和工单追溯快照，尚未接入工作流。",
        allowed_tools=["agent_quality_overview", "agent_work_order_quality"],
        read_permissions=["quality.pending", "quality.analytics", "quality.work_order"],
        write_permissions=[],
        risk_level="medium",
    ),
    "WarehouseLogisticsAgent": AgentDef(
        agent_name="WarehouseLogisticsAgent",
        business_module="仓储物流",
        description="已独立包装；仅读取真实库存批次与工单齐套快照，尚未接入工作流。",
        allowed_tools=["inventory", "agent_kitting_snapshot"],
        read_permissions=["inventory.batches", "planning.kitting"],
        write_permissions=[],
        risk_level="medium",
    ),
    "TraceabilityAgent": AgentDef(
        agent_name="TraceabilityAgent",
        business_module="追溯管理",
        description="已独立包装；通过无副作用工单追溯快照读取真实追溯事实。",
        allowed_tools=["agent_work_order_trace"],
        read_permissions=["traceability"],
        write_permissions=[],
        risk_level="low",
    ),
    "EquipmentMaintenanceAgent": AgentDef(
        agent_name="EquipmentMaintenanceAgent",
        business_module="设备管理",
        description="已独立包装；仅读取真实设备工作台摘要与待办，尚未接入工作流。",
        allowed_tools=["equipment_workbench_summary", "equipment_workbench_pending"],
        read_permissions=["equipment.workbench"],
        write_permissions=[],
        risk_level="medium",
    ),
    "AndonResponseAgent": AgentDef(
        agent_name="AndonResponseAgent",
        business_module="安灯响应",
        description="已独立包装；仅读取真实安灯事件，尚未接入工作流。",
        allowed_tools=["andon"],
        read_permissions=["andon"],
        write_permissions=[],
        risk_level="medium",
    ),
    "OperationsInsightAgent": AgentDef(
        agent_name="OperationsInsightAgent",
        business_module="经营洞察",
        description="已独立包装；仅读取真实看板、生产概览和质量待办，尚未接入工作流。",
        allowed_tools=["dashboard", "today_production_overview", "quality_task_backlog"],
        read_permissions=["dashboard", "reporting", "quality"],
        write_permissions=[],
        risk_level="low",
    ),
}


def list_agents() -> list[dict[str, Any]]:
    return [
        {
            "agent_name": a.agent_name,
            "business_module": a.business_module,
            "description": a.description,
            "allowed_tools": a.allowed_tools,
            "read_permissions": a.read_permissions,
            "write_permissions": a.write_permissions,
            "require_confirmation_actions": a.require_confirmation_actions,
            "risk_level": a.risk_level,
        }
        for a in AGENT_REGISTRY.values()
        if a.agent_name != "MainAgent"
    ]


def agent_can_use_tool(agent_name: str, tool_name: str) -> bool:
    agent = AGENT_REGISTRY.get(agent_name)
    if not agent:
        return False
    if agent_name == "MainAgent":
        return False
    return tool_name in agent.allowed_tools
