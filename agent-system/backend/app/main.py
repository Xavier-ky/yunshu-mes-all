from __future__ import annotations

import json
import re
import time
import uuid

import httpx
from fastapi import Depends, FastAPI, Header, HTTPException
from fastapi.middleware.cors import CORSMiddleware
from fastapi.responses import Response, StreamingResponse

from app.agents.registry import list_agents
from app.foundation.api import router as foundation_router
from app.companion.service import CompanionService
from app.companion.chat import CompanionChatService
from app.core.config import settings
from app.core.security import Principal, require_principal
from app.graph.workflow import workflow
from app.schemas import (
    ApproveRequest,
    ChatRequest,
    ChatResponse,
    CompanionContextRequest,
    CompanionSuggestionResponse,
    CompanionChatRequest,
    CompanionChatResponse,
    RagSearchRequest,
    AgentToolInvokeRequest,
    SchedulingOrderIntakeRequest,
)
from app.scheduling.order_intake import (
    OrderIntakeError,
    OrderIntakePermissionError,
    order_intake_service,
)
from app.scheduling.analysis import SchedulingAnalysisError, scheduling_analysis_service
from app.scheduling.execution import (
    SchedulingExecutionError,
    SchedulingExecutionPermissionError,
    scheduling_execution_service,
)
from app.scheduling.production_issue import (
    ProductionIssueError,
    ProductionIssuePermissionError,
    production_issue_service,
)
from app.scheduling.llm_analysis import scheduling_llm_analysis_service
from app.storage.memory_store import (
    get_confirmation,
    get_trace,
    save_confirmation,
    save_trace,
    update_confirmation,
)
from app.storage.mysql_store import ConversationStoreError, conversation_store
from app.llm.client import list_public_models
from app.llm.local_qwen import local_qwen, local_qwenl
from app.rag.qdrant_store import collection_summary
from app.rag.retrieval import search_knowledge
from app.quality.workflow import is_quality_analysis_request, quality_workflow
from app.quality.report_store import quality_report_store
from app.quality.report_workflow import is_quality_report_request, quality_report_workflow
from app.tools.registry import TOOL_REGISTRY, list_tools, run_tool
from app.tools.mes_read import mes_read_tools

app = FastAPI(title="云枢小智 Agent API", version="0.1.0")
companion_service = CompanionService()
companion_chat_service = CompanionChatService()

app.add_middleware(
    CORSMiddleware,
    allow_origins=settings.cors_origins,
    allow_credentials=True,
    allow_methods=["*"],
    allow_headers=["*"],
)
app.include_router(foundation_router)


@app.on_event("startup")
def warm_local_qwen_when_requested() -> None:
    """Optional warm-up; disabled by default so FastAPI starts immediately."""
    if settings.local_qwen_eager_load:
        local_qwen.warm_up()
    if settings.local_qwenl_eager_load:
        local_qwenl.warm_up()


@app.get("/api/health")
def health():
    return {
        "status": "UP",
        "service": "yunshu-agent-system",
        "mock_tools": settings.enable_mock_tools,
        "llm": settings.llm_provider,
        "local_qwen": local_qwen.status(),
        "local_qwenl": local_qwenl.status(),
    }


@app.get("/api/rag/health")
def rag_health(principal: Principal = Depends(require_principal)):
    """Authenticated status only; it exposes no document content."""
    try:
        return {"status": "UP", "qdrant": collection_summary()}
    except Exception as exc:
        raise HTTPException(status_code=503, detail="RAG 向量库暂时不可用") from exc


@app.post("/api/rag/search")
def rag_search(
    body: RagSearchRequest,
    principal: Principal = Depends(require_principal),
):
    """Retrieve approved MES knowledge within the caller's JWT role scope."""
    try:
        items = search_knowledge(
            body.query,
            roles=list(principal.roles),
            factory_id=body.factory_id,
            product_code=body.product_code,
            process_code=body.process_code,
            lifecycle_state=body.lifecycle_state,
            limit=body.limit,
        )
    except Exception as exc:
        raise HTTPException(status_code=503, detail="RAG 知识检索暂时不可用") from exc
    return {
        "query": body.query,
        "count": len(items),
        "items": items,
    }


@app.get("/api/agent/read-tools")
def agent_read_tools(principal: Principal = Depends(require_principal)):
    """List the only real P0 facts an Agent may obtain in this phase."""
    return {"tools": mes_read_tools.list_tools(), "read_only": True}


