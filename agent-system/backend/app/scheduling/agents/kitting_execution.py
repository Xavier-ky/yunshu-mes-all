"""KittingExecutionAgent wrapper for the existing atomic scheduling command."""

from __future__ import annotations

from typing import Any

from app.scheduling.agents.base import BaseSchedulingAgent, SchedulingAgentError, SchedulingAgentState
from app.scheduling.execution import SchedulingExecutionService, scheduling_execution_service


class KittingExecutionAgent(BaseSchedulingAgent):
    agent_name = "KittingExecutionAgent"

    def __init__(self, service: SchedulingExecutionService | None = None) -> None:
        self.service = service or scheduling_execution_service

    def run(self, state: SchedulingAgentState) -> dict[str, Any]:
        if not state.analysis or state.analysis["summary"]["risk_level"] != "LOW":
            raise SchedulingAgentError("Only a LOW-risk work order may enter the scheduling write command")
        # The existing MES command atomically performs stock reservation,
        # scheduling, task creation and dispatch synchronization. It is called
        # exactly once to keep the previous database semantics unchanged.
        state.execution = self.service.execute(state.work_order_id, state.principal, state.trace_id)
        return state.execution
