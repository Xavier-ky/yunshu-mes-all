"""Fact-only post-run evaluation for Agent orchestration.

This deliberately provides structured operational rationale rather than
hidden model chain-of-thought.  It reads only the persisted Agent graph
checkpoint and writes an owner-scoped reflection record to the Agent platform.
"""

from __future__ import annotations

from datetime import datetime
from typing import Any, Optional

from app.storage.agent_platform_store import AgentPlatformStore, AgentPlatformStoreError, agent_platform_store


class AgentReflectionError(RuntimeError):
    """Raised when a graph reflection is unavailable or access is denied."""


class AgentReflectionService:
    reflection_state_type = "AGENT_RUN_REFLECTION"

    def __init__(self, store: Optional[AgentPlatformStore] = None) -> None:
        self._store = store or agent_platform_store

    def reflect_latest_run(self, *, thread_id: str, user_id: int) -> dict[str, Any]:
        """Generate deterministic, owner-scoped evaluation from a graph checkpoint."""
        checkpoint = self._store.load_latest_checkpoint(thread_id)
        if checkpoint is None:
            raise AgentReflectionError("Agent graph checkpoint was not found")
        if int(checkpoint.get("user_id") or 0) != int(user_id):
            raise AgentReflectionError("the requested Agent run does not belong to the current user")
        snapshot = checkpoint.get("state") or {}
        records = list(snapshot.get("node_records") or [])
        failed = [record for record in records if str(record.get("status") or "").lower() in {"failed", "blocked", "cancelled"}]
        completed = [record for record in records if str(record.get("status") or "").lower() in {"completed", "success", "done"}]
        graph_status = str(checkpoint.get("checkpoint_status") or snapshot.get("status") or "UNKNOWN")
        summary = self._summary(graph_status, completed, failed)
        payload = {
            "user_id": int(user_id),
            "thread_id": thread_id,
            "graph_code": checkpoint.get("graph_code"),
            "graph_version": checkpoint.get("graph_version"),
            "checkpoint_token": checkpoint.get("checkpoint_token"),
            "graph_status": graph_status,
            "summary": summary,
            "completed_nodes": [self._node_view(record) for record in completed],
            "attention_nodes": [self._node_view(record) for record in failed],
            "decision_basis": self._decision_basis(snapshot),
            "next_action": self._next_action(graph_status, failed),
            "policy": "fact-only evaluation; no model chain-of-thought; does not change MES data",
            "reflected_at": datetime.utcnow().isoformat() + "Z",
        }
        self._store.save_runtime_state(
            self.reflection_state_type,
            f"thread:{thread_id}",
            payload,
            state_status="COMPLETED" if not failed else "ATTENTION",
        )
        return payload

    def get_reflection(self, *, thread_id: str, user_id: int) -> Optional[dict[str, Any]]:
        payload = self._store.get_runtime_state(self.reflection_state_type, f"thread:{thread_id}")
        if payload is None:
            return None
        if int(payload.get("user_id") or 0) != int(user_id):
            raise AgentReflectionError("the requested reflection does not belong to the current user")
        return payload

    @staticmethod
    def _node_view(record: dict[str, Any]) -> dict[str, Any]:
        return {
            "node_id": record.get("node_id"),
            "agent_name": record.get("agent_name"),
            "stage": record.get("stage"),
            "status": record.get("status"),
            "summary": record.get("summary"),
        }

    @staticmethod
    def _summary(graph_status: str, completed: list[dict[str, Any]], failed: list[dict[str, Any]]) -> str:
        if failed:
            return f"本次图运行完成了 {len(completed)} 个节点，但有 {len(failed)} 个节点需要人工关注。"
        if graph_status.upper() in {"COMPLETED", "SUCCESS"}:
            return f"本次图运行已完成，已记录 {len(completed)} 个可审计节点。"
        return f"本次图运行状态为 {graph_status}，已记录 {len(completed)} 个节点。"

    @staticmethod
    def _decision_basis(snapshot: dict[str, Any]) -> list[str]:
        facts = []
        request = snapshot.get("request") or {}
        if request.get("order_no"):
            facts.append(f"订单: {request['order_no']}")
        if snapshot.get("risk_level"):
            facts.append(f"齐套风险: {snapshot['risk_level']}")
        execution = snapshot.get("execution") or {}
        if execution.get("lifecycle_status"):
            facts.append(f"排产后状态: {execution['lifecycle_status']}")
        issue = snapshot.get("production_issue") or {}
        if issue.get("lifecycle_status"):
            facts.append(f"领料后状态: {issue['lifecycle_status']}")
        return facts[:8]

    @staticmethod
    def _next_action(graph_status: str, failed: list[dict[str, Any]]) -> str:
        if failed:
            return "请根据需要关注节点的真实 MES 业务提示处理后，再重新发起受控流程。"
        if graph_status.upper() in {"COMPLETED", "SUCCESS"}:
            return "可在 MES 页面继续查看工单、任务、派工和领料的真实状态。"
        return "请查看执行轨迹与检查点后决定是否继续或重新发起流程。"


agent_reflection_service = AgentReflectionService()
