"""Controlled write gateway for the autonomous kitting-to-Gantt workflow."""

from __future__ import annotations

from typing import Any

import httpx

from app.core.config import settings
from app.core.security import Principal


class SchedulingExecutionError(RuntimeError):
    pass


class SchedulingExecutionPermissionError(SchedulingExecutionError):
    pass


class SchedulingExecutionService:
    permitted_roles = {"MANAGER", "TESTER", "PROD_SUPERVISOR"}

    @classmethod
    def _ensure_permission(cls, principal: Principal) -> None:
        if not set(principal.roles).intersection(cls.permitted_roles):
            raise SchedulingExecutionPermissionError("Current role cannot execute autonomous scheduling")

    @staticmethod
    def _headers(principal: Principal, trace_id: str) -> dict[str, str]:
        headers = {"X-Trace-Id": trace_id}
        if principal.token:
            headers["Authorization"] = f"Bearer {principal.token}"
        return headers

    def _call(self, method: str, path: str, principal: Principal, trace_id: str) -> Any:
        try:
            with httpx.Client(timeout=settings.companion_request_timeout_seconds) as client:
                response = client.request(
                    method,
                    f"{settings.spring_boot_base_url.rstrip('/')}{path}",
                    headers=self._headers(principal, trace_id),
                )
                response.raise_for_status()
                payload = response.json()
        except httpx.HTTPStatusError as exc:
            if exc.response.status_code in (401, 403):
                raise SchedulingExecutionPermissionError("MES rejected this scheduling action") from exc
            try:
                detail = exc.response.json().get("message") or exc.response.json().get("detail")
            except Exception:
                detail = None
            raise SchedulingExecutionError(detail or "MES scheduling execution failed") from exc
        except httpx.HTTPError as exc:
            raise SchedulingExecutionError("MES scheduling service is unavailable") from exc
        return payload.get("data") if isinstance(payload, dict) and "data" in payload else payload

    def execute(self, work_order_id: int, principal: Principal, trace_id: str) -> dict[str, Any]:
        self._ensure_permission(principal)
        result = self._call(
            "POST", f"/api/agent/scheduling/work-orders/{work_order_id}/execute", principal, trace_id
        )
        if not isinstance(result, dict):
            raise SchedulingExecutionError("MES returned an invalid scheduling result")
        return result

    def advisory(self, work_order_id: int, principal: Principal, trace_id: str) -> dict[str, Any]:
        self._ensure_permission(principal)
        result = self._call(
            "GET", f"/api/agent/scheduling/work-orders/{work_order_id}/advisory", principal, trace_id
        )
        if not isinstance(result, dict):
            raise SchedulingExecutionError("MES returned an invalid scheduling advisory")
        return result


scheduling_execution_service = SchedulingExecutionService()
