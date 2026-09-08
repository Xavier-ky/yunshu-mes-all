"""ProductionIssueAgent wrapper for the controlled WMS material-issue command."""

from __future__ import annotations

from typing import Any

from app.scheduling.agents.base import BaseSchedulingAgent, SchedulingAgentError, SchedulingAgentState
from app.scheduling.production_issue import ProductionIssueError, ProductionIssueService, production_issue_service


class ProductionIssueAgent(BaseSchedulingAgent):
    agent_name = "ProductionIssueAgent"

    def __init__(self, service: ProductionIssueService | None = None) -> None:
        self.service = service or production_issue_service

    def run(self, state: SchedulingAgentState) -> dict[str, Any]:
        if not state.execution:
            raise SchedulingAgentError("Scheduling execution must finish before production material issue")
        try:
            state.production_issue = self.service.execute(state.work_order_id, state.principal, state.trace_id)
        except ProductionIssueError as exc:
            # Preserve the existing partial-success contract: scheduling has
            # already committed, while an issue failure is returned truthfully.
            state.production_issue = {
                "readyForShopFloor": False,
                "lifecycleStatus": state.execution.get("lifecycleStatus"),
                "message": "Production issue is blocked; no outbound inventory was executed",
                "error": str(exc),
            }
        return state.production_issue
