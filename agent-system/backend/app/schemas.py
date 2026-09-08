from pydantic import BaseModel, Field
from datetime import date
from typing import Any, Optional


class ChatRequest(BaseModel):
    session_id: Optional[str] = None
    user_id: str = "guest"
    user_role: str = "TESTER"
    message: str
    model: Optional[str] = None
    approval_mode: Optional[str] = "risk_only"


class ConfirmationAction(BaseModel):
    confirmation_id: str
    agent_name: str
    business_module: str
    action_name: str
    target: str
    params: dict[str, Any] = Field(default_factory=dict)
    risk_level: str = "medium"
    risk_note: str = ""
    expected_result: str = ""
    status: str = "pending"


class ToolCallRecord(BaseModel):
    tool_name: str
    agent_name: str
    module: str
    action_type: str
    input: dict[str, Any] = Field(default_factory=dict)
    output: Any = None
    status: str = "success"


class ChatResponse(BaseModel):
    session_id: str
    answer: str
    intent: str
    # The visible speaker can be a routing/sub-agent rather than the main
    # assistant.  This lets the chat render the actual orchestration order.
    agent_name: str = "云枢小智"
    status: str = "Done"
    involved_modules: list[str] = Field(default_factory=list)
    selected_agents: list[str] = Field(default_factory=list)
    plan: list[str] = Field(default_factory=list)
    tool_calls: list[ToolCallRecord] = Field(default_factory=list)
    confirmations: list[ConfirmationAction] = Field(default_factory=list)
    trace_id: str
    steps: list[dict[str, Any]] = Field(default_factory=list)
    interaction: dict[str, Any] = Field(default_factory=dict)
    workflow_events: list[dict[str, Any]] = Field(default_factory=list)
    model_analysis: dict[str, Any] = Field(default_factory=dict)


class SchedulingOrderIntakeRequest(BaseModel):
    session_id: Optional[str] = Field(default=None, max_length=64)
    model: Optional[str] = Field(default=None, max_length=32)
    # When the confirmation card was populated from Order Center, this points
    # to that durable order.  The server re-reads it before any write, so the
    # browser never becomes the source of truth for an existing order.
    source_order_id: Optional[int] = Field(default=None, gt=0)
    order_no: str = Field(min_length=1, max_length=64)
    customer_name: str = Field(min_length=1, max_length=100)
    product_id: int = Field(gt=0)
    order_qty: int = Field(gt=0)
    delivery_date: date
    confirmed: bool = False


class ApproveRequest(BaseModel):
    user_id: str = "guest"


class CompanionContextRequest(BaseModel):
    route_path: str = Field(min_length=1, max_length=240)
    route_title: str = Field(default="", max_length=120)
    work_order_no: Optional[str] = Field(default=None, max_length=80)
    line_id: Optional[str] = Field(default=None, max_length=80)
    device_id: Optional[str] = Field(default=None, max_length=80)
    andon_id: Optional[str] = Field(default=None, max_length=80)


class CompanionAction(BaseModel):
    label: str
    type: str


class CompanionSuggestionResponse(BaseModel):
    title: str
    message: str
    visual_state: str
    urgency: str
    source: str
    actions: list[CompanionAction] = Field(default_factory=list)
    trace_id: str


class CompanionChatRequest(BaseModel):
    message: str = Field(min_length=1, max_length=500)
    session_id: Optional[str] = Field(default=None, max_length=32)
    preset_id: Optional[str] = Field(default=None, max_length=64)
    model_key: str = Field(default="qwen3", min_length=1, max_length=16)
    route_path: str = Field(default="/app", min_length=1, max_length=240)
    route_title: str = Field(default="", max_length=120)


class CompanionChatResponse(BaseModel):
    answer: str
    session_id: str
    source: str = "local-qwen3-0.6b"
    model: str
    trace_id: str
    follow_ups: list[str] = Field(default_factory=list)


class RagSearchRequest(BaseModel):
    """A read-only knowledge request. Role scope always comes from the JWT."""

    query: str = Field(min_length=1, max_length=1000)
    factory_id: Optional[str] = Field(default=None, max_length=80)
    product_code: Optional[str] = Field(default=None, max_length=80)
    process_code: Optional[str] = Field(default=None, max_length=80)
    lifecycle_state: Optional[str] = Field(default=None, max_length=80)
    limit: int = Field(default=5, ge=1, le=8)


class AgentToolInvokeRequest(BaseModel):
    """Tool arguments are validated again by the registered tool itself."""

    arguments: dict[str, Any] = Field(default_factory=dict)