@app.post("/api/agent/read-tools/{tool_name}")
def invoke_agent_read_tool(
    tool_name: str,
    body: AgentToolInvokeRequest,
    principal: Principal = Depends(require_principal),
    x_trace_id: str | None = Header(default=None, alias="X-Trace-Id"),
):
    trace_id = x_trace_id or str(uuid.uuid4())
    try:
        result = mes_read_tools.invoke(tool_name, body.arguments, principal, trace_id)
    except ValueError as exc:
        raise HTTPException(status_code=422, detail=str(exc)) from exc
    except httpx.HTTPStatusError as exc:
        status = exc.response.status_code
        detail = "MES Agent 只读工具访问被拒绝" if status in (401, 403) else "MES 实时数据查询失败"
        raise HTTPException(status_code=status if status in (401, 403, 404) else 502, detail=detail) from exc
    except httpx.HTTPError as exc:
        raise HTTPException(status_code=503, detail="MES 实时数据暂时不可用") from exc
    save_trace(
        trace_id,
        {
            "trace_id": trace_id,
            "kind": "agent_read_tool",
            "tool_name": tool_name,
            "user_id": principal.user_id,
            "roles": list(principal.roles),
            "status": "Done",
            "source": result["source"],
        },
    )
    return result


@app.post("/api/companion/suggest", response_model=CompanionSuggestionResponse)
def companion_suggest(
    body: CompanionContextRequest,
    principal: Principal = Depends(require_principal),
    x_trace_id: str | None = Header(default=None, alias="X-Trace-Id"),
):
    response = companion_service.suggest(body, principal, x_trace_id)
    save_trace(
        response.trace_id,
        {
            "trace_id": response.trace_id,
            "kind": "companion_suggestion",
            "user_id": principal.user_id,
            "role": principal.primary_role,
            "route_path": body.route_path,
            "source": response.source,
            "status": "Done",
        },
    )
    return response


@app.post("/api/companion/chat", response_model=CompanionChatResponse)
def companion_chat(
    body: CompanionChatRequest,
    principal: Principal = Depends(require_principal),
    x_trace_id: str | None = Header(default=None, alias="X-Trace-Id"),
):
    try:
        response = companion_chat_service.reply(body, principal, x_trace_id)
    except RuntimeError as exc:
        raise HTTPException(status_code=503, detail=f"本地模型或会话存储暂时不可用：{exc}") from exc
    save_trace(
        response.trace_id,
        {
            "trace_id": response.trace_id,
            "kind": "companion_chat",
            "user_id": principal.user_id,
            "role": principal.primary_role,
            "session_id": response.session_id,
            "route_path": body.route_path,
            "source": response.source,
            "status": "Done",
        },
    )
    return response


@app.post("/api/companion/chat/stream")
def companion_chat_stream(
    body: CompanionChatRequest,
    principal: Principal = Depends(require_principal),
    x_trace_id: str | None = Header(default=None, alias="X-Trace-Id"),
):
    def event_stream():
        try:
            for payload in companion_chat_service.stream_reply(body, principal, x_trace_id):
                if payload["type"] == "done":
                    save_trace(
                        payload["trace_id"],
                        {
                            "trace_id": payload["trace_id"],
                            "kind": "companion_chat_stream",
                            "user_id": principal.user_id,
                            "role": principal.primary_role,
                            "session_id": payload["session_id"],
                            "route_path": body.route_path,
                            "source": payload["source"],
                            "status": "Done",
                        },
                    )
                yield f"data: {json.dumps(payload, ensure_ascii=False)}\n\n"
        except RuntimeError as exc:
            yield f"data: {json.dumps({'type': 'error', 'message': str(exc)}, ensure_ascii=False)}\n\n"

    return StreamingResponse(
        event_stream(),
        media_type="text/event-stream",
        headers={"Cache-Control": "no-cache", "X-Accel-Buffering": "no"},
    )


@app.get("/api/agent/sessions")
def list_agent_sessions(principal: Principal = Depends(require_principal)):
    """List the authenticated user's persisted 云枢小智 main-chat history."""
    try:
        return {"sessions": conversation_store.list_main_sessions(principal.user_id)}
    except ConversationStoreError as exc:
        raise HTTPException(status_code=503, detail="主对话历史暂时无法读取") from exc


@app.get("/api/agent/sessions/{session_id}")
def get_agent_session(session_id: str, principal: Principal = Depends(require_principal)):
    """Load a main-chat session only when it belongs to the JWT user."""
    try:
        session = conversation_store.get_main_session(principal.user_id, session_id)
    except ConversationStoreError as exc:
        raise HTTPException(status_code=503, detail="主对话历史暂时无法读取") from exc
    if not session:
        raise HTTPException(status_code=404, detail="对话不存在或无权访问")
    return session


