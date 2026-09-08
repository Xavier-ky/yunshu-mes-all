from __future__ import annotations

import uuid

import httpx

from app.companion.mock_data import read_mock
from app.companion.policy import build_suggestion, select_resource
from app.core.config import settings
from app.core.security import Principal
from app.integrations.spring_boot_client import SpringBootClient
from app.schemas import CompanionContextRequest, CompanionSuggestionResponse


class CompanionService:
    def __init__(self, client: SpringBootClient | None = None) -> None:
        self._client = client or SpringBootClient()

    def suggest(self, context: CompanionContextRequest, principal: Principal, trace_id: str | None) -> CompanionSuggestionResponse:
        request_trace_id = trace_id or str(uuid.uuid4())
        resource = select_resource(context.route_path)
        source = "live"
        try:
            data = self._client.read(resource, principal, request_trace_id)
        except (httpx.HTTPError, ValueError):
            if not settings.enable_mock_tools:
                raise
            source = "mock"
            data = read_mock(resource)
        title, message, actions = build_suggestion(context.route_title, resource, data, source)
        return CompanionSuggestionResponse(
            title=title,
            message=message,
            visual_state="notice" if resource in {"andon", "quality"} else "idle",
            urgency="warning" if resource in {"andon", "quality"} else "routine",
            source=source,
            actions=actions,
            trace_id=request_trace_id,
        )