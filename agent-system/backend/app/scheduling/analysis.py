"""Real-data pre-scheduling analysis sub-agents.

These services are deterministic orchestration around the MES read facade.
They do not ask an LLM to invent BOM, routing, inventory or shortage facts;
an LLM can later turn this result into an explanation or plan.
"""

from __future__ import annotations

from typing import Any

from app.core.security import Principal
from app.integrations.spring_boot_client import SpringBootClient


class SchedulingAnalysisError(RuntimeError):
    pass


class SchedulingAnalysisService:
    """Coordinates the BOM/Route and material-risk agents for one work order."""

    def __init__(self, client: SpringBootClient | None = None) -> None:
        self.client = client or SpringBootClient()

    def analyze_work_order(
        self,
        work_order_id: int,
        principal: Principal,
        trace_id: str,
    ) -> dict[str, Any]:
        try:
            bom_route = self.client.read(
                "agent_bom_route_snapshot",
                principal,
                trace_id,
                path_params={"work_order_id": work_order_id},
            )
            kitting = self.client.read(
                "agent_kitting_snapshot",
                principal,
                trace_id,
                path_params={"work_order_id": work_order_id},
            )
        except Exception as exc:  # normalized at the API boundary
            raise SchedulingAnalysisError("Unable to retrieve the MES scheduling facts") from exc

        if not isinstance(bom_route, dict) or not bom_route:
            raise SchedulingAnalysisError("The work order was not found in MES")
        if not isinstance(kitting, dict) or not kitting:
            raise SchedulingAnalysisError("The material snapshot was not returned by MES")

        materials = list(bom_route.get("materials") or [])
        steps = list(bom_route.get("steps") or [])
        shortages = list(kitting.get("shortages") or [])
        details = list(kitting.get("details") or [])
        key_shortages = [item for item in shortages if bool(item.get("isKeyMaterial"))]

        bom_ready = bool(bom_route.get("bomReady"))
        route_ready = bool(bom_route.get("routeReady"))
        all_sufficient = bool(kitting.get("allSufficient"))
        if not bom_ready or not route_ready:
            risk_level = "BLOCKED"
            recommendation = "缺少已发布 BOM 或可用工艺路线，不能进入自动排产。"
        elif not all_sufficient:
            risk_level = "HIGH" if key_shortages else "MEDIUM"
            recommendation = "先补齐短缺物料或调整交付承诺，再进入产能排程。"
        else:
            risk_level = "LOW"
            recommendation = "BOM、工艺路线和当前合格库存满足计划量，可进入下一步产能排程。"

        return {
            "work_order": {
                "work_order_id": int(bom_route.get("workOrderId") or work_order_id),
                "work_order_no": bom_route.get("workOrderNo"),
                "product_id": bom_route.get("productId"),
                "product_code": bom_route.get("productCode"),
                "product_name": bom_route.get("productName"),
                "plan_qty": bom_route.get("planQty"),
            },
            "bom_route_agent": {
                "status": "READY" if bom_ready and route_ready else "BLOCKED",
                "bom_ready": bom_ready,
                "route_ready": route_ready,
                "bom": bom_route.get("bom") or {},
                "materials": materials,
                "route": bom_route.get("route") or {},
                "steps": steps,
                "material_count": len(materials),
                "process_step_count": len(steps),
                "source": bom_route.get("source"),
                "queried_at": bom_route.get("queriedAt"),
            },
            "kitting_risk_agent": {
                "status": "SUFFICIENT" if all_sufficient else "SHORTAGE",
                "all_sufficient": all_sufficient,
                "material_count": len(details),
                "sufficient_material_count": len(details) - len(shortages),
                "shortage_material_count": len(shortages),
                "key_shortage_count": len(key_shortages),
                "shortages": shortages,
                "source": kitting.get("source"),
                "queried_at": kitting.get("queriedAt"),
            },
            "summary": {
                "risk_level": risk_level,
                "recommendation": recommendation,
                "next_agent": "CapacitySchedulingAgent" if risk_level == "LOW" else "MaterialResolutionAgent",
            },
        }


scheduling_analysis_service = SchedulingAnalysisService()
