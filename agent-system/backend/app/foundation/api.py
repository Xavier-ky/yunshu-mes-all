"""Read-mostly HTTP surface for the non-invasive Agent foundation layer."""

from __future__ import annotations

from typing import Optional
import uuid

import httpx
from fastapi import APIRouter, Depends, HTTPException

from app.core.security import Principal, require_principal
from app.foundation.reflection import AgentReflectionError, agent_reflection_service
from app.foundation.runtime_memory import AgentMemoryError, agent_memory_service
from app.foundation.tool_catalog import agent_tool_catalog
from app.foundation.tools import PackagedReadToolError, foundation_read_tools
from app.schemas import AgentToolInvokeRequest
from app.storage.agent_platform_store import AgentPlatformStoreError


router = APIRouter(prefix="/api/agent/foundation", tags=["Agent foundation"])


@router.get("/tools")
def list_foundation_tools(
    domain: Optional[str] = None,
    principal: Principal = Depends(require_principal),
):
    """Return the governance catalogue; this endpoint grants no tool access."""
    return {
        "tools": agent_tool_catalog.list_tools(domain=domain),
        "catalog_only": True,
        "business_execution_changed": False,
    }


@router.get("/tools/real")
def list_packaged_real_tools(principal: Principal = Depends(require_principal)):
    """List only the actual packaged, side-effect-free MES/RAG Tools."""
    return {
        "tools": foundation_read_tools.list_tools(),
        "read_only": True,
        "workflow_integration": "NOT_ENABLED",
        "business_execution_changed": False,
    }


@router.post("/tools/real/{tool_name}")
def invoke_packaged_real_tool(
    tool_name: str,
    body: AgentToolInvokeRequest,
    principal: Principal = Depends(require_principal),
):
    """Invoke one existing real read Tool without changing a MES business row."""
    trace_id = str(uuid.uuid4())
    try:
        return foundation_read_tools.invoke(tool_name, body.arguments, principal, trace_id)
    except PackagedReadToolError as exc:
        raise HTTPException(status_code=422, detail=str(exc)) from exc
    except ValueError as exc:
        raise HTTPException(status_code=422, detail=str(exc)) from exc
    except httpx.HTTPStatusError as exc:
        status = exc.response.status_code
        detail = "MES 只读 Tool 访问被拒绝" if status in (401, 403) else "MES 实时数据查询失败"
        raise HTTPException(status_code=status if status in (401, 403, 404) else 502, detail=detail) from exc
    except httpx.HTTPError as exc:
        raise HTTPException(status_code=503, detail="MES 实时数据暂时不可用") from exc


@router.get("/memory/short/{session_id}")
def get_short_term_memory(
    session_id: int,
    principal: Principal = Depends(require_principal),
):
    try:
        memory = agent_memory_service.get_short_term(user_id=int(principal.user_id), session_id=session_id)
    except (AgentMemoryError, AgentPlatformStoreError) as exc:
        raise HTTPException(status_code=503, detail="Agent 短期记忆暂时不可用") from exc
    return {"memory": memory, "prompt_injection": "DISABLED"}


@router.get("/memory/long")
def list_long_term_memory(principal: Principal = Depends(require_principal)):
    try:
        records = agent_memory_service.list_long_term(user_id=int(principal.user_id))
    except (AgentMemoryError, AgentPlatformStoreError) as exc:
        raise HTTPException(status_code=503, detail="Agent 长期记忆暂时不可用") from exc
    return {"memories": records, "prompt_injection": "DISABLED", "write_mode": "EXPLICIT_REVIEW_REQUIRED"}


@router.post("/reflections/{thread_id}")
def create_run_reflection(
    thread_id: str,
    principal: Principal = Depends(require_principal),
):
    """Persist a fact-only reflection from an existing owner-scoped graph run."""
    try:
        reflection = agent_reflection_service.reflect_latest_run(thread_id=thread_id, user_id=int(principal.user_id))
    except AgentReflectionError as exc:
        raise HTTPException(status_code=404, detail=str(exc)) from exc
    except AgentPlatformStoreError as exc:
        raise HTTPException(status_code=503, detail="Agent 执行复盘暂时不可用") from exc
    return {"reflection": reflection, "business_execution_changed": False}


@router.get("/reflections/{thread_id}")
def get_run_reflection(
    thread_id: str,
    principal: Principal = Depends(require_principal),
):
    try:
        reflection = agent_reflection_service.get_reflection(thread_id=thread_id, user_id=int(principal.user_id))
    except AgentReflectionError as exc:
        raise HTTPException(status_code=404, detail=str(exc)) from exc
    except AgentPlatformStoreError as exc:
        raise HTTPException(status_code=503, detail="Agent 执行复盘暂时不可用") from exc
    if reflection is None:
        raise HTTPException(status_code=404, detail="当前图运行尚未生成复盘记录")
    return {"reflection": reflection}
