"""CapacitySchedulingAgent wrapper for post-write scheduling advisory facts."""

from __future__ import annotations

from typing import Any

from app.scheduling.agents.base import BaseSchedulingAgent, SchedulingAgentError, SchedulingAgentState
from app.scheduling.execution import SchedulingExecutionService, scheduling_execution_service


class CapacitySchedulingAgent(BaseSchedulingAgent):
    agent_name = "CapacitySchedulingAgent"

    def __init__(self, service: SchedulingExecutionService | None = None) -> None:
        self.service = service or scheduling_execution_service

    def run(self, state: SchedulingAgentState) -> dict[str, Any]:
        if not state.execution:
            raise SchedulingAgentError("Scheduling execution must finish before capacity advisory")
        state.advisory = self.service.advisory(state.work_order_id, state.principal, state.trace_id)
        return state.advisory