@app.get("/api/agent/scheduling/order-intake/products")
def list_order_intake_products(
    principal: Principal = Depends(require_principal),
    x_trace_id: str | None = Header(default=None, alias="X-Trace-Id"),
):
    """Real product options for the Order Intake Agent's controlled form."""
    trace_id = x_trace_id or str(uuid.uuid4())
    try:
        products = order_intake_service.list_products(principal, trace_id)
    except OrderIntakePermissionError as exc:
        raise HTTPException(status_code=403, detail=str(exc)) from exc
    except OrderIntakeError as exc:
        raise HTTPException(status_code=503, detail=str(exc)) from exc
    return {"products": products, "source": "MES_PRODUCT_MASTER", "trace_id": trace_id}


@app.get("/api/agent/scheduling/order-intake/latest-order")
def get_latest_order_intake_order(
    principal: Principal = Depends(require_principal),
    x_trace_id: str | None = Header(default=None, alias="X-Trace-Id"),
):
    """Read the same newest real order displayed first in Order Center."""
    trace_id = x_trace_id or str(uuid.uuid4())
    try:
        order = order_intake_service.get_latest_order(principal, trace_id)
    except OrderIntakePermissionError as exc:
        raise HTTPException(status_code=403, detail=str(exc)) from exc
    except OrderIntakeError as exc:
        raise HTTPException(status_code=422, detail=str(exc)) from exc
    return {"order": order, "trace_id": trace_id}


@app.post("/api/agent/scheduling/order-intake/orders")
def create_scheduling_order(
    body: SchedulingOrderIntakeRequest,
    principal: Principal = Depends(require_principal),
    x_trace_id: str | None = Header(default=None, alias="X-Trace-Id"),
):
    """Create, confirm and pre-analyze one real scheduling order."""
    trace_id = x_trace_id or str(uuid.uuid4())
    try:
        user_id, agent_id, db_session_id, public_session_id = conversation_store.require_main_session(
            principal.user_id, body.session_id
        )
        result = order_intake_service.create_order(body, principal, trace_id)
    except OrderIntakePermissionError as exc:
        raise HTTPException(status_code=403, detail=str(exc)) from exc
    except SchedulingAnalysisError as exc:
        raise HTTPException(status_code=422, detail=str(exc)) from exc
    except SchedulingExecutionPermissionError as exc:
        raise HTTPException(status_code=403, detail=str(exc)) from exc
    except SchedulingExecutionError as exc:
        raise HTTPException(status_code=422, detail=str(exc)) from exc
    except ProductionIssuePermissionError as exc:
        raise HTTPException(status_code=403, detail=str(exc)) from exc
    except ProductionIssueError as exc:
        raise HTTPException(status_code=422, detail=str(exc)) from exc
    except OrderIntakeError as exc:
        raise HTTPException(status_code=422, detail=str(exc)) from exc
    except ConversationStoreError as exc:
        raise HTTPException(status_code=422, detail=str(exc)) from exc

    conversation_messages = []
    try:
        for event in result.get("workflow_events", []):
            content = _workflow_event_text(event)
            metadata = {"agent_name": event.get("agent") or "云枢小智", "workflow_event": event}
            conversation_store.append_message(db_session_id, "AGENT", agent_id, content, metadata=metadata)
            conversation_messages.append({"role": "assistant", "content": content, **metadata})

        final_analysis = scheduling_llm_analysis_service.summarize_completed_schedule(
            _scheduling_llm_facts(result), body.model, list(principal.roles)
        )
        final_content = f"{final_analysis['provider_label']} API 排产总结与风险建议\n{final_analysis['text']}"
        rag_titles = "、".join(
            str(item.get("title") or "")
            for item in final_analysis.get("rag", {}).get("citations", [])
            if item.get("title")
        )
        if rag_titles and "规则依据" not in final_analysis["text"]:
            final_content += f"\n规则依据：{rag_titles}"
        final_metadata = {
            "agent_name": "云枢小智",
            "workflow_event": None,
            "model_analysis": final_analysis,
        }
        conversation_store.append_message(db_session_id, "AGENT", agent_id, final_content, metadata=final_metadata)
        conversation_messages.append({"role": "assistant", "content": final_content, **final_metadata})
    except ConversationStoreError as exc:
        # MES writes have already been committed by their respective bounded
        # business services.  Never pretend their Agent explanation is safely
        # auditable when its durable conversation write failed.
        raise HTTPException(status_code=503, detail="排产已执行，但 Agent 过程输出未能写入数据库，请立即检查会话存储") from exc
    save_trace(
        trace_id,
        {
            "trace_id": trace_id,
            "kind": "scheduling_order_intake",
            "agent": "OrderIntakeAgent",
            "user_id": principal.user_id,
            "role": principal.primary_role,
            "order_id": result["order"]["order_id"],
            "order_no": result["order"]["order_no"],
            "work_order_id": result["work_order"]["work_order_id"],
            "agents": ["OrderIntakeAgent", "BomRouteAgent", "KittingRiskAgent", "CapacitySchedulingAgent", "ProductionIssueAgent"],
            "workflow_events": result.get("workflow_events", []),
            "status": "Done",
        },
    )
    return {
        **result,
        "trace_id": trace_id,
        "session_id": public_session_id,
        "conversation_messages": conversation_messages,
    }


