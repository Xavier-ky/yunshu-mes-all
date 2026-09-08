"""BomRouteAgent wrapper for the real MES BOM and route snapshot."""

from __future__ import annotations

from typing import Any

from app.scheduling.agents.base import BaseSchedulingAgent, SchedulingAgentState
from app.scheduling.analysis import SchedulingAnalysisService, scheduling_analysis_service


class BomRouteAgent(BaseSchedulingAgent):
    agent_name = "BomRouteAgent"

    def __init__(self, service: SchedulingAnalysisService | None = None) -> None:
        self.service = service or scheduling_analysis_service

    def run(self, state: SchedulingAgentState) -> dict[str, Any]:
        analysis = self.service.analyze_work_order(state.work_order_id, state.principal, state.trace_id)
        state.analysis = analysis
        return analysis["bom_route_agent"]
