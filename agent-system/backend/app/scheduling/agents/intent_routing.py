"""IntentRoutingAgent wrapper for bounded model and RAG intent analysis."""

from __future__ import annotations

from typing import Any

from app.scheduling.agents.base import BaseSchedulingAgent
from app.scheduling.llm_analysis import SchedulingLlmAnalysisService, scheduling_llm_analysis_service


class IntentRoutingAgent(BaseSchedulingAgent):
    agent_name = "IntentRoutingAgent"

    def __init__(self, service: SchedulingLlmAnalysisService | None = None) -> None:
        self.service = service or scheduling_llm_analysis_service

    def run(self, message: str, model: str | None, roles: list[str]) -> dict[str, Any]:
        return self.service.analyze_intent(message, model, roles)
