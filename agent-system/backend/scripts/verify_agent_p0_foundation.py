"""Verify the durable P0 Agent baseline without leaving test rows behind."""

from __future__ import annotations

import json
import sys
from pathlib import Path

ROOT = Path(__file__).resolve().parents[1]
sys.path.insert(0, str(ROOT))

from app.storage.agent_platform_store import agent_platform_store  # noqa: E402
from app.storage.memory_store import get_confirmation, get_trace, save_confirmation, save_trace  # noqa: E402


TRACE_ID = "P0-VERIFY-TRACE"
CONFIRMATION_ID = "P0-VERIFY-CONFIRM"
THREAD_ID = "P0-VERIFY-THREAD"


def cleanup() -> None:
    with agent_platform_store._connection() as connection:
        with connection.cursor() as cursor:
            cursor.execute(
                "DELETE FROM agent_runtime_state WHERE state_key IN (%s, %s)",
                (TRACE_ID, CONFIRMATION_ID),
            )
            cursor.execute("DELETE FROM agent_graph_checkpoint WHERE thread_id = %s", (THREAD_ID,))


def main() -> None:
    cleanup()
    try:
        save_trace(TRACE_ID, {"trace_id": TRACE_ID, "user_id": "1", "status": "Done"})
        save_confirmation(
            CONFIRMATION_ID,
            {"confirmation_id": CONFIRMATION_ID, "status": "pending", "action_name": "query_work_order_pipeline"},
        )
        agent_platform_store.save_checkpoint(
            thread_id=THREAD_ID,
            checkpoint_token="cp-001",
            graph_code="MES_P0_ORCHESTRATOR",
            graph_version="0.1.0",
            checkpoint_status="INTERRUPTED",
            state={"node": "approval_gate", "resume_from": "tool_approval"},
        )
        trace = get_trace(TRACE_ID)
        confirmation = get_confirmation(CONFIRMATION_ID)
        checkpoint = agent_platform_store.load_latest_checkpoint(THREAD_ID)
        assert trace and trace["status"] == "Done"
        assert confirmation and confirmation["status"] == "pending"
        assert checkpoint and checkpoint["checkpoint_status"] == "INTERRUPTED"
        assert checkpoint["state"]["node"] == "approval_gate"
        print(
            json.dumps(
                {
                    "trace": trace["status"],
                    "confirmation": confirmation["status"],
                    "checkpoint": checkpoint["checkpoint_status"],
                    "resume_node": checkpoint["state"]["node"],
                },
                ensure_ascii=False,
            )
        )
    finally:
        cleanup()


if __name__ == "__main__":
    main()
