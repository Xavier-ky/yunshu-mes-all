"""Stable, serialisable contracts for the Agent orchestration sidecar.

These contracts deliberately contain only Agent-control data.  MES business
facts and writes stay inside the existing Spring Boot domain services.
"""

from __future__ import annotations

from dataclasses import asdict, dataclass, field
from typing import Any


@dataclass(frozen=True)
class AgentNodeRecord:
    """One auditable planning, policy, or observed workflow node."""

    node_id: str
    agent_name: str
    stage: str
    status: str
    summary: str
    evidence: list[str] = field(default_factory=list)

    def to_dict(self) -> dict[str, Any]:
        return asdict(self)


@dataclass(frozen=True)
class ApprovalDecision:
    """A user-owned decision required before a future delegated write."""

    approval_required: bool
    approved: bool | None
    reason: str
    requested_by: int

    def to_dict(self) -> dict[str, Any]:
        return asdict(self)


DEFAULT_SCHEDULING_AGENT_ORDER: tuple[str, ...] = (
    "IntentRoutingAgent",
    "OrderIntakeAgent",
    "BomRouteAgent",
    "KittingRiskAgent",
    "KittingExecutionAgent",
    "CapacitySchedulingAgent",
    "DispatchExecutionAgent",
    "SchedulingValidationAgent",
    "ProductionIssueAgent",
)
