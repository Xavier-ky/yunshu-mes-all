"""Stable contracts for packaged real Agent tools.

The contract is deliberately read-only in this release.  It describes a tool
without giving it any additional authority beyond the existing gateway.
"""

from __future__ import annotations

from dataclasses import asdict, dataclass
from typing import Any


@dataclass(frozen=True)
class PackagedReadToolDefinition:
    tool_name: str
    governance_tool_id: str
    display_name: str
    domain: str
    source: str
    required_arguments: tuple[str, ...]
    description: str
    access_mode: str = "READ"
    side_effect_free: bool = True
    contract_version: str = "1.0.0"

    def to_dict(self) -> dict[str, Any]:
        payload = asdict(self)
        payload["required_arguments"] = list(self.required_arguments)
        return payload