@app.get("/api/agent/scheduling/work-orders/{work_order_id}/analysis")
def analyze_scheduling_work_order(
    work_order_id: int,
    principal: Principal = Depends(require_principal),
    x_trace_id: str | None = Header(default=None, alias="X-Trace-Id"),
):
    """Run BOM/route and kitting-risk sub-agents against current MES facts."""
    trace_id = x_trace_id or str(uuid.uuid4())
    try:
        order_intake_service._ensure_permission(principal)
        analysis = scheduling_analysis_service.analyze_work_order(work_order_id, principal, trace_id)
    except OrderIntakePermissionError as exc:
        raise HTTPException(status_code=403, detail=str(exc)) from exc
    except SchedulingAnalysisError as exc:
        raise HTTPException(status_code=422, detail=str(exc)) from exc
    save_trace(
        trace_id,
        {
            "trace_id": trace_id,
            "kind": "scheduling_pre_analysis",
            "agents": ["BomRouteAgent", "KittingRiskAgent"],
            "user_id": principal.user_id,
            "work_order_id": work_order_id,
            "risk_level": analysis["summary"]["risk_level"],
            "status": "Done",
        },
    )
    return {"analysis": analysis, "trace_id": trace_id}


@app.get("/api/agent/scheduling/work-orders/{work_order_id}/advisory")
def get_scheduling_advisory(
    work_order_id: int,
    principal: Principal = Depends(require_principal),
    x_trace_id: str | None = Header(default=None, alias="X-Trace-Id"),
):
    """Real device/personnel advisory; it is explicitly informational today."""
    trace_id = x_trace_id or str(uuid.uuid4())
    try:
        advisory = scheduling_execution_service.advisory(work_order_id, principal, trace_id)
    except SchedulingExecutionPermissionError as exc:
        raise HTTPException(status_code=403, detail=str(exc)) from exc
    except SchedulingExecutionError as exc:
        raise HTTPException(status_code=422, detail=str(exc)) from exc
    return {"advisory": advisory, "trace_id": trace_id}


@app.post("/api/agent/scheduling/work-orders/{work_order_id}/execute")
def execute_scheduling_work_order(
    work_order_id: int,
    principal: Principal = Depends(require_principal),
    x_trace_id: str | None = Header(default=None, alias="X-Trace-Id"),
):
    """Perform the controlled MES transaction from kitting reservation to Gantt tasks."""
    trace_id = x_trace_id or str(uuid.uuid4())
    try:
        result = scheduling_execution_service.execute(work_order_id, principal, trace_id)
    except SchedulingExecutionPermissionError as exc:
        raise HTTPException(status_code=403, detail=str(exc)) from exc
    except SchedulingExecutionError as exc:
        raise HTTPException(status_code=422, detail=str(exc)) from exc
    save_trace(
        trace_id,
        {
            "trace_id": trace_id,
            "kind": "autonomous_scheduling_execution",
            "agents": ["KittingExecutionAgent", "CapacitySchedulingAgent", "DispatchExecutionAgent", "SchedulingValidationAgent"],
            "user_id": principal.user_id,
            "work_order_id": work_order_id,
            "production_task_count": result.get("productionTaskCount"),
            "dispatch_task_count": result.get("dispatchTaskCount"),
            "lifecycle_status": result.get("lifecycleStatus"),
            "status": "Done",
        },
    )
    return {"execution": result, "trace_id": trace_id}


@app.post("/api/agent/production-issue/work-orders/{work_order_id}/execute")
def execute_production_issue_work_order(
    work_order_id: int,
    principal: Principal = Depends(require_principal),
    x_trace_id: str | None = Header(default=None, alias="X-Trace-Id"),
):
    """Complete the real WMS production issue and prepare the work order for shop-floor work."""
    trace_id = x_trace_id or str(uuid.uuid4())
    try:
        result = production_issue_service.execute(work_order_id, principal, trace_id)
    except ProductionIssuePermissionError as exc:
        raise HTTPException(status_code=403, detail=str(exc)) from exc
    except ProductionIssueError as exc:
        raise HTTPException(status_code=422, detail=str(exc)) from exc
    save_trace(
        trace_id,
        {
            "trace_id": trace_id,
            "kind": "autonomous_production_issue",
            "agents": ["ProductionIssueAgent"],
            "user_id": principal.user_id,
            "work_order_id": work_order_id,
            "issue_id": result.get("issueId"),
            "lifecycle_status": result.get("lifecycleStatus"),
            "ready_for_shop_floor": result.get("readyForShopFloor"),
            "status": "Done",
        },
    )
    return {"production_issue": result, "trace_id": trace_id}


