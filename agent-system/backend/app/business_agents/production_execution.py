"""Production-execution Agent backed by live task and dispatch snapshots."""

from __future__ import annotations

from app.business_agents.base import BaseBusinessAgent, BusinessAgentState, BusinessReadRequest


class ProductionExecutionAgent(BaseBusinessAgent):
    agent_name = "ProductionExecutionAgent"
    business_module = "生产执行"
    description = "读取真实工单、生产任务和派工进度，为后续报工与现场异常处置提供事实。"
    future_write_actions = ("production.feedback.draft", "production.feedback.execute")

    def read_requests(self, state: BusinessAgentState) -> tuple[BusinessReadRequest, ...]:
        work_order_id = self.require_work_order_id(state)
        requests = [
            BusinessReadRequest("production_tasks", "agent_task_snapshot", path_params={"work_order_id": work_order_id}),
            BusinessReadRequest("dispatches", "agent_dispatch_snapshot", path_params={"work_order_id": work_order_id}),
        ]
        if state.work_order_no:
            requests.insert(
                0,
                BusinessReadRequest(
                    "pipeline", "agent_work_order_pipeline", path_params={"work_order_no": self.require_work_order_no(state)}
                ),
            )
        return tuple(requests)
