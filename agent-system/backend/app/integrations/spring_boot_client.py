from __future__ import annotations

from typing import Any

import httpx

from app.core.config import settings
from app.core.security import Principal


class SpringBootClient:
    """The companion's single, read-only gateway to the MES service."""

    _ALLOWLIST: dict[str, tuple[str, str]] = {
        "dashboard": ("GET", "/api/dashboard/summary"),
        "today_production_overview": ("GET", "/api/agent/companion/production-overview"),
        "quality_task_backlog": ("GET", "/api/agent/companion/quality-backlog"),
        "work_orders": ("GET", "/api/planning/work-orders"),
        "inventory": ("GET", "/api/inventory/batches"),
        "andon": ("GET", "/api/andon/events"),
        "equipment": ("GET", "/api/equipment/devices"),
        "quality": ("GET", "/api/quality/records"),
        # The legacy quality-record endpoint is retained for older pages but
        # is not reliable as an Agent fact source. Use the active Yunshu
        # quality workbench endpoints below for new Agent packages.
        "quality_pending": ("GET", "/api/mes/qc/pending/list"),
        "quality_analytics_summary": ("GET", "/api/mes/qc/analytics/summary"),
        "equipment_workbench_summary": ("GET", "/api/mes/dv/workbench/summary"),
        "equipment_workbench_pending": ("GET", "/api/mes/dv/workbench/pending"),
        # P0 Agent real-fact facade. Paths are intentionally separate from
        # page APIs: all of these handlers are side-effect-free snapshots.
        "agent_work_order_pipeline": ("GET", "/api/agent/read/work-orders/{work_order_no}/pipeline"),
        "agent_bom_route_snapshot": ("GET", "/api/agent/read/work-orders/{work_order_id}/bom-route"),
        "agent_kitting_snapshot": ("GET", "/api/agent/read/work-orders/{work_order_id}/kitting"),
        "agent_task_snapshot": ("GET", "/api/agent/read/work-orders/{work_order_id}/tasks"),
        "agent_dispatch_snapshot": ("GET", "/api/agent/read/work-orders/{work_order_id}/dispatches"),
        "agent_work_order_trace": ("GET", "/api/agent/read/work-orders/{work_order_no}/trace"),
        "agent_quality_overview": ("GET", "/api/agent/read/quality/overview"),
        "agent_quality_report_snapshot": ("GET", "/api/agent/read/quality/report-snapshot"),
        "agent_work_order_quality": ("GET", "/api/agent/read/work-orders/{work_order_no}/quality"),
    }

    def read(
        self,
        resource: str,
        principal: Principal,
        trace_id: str,
        params: dict[str, str | int] | None = None,
        path_params: dict[str, str | int] | None = None,
    ) -> Any:
        spec = self._ALLOWLIST.get(resource)
        if not spec:
            raise ValueError(f"小精灵不允许读取资源: {resource}")
        method, path = spec
        if method != "GET":
            raise ValueError("小精灵只允许调用只读 MES 接口")

        path_params = path_params or {}
        try:
            path = path.format(**{key: self._safe_path_value(value) for key, value in path_params.items()})
        except KeyError as exc:
            raise ValueError(f"Agent 工具缺少路径参数: {exc.args[0]}") from exc
        if "{" in path or "}" in path:
            raise ValueError("Agent 工具路径参数不完整")

        headers = {"X-Trace-Id": trace_id}
        if principal.token:
            headers["Authorization"] = f"Bearer {principal.token}"
        with httpx.Client(timeout=settings.companion_request_timeout_seconds) as client:
            response = client.get(f"{settings.spring_boot_base_url.rstrip('/')}{path}", headers=headers, params=params)
            response.raise_for_status()
            payload = response.json()
        if isinstance(payload, dict) and "data" in payload:
            return payload["data"]
        return payload

    @staticmethod
    def _safe_path_value(value: str | int) -> str:
        text = str(value).strip()
        if not text or any(char in text for char in ("/", "\\", "?", "#")):
            raise ValueError("Agent 工具路径参数非法")
        return text