def _run_persisted_main_chat(body: ChatRequest, principal: Principal) -> ChatResponse:
    """Persist both sides of the main chat before returning its Agent result.

    Caller identity and role always come from the MES JWT.  ``user_id`` and
    ``user_role`` in the browser payload remain backwards-compatible fields,
    but are deliberately not trusted for storage or agent authorization.
    """
    try:
        user_id, agent_id, db_session_id, public_session_id = conversation_store.prepare_main_conversation(
            principal.user_id,
            body.session_id,
            body.message,
        )
        conversation_store.append_message(db_session_id, "USER", user_id, body.message)
    except ConversationStoreError as exc:
        raise HTTPException(status_code=503, detail="主对话暂时无法写入数据库") from exc

    resp = workflow.run(
        body.message,
        public_session_id,
        principal.user_id,
        principal.primary_role,
        model=body.model,
        approval_mode=body.approval_mode or "risk_only",
    )
    resp.session_id = public_session_id
    try:
        conversation_store.append_message(
            db_session_id,
            "AGENT",
            agent_id,
            resp.answer,
            metadata={
                "agent_name": resp.agent_name,
                "workflow_event": resp.workflow_events[0] if resp.workflow_events else None,
                "model_analysis": resp.model_analysis,
            },
        )
    except ConversationStoreError as exc:
        raise HTTPException(status_code=503, detail="主对话回复暂时无法写入数据库") from exc
    save_trace(
        resp.trace_id,
        {
            "trace_id": resp.trace_id,
            "session_id": public_session_id,
            "user_id": principal.user_id,
            "role": principal.primary_role,
            "intent": resp.intent,
            "steps": resp.steps,
            "tool_calls": [t.model_dump() for t in resp.tool_calls],
            "selected_agents": resp.selected_agents,
            "status": resp.status,
        },
    )
    for c in resp.confirmations:
        save_confirmation(c.confirmation_id, c.model_dump())
    return resp


def _sse(payload: dict) -> str:
    return f"data: {json.dumps(payload, ensure_ascii=False)}\n\n"


def _workflow_event_text(event: dict) -> str:
    """Produce the exact durable text displayed for one real Agent node."""
    sources = "；".join(str(item) for item in event.get("sources", []) if item) or "MES 业务服务"
    status = {"completed": "已完成", "waiting": "等待输入", "blocked": "已阻断"}.get(
        event.get("status"), "已记录"
    )
    return (
        f"调用结果 · {event.get('title') or 'Agent 节点'}\n"
        f"{event.get('summary') or '未返回可展示的业务结论。'}\n"
        f"数据来源：{sources}\n"
        f"节点状态：{status}"
    )


def _scheduling_llm_facts(result: dict) -> dict:
    """Build the exact, minimal facts package allowed to leave the MES boundary."""
    order = result["order"]
    work_order = result["work_order"]
    analysis = result.get("analysis", {})
    bom = analysis.get("bom_route_agent", {})
    kitting = analysis.get("kitting_risk_agent", {})
    summary = analysis.get("summary", {})
    execution = result.get("scheduling_execution") or {}
    issue = result.get("production_issue_execution") or {}
    return {
        "order_no": order.get("order_no"),
        "product_name": order.get("product_name"),
        "order_qty": order.get("order_qty"),
        "delivery_date": order.get("delivery_date"),
        "work_order_no": work_order.get("work_order_no"),
        "bom_material_count": bom.get("material_count", 0),
        "process_step_count": bom.get("process_step_count", 0),
        "sufficient_material_count": kitting.get("sufficient_material_count", 0),
        "shortfall_count": kitting.get("shortage_material_count", 0),
        "risk_level": summary.get("risk_level"),
        "production_task_count": execution.get("productionTaskCount", 0),
        "dispatch_task_count": execution.get("dispatchTaskCount", 0),
        "issue_ready_for_shop_floor": bool(issue.get("readyForShopFloor")),
        "issue_status": issue.get("issueStatus") or issue.get("message"),
        "lifecycle_status": issue.get("lifecycleStatus") or execution.get("lifecycleStatus"),
    }


