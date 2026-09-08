"""OrderIntakeAgent wrapper around the existing confirmed-order gateway."""

from __future__ import annotations

from typing import TYPE_CHECKING, Any

from app.scheduling.agents.base import BaseSchedulingAgent, SchedulingAgentState

if TYPE_CHECKING:
    from app.scheduling.order_intake import OrderIntakeService


class OrderIntakeAgent(BaseSchedulingAgent):
    agent_name = "OrderIntakeAgent"

    def run(self, state: SchedulingAgentState, gateway: "OrderIntakeService") -> dict[str, Any]:
        result = gateway.create_order_and_work_order(state.request, state.principal, state.trace_id)
        state.order = result["order"]
        state.work_order = result["work_order"]
        return result
