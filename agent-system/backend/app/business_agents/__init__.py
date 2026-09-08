"""Standalone, real-MES-data business Agent packages.

They are intentionally not imported by the default main-chat or scheduling
workflow.  A future orchestrator can opt into one after its read scope and
approval-gated write facade are enabled.
"""

from app.business_agents.andon_response import AndonResponseAgent
from app.business_agents.equipment_maintenance import EquipmentMaintenanceAgent
from app.business_agents.operations_insight import OperationsInsightAgent
from app.business_agents.production_execution import ProductionExecutionAgent
from app.business_agents.quality_management import QualityManagementAgent
from app.business_agents.traceability import TraceabilityAgent
from app.business_agents.warehouse_logistics import WarehouseLogisticsAgent

BUSINESS_AGENT_TYPES = (
    ProductionExecutionAgent,
    QualityManagementAgent,
    WarehouseLogisticsAgent,
    TraceabilityAgent,
    EquipmentMaintenanceAgent,
    AndonResponseAgent,
    OperationsInsightAgent,
)

__all__ = [
    "AndonResponseAgent",
    "EquipmentMaintenanceAgent",
    "OperationsInsightAgent",
    "ProductionExecutionAgent",
    "QualityManagementAgent",
    "TraceabilityAgent",
    "WarehouseLogisticsAgent",
    "BUSINESS_AGENT_TYPES",
]
