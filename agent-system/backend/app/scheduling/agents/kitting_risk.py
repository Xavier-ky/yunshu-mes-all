"""KittingRiskAgent wrapper for deterministic shortage and risk evaluation."""

from __future__ import annotations

from typing import Any

from app.scheduling.agents.base import BaseSchedulingAgent, SchedulingAgentError, SchedulingAgentState


class KittingRiskAgent(BaseSchedulingAgent):
    agent_name = "KittingRiskAgent"

    def run(self, state: SchedulingAgentState) -> dict[str, Any]:
        if not state.analysis:
            raise SchedulingAgentError("BOM and route analysis must finish before kitting risk analysis")
        return state.analysis["kitting_risk_agent"]
