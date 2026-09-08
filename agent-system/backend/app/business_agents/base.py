"""Shared contracts for standalone MES business Agents.

These Agents intentionally have no workflow registration or write capability
yet.  They can only read live facts through ``SpringBootClient``'s GET-only
allowlist, which forwards the current MES JWT to Spring Boot.  This keeps the
package useful now without introducing mock data or a parallel data path.
"""

from __future__ import annotations

from dataclasses import dataclass, field
from typing import Any

from app.core.security import Principal
from app.integrations.spring_boot_client import SpringBootClient


class BusinessAgentError(RuntimeError):
    """Raised when a standalone Agent lacks the real MES facts it requires."""


@dataclass
class BusinessAgentState:
    """Ephemeral input for one standalone Agent read.

    ``work_order_id`` and ``work_order_no`` are deliberately distinct because
    current MES read facades use both identifier forms.  The state is never
    persisted and never replaces a domain-service transaction.
    """

    principal: Principal
    trace_id: str
    work_order_id: int | None = None
    work_order_no: str | None = None
    params: dict[str, str | int] = field(default_factory=dict)


@dataclass(frozen=True)
class BusinessReadRequest:
    """One live, GET-only MES resource required by an Agent."""

    key: str
    resource: str
    params: dict[str, str | int] | None = None
    path_params: dict[str, str | int] | None = None


class BaseBusinessAgent:
    """Non-writing base class for a separately packaged MES business Agent."""

    agent_name = "BusinessAgent"
    business_module = "未分类业务"
    description = ""
    future_write_actions: tuple[str, ...] = ()
    requires_confirmation = True

    def __init__(self, client: SpringBootClient | None = None) -> None:
        self._client = client or SpringBootClient()

    def read_requests(self, state: BusinessAgentState) -> tuple[BusinessReadRequest, ...]:
        raise NotImplementedError

    def run(self, state: BusinessAgentState) -> dict[str, Any]:
        """Return only current MES facts; failures are never replaced by demos."""

        if not state.principal.token:
            raise BusinessAgentError("独立业务 Agent 必须携带 MES 登录凭证")
        if not state.trace_id.strip():
            raise BusinessAgentError("独立业务 Agent 必须携带 trace_id")

        requests = self.read_requests(state)
        if not requests:
            raise BusinessAgentError("业务 Agent 未声明任何真实 MES 数据读取")

        facts: dict[str, Any] = {}
        for request in requests:
            # SpringBootClient rejects resources outside its GET-only allowlist.
            # Do not catch this: returning an invented fallback would violate the
            # real-database boundary of these Agent packages.
            facts[request.key] = self._client.read(
                request.resource,
                state.principal,
                state.trace_id,
                params=request.params,
                path_params=request.path_params,
            )

        return {
            "agent_name": self.agent_name,
            "business_module": self.business_module,
            "description": self.description,
            "source": "MES_DATABASE_VIA_SPRING_BOOT",
            "read_only": True,
            "workflow_connected": False,
            "trace_id": state.trace_id,
            "facts": facts,
            "future_write_actions": list(self.future_write_actions),
            "requires_confirmation_for_future_writes": self.requires_confirmation,
        }

    @staticmethod
    def require_work_order_id(state: BusinessAgentState) -> int:
        if state.work_order_id is None or int(state.work_order_id) <= 0:
            raise BusinessAgentError("该 Agent 需要真实的 work_order_id")
        return int(state.work_order_id)

    @staticmethod
    def require_work_order_no(state: BusinessAgentState) -> str:
        value = (state.work_order_no or "").strip().upper()
        if not value:
            raise BusinessAgentError("该 Agent 需要真实的 work_order_no")
        return value
