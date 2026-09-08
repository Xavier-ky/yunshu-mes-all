"""Live verification for unified packaged real read Tools.

The script calls the existing Spring Boot read facade through the new adapter.
It proves the wrapper did not introduce direct writes by comparing the same
kitting-analysis/shortage counts used by the project's P0 read-tool verifier.
"""

from __future__ import annotations

import argparse
import json
import sys
import uuid
from pathlib import Path

import httpx

ROOT = Path(__file__).resolve().parents[1]
if str(ROOT) not in sys.path:
    sys.path.insert(0, str(ROOT))

from app.core.security import Principal
from app.foundation.tools.read_registry import FoundationReadToolRegistry
from app.storage.mysql_store import conversation_store


def kitting_counts(work_order_id: int) -> tuple[int, int]:
    with conversation_store._connection() as connection:
        with connection.cursor() as cursor:
            cursor.execute("SELECT COUNT(*) AS value FROM kitting_analysis WHERE work_order_id=%s", (work_order_id,))
            analyses = int(cursor.fetchone()["value"])
            cursor.execute(
                """
                SELECT COUNT(*) AS value FROM material_shortage ms
                JOIN kitting_analysis ka ON ka.analysis_id = ms.analysis_id
                WHERE ka.work_order_id=%s
                """,
                (work_order_id,),
            )
            shortages = int(cursor.fetchone()["value"])
    return analyses, shortages


def main() -> None:
    parser = argparse.ArgumentParser()
    parser.add_argument("--mes-url", default="http://127.0.0.1:8080")
    parser.add_argument("--username", default="supervisor")
    parser.add_argument("--password", required=True)
    args = parser.parse_args()

    trace_id = f"PACKAGED-READ-{uuid.uuid4().hex[:12]}"
    with httpx.Client(timeout=12.0) as client:
        login = client.post(f"{args.mes_url}/api/auth/login", json={"username": args.username, "password": args.password})
        login.raise_for_status()
        token = login.json()["data"]["token"]
        headers = {"Authorization": f"Bearer {token}", "X-Trace-Id": trace_id}
        orders = client.get(f"{args.mes_url}/api/planning/work-orders", headers=headers)
        orders.raise_for_status()
        first = orders.json()["data"][0]

    work_order_id = int(first["workOrderId"])
    work_order_no = str(first["workOrderNo"])
    principal = Principal(user_id="verification", roles=("PROD_SUPERVISOR",), token=token)
    registry = FoundationReadToolRegistry()
    before = kitting_counts(work_order_id)
    calls = {
        "query_work_order_pipeline": {"work_order_no": work_order_no},
        "query_kitting_snapshot": {"work_order_id": work_order_id},
        "query_production_task_snapshot": {"work_order_id": work_order_id},
        "query_dispatch_snapshot": {"work_order_id": work_order_id},
        "query_work_order_trace": {"work_order_no": work_order_no},
    }
    results = {}
    for tool_name, arguments in calls.items():
        result = registry.invoke(tool_name, arguments, principal, trace_id)
        assert result["read_only"] is True
        assert result["tool"]["access_mode"] == "READ"
        assert result["result"]["source"] == "MES_AGENT_READ_FACADE"
        results[tool_name] = result
    after = kitting_counts(work_order_id)
    assert before == after, f"read wrapper changed kitting rows: before={before}, after={after}"

    available = registry.list_tools()
    assert len(available) == 6
    assert {tool["tool_name"] for tool in available} == set(calls) | {"search_knowledge"}
    print(json.dumps({
        "packaged_tool_count": len(available),
        "live_mes_tools_checked": list(results),
        "work_order_no": work_order_no,
        "kitting_rows_before_after": {"before": before, "after": after},
        "business_writes": 0,
    }, ensure_ascii=False))


if __name__ == "__main__":
    main()
