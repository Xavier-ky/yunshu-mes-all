"""Equipment-maintenance Agent backed by the real equipment module."""

from __future__ import annotations

from app.business_agents.base import BaseBusinessAgent, BusinessAgentState, BusinessReadRequest


class EquipmentMaintenanceAgent(BaseBusinessAgent):
    agent_name = "EquipmentMaintenanceAgent"
    business_module = "设备管理"
    description = "读取真实设备工作台摘要与待办，为点检、保养、维修及停机影响分析提供事实。"
    future_write_actions = ("equipment.maintenance.draft", "equipment.repair.draft", "equipment.repair.execute")

    def read_requests(self, state: BusinessAgentState) -> tuple[BusinessReadRequest, ...]:
        return (
            BusinessReadRequest("equipment_summary", "equipment_workbench_summary"),
            BusinessReadRequest("equipment_pending", "equipment_workbench_pending", params=state.params or None),
        )
