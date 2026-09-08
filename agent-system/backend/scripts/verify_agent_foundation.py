"""Cleanup-safe verification for the non-invasive Agent foundation layer.

It writes only unique records to the existing Agent-platform tables and
removes them in ``finally``.  No MES transactional table or business endpoint
is invoked.
"""

from __future__ import annotations

import json
import sys
import uuid
from pathlib import Path

ROOT = Path(__file__).resolve().parents[1]
if str(ROOT) not in sys.path:
    sys.path.insert(0, str(ROOT))

from app.foundation.reflection import AgentReflectionService
from app.foundation.runtime_memory import AgentMemoryService
from app.foundation.tool_catalog import AgentToolCatalog
from app.storage.agent_platform_store import AgentPlatformStore


def main() -> None:
    store = AgentPlatformStore()
    memory = AgentMemoryService(store)
    reflection = AgentReflectionService(store)
    user_id = 1
    session_id = 990001
    token = uuid.uuid4().hex[:12]
    thread_id = f"foundation-verify:{token}"
    short_key = memory._short_key(user_id, session_id)
    long_key = memory._long_key(user_id, f"verify-{token}")
    reflection_key = f"thread:{thread_id}"
    try:
        short = memory.save_short_term(
            user_id=user_id,
            session_id=session_id,
            active_task="验证 Agent 运行时短期记忆",
            facts={"workflow": "scheduling", "node_count": 2, "sources": ["MES", "RAG"]},
            trace_id=thread_id,
        )
        assert short["memory_scope"] == "short_term"
        assert memory.get_short_term(user_id=user_id, session_id=session_id)["active_task"] == "验证 Agent 运行时短期记忆"

        long = memory.save_long_term(
            user_id=user_id,
            memory_key=f"verify-{token}",
            kind="operational_note",
            content="这是一条显式确认的验证记忆，不会自动进入模型提示词。",
        )
        assert long["memory_scope"] == "long_term"
        assert any(item["memory_key"] == f"verify-{token}" for item in memory.list_long_term(user_id=user_id))

        store.save_checkpoint(
            thread_id=thread_id,
            checkpoint_token="foundation-verify-1",
            graph_code="AGENT_FOUNDATION_VERIFY",
            graph_version="1.0.0",
            checkpoint_status="COMPLETED",
            user_id=user_id,
            state={
                "request": {"order_no": "VERIFY-ONLY"},
                "risk_level": "LOW",
                "execution": {"lifecycle_status": "SCHEDULED"},
                "production_issue": {"lifecycle_status": "MATERIAL_ISSUED"},
                "node_records": [
                    {"node_id": "order", "agent_name": "OrderIntakeAgent", "stage": "order", "status": "completed", "summary": "验证节点"},
                    {"node_id": "schedule", "agent_name": "CapacitySchedulingAgent", "stage": "schedule", "status": "completed", "summary": "验证节点"},
                ],
            },
        )
        report = reflection.reflect_latest_run(thread_id=thread_id, user_id=user_id)
        assert report["graph_code"] == "AGENT_FOUNDATION_VERIFY"
        assert len(report["completed_nodes"]) == 2
        assert reflection.get_reflection(thread_id=thread_id, user_id=user_id)["thread_id"] == thread_id

        tools = AgentToolCatalog().list_tools()
        assert len(tools) >= 10
        assert any(tool["tool_id"] == "scheduling_graph_execution" and tool["approval_required"] for tool in tools)
        assert any(tool["tool_id"] == "image_quality_analysis" and tool["execution_status"] == "PLANNED" for tool in tools)
        print(json.dumps({
            "short_memory": "passed",
            "long_memory": "passed",
            "reflection": "passed",
            "tool_catalog_count": len(tools),
            "business_writes": 0,
        }, ensure_ascii=False))
    finally:
        store.delete_runtime_state(memory.short_term_state_type, short_key)
        store.delete_runtime_state(memory.long_term_state_type, long_key)
        store.delete_runtime_state(reflection.reflection_state_type, reflection_key)
        store.delete_checkpoints(thread_id)


if __name__ == "__main__":
    main()
