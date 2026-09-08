"""Warehouse-logistics Agent backed by real inventory and kitting facts."""

from __future__ import annotations

from app.business_agents.base import BaseBusinessAgent, BusinessAgentState, BusinessReadRequest


class WarehouseLogisticsAgent(BaseBusinessAgent):
    agent_name = "WarehouseLogisticsAgent"
    business_module = "仓储物流"
    description = "读取真实库存批次；带工单时读取实际齐套快照，不自行推算或锁定库存。"
    future_write_actions = ("warehouse.receipt.draft", "warehouse.return.draft", "warehouse.finished_receipt.execute")

    def read_requests(self, state: BusinessAgentState) -> tuple[BusinessReadRequest, ...]:
        requests = [BusinessReadRequest("inventory_batches", "inventory", params=state.params or None)]
        if state.work_order_id:
            requests.append(
                BusinessReadRequest(
                    "kitting", "agent_kitting_snapshot", path_params={"work_order_id": self.require_work_order_id(state)}
                )
            )
        return tuple(requests)
