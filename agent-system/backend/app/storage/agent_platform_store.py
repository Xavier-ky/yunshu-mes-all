"""Durable runtime storage for the Agent platform.

This store deliberately persists only Agent state.  It never reads or writes
MES production tables directly; business facts continue to come from the
authenticated Spring Boot APIs.
"""

from __future__ import annotations

import json
from datetime import datetime
from typing import Any

from app.storage.mysql_store import ConversationStoreError, MySqlConversationStore


class AgentPlatformStoreError(ConversationStoreError):
    """Raised when durable Agent runtime state cannot be read or written."""


class AgentPlatformStore(MySqlConversationStore):
    """MySQL-backed persistence for LangGraph checkpoints and runtime records."""

    @staticmethod
    def _decode(value: Any) -> dict[str, Any] | None:
        if value is None:
            return None
        if isinstance(value, dict):
            return value
        return json.loads(value)

    @staticmethod
    def _owner_id(payload: dict[str, Any]) -> int | None:
        value = payload.get("user_id") or payload.get("owner_user_id")
        if value is None:
            return None
        return int(value) if str(value).isdigit() else None

    def save_runtime_state(
        self,
        state_type: str,
        state_key: str,
        payload: dict[str, Any],
        *,
        state_status: str = "ACTIVE",
        expires_at: datetime | None = None,
    ) -> None:
        try:
            with self._connection() as connection:
                with connection.cursor() as cursor:
                    cursor.execute(
                        """
                        INSERT INTO agent_runtime_state
                          (state_type, state_key, owner_user_id, state_status, payload_json, expires_at)
                        VALUES (%s, %s, %s, %s, %s, %s)
                        ON DUPLICATE KEY UPDATE
                          owner_user_id = VALUES(owner_user_id),
                          state_status = VALUES(state_status),
                          payload_json = VALUES(payload_json),
                          expires_at = VALUES(expires_at),
                          updated_at = CURRENT_TIMESTAMP(3)
                        """,
                        (
                            state_type,
                            state_key,
                            self._owner_id(payload),
                            state_status,
                            json.dumps(payload, ensure_ascii=False, default=str),
                            expires_at,
                        ),
                    )
        except Exception as exc:
            raise AgentPlatformStoreError(str(exc)) from exc

    def get_runtime_state(self, state_type: str, state_key: str) -> dict[str, Any] | None:
        try:
            with self._connection() as connection:
                with connection.cursor() as cursor:
                    cursor.execute(
                        """
                        SELECT payload_json FROM agent_runtime_state
                        WHERE state_type = %s AND state_key = %s
                          AND (expires_at IS NULL OR expires_at > CURRENT_TIMESTAMP(3))
                        """,
                        (state_type, state_key),
                    )
                    row = cursor.fetchone()
        except Exception as exc:
            raise AgentPlatformStoreError(str(exc)) from exc
        return self._decode(row["payload_json"]) if row else None

    def list_runtime_states(
        self,
        state_type: str,
        owner_user_id: int,
        *,
        limit: int = 50,
    ) -> list[dict[str, Any]]:
        """List non-expired runtime records owned by one MES user.

        This is intentionally scoped to Agent-platform state only.  It is
        used by memory/reflection read models and never exposes MES business
        records or another user's state.
        """
        try:
            with self._connection() as connection:
                with connection.cursor() as cursor:
                    cursor.execute(
                        """
                        SELECT state_key, state_status, payload_json, expires_at, created_at, updated_at
                        FROM agent_runtime_state
                        WHERE state_type = %s AND owner_user_id = %s
                          AND (expires_at IS NULL OR expires_at > CURRENT_TIMESTAMP(3))
                        ORDER BY updated_at DESC, runtime_state_id DESC
                        LIMIT %s
                        """,
                        (state_type, int(owner_user_id), max(1, min(int(limit), 100))),
                    )
                    rows = cursor.fetchall()
        except Exception as exc:
            raise AgentPlatformStoreError(str(exc)) from exc
        result = []
        for row in rows:
            result.append(
                {
                    "state_key": row["state_key"],
                    "state_status": row["state_status"],
                    "payload": self._decode(row["payload_json"]) or {},
                    "expires_at": row["expires_at"],
                    "created_at": row["created_at"],
                    "updated_at": row["updated_at"],
                }
            )
        return result

    def update_runtime_state(
        self,
        state_type: str,
        state_key: str,
        *,
        state_status: str,
        payload: dict[str, Any],
    ) -> dict[str, Any] | None:
        existing = self.get_runtime_state(state_type, state_key)
        if existing is None:
            return None
        self.save_runtime_state(state_type, state_key, payload, state_status=state_status)
        return payload

    def delete_runtime_state(self, state_type: str, state_key: str) -> None:
        """Delete one exact Agent-runtime record (maintenance/test cleanup only)."""
        try:
            with self._connection() as connection:
                with connection.cursor() as cursor:
                    cursor.execute(
                        "DELETE FROM agent_runtime_state WHERE state_type = %s AND state_key = %s",
                        (state_type, state_key),
                    )
        except Exception as exc:
            raise AgentPlatformStoreError(str(exc)) from exc

    def delete_checkpoints(self, thread_id: str) -> None:
        """Delete checkpoints for one exact test/maintenance graph thread."""
        try:
            with self._connection() as connection:
                with connection.cursor() as cursor:
                    cursor.execute("DELETE FROM agent_graph_checkpoint WHERE thread_id = %s", (thread_id,))
        except Exception as exc:
            raise AgentPlatformStoreError(str(exc)) from exc

    def save_checkpoint(
        self,
        *,
        thread_id: str,
        checkpoint_token: str,
        graph_code: str,
        graph_version: str,
        state: dict[str, Any],
        checkpoint_status: str = "RUNNING",
        parent_checkpoint_token: str | None = None,
        session_id: int | None = None,
        task_id: int | None = None,
        user_id: int | None = None,
        biz_object_type: str | None = None,
        biz_object_id: int | None = None,
    ) -> None:
        try:
            with self._connection() as connection:
                with connection.cursor() as cursor:
                    cursor.execute(
                        """
                        INSERT INTO agent_graph_checkpoint
                          (thread_id, checkpoint_token, parent_checkpoint_token, graph_code, graph_version,
                           session_id, task_id, user_id, biz_object_type, biz_object_id,
                           checkpoint_status, state_json)
                        VALUES (%s, %s, %s, %s, %s, %s, %s, %s, %s, %s, %s, %s)
                        ON DUPLICATE KEY UPDATE
                          parent_checkpoint_token = VALUES(parent_checkpoint_token),
                          graph_code = VALUES(graph_code), graph_version = VALUES(graph_version),
                          session_id = VALUES(session_id), task_id = VALUES(task_id), user_id = VALUES(user_id),
                          biz_object_type = VALUES(biz_object_type), biz_object_id = VALUES(biz_object_id),
                          checkpoint_status = VALUES(checkpoint_status), state_json = VALUES(state_json),
                          updated_at = CURRENT_TIMESTAMP(3)
                        """,
                        (
                            thread_id,
                            checkpoint_token,
                            parent_checkpoint_token,
                            graph_code,
                            graph_version,
                            session_id,
                            task_id,
                            user_id,
                            biz_object_type,
                            biz_object_id,
                            checkpoint_status,
                            json.dumps(state, ensure_ascii=False, default=str),
                        ),
                    )
        except Exception as exc:
            raise AgentPlatformStoreError(str(exc)) from exc

    def load_latest_checkpoint(self, thread_id: str) -> dict[str, Any] | None:
        try:
            with self._connection() as connection:
                with connection.cursor() as cursor:
                    cursor.execute(
                        """
                        SELECT checkpoint_token, parent_checkpoint_token, graph_code, graph_version,
                               session_id, task_id, user_id, biz_object_type, biz_object_id,
                               checkpoint_status, state_json, created_at, updated_at
                        FROM agent_graph_checkpoint
                        WHERE thread_id = %s
                        ORDER BY updated_at DESC, checkpoint_id DESC LIMIT 1
                        """,
                        (thread_id,),
                    )
                    row = cursor.fetchone()
        except Exception as exc:
            raise AgentPlatformStoreError(str(exc)) from exc
        if not row:
            return None
        row["state"] = self._decode(row.pop("state_json")) or {}
        return row


agent_platform_store = AgentPlatformStore()
