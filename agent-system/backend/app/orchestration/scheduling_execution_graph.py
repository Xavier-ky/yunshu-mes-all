"""LangGraph-owned execution graph for the existing intelligent scheduling flow.

This graph owns *orchestration only*.  Every business action is delegated to
the pre-existing scheduling Agent wrappers and their Spring Boot gateways.
Therefore order confirmation, atomic scheduling, dispatch synchronization and
material issue retain their current transaction, permission and data semantics.
"""

from __future__ import annotations

import copy
from typing import Any, Callable, Optional, TypedDict

from langgraph.graph import END, START, StateGraph

from app.core.security import Principal
from app.orchestration.contracts import AgentNodeRecord
from app.scheduling.agents.base import SchedulingAgentState
from app.scheduling.agents.bom_route import BomRouteAgent
from app.scheduling.agents.capacity_scheduling import CapacitySchedulingAgent
from app.scheduling.agents.dispatch_execution import DispatchExecutionAgent
from app.scheduling.agents.events import build_scheduling_workflow_events
from app.scheduling.agents.kitting_execution import KittingExecutionAgent
from app.scheduling.agents.kitting_risk import KittingRiskAgent
from app.scheduling.agents.order_intake import OrderIntakeAgent
from app.scheduling.agents.production_issue import ProductionIssueAgent
from app.scheduling.agents.scheduling_validation import SchedulingValidationAgent
from app.scheduling.order_intake import OrderIntakeService
from app.schemas import SchedulingOrderIntakeRequest
from app.storage.agent_platform_store import AgentPlatformStore, AgentPlatformStoreError, agent_platform_store


class SchedulingExecutionGraphState(TypedDict, total=False):
    """In-process LangGraph state; only its safe projection is persisted."""

    runtime: SchedulingAgentState
    gateway: Any
    thread_id: str
    trace_id: str
    user_id: str
    status: str
    current_node: str
    node_index: int
    checkpoint_token: str
    node_records: list[dict[str, Any]]
    persistence_status: str
    result: dict[str, Any]


