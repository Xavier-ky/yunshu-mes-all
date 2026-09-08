"""Compatibility-first orchestrator for the individually packaged sub-Agents."""

from __future__ import annotations

from typing import TYPE_CHECKING, Any

from app.core.security import Principal
from app.schemas import SchedulingOrderIntakeRequest
from app.scheduling.agents.base import SchedulingAgentState
from app.scheduling.agents.bom_route import BomRouteAgent
from app.scheduling.agents.capacity_scheduling import CapacitySchedulingAgent
from app.scheduling.agents.dispatch_execution import DispatchExecutionAgent
from app.scheduling.agents.events import build_scheduling_workflow_events
from app.scheduling.agents.kitting_execution import KittingExecutionAgent
from app.scheduling.agents.kitting_risk import KittingRiskAgent
from app.scheduling.agents.order_intake import OrderIntakeAgent
from app.scheduling.agents.production_issue import ProductionIssueAgent
from app.scheduling.agents.scheduling_validation import SchedulingValidationAgent

if TYPE_CHECKING:
    from app.scheduling.order_intake import OrderIntakeService


class SchedulingSubAgentWorkflow:
    """Run the existing scheduling order in individually addressable wrappers.

    This deliberately remains sequential until the later StateGraph migration.
    No wrapper owns a database transaction; all writes still pass through the
    existing constrained MES services in their original order.
    """

    def __init__(self) -> None:
        self.order_intake_agent = OrderIntakeAgent()
        self.bom_route_agent = BomRouteAgent()
        self.kitting_risk_agent = KittingRiskAgent()
        self.kitting_execution_agent = KittingExecutionAgent()
        self.capacity_scheduling_agent = CapacitySchedulingAgent()
        self.dispatch_execution_agent = DispatchExecutionAgent()
        self.scheduling_validation_agent = SchedulingValidationAgent()
        self.production_issue_agent = ProductionIssueAgent()

    def run_order_intake(
        self,
        request: SchedulingOrderIntakeRequest,
        principal: Principal,
        trace_id: str,
        *,
        order_intake_gateway: "OrderIntakeService",
    ) -> dict[str, Any]:
        state = SchedulingAgentState(request=request, principal=principal, trace_id=trace_id)
        self.order_intake_agent.run(state, order_intake_gateway)
        self.bom_route_agent.run(state)
        self.kitting_risk_agent.run(state)

        if state.analysis and state.analysis["summary"]["risk_level"] == "LOW":
            self.kitting_execution_agent.run(state)
            self.capacity_scheduling_agent.run(state)
            self.dispatch_execution_agent.run(state)
            self.scheduling_validation_agent.run(state)
            self.production_issue_agent.run(state)

        state.workflow_events = build_scheduling_workflow_events(
            state.analysis or {},
            state.execution,
            state.production_issue,
            order_no=str((state.order or {}).get("order_no") or request.order_no),
            work_order_no=str((state.work_order or {}).get("work_order_no") or request.order_no),
        )
        return {
            "order": state.order,
            "work_order": state.work_order,
            "analysis": state.analysis,
            "scheduling_execution": state.execution,
            "scheduling_advisory": state.advisory,
            "production_issue_execution": state.production_issue,
            "workflow_events": state.workflow_events,
        }


scheduling_subagent_workflow = SchedulingSubAgentWorkflow()
