"""Durable, user-owned snapshots for quality report rendering and export."""

from __future__ import annotations

import json
from datetime import datetime, timedelta, timezone
from typing import Any

from app.storage.mysql_store import ConversationStoreError, conversation_store


class QualityReportStore:
    """Stores a frozen report payload outside mutable MES quality tables."""

    _create_table_sql = """
        CREATE TABLE IF NOT EXISTS agent_report_artifact (
            report_id VARCHAR(64) NOT NULL,
            session_id BIGINT UNSIGNED NOT NULL,
            user_id BIGINT UNSIGNED NOT NULL,
            agent_id BIGINT UNSIGNED NOT NULL,
            report_type VARCHAR(48) NOT NULL,
            status VARCHAR(24) NOT NULL,
            window_days INT NOT NULL,
            queried_at DATETIME(3) NULL,
            snapshot_json MEDIUMTEXT NOT NULL,
            created_at DATETIME(3) NOT NULL DEFAULT CURRENT_TIMESTAMP(3),
            ready_at DATETIME(3) NULL,
            exported_at DATETIME(3) NULL,
            expires_at DATETIME(3) NULL,
            PRIMARY KEY (report_id),
            KEY idx_agent_report_session (session_id, created_at),
            KEY idx_agent_report_owner (user_id, created_at)
        ) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='Agent quality report snapshots'
    """

    def _ensure_table(self, cursor: Any) -> None:
        cursor.execute(self._create_table_sql)

    @staticmethod
    def _json(snapshot: dict[str, Any]) -> str:
        return json.dumps(snapshot, ensure_ascii=False, separators=(",", ":"), default=str)

    def create(
        self,
        *,
        report_id: str,
        session_id: int,
        user_id: int,
        agent_id: int,
        window_days: int,
        queried_at: datetime | None,
        snapshot: dict[str, Any],
    ) -> None:
        expires_at = datetime.now(timezone.utc).replace(tzinfo=None) + timedelta(days=30)
        with conversation_store._connection() as connection:
            with connection.cursor() as cursor:
                self._ensure_table(cursor)
                cursor.execute(
                    """
                    INSERT INTO agent_report_artifact
                    (report_id, session_id, user_id, agent_id, report_type, status,
                     window_days, queried_at, snapshot_json, expires_at)
                    VALUES (%s, %s, %s, %s, 'QUALITY_ANALYSIS', 'GENERATING',
                            %s, %s, %s, %s)
                    """,
                    (report_id, session_id, user_id, agent_id, window_days, queried_at, self._json(snapshot), expires_at),
                )

    def mark_ready(self, report_id: str, snapshot: dict[str, Any]) -> None:
        with conversation_store._connection() as connection:
            with connection.cursor() as cursor:
                self._ensure_table(cursor)
                cursor.execute(
                    """
                    UPDATE agent_report_artifact
                    SET status = CASE WHEN status = 'EXPORTED' THEN 'EXPORTED' ELSE 'READY' END,
                        snapshot_json = %s,
                        ready_at = CURRENT_TIMESTAMP(3)
                    WHERE report_id = %s
                    """,
                    (self._json(snapshot), report_id),
                )
                if cursor.rowcount != 1:
                    raise ConversationStoreError("Quality report snapshot was not found.")

    def mark_exported(self, report_id: str, external_user_id: str) -> None:
        with conversation_store._connection() as connection:
            with connection.cursor() as cursor:
                self._ensure_table(cursor)
                user_id = conversation_store._resolve_user_id(cursor, external_user_id)
                cursor.execute(
                    """
                    UPDATE agent_report_artifact
                    SET status = 'EXPORTED', exported_at = CURRENT_TIMESTAMP(3)
                    WHERE report_id = %s AND user_id = %s AND status IN ('READY', 'EXPORTED')
                    """,
                    (report_id, user_id),
                )
                if cursor.rowcount != 1:
                    raise ConversationStoreError("Quality report does not exist or does not belong to the current user.")

    def get_owned(self, report_id: str, external_user_id: str) -> dict[str, Any] | None:
        with conversation_store._connection() as connection:
            with connection.cursor() as cursor:
                self._ensure_table(cursor)
                user_id = conversation_store._resolve_user_id(cursor, external_user_id)
                cursor.execute(
                    """
                    SELECT report_id, status, snapshot_json
                    FROM agent_report_artifact
                    WHERE report_id = %s AND user_id = %s
                    """,
                    (report_id, user_id),
                )
                row = cursor.fetchone()
        if not row:
            return None
        try:
            snapshot = json.loads(row.get("snapshot_json") or "{}")
        except (TypeError, ValueError):
            snapshot = {}
        return {"report_id": str(row["report_id"]), "status": str(row["status"]), "snapshot": snapshot}


quality_report_store = QualityReportStore()