def _scheduling_final_text(result: dict) -> str:
    """Summarize only the bounded workflow result that has been persisted."""
    order = result["order"]
    work_order = result["work_order"]
    summary = result.get("analysis", {}).get("summary", {})
    execution = result.get("scheduling_execution")
    issue = result.get("production_issue_execution")
    schedule_text = (
        f"已完成齐套锁定、甘特排产与派工同步：{execution.get('productionTaskCount', 0)} 道任务、"
        f"{execution.get('dispatchTaskCount', 0)} 条派工，工单流程状态为 {execution.get('lifecycleStatus')}。"
        if execution
        else "当前未进入自动排产，请查看以上物料风险分析结论。"
    )
    issue_text = (
        f"生产领料已完成：领料单 {issue.get('issueCode') or issue.get('issueId')} 已出库，生成 "
        f"{issue.get('stockTransactionCount', 0)} 条库存流水和 {issue.get('consumeDetailCount', 0)} 条物料消耗追溯明细；"
        f"工单生命周期已进入 {issue.get('lifecycleStatus')}，可进入现场作业。"
        if issue and issue.get("readyForShopFloor")
        else "生产领料尚未完成，工单仍停留在排产后的待领料阶段。"
    )
    return (
        f"订单 {order['order_no']} 已自动确认，工单 {work_order['work_order_no']} 已生成。"
        f"{summary.get('recommendation') or '请查看以上各节点的真实分析结果。'} "
        f"{schedule_text} {issue_text}"
    )


def _answer_fragments(answer: str):
    """Emit a stable, human-readable cadence even for deterministic Agent replies.

    Some Agent routes produce an answer synchronously from tools instead of a
    token-streaming model. Sending one small fragment at a time keeps their UI
    behaviour consistent with model-backed responses and avoids a final burst.
    """
    for char in answer:
        yield char
        time.sleep(0.042 if char in "，。！？；：\n" else 0.021)


def _quality_chat_event_stream(body: ChatRequest, principal: Principal):
    """Run the quality workflow as real SSE while durably recording each card.

    Quality facts and model prompts are read-only. These are only the existing
    user-owned chat-audit writes; no quality business state is changed.
    """
    try:
        user_id, agent_id, db_session_id, public_session_id = conversation_store.prepare_main_conversation(
            principal.user_id, body.session_id, body.message
        )
        conversation_store.append_message(db_session_id, "USER", user_id, body.message)
    except ConversationStoreError:
        yield _sse({"type": "error", "message": "主对话暂时无法写入数据库"})
        return

    yield _sse({
        "type": "meta",
        "session_id": public_session_id,
        "status": "Running",
        "intent": "quality_daily_analysis",
        "agent_name": "MainAgent",
        "involved_modules": ["质量管理"],
        "selected_agents": quality_workflow.selected_agents,
        "plan": quality_workflow.plan,
        "tool_calls": [],
        "confirmations": [],
        "steps": [{"step": "QualityManagementAgent", "detail": "开始质量只读分析"}],
        "workflow_events": [],
        "model_analysis": {},
    })
    try:
        for item in quality_workflow.stream(
            message=body.message,
            principal=principal,
            provider_key=body.model,
        ):
            item_type = item.get("type")
            if item_type in {"node", "node_complete"}:
                content = str(item.get("content") or "")
                event = item.get("workflow_event") or {}
                if content:
                    conversation_store.append_message(
                        db_session_id,
                        "AGENT",
                        agent_id,
                        content,
                        metadata={
                            "agent_name": item.get("agent_name") or event.get("agent") or "QualityManagementAgent",
                            "workflow_event": event,
                            "model_analysis": {"source": "quality_workflow_node"},
                        },
                    )
                yield _sse(item)
                continue
            if item_type == "done":
                answer = str(item.get("answer") or "")
                conversation_store.append_message(
                    db_session_id,
                    "AGENT",
                    agent_id,
                    answer,
                    metadata={
                        "agent_name": item.get("agent_name") or "MainAgent",
                        "workflow_event": (item.get("workflow_events") or [])[-1] if item.get("workflow_events") else None,
                        "model_analysis": item.get("model_analysis") or {},
                    },
                )
                save_trace(
                    item["trace_id"],
                    {
                        "trace_id": item["trace_id"],
                        "session_id": public_session_id,
                        "user_id": principal.user_id,
                        "role": principal.primary_role,
                        "intent": item.get("intent"),
                        "steps": item.get("steps") or [],
                        "tool_calls": [],
                        "selected_agents": item.get("selected_agents") or [],
                        "workflow_events": item.get("workflow_events") or [],
                        "model_analysis": item.get("model_analysis") or {},
                        "status": item.get("status") or "Done",
                    },
                )
                yield _sse({"type": "done", "session_id": public_session_id, **item})
                continue
            yield _sse(item)
    except ConversationStoreError:
        yield _sse({"type": "error", "message": "质量分析结果暂时无法写入数据库"})
    except Exception:
        yield _sse({"type": "error", "message": "质量 Agent 暂时无法完成本次分析，请稍后重试。"})


