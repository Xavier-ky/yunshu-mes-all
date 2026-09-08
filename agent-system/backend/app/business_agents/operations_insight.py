"""Operations-insight Agent backed by real dashboard and companion summaries."""

from __future__ import annotations

from app.business_agents.base import BaseBusinessAgent, BusinessAgentState, BusinessReadRequest


class OperationsInsightAgent(BaseBusinessAgent):
    agent_name = "OperationsInsightAgent"
    business_module = "经营洞察"
    description = "汇总真实经营看板、今日生产与质量待办；仅形成分析输入，不修改业务数据。"
    future_write_actions: tuple[str, ...] = ()

    def read_requests(self, state: BusinessAgentState) -> tuple[BusinessReadRequest, ...]:
        return (
            BusinessReadRequest("dashboard", "dashboard", params=state.params or None),
            BusinessReadRequest("today_production", "today_production_overview"),
            BusinessReadRequest("quality_backlog", "quality_task_backlog"),
        )
