"""Read-only verification for standalone business Agent packages.

It logs in through the MES API, loads one real work order, then calls every
new package directly.  The script creates no business or conversation data.
"""

from __future__ import annotations

import argparse
import json
import sys
from pathlib import Path

import httpx

ROOT = Path(__file__).resolve().parents[1]
sys.path.insert(0, str(ROOT))

from app.business_agents import (  # noqa: E402
    AndonResponseAgent,
    EquipmentMaintenanceAgent,
    OperationsInsightAgent,
    ProductionExecutionAgent,
    QualityManagementAgent,
    TraceabilityAgent,
    WarehouseLogisticsAgent,
)
from app.business_agents.base import BusinessAgentState  # noqa: E402
from app.core.security import Principal  # noqa: E402


def main() -> None:
    parser = argparse.ArgumentParser()
    parser.add_argument("--mes-url", default="http://127.0.0.1:8080")
    parser.add_argument("--username", default="supervisor")
    parser.add_argument("--password", required=True)
    args = parser.parse_args()

    with httpx.Client(timeout=10.0) as client:
        login = client.post(
            f"{args.mes_url}/api/auth/login",
            json={"username": args.username, "password": args.password},
        )
        login.raise_for_status()
        token = login.json()["data"]["token"]
        headers = {"Authorization": f"Bearer {token}", "X-Trace-Id": "VERIFY-BUSINESS-AGENTS"}
        work_orders = client.get(f"{args.mes_url}/api/planning/work-orders", headers=headers)
        work_orders.raise_for_status()
        first = work_orders.json()["data"][0]

    principal = Principal(user_id=args.username, roles=("PROD_SUPERVISOR",), token=token)
    state = BusinessAgentState(
        principal=principal,
        trace_id="VERIFY-BUSINESS-AGENTS",
        work_order_id=int(first["workOrderId"]),
        work_order_no=str(first["workOrderNo"]),
    )
    agents = (
        ProductionExecutionAgent(),
        QualityManagementAgent(),
        WarehouseLogisticsAgent(),
        TraceabilityAgent(),
        EquipmentMaintenanceAgent(),
        AndonResponseAgent(),
        OperationsInsightAgent(),
    )
    results = []
    for agent in agents:
        result = agent.run(state)
        assert result["source"] == "MES_DATABASE_VIA_SPRING_BOOT", result
        assert result["read_only"] is True, result
        assert result["workflow_connected"] is False, result
        assert result["facts"], result
        results.append({"agent": agent.agent_name, "fact_sets": list(result["facts"])})

    print(
        json.dumps(
            {
                "work_order_no": state.work_order_no,
                "verified_agents": results,
                "writes_created": 0,
            },
            ensure_ascii=False,
            indent=2,
        )
    )


if __name__ == "__main__":
    main()
