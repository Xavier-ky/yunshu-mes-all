"""Traceability Agent backed by the side-effect-free Agent trace facade."""

from __future__ import annotations

from app.business_agents.base import BaseBusinessAgent, BusinessAgentState, BusinessReadRequest


class TraceabilityAgent(BaseBusinessAgent):
    agent_name = "TraceabilityAgent"
    business_module = "追溯管理"
    description = "读取工单的领料、报工、质量、入库、批次与 SN 追溯快照。"
    future_write_actions: tuple[str, ...] = ()

    def read_requests(self, state: BusinessAgentState) -> tuple[BusinessReadRequest, ...]:
        return (
            BusinessReadRequest(
                "work_order_trace", "agent_work_order_trace", path_params={"work_order_no": self.require_work_order_no(state)}
            ),
        )
