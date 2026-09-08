"""Andon-response Agent backed by the real Andon event stream."""

from __future__ import annotations

from app.business_agents.base import BaseBusinessAgent, BusinessAgentState, BusinessReadRequest


class AndonResponseAgent(BaseBusinessAgent):
    agent_name = "AndonResponseAgent"
    business_module = "安灯响应"
    description = "读取真实安灯事件，支持后续分级、响应建议、升级和关闭草案。"
    future_write_actions = ("andon.assign.draft", "andon.close.execute")

    def read_requests(self, state: BusinessAgentState) -> tuple[BusinessReadRequest, ...]:
        return (BusinessReadRequest("andon_events", "andon", params=state.params or None),)
