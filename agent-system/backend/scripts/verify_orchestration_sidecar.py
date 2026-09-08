"""Verify the opt-in LangGraph sidecar without calling MES business APIs.

It writes only temporary Agent-platform checkpoint/runtime rows and removes
them in ``finally``.  No order, work-order, inventory, task, dispatch, issue
or quality business row is created or changed.
"""

from __future__ import annotations

import json
import sys
from pathlib import Path

ROOT = Path(__file__).resolve().parents[1]
sys.path.insert(0, str(ROOT))

from app.orchestration.scheduling_sidecar import SchedulingOrchestrationSidecar  # noqa: E402
from app.storage.agent_platform_store import agent_platform_store  # noqa: E402


THREAD_PREFIX = "sidecar-scheduling:SIDECAR-VERIFY-"
RUNTIME_TYPE = "ORCHESTRATION_SIDECAR"


def cleanup() -> None:
    with agent_platform_store._connection() as connection:
        with connection.cursor() as cursor:
            cursor.execute("DELETE FROM agent_runtime_state WHERE state_type = %s AND state_key LIKE %s", (RUNTIME_TYPE, f"{THREAD_PREFIX}%"))
            cursor.execute("DELETE FROM agent_graph_checkpoint WHERE thread_id LIKE %s", (f"{THREAD_PREFIX}%",))


def main() -> None:
    cleanup()
    sidecar = SchedulingOrchestrationSidecar()
    try:
        paused = sidecar.preview(
            user_id=1,
            trace_id="SIDECAR-VERIFY-PAUSE",
            request_summary="验证：新订单智能排产。",
            approval_required=True,
        )
        assert paused["status"] == "WAITING_APPROVAL", paused
        assert paused["checkpoint_status"] == "INTERRUPTED", paused
        assert len(paused["node_records"]) >= 10, paused
        persisted = agent_platform_store.load_latest_checkpoint(paused["thread_id"])
        assert persisted and persisted["checkpoint_status"] == "INTERRUPTED", persisted

        resumed = sidecar.resume(thread_id=paused["thread_id"], user_id=1, approved=True)
        assert resumed["status"] == "READY_FOR_EXISTING_WORKFLOW", resumed
        assert resumed["checkpoint_status"] == "COMPLETED", resumed
        assert resumed["delegates_to_existing_workflow"] is True

        denied = sidecar.preview(
            user_id=1,
            trace_id="SIDECAR-VERIFY-DENY",
            request_summary="验证：用户拒绝委托。",
            approval_required=True,
        )
        cancelled = sidecar.resume(thread_id=denied["thread_id"], user_id=1, approved=False)
        assert cancelled["status"] == "CANCELLED", cancelled
        assert cancelled["checkpoint_status"] == "CANCELLED", cancelled

        observed = sidecar.observe_existing_result(
            user_id=1,
            trace_id="SIDECAR-VERIFY-OBSERVE",
            workflow_events=[
                {"event_id": "bom-route", "agent": "BomRouteAgent", "status": "completed", "summary": "已读取既有 BOM 与路线快照。", "sources": ["GET /api/agent/read/work-orders/{id}/bom-route"]},
                {"event_id": "kitting-risk", "agent": "KittingRiskAgent", "status": "completed", "summary": "已读取既有齐套风险结果。", "sources": ["GET /api/agent/read/work-orders/{id}/kitting"]},
            ],
        )
        assert observed["status"] == "OBSERVED", observed
        assert observed["delegates_to_existing_workflow"] is False
        assert any(item["stage"] == "observed_existing_workflow" for item in observed["node_records"])

        print(json.dumps({
            "paused": paused["checkpoint_status"],
            "resumed": resumed["status"],
            "denied": cancelled["status"],
            "observed": observed["status"],
            "business_writes": 0,
        }, ensure_ascii=False))
    finally:
        cleanup()


if __name__ == "__main__":
    main()