def _quality_report_event_stream(body: ChatRequest, principal: Principal):
    """Run the quality-report continuation workflow with durable user ownership."""
    try:
        user_id, agent_id, db_session_id, public_session_id = conversation_store.prepare_main_conversation(
            principal.user_id, body.session_id, body.message
        )
        conversation_store.append_message(db_session_id, "USER", user_id, body.message)
    except ConversationStoreError:
        yield _sse({"type": "error", "message": "主对话暂时无法写入数据库"})
        return

    yield _sse({
        "type": "meta",
        "session_id": public_session_id,
        "status": "Running",
        "intent": "quality_analysis_report",
        "agent_name": "QualityManagementAgent",
        "involved_modules": ["质量管理"],
        "selected_agents": quality_report_workflow.selected_agents,
        "plan": quality_report_workflow.plan,
        "tool_calls": [],
        "confirmations": [],
        "steps": [{"step": "QualityReportAgent", "detail": "开始生成实时质量分析报告"}],
        "workflow_events": [],
        "model_analysis": {},
    })
    try:
        for item in quality_report_workflow.stream(
            message=body.message,
            principal=principal,
            provider_key=body.model,
            session_id=db_session_id,
            user_id=user_id,
            agent_id=agent_id,
        ):
            item_type = item.get("type")
            if item_type in {"node", "node_complete"}:
                content = str(item.get("content") or "")
                event = item.get("workflow_event") or {}
                if content:
                    conversation_store.append_message(
                        db_session_id, "AGENT", agent_id, content,
                        metadata={
                            "agent_name": item.get("agent_name") or event.get("agent") or "QualityReportAgent",
                            "workflow_event": event,
                            "model_analysis": {"source": "quality_report_workflow_node"},
                        },
                    )
                yield _sse(item)
                continue
            if item_type == "done":
                answer = str(item.get("answer") or "质量分析报告已完成。")
                report_id = item.get("report_id")
                report_payload = None
                if report_id:
                    stored = quality_report_store.get_owned(str(report_id), principal.user_id)
                    report_payload = stored.get("snapshot") if stored else None
                conversation_store.append_message(
                    db_session_id, "AGENT", agent_id, answer,
                    metadata={
                        "agent_name": "QualityManagementAgent",
                        "workflow_event": (item.get("workflow_events") or [])[-1] if item.get("workflow_events") else None,
                        "model_analysis": item.get("model_analysis") or {},
                        "report_id": report_id,
                        "report": report_payload,
                    },
                )
                save_trace(
                    item["trace_id"],
                    {
                        "trace_id": item["trace_id"], "session_id": public_session_id,
                        "user_id": principal.user_id, "role": principal.primary_role,
                        "intent": "quality_analysis_report", "selected_agents": quality_report_workflow.selected_agents,
                        "workflow_events": item.get("workflow_events") or [],
                        "model_analysis": item.get("model_analysis") or {},
                        "report_id": report_id, "status": item.get("status") or "Done",
                    },
                )
                yield _sse({"type": "done", "session_id": public_session_id, **item})
                continue
            yield _sse(item)
    except ConversationStoreError:
        yield _sse({"type": "error", "message": "质量报告结果暂时无法写入数据库"})
    except Exception:
        yield _sse({"type": "error", "message": "质量报告 Agent 暂时无法完成本次报告，请稍后重试。"})


@app.post("/api/agent/quality/reports/{report_id}/export")
def export_quality_report(report_id: str, principal: Principal = Depends(require_principal)):
    """Export only the caller's frozen quality-report snapshot as a DOCX."""
    if not re.fullmatch(r"QAR-[A-Z0-9]{12,40}", report_id or ""):
        raise HTTPException(status_code=422, detail="质量报告标识无效")
    try:
        stored = quality_report_store.get_owned(report_id, principal.user_id)
    except ConversationStoreError as exc:
        raise HTTPException(status_code=503, detail="质量报告存储暂时不可用") from exc
    if not stored:
        raise HTTPException(status_code=404, detail="质量报告不存在或不属于当前用户")
    if stored["status"] not in {"READY", "EXPORTED"}:
        raise HTTPException(status_code=409, detail="质量报告尚未生成完成，暂不可导出")
    headers = {"Accept": "application/vnd.openxmlformats-officedocument.wordprocessingml.document"}
    if principal.token:
        headers["Authorization"] = f"Bearer {principal.token}"
    try:
        with httpx.Client(timeout=45.0) as client:
            response = client.post(
                f"{settings.spring_boot_base_url.rstrip('/')}/api/agent/quality/reports/{report_id}/export",
                headers=headers,
            )
            response.raise_for_status()
    except httpx.HTTPStatusError as exc:
        status = exc.response.status_code
        raise HTTPException(status_code=status if status in {401, 403, 404, 409} else 502, detail="质量报告 Word 导出失败") from exc
    except httpx.HTTPError as exc:
        raise HTTPException(status_code=503, detail="质量报告 Word 服务暂时不可用") from exc
    try:
        quality_report_store.mark_exported(report_id, principal.user_id)
    except ConversationStoreError:
        # The document is already produced; do not fail a safe download just
        # because the secondary audit-status update needs a later retry.
        pass
    return Response(
        content=response.content,
        media_type="application/vnd.openxmlformats-officedocument.wordprocessingml.document",
        headers={"Content-Disposition": response.headers.get("Content-Disposition", "attachment; filename=quality-analysis-report.docx")},
    )


