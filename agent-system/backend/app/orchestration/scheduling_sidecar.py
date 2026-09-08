"""A LangGraph sidecar for professional scheduling orchestration.

The sidecar is intentionally *not* wired into existing HTTP endpoints.  It
does not call MES APIs or domain services, so it cannot change an order,
work-order, inventory, task, dispatch, issue document, or quality record.

Its role is to make the future cutover safe: plan the known scheduling chain,
enforce an approval pause, persist user-owned checkpoints, and normalize
already-emitted workflow events for audit.  The existing sequential workflow
remains the sole production executor until an explicit integration release.
"""

from __future__ import annotations

import copy
import uuid
from collections.abc import Iterable
from typing import Any, Literal, Optional, TypedDict

from langgraph.graph import END, START, StateGraph

from app.orchestration.contracts import AgentNodeRecord, ApprovalDecision, DEFAULT_SCHEDULING_AGENT_ORDER
from app.storage.agent_platform_store import AgentPlatformStore, agent_platform_store


class OrchestrationError(RuntimeError):
    """Raised for invalid sidecar transitions, never for MES business errors."""


class SchedulingSidecarState(TypedDict, total=False):
    thread_id: str
    trace_id: str
    user_id: int
    session_id: Optional[int]
    mode: Literal["preview", "observe"]
    request_summary: str
    agent_order: list[str]
    observed_events: list[dict[str, Any]]
    approval_required: bool
    approved: Optional[bool]
    approval_reason: str
    checkpoint_token: str
    checkpoint_status: str
    status: str
    current_node: str
    plan: list[str]
    node_records: list[dict[str, Any]]


