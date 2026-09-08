from __future__ import annotations

from typing import Any

from app.storage.agent_platform_store import AgentPlatformStoreError, agent_platform_store

_traces: dict[str, dict[str, Any]] = {}
_confirmations: dict[str, dict[str, Any]] = {}


def save_trace(trace_id: str, payload: dict[str, Any]) -> None:
    _traces[trace_id] = payload
    try:
        agent_platform_store.save_runtime_state("TRACE", trace_id, payload, state_status="DONE")
    except AgentPlatformStoreError:
        # Keep the API responsive if the database is briefly unavailable; the
        # in-process record remains a short-lived fallback, not the authority.
        pass


def get_trace(trace_id: str) -> dict[str, Any] | None:
    try:
        item = agent_platform_store.get_runtime_state("TRACE", trace_id)
        if item is not None:
            return item
    except AgentPlatformStoreError:
        pass
    return _traces.get(trace_id)


def save_confirmation(cid: str, payload: dict[str, Any]) -> None:
    _confirmations[cid] = payload
    try:
        agent_platform_store.save_runtime_state("CONFIRMATION", cid, payload, state_status="PENDING")
    except AgentPlatformStoreError:
        pass


def get_confirmation(cid: str) -> dict[str, Any] | None:
    try:
        item = agent_platform_store.get_runtime_state("CONFIRMATION", cid)
        if item is not None:
            return item
    except AgentPlatformStoreError:
        pass
    return _confirmations.get(cid)


def update_confirmation(cid: str, status: str, result: Any = None) -> dict[str, Any] | None:
    item = get_confirmation(cid)
    if not item:
        return None
    item["status"] = status
    if result is not None:
        item["result"] = result
    _confirmations[cid] = item
    try:
        agent_platform_store.update_runtime_state(
            "CONFIRMATION", cid, state_status=status.upper(), payload=item
        )
    except AgentPlatformStoreError:
        pass
    return item
