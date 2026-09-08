"""DispatchExecutionAgent wrapper for the dispatch portion of one atomic command."""

from __future__ import annotations

from typing import Any

from app.scheduling.agents.base import BaseSchedulingAgent, SchedulingAgentError, SchedulingAgentState


class DispatchExecutionAgent(BaseSchedulingAgent):
    agent_name = "DispatchExecutionAgent"

    def run(self, state: SchedulingAgentState) -> dict[str, Any]:
        if not state.execution:
            raise SchedulingAgentError("Scheduling execution must finish before dispatch synchronization")
        return {
            "dispatch_task_count": state.execution.get("dispatchTaskCount", 0),
            "lifecycle_status": state.execution.get("lifecycleStatus"),
        }
