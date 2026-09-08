"""SchedulingValidationAgent wrapper for the existing execution writeback result."""

from __future__ import annotations

from typing import Any

from app.scheduling.agents.base import BaseSchedulingAgent, SchedulingAgentError, SchedulingAgentState


class SchedulingValidationAgent(BaseSchedulingAgent):
    agent_name = "SchedulingValidationAgent"

    def run(self, state: SchedulingAgentState) -> dict[str, Any]:
        if not state.execution:
            raise SchedulingAgentError("Scheduling execution must finish before writeback validation")
        return {
            "production_task_count": state.execution.get("productionTaskCount", 0),
            "dispatch_task_count": state.execution.get("dispatchTaskCount", 0),
            "lifecycle_status": state.execution.get("lifecycleStatus"),
        }
