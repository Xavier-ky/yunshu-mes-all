"""Owner-scoped Agent memory primitives.

RAG remains the project's governed MES knowledge base.  This module stores
only runtime context and explicit, reviewable user-owned memory records in
the existing Agent-platform state table.  It never joins or updates MES
transactional tables and it is not automatically injected into model prompts.
"""

from __future__ import annotations

import re
from datetime import datetime, timedelta
from typing import Any, Optional

from app.storage.agent_platform_store import AgentPlatformStore, AgentPlatformStoreError, agent_platform_store


class AgentMemoryError(RuntimeError):
    """Raised when a memory request is invalid or not owned by the caller."""


class AgentMemoryService:
    short_term_state_type = "AGENT_SHORT_TERM_MEMORY"
    long_term_state_type = "AGENT_LONG_TERM_MEMORY"
    short_term_ttl_hours = 8
    _memory_key_pattern = re.compile(r"^[a-z0-9][a-z0-9_-]{0,63}$")
    _allowed_long_term_kinds = {"preference", "decision_pattern", "operational_note"}

    def __init__(self, store: Optional[AgentPlatformStore] = None) -> None:
        self._store = store or agent_platform_store

    @staticmethod
    def _short_key(user_id: int, session_id: int) -> str:
        return f"user:{int(user_id)}:session:{int(session_id)}"

    @staticmethod
    def _long_key(user_id: int, memory_key: str) -> str:
        return f"user:{int(user_id)}:memory:{memory_key}"

    @staticmethod
    def _clean_text(value: Any, *, max_length: int = 800) -> str:
        return " ".join(str(value or "").split())[:max_length]

    def save_short_term(
        self,
        *,
        user_id: int,
        session_id: int,
        active_task: str,
        facts: Optional[dict[str, Any]] = None,
        trace_id: Optional[str] = None,
    ) -> dict[str, Any]:
        """Store bounded current-task context for one owner and one session."""
        if int(user_id) <= 0 or int(session_id) <= 0:
            raise AgentMemoryError("short-term memory requires a valid user and session")
        task = self._clean_text(active_task)
        if not task:
            raise AgentMemoryError("short-term memory requires an active task")
        payload = {
            "user_id": int(user_id),
            "session_id": int(session_id),
            "memory_scope": "short_term",
            "active_task": task,
            "facts": self._safe_facts(facts or {}),
            "trace_id": self._clean_text(trace_id, max_length=128) if trace_id else None,
            "policy": "runtime-context-only; not automatically injected into model prompts",
        }
        # The project's local MySQL profile evaluates CURRENT_TIMESTAMP in the
        # server's local timezone.  Use the same naive local clock for this
        # DATETIME field; datetime.utcnow() would make a freshly written row
        # appear expired when the two clocks differ by the local UTC offset.
        expires_at = datetime.now() + timedelta(hours=self.short_term_ttl_hours)
        self._store.save_runtime_state(
            self.short_term_state_type,
            self._short_key(user_id, session_id),
            payload,
            state_status="ACTIVE",
            expires_at=expires_at,
        )
        return {**payload, "expires_at": expires_at.isoformat() + "Z"}

    def get_short_term(self, *, user_id: int, session_id: int) -> Optional[dict[str, Any]]:
        payload = self._store.get_runtime_state(self.short_term_state_type, self._short_key(user_id, session_id))
        if payload is None:
            return None
        self._require_owner(payload, user_id)
        return payload

    def save_long_term(
        self,
        *,
        user_id: int,
        memory_key: str,
        kind: str,
        content: str,
        source: str = "USER_CONFIRMED",
    ) -> dict[str, Any]:
        """Store explicit, reviewable long-term memory; no automatic learning."""
        if int(user_id) <= 0:
            raise AgentMemoryError("long-term memory requires a valid user")
        normalized_key = str(memory_key or "").strip().lower()
        normalized_kind = str(kind or "").strip().lower()
        if not self._memory_key_pattern.fullmatch(normalized_key):
            raise AgentMemoryError("memory key must use lowercase letters, digits, _ or -")
        if normalized_kind not in self._allowed_long_term_kinds:
            raise AgentMemoryError("unsupported long-term memory kind")
        text = self._clean_text(content, max_length=1200)
        if not text:
            raise AgentMemoryError("long-term memory requires content")
        payload = {
            "user_id": int(user_id),
            "memory_scope": "long_term",
            "memory_key": normalized_key,
            "kind": normalized_kind,
            "content": text,
            "source": self._clean_text(source, max_length=64) or "USER_CONFIRMED",
            "policy": "explicit-and-reviewable; not automatically injected into model prompts",
        }
        self._store.save_runtime_state(
            self.long_term_state_type,
            self._long_key(user_id, normalized_key),
            payload,
            state_status="ACTIVE",
        )
        return payload

    def list_long_term(self, *, user_id: int, limit: int = 30) -> list[dict[str, Any]]:
        rows = self._store.list_runtime_states(self.long_term_state_type, int(user_id), limit=limit)
        result = []
        for row in rows:
            payload = row["payload"]
            self._require_owner(payload, user_id)
            result.append(
                {
                    "memory_key": payload.get("memory_key"),
                    "kind": payload.get("kind"),
                    "content": payload.get("content"),
                    "source": payload.get("source"),
                    "updated_at": str(row.get("updated_at") or ""),
                }
            )
        return result

    @staticmethod
    def _require_owner(payload: dict[str, Any], user_id: int) -> None:
        if int(payload.get("user_id") or 0) != int(user_id):
            raise AgentMemoryError("the requested Agent memory does not belong to the current user")

    @classmethod
    def _safe_facts(cls, facts: dict[str, Any]) -> dict[str, Any]:
        """Bound memory payload size and preserve only JSON-friendly context."""
        safe: dict[str, Any] = {}
        for key, value in list(facts.items())[:20]:
            clean_key = cls._clean_text(key, max_length=80)
            if not clean_key:
                continue
            if isinstance(value, (str, int, float, bool)) or value is None:
                safe[clean_key] = cls._clean_text(value, max_length=400) if isinstance(value, str) else value
            elif isinstance(value, list):
                safe[clean_key] = [cls._clean_text(item, max_length=160) for item in value[:10]]
            elif isinstance(value, dict):
                safe[clean_key] = {cls._clean_text(k, max_length=60): cls._clean_text(v, max_length=160) for k, v in list(value.items())[:10]}
        return safe


agent_memory_service = AgentMemoryService()
