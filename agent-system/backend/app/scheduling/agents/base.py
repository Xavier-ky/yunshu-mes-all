"""Shared contracts for individually packaged scheduling Agents."""

from __future__ import annotations

from dataclasses import dataclass, field
from typing import Any

from app.core.security import Principal
from app.schemas import SchedulingOrderIntakeRequest


class SchedulingAgentError(RuntimeError):
    """A wrapper-level error; domain services retain their original errors."""


@dataclass
class SchedulingAgentState:
    """In-process state passed between the existing sequential Agent steps.

    This is deliberately not a database model and does not alter the durable
    MES workflow. It is a compatibility-friendly precursor to a future
    LangGraph state object.
    """

    request: SchedulingOrderIntakeRequest
    principal: Principal
    trace_id: str
    order: dict[str, Any] | None = None
    work_order: dict[str, Any] | None = None
    analysis: dict[str, Any] | None = None
    execution: dict[str, Any] | None = None
    advisory: dict[str, Any] | None = None
    production_issue: dict[str, Any] | None = None
    workflow_events: list[dict[str, Any]] = field(default_factory=list)

    @property
    def work_order_id(self) -> int:
        if not self.work_order or not self.work_order.get("work_order_id"):
            raise SchedulingAgentError("A work order is required before this scheduling Agent can run")
        return int(self.work_order["work_order_id"])


class BaseSchedulingAgent:
    """Minimal common identity for a scheduling sub-Agent wrapper."""

    agent_name = "SchedulingAgent"
