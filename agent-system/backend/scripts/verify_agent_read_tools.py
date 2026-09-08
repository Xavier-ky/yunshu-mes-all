"""End-to-end verification for the JWT-protected P0 Agent read-tool gateway.

The test deliberately proves that the Agent kitting snapshot does not create
``kitting_analysis`` or ``material_shortage`` records. It removes its own
durable trace record after the check.
"""

from __future__ import annotations

import argparse
import json
import sys
from pathlib import Path

import httpx

ROOT = Path(__file__).resolve().parents[1]
sys.path.insert(0, str(ROOT))

from app.storage.mysql_store import conversation_store  # noqa: E402


TRACE_ID = "P0-VERIFY-AGENT-READ"


def count_for_work_order(work_order_id: int) -> tuple[int, int]:
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


def cleanup() -> None:
    with conversation_store._connection() as connection:
        with connection.cursor() as cursor:
            cursor.execute("DELETE FROM agent_runtime_state WHERE state_type='TRACE' AND state_key=%s", (TRACE_ID,))


def main() -> None:
    parser = argparse.ArgumentParser()
    parser.add_argument("--mes-url", default="http://127.0.0.1:8080")
    parser.add_argument("--agent-url", default="http://127.0.0.1:8090")
    parser.add_argument("--username", default="supervisor")
    parser.add_argument("--password", required=True)
    args = parser.parse_args()

    cleanup()
    try:
        with httpx.Client(timeout=10.0) as client:
            anonymous = client.get(f"{args.agent_url}/api/agent/read-tools")
            assert anonymous.status_code == 401, anonymous.text
            login = client.post(
                f"{args.mes_url}/api/auth/login",
                json={"username": args.username, "password": args.password},
            )
            login.raise_for_status()
            token = login.json()["data"]["token"]
            headers = {"Authorization": f"Bearer {token}", "X-Trace-Id": TRACE_ID}
            orders = client.get(f"{args.mes_url}/api/planning/work-orders", headers=headers)
            orders.raise_for_status()
            first = orders.json()["data"][0]
            work_order_id = int(first["workOrderId"])
            work_order_no = first["workOrderNo"]
            before = count_for_work_order(work_order_id)

            catalog = client.get(f"{args.agent_url}/api/agent/read-tools", headers=headers)
            catalog.raise_for_status()
            calls = {
                "query_work_order_pipeline": {"work_order_no": work_order_no},
                "query_kitting_snapshot": {"work_order_id": work_order_id},
                "query_production_task_snapshot": {"work_order_id": work_order_id},
                "query_dispatch_snapshot": {"work_order_id": work_order_id},
                "query_work_order_trace": {"work_order_no": work_order_no},
            }
            results: dict[str, dict] = {}
            for tool_name, arguments in calls.items():
                response = client.post(
                    f"{args.agent_url}/api/agent/read-tools/{tool_name}",
                    headers=headers,
                    json={"arguments": arguments},
                )
                response.raise_for_status()
                payload = response.json()
                assert payload["source"] == "MES_AGENT_READ_FACADE", payload
                assert payload["read_only"] is True, payload
                results[tool_name] = payload
            after = count_for_work_order(work_order_id)
            assert before == after, f"Read-only kitting changed rows: before={before}, after={after}"
            print(
                json.dumps(
                    {
                        "anonymous": anonymous.status_code,
                        "catalog_tools": len(catalog.json()["tools"]),
                        "work_order_no": work_order_no,
                        "checked_tools": list(results),
                        "kitting_rows_before_after": {"before": before, "after": after},
                    },
                    ensure_ascii=False,
                    indent=2,
                )
            )
    finally:
        cleanup()


if __name__ == "__main__":
    main()
