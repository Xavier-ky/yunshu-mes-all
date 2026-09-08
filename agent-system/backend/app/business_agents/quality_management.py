"""Quality-management Agent backed by the real quality workbench."""

from __future__ import annotations

from app.business_agents.base import BaseBusinessAgent, BusinessAgentState, BusinessReadRequest


class QualityManagementAgent(BaseBusinessAgent):
    agent_name = "QualityManagementAgent"
    business_module = "质量管理"
    description = "读取真实待检任务与质量指标；有工单上下文时同时读取该工单追溯快照。"
    future_write_actions = ("quality.result.draft", "quality.result.submit", "quality.disposition.draft")

    def read_requests(self, state: BusinessAgentState) -> tuple[BusinessReadRequest, ...]:
        days = state.params.get("days", 7)
        requests = [
            BusinessReadRequest("quality_overview", "agent_quality_overview", params={"days": days}),
        ]
        if state.work_order_no:
            requests.append(
                BusinessReadRequest(
                    "work_order_quality",
                    "agent_work_order_quality",
                    params={"days": days},
                    path_params={"work_order_no": self.require_work_order_no(state)},
                )
            )
        return tuple(requests)