@app.post("/api/agent/chat/stream")
def chat_stream(body: ChatRequest, principal: Principal = Depends(require_principal)):
    """SSE version of the main chat, with the exact same persisted result."""

    # Report phrases also contain “quality” and “analysis”, so this more
    # specific route must be evaluated before the existing five-node analysis.
    if is_quality_report_request(body.message):
        return StreamingResponse(
            _quality_report_event_stream(body, principal),
            media_type="text/event-stream",
            headers={"Cache-Control": "no-cache, no-transform", "X-Accel-Buffering": "no"},
        )
    if is_quality_analysis_request(body.message):
        return StreamingResponse(
            _quality_chat_event_stream(body, principal),
            media_type="text/event-stream",
            headers={"Cache-Control": "no-cache, no-transform", "X-Accel-Buffering": "no"},
        )

    def event_stream():
        try:
            response = _run_persisted_main_chat(body, principal)
            metadata = {
                "session_id": response.session_id,
                "status": response.status,
                "intent": response.intent,
                "agent_name": response.agent_name,
                "involved_modules": response.involved_modules,
                "selected_agents": response.selected_agents,
                "plan": response.plan,
                "tool_calls": [item.model_dump() for item in response.tool_calls],
                "confirmations": [item.model_dump() for item in response.confirmations],
                "trace_id": response.trace_id,
                "steps": response.steps,
                "interaction": response.interaction,
                "workflow_events": response.workflow_events,
                "model_analysis": response.model_analysis,
            }
            yield _sse({"type": "meta", **metadata})
            for fragment in _answer_fragments(response.answer):
                yield _sse({"type": "delta", "content": fragment})
            yield _sse({"type": "done", **metadata})
        except HTTPException as exc:
            yield _sse({"type": "error", "message": str(exc.detail)})
        except Exception:
            # Do not expose internal database or provider details to the browser.
            yield _sse({"type": "error", "message": "云枢小智暂时无法完成本次回复，请稍后重试。"})

    return StreamingResponse(
        event_stream(),
        media_type="text/event-stream",
        headers={"Cache-Control": "no-cache, no-transform", "X-Accel-Buffering": "no"},
    )


@app.post("/api/agent/chat", response_model=ChatResponse)
def chat(body: ChatRequest, principal: Principal = Depends(require_principal)):
    """Compatibility endpoint for clients that do not yet consume SSE."""
    return _run_persisted_main_chat(body, principal)


@app.get("/api/agents")
def agents():
    return {"agents": list_agents()}


@app.get("/api/tools")
def tools():
    return {"tools": list_tools()}


@app.get("/api/models")
def models():
    return {"models": list_public_models(), "default": settings.default_provider}


@app.post("/api/confirmations/{confirmation_id}/approve")
def approve(confirmation_id: str, body: ApproveRequest):
    item = get_confirmation(confirmation_id)
    if not item:
        raise HTTPException(404, "确认单不存在")
    if item.get("status") != "pending":
        raise HTTPException(400, "确认单已处理")

    action = item.get("action_name")
    agent = item.get("agent_name")
    params = item.get("params") or {}
    tool = TOOL_REGISTRY.get(action)
    if not tool:
        raise HTTPException(400, "未知操作")

    result, _ = run_tool(agent, action, params)
    update_confirmation(confirmation_id, "approved", result)
    return {"confirmation_id": confirmation_id, "status": "approved", "result": result}


@app.post("/api/confirmations/{confirmation_id}/reject")
def reject(confirmation_id: str):
    item = get_confirmation(confirmation_id)
    if not item:
        raise HTTPException(404, "确认单不存在")
    update_confirmation(confirmation_id, "rejected")
    return {"confirmation_id": confirmation_id, "status": "rejected"}


@app.get("/api/traces/{trace_id}")
def trace(trace_id: str):
    t = get_trace(trace_id)
    if not t:
        raise HTTPException(404, "Trace 不存在")
    return t
