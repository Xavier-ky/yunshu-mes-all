"""Controlled gateway for the final material-issue stage of smart scheduling."""

from __future__ import annotations

from typing import Any

import httpx

from app.core.config import settings
from app.core.security import Principal


class ProductionIssueError(RuntimeError):
    pass


class ProductionIssuePermissionError(ProductionIssueError):
    pass


class ProductionIssueService:
    """Forwards only the authenticated caller to the MES production-issue boundary."""

    permitted_roles = {"MANAGER", "TESTER", "PROD_SUPERVISOR", "WAREHOUSE_CLERK"}

    @classmethod
    def _ensure_permission(cls, principal: Principal) -> None:
        if not set(principal.roles).intersection(cls.permitted_roles):
            raise ProductionIssuePermissionError("Current role cannot execute ProductionIssueAgent")

    @staticmethod
    def _headers(principal: Principal, trace_id: str) -> dict[str, str]:
        headers = {"X-Trace-Id": trace_id}
        if principal.token:
            headers["Authorization"] = f"Bearer {principal.token}"
        return headers

    def execute(self, work_order_id: int, principal: Principal, trace_id: str) -> dict[str, Any]:
        self._ensure_permission(principal)
        try:
            with httpx.Client(timeout=settings.companion_request_timeout_seconds) as client:
                response = client.post(
                    f"{settings.spring_boot_base_url.rstrip('/')}/api/agent/production-issue/work-orders/{work_order_id}/execute",
                    headers=self._headers(principal, trace_id),
                )
                response.raise_for_status()
                payload = response.json()
        except httpx.HTTPStatusError as exc:
            if exc.response.status_code in (401, 403):
                raise ProductionIssuePermissionError("MES rejected this production-issue action") from exc
            try:
                detail = exc.response.json().get("message") or exc.response.json().get("detail")
            except Exception:
                detail = None
            raise ProductionIssueError(detail or "MES production issue failed") from exc
        except httpx.HTTPError as exc:
            raise ProductionIssueError("MES production-issue service is unavailable") from exc
        result = payload.get("data") if isinstance(payload, dict) and "data" in payload else payload
        if not isinstance(result, dict):
            raise ProductionIssueError("MES returned an invalid production-issue result")
        return result


production_issue_service = ProductionIssueService()
