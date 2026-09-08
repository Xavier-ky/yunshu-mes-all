"""User-scoped conversation persistence in the MES Agent platform tables."""

from __future__ import annotations

import json
from contextlib import contextmanager
from typing import Any, Iterator

from app.core.config import settings


class ConversationStoreError(RuntimeError):
    pass


class MySqlConversationStore:
    companion_agent_code = "AGENT_COMPANION"
    main_agent_code = "AGENT_MAIN"
    _message_envelope_key = "__yunshu_agent_message_v1__"

    @classmethod
    def _encode_message(cls, content: str, metadata: dict[str, Any] | None) -> str:
        """Keep rich Agent-card metadata in the existing auditable message row.

        The current local profile has Flyway disabled, so introducing a new
        column would make deployments fragile.  A versioned envelope keeps
        the visible text and its speaker/evidence together without changing
        the established `agent_message` contract.  Plain historic messages
        remain fully compatible.
        """
        if not metadata:
            return content
        return json.dumps(
            {cls._message_envelope_key: True, "content": content, "metadata": metadata},
            ensure_ascii=False,
            separators=(",", ":"),
        )

    @classmethod
    def _decode_message(cls, raw: str) -> tuple[str, dict[str, Any]]:
        try:
            value = json.loads(raw)
        except (TypeError, ValueError):
            return raw, {}
        if not isinstance(value, dict) or not value.get(cls._message_envelope_key):
            return raw, {}
        content = value.get("content")
        metadata = value.get("metadata")
        return str(content or ""), metadata if isinstance(metadata, dict) else {}

    @contextmanager
    def _connection(self) -> Iterator[Any]:
        try:
            import pymysql

            connection = pymysql.connect(
                host=settings.agent_db_host,
                port=settings.agent_db_port,
                user=settings.agent_db_user,
                password=settings.agent_db_password,
                database=settings.agent_db_name,
                charset="utf8mb4",
                cursorclass=pymysql.cursors.DictCursor,
                autocommit=False,
            )
        except Exception as exc:
            raise ConversationStoreError(f"Unable to connect to MES database: {exc}") from exc
        try:
            yield connection
            connection.commit()
        except Exception:
            connection.rollback()
            raise
        finally:
            connection.close()

    @staticmethod
    def _one(cursor: Any, sql: str, params: tuple[Any, ...]) -> dict[str, Any] | None:
        cursor.execute(sql, params)
        return cursor.fetchone()

    def _resolve_user_id(self, cursor: Any, principal_user_id: str) -> int:
        if principal_user_id.isdigit():
            row = self._one(
                cursor, "SELECT user_id FROM sys_user WHERE user_id = %s AND is_deleted = 0", (principal_user_id,)
            )
        else:
            row = self._one(
                cursor, "SELECT user_id FROM sys_user WHERE username = %s AND is_deleted = 0", (principal_user_id,)
            )
        if not row:
            raise ConversationStoreError("The logged-in MES user cannot be found for conversation storage.")
        return int(row["user_id"])

    def _ensure_agent(
        self,
        cursor: Any,
        agent_code: str,
        agent_name: str,
        agent_type: str,
        description: str,
    ) -> int:
        cursor.execute(
            """
            INSERT INTO agent_profile (agent_code, agent_name, agent_type, description, status)
            VALUES (%s, %s, %s, %s, 'ENABLED')
            ON DUPLICATE KEY UPDATE status = 'ENABLED', is_deleted = 0, updated_at = CURRENT_TIMESTAMP(3)
            """,
            (
                agent_code,
                agent_name,
                agent_type,
                description,
            ),
        )
        row = self._one(
            cursor, "SELECT agent_id FROM agent_profile WHERE agent_code = %s", (agent_code,)
        )
        if not row:
            raise ConversationStoreError(f"Unable to initialize Agent profile: {agent_code}.")
        return int(row["agent_id"])

    def _ensure_companion_agent(self, cursor: Any) -> int:
        return self._ensure_agent(
            cursor,
            self.companion_agent_code,
            "云枢小智陪伴助手",
            "COMPANION",
            "右下角小精灵使用本地 Qwen3-0.6B 的只读对话助手",
        )

    def _ensure_main_agent(self, cursor: Any) -> int:
        return self._ensure_agent(
            cursor,
            self.main_agent_code,
            "云枢小智",
            "ORCHESTRATOR",
            "云枢智造 MES 的主对话与多 Agent 编排助手",
        )

    def prepare_conversation(
        self, external_user_id: str, public_session_id: str | None, title: str
    ) -> tuple[int, int, int, str]:
        return self._prepare_conversation(
            external_user_id,
            public_session_id,
            title,
            self._ensure_companion_agent,
        )

    def prepare_main_conversation(
        self, external_user_id: str, public_session_id: str | None, title: str
    ) -> tuple[int, int, int, str]:
        """Create or resume the authenticated user's 云枢小智 main-chat session."""
        return self._prepare_conversation(
            external_user_id,
            public_session_id,
            title,
            self._ensure_main_agent,
        )

    def _prepare_conversation(
        self,
        external_user_id: str,
        public_session_id: str | None,
        title: str,
        ensure_agent: Any,
    ) -> tuple[int, int, int, str]:
        with self._connection() as connection:
            with connection.cursor() as cursor:
                user_id = self._resolve_user_id(cursor, external_user_id)
                agent_id = ensure_agent(cursor)
                session_id: int | None = None
                if public_session_id and public_session_id.isdigit():
                    row = self._one(
                        cursor,
                        """
                        SELECT session_id FROM agent_session
                        WHERE session_id = %s AND user_id = %s AND agent_id = %s AND session_status = 'OPEN'
                        """,
                        (public_session_id, user_id, agent_id),
                    )
                    session_id = int(row["session_id"]) if row else None
                if session_id is None:
                    cursor.execute(
                        """
                        INSERT INTO agent_session (agent_id, user_id, session_title, session_status)
                        VALUES (%s, %s, %s, 'OPEN')
                        """,
                        (agent_id, user_id, title[:120]),
                    )
                    session_id = int(cursor.lastrowid)
                return user_id, agent_id, session_id, str(session_id)

    def list_main_sessions(self, external_user_id: str, limit: int = 40) -> list[dict[str, str]]:
        """Return only the caller's persisted main-chat sessions, newest first."""
        with self._connection() as connection:
            with connection.cursor() as cursor:
                user_id = self._resolve_user_id(cursor, external_user_id)
                agent_id = self._ensure_main_agent(cursor)
                cursor.execute(
                    """
                    SELECT s.session_id, s.session_title, s.session_status,
                           DATE_FORMAT(s.start_time, '%%Y-%%m-%%d %%H:%%i:%%s') AS created_at,
                           DATE_FORMAT(COALESCE(MAX(m.send_time), s.start_time), '%%Y-%%m-%%d %%H:%%i:%%s') AS updated_at
                    FROM agent_session s
                    LEFT JOIN agent_message m ON m.session_id = s.session_id
                    WHERE s.user_id = %s AND s.agent_id = %s
                    GROUP BY s.session_id, s.session_title, s.session_status, s.start_time
                    ORDER BY COALESCE(MAX(m.send_time), s.start_time) DESC, s.session_id DESC
                    LIMIT %s
                    """,
                    (user_id, agent_id, max(1, min(limit, 100))),
                )
                rows = cursor.fetchall()
        return [
            {
                "id": str(row["session_id"]),
                "title": row["session_title"] or "新对话",
                "status": row["session_status"] or "OPEN",
                "created_at": row["created_at"] or "",
                "updated_at": row["updated_at"] or "",
            }
            for row in rows
        ]

    def get_main_session(self, external_user_id: str, public_session_id: str) -> dict[str, Any] | None:
        """Load one main-chat session after verifying it belongs to the caller."""
        if not public_session_id.isdigit():
            return None
        with self._connection() as connection:
            with connection.cursor() as cursor:
                user_id = self._resolve_user_id(cursor, external_user_id)
                agent_id = self._ensure_main_agent(cursor)
                row = self._one(
                    cursor,
                    """
                    SELECT session_id, session_title, session_status
                    FROM agent_session
                    WHERE session_id = %s AND user_id = %s AND agent_id = %s
                    """,
                    (public_session_id, user_id, agent_id),
                )
                if not row:
                    return None
                cursor.execute(
                    """
                    SELECT message_id, sender_type, message_content,
                           DATE_FORMAT(send_time, '%%Y-%%m-%%d %%H:%%i:%%s') AS send_time
                    FROM agent_message
                    WHERE session_id = %s AND sender_type IN ('USER', 'AGENT')
                    ORDER BY send_time ASC, message_id ASC
                    """,
                    (row["session_id"],),
                )
                messages = cursor.fetchall()
        return {
            "id": str(row["session_id"]),
            "title": row["session_title"] or "新对话",
            "status": row["session_status"] or "OPEN",
            "messages": [self._history_message(message) for message in messages],
        }

    def _history_message(self, message: dict[str, Any]) -> dict[str, Any]:
        content, metadata = self._decode_message(message["message_content"])
        return {
            "id": str(message["message_id"]),
            "role": "user" if message["sender_type"] == "USER" else "assistant",
            "content": content,
            "time": message["send_time"] or "",
            "agent_name": metadata.get("agent_name"),
            "workflow_event": metadata.get("workflow_event"),
            "model_analysis": metadata.get("model_analysis"),
            "report": metadata.get("report"),
            "report_id": metadata.get("report_id"),
        }

    def load_history(self, session_id: int, max_turns: int) -> list[dict[str, str]]:
        limit = max(2, max_turns * 2)
        with self._connection() as connection:
            with connection.cursor() as cursor:
                cursor.execute(
                    """
                    SELECT sender_type, message_content FROM agent_message
                    WHERE session_id = %s AND sender_type IN ('USER', 'AGENT')
                    ORDER BY send_time DESC, message_id DESC LIMIT %s
                    """,
                    (session_id, limit),
                )
                rows = list(reversed(cursor.fetchall()))
        history = []
        for row in rows:
            content, _metadata = self._decode_message(row["message_content"])
            history.append({"role": "user" if row["sender_type"] == "USER" else "assistant", "content": content})
        return history

    def append_message(
        self,
        session_id: int,
        sender_type: str,
        sender_id: int | None,
        content: str,
        token_count: int | None = None,
        metadata: dict[str, Any] | None = None,
    ) -> None:
        stored_content = self._encode_message(content, metadata)
        with self._connection() as connection:
            with connection.cursor() as cursor:
                cursor.execute(
                    """
                    INSERT INTO agent_message (session_id, sender_type, sender_id, message_content, token_count)
                    VALUES (%s, %s, %s, %s, %s)
                    """,
                    (session_id, sender_type, sender_id, stored_content, token_count),
                )

    def require_main_session(
        self, external_user_id: str, public_session_id: str | None
    ) -> tuple[int, int, int, str]:
        """Resolve an existing main-chat session owned by the current JWT user.

        Scheduling results must never be appended to another user's session
        or silently create an unrelated session after an expired browser tab.
        """
        if not public_session_id or not public_session_id.isdigit():
            raise ConversationStoreError("请先在云枢小智主对话中发起排产请求")
        with self._connection() as connection:
            with connection.cursor() as cursor:
                user_id = self._resolve_user_id(cursor, external_user_id)
                agent_id = self._ensure_main_agent(cursor)
                row = self._one(
                    cursor,
                    """
                    SELECT session_id FROM agent_session
                    WHERE session_id = %s AND user_id = %s AND agent_id = %s AND session_status = 'OPEN'
                    """,
                    (int(public_session_id), user_id, agent_id),
                )
                if not row:
                    raise ConversationStoreError("当前排产会话不存在、已关闭或不属于当前用户")
                return user_id, agent_id, int(row["session_id"]), str(row["session_id"])


conversation_store = MySqlConversationStore()