class SchedulingOrchestrationSidecar:
    """Durable planning/approval graph that delegates no business execution.

    It is intentionally small and deterministic.  This makes it suitable for
    shadow verification against the current sequential scheduling workflow.
    """

    graph_code = "MES_SCHEDULING_SIDECAR"
    graph_version = "1.0.0"
    runtime_state_type = "ORCHESTRATION_SIDECAR"

    def __init__(self, store: AgentPlatformStore | None = None) -> None:
        self._store = store or agent_platform_store
        builder = StateGraph(SchedulingSidecarState)
        builder.add_node("plan_known_chain", self._plan_known_chain)
        builder.add_node("approval_gate", self._approval_gate)
        builder.add_node("finalize_sidecar", self._finalize_sidecar)
        builder.add_edge(START, "plan_known_chain")
        builder.add_edge("plan_known_chain", "approval_gate")
        builder.add_conditional_edges(
            "approval_gate",
            self._approval_route,
            {"pause": END, "finalize": "finalize_sidecar"},
        )
        builder.add_edge("finalize_sidecar", END)
        self._graph = builder.compile()

    def preview(
        self,
        *,
        user_id: int,
        request_summary: str,
        session_id: int | None = None,
        trace_id: str | None = None,
        approval_required: bool = True,
        approval_reason: str = "未来委托现有排产写入前需要用户确认。",
        agent_order: Iterable[str] | None = None,
    ) -> dict[str, Any]:
        """Plan a scheduling delegation without executing it."""

        if user_id <= 0:
            raise OrchestrationError("sidecar requires a positive owner user_id")
        if not request_summary.strip():
            raise OrchestrationError("sidecar requires a request summary")
        state: SchedulingSidecarState = {
            "thread_id": self._thread_id(trace_id),
            "trace_id": trace_id or str(uuid.uuid4()),
            "user_id": int(user_id),
            "session_id": session_id,
            "mode": "preview",
            "request_summary": request_summary.strip(),
            "agent_order": list(agent_order or DEFAULT_SCHEDULING_AGENT_ORDER),
            "approval_required": bool(approval_required),
            "approved": None,
            "approval_reason": approval_reason,
            "checkpoint_token": self._checkpoint_token(),
            "checkpoint_status": "RUNNING",
            "status": "PLANNING",
            "current_node": "start",
            "plan": [],
            "node_records": [],
        }
        final_state = self._invoke(state)
        self._persist(final_state)
        return self._public(final_state)

    def resume(self, *, thread_id: str, user_id: int, approved: bool) -> dict[str, Any]:
        """Resume only an owner-scoped, interrupted sidecar checkpoint."""

        checkpoint = self._store.load_latest_checkpoint(thread_id)
        if checkpoint is None:
            raise OrchestrationError("orchestration checkpoint was not found")
        state = copy.deepcopy(checkpoint.get("state") or {})
        if int(state.get("user_id") or 0) != int(user_id):
            raise OrchestrationError("current user does not own this orchestration checkpoint")
        if checkpoint.get("checkpoint_status") != "INTERRUPTED":
            raise OrchestrationError("only interrupted sidecar checkpoints can be resumed")

        state["approved"] = bool(approved)
        state["checkpoint_token"] = self._checkpoint_token()
        state["checkpoint_status"] = "RUNNING"
        state["current_node"] = "approval_gate"
        if not approved:
            state["status"] = "CANCELLED"
            records = list(state.get("node_records") or [])
            records.append(
                self._record(
                    "approval-denied", "PolicyGuard", "approval", "cancelled",
                    "用户拒绝委托，未调用任何现有 MES 写入流程。",
                )
            )
            state["node_records"] = records
            state["checkpoint_status"] = "CANCELLED"
            self._persist(state)
            return self._public(state)

        final_state = self._invoke(state)
        self._persist(final_state)
        return self._public(final_state)

    def observe_existing_result(
        self,
        *,
        user_id: int,
        trace_id: str,
        workflow_events: Iterable[dict[str, Any]],
        session_id: int | None = None,
    ) -> dict[str, Any]:
        """Normalize an existing result for audit without replaying it.

        Future integration may call this after the current scheduling route has
        completed.  It deliberately accepts the result as evidence and never
        invokes the underlying workflow again.
        """

        events = [dict(event) for event in workflow_events]
        if not events:
            raise OrchestrationError("existing workflow observation requires events")
        state: SchedulingSidecarState = {
            "thread_id": self._thread_id(trace_id),
            "trace_id": trace_id,
            "user_id": int(user_id),
            "session_id": session_id,
            "mode": "observe",
            "request_summary": "已完成的现有智能排产工作流审计观察。",
            "agent_order": [str(event.get("agent") or "UnknownAgent") for event in events],
            "observed_events": events,
            "approval_required": False,
            "approved": True,
            "approval_reason": "观察模式不执行业务写入。",
            "checkpoint_token": self._checkpoint_token(),
            "checkpoint_status": "RUNNING",
            "status": "PLANNING",
            "current_node": "start",
            "plan": [],
            "node_records": [],
        }
        final_state = self._invoke(state)
        self._persist(final_state)
        return self._public(final_state)

    def _invoke(self, state: SchedulingSidecarState) -> SchedulingSidecarState:
        return dict(self._graph.invoke(state))

    def _plan_known_chain(self, state: SchedulingSidecarState) -> dict[str, Any]:
        if state.get("plan"):
            return {"current_node": "approval_gate"}
        agent_order = list(state.get("agent_order") or DEFAULT_SCHEDULING_AGENT_ORDER)
        records = list(state.get("node_records") or [])
        if state.get("mode") == "observe":
            for index, event in enumerate(state.get("observed_events") or [], start=1):
                records.append(
                    self._record(
                        str(event.get("event_id") or f"observed-{index}"),
                        str(event.get("agent") or "UnknownAgent"),
                        "observed_existing_workflow",
                        str(event.get("status") or "completed"),
                        str(event.get("summary") or event.get("title") or "已接收现有工作流事件。"),
                        [str(source) for source in event.get("sources") or []],
                    )
                )
            plan = ["采集并规范化现有工作流事件", "保存用户隔离的运行审计"]
        else:
            plan = [
                "识别新订单排产请求，并读取订单中心最新订单供用户确认",
                "按既有顺序执行 BOM/工艺、齐套风险、原子排产、派工核验与生产领料",
                "由既有 MES 领域服务执行所有业务写入；Sidecar 只保存计划和审批审计",
            ]
            for index, agent in enumerate(agent_order, start=1):
                records.append(
                    self._record(
                        f"planned-{index}", agent, "planned_existing_workflow", "planned",
                        f"将按既有工作流顺序委托 {agent}；本 Sidecar 不直接执行该业务节点。",
                    )
                )
        return {
            "current_node": "approval_gate",
            "status": "PLANNED",
            "plan": plan,
            "node_records": records,
        }

    def _approval_gate(self, state: SchedulingSidecarState) -> dict[str, Any]:
        if not state.get("approval_required"):
            return {"current_node": "finalize_sidecar", "status": "APPROVED"}
        if state.get("approved") is True:
            records = list(state.get("node_records") or [])
            records.append(
                self._record(
                    "approval-granted", "PolicyGuard", "approval", "approved",
                    "用户已批准未来委托；本次 Sidecar 仍未执行任何 MES 业务写入。",
                )
            )
            return {"current_node": "finalize_sidecar", "status": "APPROVED", "node_records": records}
        records = list(state.get("node_records") or [])
        if not any(item.get("node_id") == "approval-required" for item in records):
            records.append(
                self._record(
                    "approval-required", "PolicyGuard", "approval", "waiting_approval",
                    str(state.get("approval_reason") or "需要用户确认。"),
                )
            )
        return {
            "current_node": "approval_gate",
            "status": "WAITING_APPROVAL",
            "checkpoint_status": "INTERRUPTED",
            "node_records": records,
        }

    @staticmethod
    def _approval_route(state: SchedulingSidecarState) -> str:
        return "pause" if state.get("status") == "WAITING_APPROVAL" else "finalize"

    def _finalize_sidecar(self, state: SchedulingSidecarState) -> dict[str, Any]:
        records = list(state.get("node_records") or [])
        observed = state.get("mode") == "observe"
        records.append(
            self._record(
                "sidecar-finalized", "MainAgent", "orchestration", "completed",
                "已完成现有流程的审计观察。" if observed else "编排计划已就绪；现有工作流仍是唯一业务执行者。",
            )
        )
        return {
            "current_node": "done",
            "status": "OBSERVED" if observed else "READY_FOR_EXISTING_WORKFLOW",
            "checkpoint_status": "COMPLETED",
            "node_records": records,
        }

    def _persist(self, state: SchedulingSidecarState) -> None:
        checkpoint_status = str(state.get("checkpoint_status") or "COMPLETED")
        self._store.save_checkpoint(
            thread_id=str(state["thread_id"]),
            checkpoint_token=str(state["checkpoint_token"]),
            graph_code=self.graph_code,
            graph_version=self.graph_version,
            state=self._serializable_state(state),
            checkpoint_status=checkpoint_status,
            session_id=state.get("session_id"),
            user_id=int(state["user_id"]),
            biz_object_type="AGENT_ORCHESTRATION",
            biz_object_id=None,
        )
        self._store.save_runtime_state(
            self.runtime_state_type,
            str(state["thread_id"]),
            self._serializable_state(state),
            state_status=str(state.get("status") or "UNKNOWN"),
        )

    @staticmethod
    def _serializable_state(state: SchedulingSidecarState | dict[str, Any]) -> dict[str, Any]:
        return {str(key): value for key, value in dict(state).items()}

    @staticmethod
    def _record(
        node_id: str,
        agent_name: str,
        stage: str,
        status: str,
        summary: str,
        evidence: list[str] | None = None,
    ) -> dict[str, Any]:
        return AgentNodeRecord(node_id, agent_name, stage, status, summary, evidence or []).to_dict()

    @staticmethod
    def _thread_id(trace_id: str | None) -> str:
        return f"sidecar-scheduling:{trace_id or uuid.uuid4()}"

    @staticmethod
    def _checkpoint_token() -> str:
        return f"scp-{uuid.uuid4().hex}"

    @staticmethod
    def _public(state: SchedulingSidecarState | dict[str, Any]) -> dict[str, Any]:
        data = dict(state)
        decision = ApprovalDecision(
            approval_required=bool(data.get("approval_required")),
            approved=data.get("approved"),
            reason=str(data.get("approval_reason") or ""),
            requested_by=int(data.get("user_id") or 0),
        )
        return {
            "thread_id": data.get("thread_id"),
            "trace_id": data.get("trace_id"),
            "status": data.get("status"),
            "checkpoint_status": data.get("checkpoint_status"),
            "mode": data.get("mode"),
            "plan": data.get("plan") or [],
            "approval": decision.to_dict(),
            "node_records": data.get("node_records") or [],
            "delegates_to_existing_workflow": data.get("mode") != "observe",
        }