class SchedulingExecutionGraph:
    """The real graph implementation behind a confirmed scheduling request.

    The graph deliberately starts *after* the existing user confirmation form.
    That preserves the current UI/API contract while making every downstream
    scheduling responsibility an explicit LangGraph node and the kitting gate
    an explicit conditional edge.
    """

    graph_code = "MES_SCHEDULING_EXECUTION"
    graph_version = "1.0.0"
    runtime_state_type = "SCHEDULING_GRAPH_RUN"

    def __init__(self, store: Optional[AgentPlatformStore] = None) -> None:
        self._store = store or agent_platform_store
        self.order_intake_agent = OrderIntakeAgent()
        self.bom_route_agent = BomRouteAgent()
        self.kitting_risk_agent = KittingRiskAgent()
        self.kitting_execution_agent = KittingExecutionAgent()
        self.capacity_scheduling_agent = CapacitySchedulingAgent()
        self.dispatch_execution_agent = DispatchExecutionAgent()
        self.scheduling_validation_agent = SchedulingValidationAgent()
        self.production_issue_agent = ProductionIssueAgent()

        builder = StateGraph(SchedulingExecutionGraphState)
        builder.add_node("order_intake", self._order_intake)
        builder.add_node("bom_route", self._bom_route)
        builder.add_node("kitting_risk", self._kitting_risk)
        builder.add_node("kitting_execution", self._kitting_execution)
        builder.add_node("capacity_scheduling", self._capacity_scheduling)
        builder.add_node("dispatch_execution", self._dispatch_execution)
        builder.add_node("scheduling_validation", self._scheduling_validation)
        builder.add_node("production_issue", self._production_issue)
        builder.add_node("finalize", self._finalize)
        builder.add_edge(START, "order_intake")
        builder.add_edge("order_intake", "bom_route")
        builder.add_edge("bom_route", "kitting_risk")
        builder.add_conditional_edges(
            "kitting_risk",
            self._after_kitting_risk,
            {"execute": "kitting_execution", "blocked": "finalize"},
        )
        builder.add_edge("kitting_execution", "capacity_scheduling")
        builder.add_edge("capacity_scheduling", "dispatch_execution")
        builder.add_edge("dispatch_execution", "scheduling_validation")
        builder.add_edge("scheduling_validation", "production_issue")
        builder.add_edge("production_issue", "finalize")
        builder.add_edge("finalize", END)
        self._graph = builder.compile()

    def run(
        self,
        request: SchedulingOrderIntakeRequest,
        principal: Principal,
        trace_id: str,
        *,
        order_intake_gateway: OrderIntakeService,
    ) -> dict[str, Any]:
        """Execute the existing scheduling chain through explicit graph nodes."""

        runtime = SchedulingAgentState(request=request, principal=principal, trace_id=trace_id)
        initial: SchedulingExecutionGraphState = {
            "runtime": runtime,
            "gateway": order_intake_gateway,
            "thread_id": f"scheduling-execution:{trace_id}",
            "trace_id": trace_id,
            "user_id": str(principal.user_id),
            "status": "RUNNING",
            "current_node": "start",
            "node_index": 0,
            "checkpoint_token": "",
            "node_records": [],
            "persistence_status": "ACTIVE",
        }
        final_state = dict(self._graph.invoke(initial))
        result = final_state.get("result")
        if not isinstance(result, dict):
            raise RuntimeError("LangGraph scheduling execution did not return a result")
        return result

    def _order_intake(self, state: SchedulingExecutionGraphState) -> dict[str, Any]:
        return self._delegate(
            state, "order-intake", "OrderIntakeAgent", "订单确认与工单生成",
            lambda runtime: self.order_intake_agent.run(runtime, state["gateway"]),
        )

    def _bom_route(self, state: SchedulingExecutionGraphState) -> dict[str, Any]:
        return self._delegate(
            state, "bom-route", "BomRouteAgent", "BOM 与工艺路线实时读取",
            self.bom_route_agent.run,
        )

    def _kitting_risk(self, state: SchedulingExecutionGraphState) -> dict[str, Any]:
        return self._delegate(
            state, "kitting-risk", "KittingRiskAgent", "齐套与物料风险判断",
            self.kitting_risk_agent.run,
        )

    def _kitting_execution(self, state: SchedulingExecutionGraphState) -> dict[str, Any]:
        return self._delegate(
            state, "kitting-execution", "KittingExecutionAgent", "原子锁料、任务生成、排产与派工同步",
            self.kitting_execution_agent.run,
        )

    def _capacity_scheduling(self, state: SchedulingExecutionGraphState) -> dict[str, Any]:
        return self._delegate(
            state, "capacity-scheduling", "CapacitySchedulingAgent", "排产辅助建议读取",
            self.capacity_scheduling_agent.run,
        )

    def _dispatch_execution(self, state: SchedulingExecutionGraphState) -> dict[str, Any]:
        return self._delegate(
            state, "dispatch-execution", "DispatchExecutionAgent", "派工同步结果投影",
            self.dispatch_execution_agent.run,
        )

    def _scheduling_validation(self, state: SchedulingExecutionGraphState) -> dict[str, Any]:
        return self._delegate(
            state, "scheduling-validation", "SchedulingValidationAgent", "工单、任务与派工写回校验",
            self.scheduling_validation_agent.run,
        )

    def _production_issue(self, state: SchedulingExecutionGraphState) -> dict[str, Any]:
        return self._delegate(
            state, "production-issue", "ProductionIssueAgent", "生产领料、出库与消耗追溯",
            self.production_issue_agent.run,
        )

    @staticmethod
    def _after_kitting_risk(state: SchedulingExecutionGraphState) -> str:
        analysis = state["runtime"].analysis or {}
        risk = str((analysis.get("summary") or {}).get("risk_level") or "HIGH").upper()
        return "execute" if risk == "LOW" else "blocked"

    def _finalize(self, state: SchedulingExecutionGraphState) -> dict[str, Any]:
        runtime = state["runtime"]
        analysis = runtime.analysis or {}
        workflow_events = build_scheduling_workflow_events(
            analysis,
            runtime.execution,
            runtime.production_issue,
            order_no=str((runtime.order or {}).get("order_no") or runtime.request.order_no),
            work_order_no=str((runtime.work_order or {}).get("work_order_no") or runtime.request.order_no),
        )
        blocked = runtime.execution is None
        updates = self._transition(
            state,
            node_id="finalize",
            agent_name="MainAgent",
            stage="现有工作流事件投影",
            status="blocked" if blocked else "completed",
            summary="齐套风险阻断，未调用原子排产或领料。" if blocked else "LangGraph 已完成对既有排产链的节点编排。",
            graph_status="BLOCKED" if blocked else "COMPLETED",
            checkpoint_status="COMPLETED",
        )
        projected_state = dict(state)
        projected_state.update(updates)
        result = {
            "order": runtime.order,
            "work_order": runtime.work_order,
            "analysis": runtime.analysis,
            "scheduling_execution": runtime.execution,
            "scheduling_advisory": runtime.advisory,
            "production_issue_execution": runtime.production_issue,
            "workflow_events": workflow_events,
            "graph_audit": self._audit_view(projected_state),
        }
        updates["result"] = result
        return updates

    def _delegate(
        self,
        state: SchedulingExecutionGraphState,
        node_id: str,
        agent_name: str,
        stage: str,
        action: Callable[[SchedulingAgentState], Any],
    ) -> dict[str, Any]:
        try:
            action(state["runtime"])
        except Exception as exc:
            self._transition(
                state,
                node_id=node_id,
                agent_name=agent_name,
                stage=stage,
                status="failed",
                summary=f"既有 {agent_name} 返回失败：{exc}",
                graph_status="FAILED",
                checkpoint_status="FAILED",
            )
            raise
        return self._transition(
            state,
            node_id=node_id,
            agent_name=agent_name,
            stage=stage,
            status="completed",
            summary=f"已委托既有 {agent_name} 完成 {stage}。",
            graph_status="RUNNING",
            checkpoint_status="RUNNING",
        )

    def _transition(
        self,
        state: SchedulingExecutionGraphState,
        *,
        node_id: str,
        agent_name: str,
        stage: str,
        status: str,
        summary: str,
        graph_status: str,
        checkpoint_status: str,
    ) -> dict[str, Any]:
        records = list(state.get("node_records") or [])
        records.append(AgentNodeRecord(node_id, agent_name, stage, status, summary).to_dict())
        node_index = int(state.get("node_index") or 0) + 1
        checkpoint_token = f"lgs-{state['trace_id']}-{node_index}"
        updates: dict[str, Any] = {
            "current_node": node_id,
            "node_records": records,
            "node_index": node_index,
            "checkpoint_token": checkpoint_token,
            "status": graph_status,
            "persistence_status": state.get("persistence_status") or "ACTIVE",
        }
        projected = dict(state)
        projected.update(updates)
        try:
            self._store.save_checkpoint(
                thread_id=str(projected["thread_id"]),
                checkpoint_token=checkpoint_token,
                parent_checkpoint_token=str(state.get("checkpoint_token") or "") or None,
                graph_code=self.graph_code,
                graph_version=self.graph_version,
                checkpoint_status=checkpoint_status,
                state=self._checkpoint_snapshot(projected),
                user_id=self._owner_id(projected.get("user_id")),
                biz_object_type="SCHEDULING_EXECUTION",
                biz_object_id=self._work_order_id(projected["runtime"]),
            )
            self._store.save_runtime_state(
                self.runtime_state_type,
                str(projected["thread_id"]),
                self._checkpoint_snapshot(projected),
                state_status=graph_status,
            )
        except AgentPlatformStoreError:
            # A business node may already have committed through the existing
            # MES service. Preserve its historical error semantics rather than
            # reporting a false business rollback because audit persistence is
            # unavailable. The result carries this degradation explicitly.
            updates["persistence_status"] = "DEGRADED"
        return updates

    def _checkpoint_snapshot(self, state: SchedulingExecutionGraphState) -> dict[str, Any]:
        runtime = state["runtime"]
        analysis = runtime.analysis or {}
        execution = runtime.execution or {}
        issue = runtime.production_issue or {}
        return {
            "thread_id": state.get("thread_id"),
            "trace_id": state.get("trace_id"),
            "user_id": state.get("user_id"),
            "current_node": state.get("current_node"),
            "status": state.get("status"),
            "node_index": state.get("node_index"),
            "checkpoint_token": state.get("checkpoint_token"),
            "request": {
                "order_no": runtime.request.order_no,
                "source_order_id": runtime.request.source_order_id,
                "confirmed": runtime.request.confirmed,
            },
            "order": self._public_order(runtime.order),
            "work_order": self._public_work_order(runtime.work_order),
            "risk_level": (analysis.get("summary") or {}).get("risk_level"),
            "execution": {
                "lifecycle_status": execution.get("lifecycleStatus"),
                "production_task_count": execution.get("productionTaskCount"),
                "dispatch_task_count": execution.get("dispatchTaskCount"),
            },
            "production_issue": {
                "issue_id": issue.get("issueId"),
                "issue_status": issue.get("issueStatus"),
                "lifecycle_status": issue.get("lifecycleStatus"),
                "ready_for_shop_floor": issue.get("readyForShopFloor"),
            },
            "node_records": copy.deepcopy(state.get("node_records") or []),
        }

    @staticmethod
    def _public_order(order: Optional[dict[str, Any]]) -> dict[str, Any]:
        if not order:
            return {}
        return {key: order.get(key) for key in ("order_id", "order_no", "status")}

    @staticmethod
    def _public_work_order(work_order: Optional[dict[str, Any]]) -> dict[str, Any]:
        if not work_order:
            return {}
        return {key: work_order.get(key) for key in ("work_order_id", "work_order_no", "status")}

    @staticmethod
    def _owner_id(value: Any) -> Optional[int]:
        try:
            return int(str(value))
        except (TypeError, ValueError):
            return None

    @staticmethod
    def _work_order_id(runtime: SchedulingAgentState) -> Optional[int]:
        if not runtime.work_order:
            return None
        try:
            return int(runtime.work_order.get("work_order_id") or 0) or None
        except (TypeError, ValueError):
            return None

    @staticmethod
    def _audit_view(state: SchedulingExecutionGraphState) -> dict[str, Any]:
        return {
            "graph_code": SchedulingExecutionGraph.graph_code,
            "graph_version": SchedulingExecutionGraph.graph_version,
            "thread_id": state.get("thread_id"),
            "trace_id": state.get("trace_id"),
            "status": state.get("status"),
            "current_node": state.get("current_node"),
            "checkpoint_token": state.get("checkpoint_token"),
            "persistence_status": state.get("persistence_status"),
            "node_records": copy.deepcopy(state.get("node_records") or []),
        }


scheduling_execution_graph = SchedulingExecutionGraph()
