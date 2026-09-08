from __future__ import annotations

from dataclasses import dataclass, field
from typing import Any, Callable


@dataclass
class ToolDef:
    name: str
    module: str
    action_type: str  # read | write
    risk_level: str
    require_confirmation: bool
    description: str
    handler: Callable[..., Any]


MOCK_DATA = {
    "overview": {
        "today_output": 1280,
        "running_lines": 4,
        "open_andon": 3,
        "quality_pass_rate": 97.6,
        "equipment_fault": 1,
        "inventory_alert": 2,
    },
    "work_orders": {
        "WO20260708001": {
            "status": "DELAYED",
            "plan_qty": 500,
            "completed_qty": 320,
            "delay_reason_hint": "物料齐套不足 + 质检待复判",
        }
    },
    "kitting": {"WO20260708001": {"shortage": ["电机壳体", "轴承组件"], "kitting_rate": 82}},
    "production": {"WO20260708001": {"progress": 64, "bottleneck": "工位 B-03 报工滞后"}},
    "quality": {"WO20260708001": {"pending_tasks": 2, "defects_today": 5}},
    "andon": {"WO20260708001": [{"type": "物料异常", "status": "OPEN"}]},
    "inventory": {"motor_housing": {"qty": 120, "locked": 80}},
    "equipment": {"line_a": {"status": "RUNNING", "oee": 86.2}},
    "quality_analysis": {
        "top_defects": ["异响", "螺丝松动", "外观划伤"],
        "risk": "异响集中在 B 线末检工位",
    },
}


def _query_system_overview(**_: Any) -> dict:
    return MOCK_DATA["overview"]


def _query_work_order(work_order_no: str = "WO20260708001", **_: Any) -> dict:
    return MOCK_DATA["work_orders"].get(work_order_no, {"status": "NOT_FOUND"})


def _query_material_kitting(work_order_no: str = "WO20260708001", **_: Any) -> dict:
    return MOCK_DATA["kitting"].get(work_order_no, {})


def _query_production_progress(work_order_no: str = "WO20260708001", **_: Any) -> dict:
    return MOCK_DATA["production"].get(work_order_no, {})


def _query_quality_record(work_order_no: str = "WO20260708001", **_: Any) -> dict:
    return MOCK_DATA["quality"].get(work_order_no, {})


def _query_andon_events(work_order_no: str | None = None, **_: Any) -> list:
    if work_order_no:
        return MOCK_DATA["andon"].get(work_order_no, [])
    return [{"type": "电机异响", "status": "OPEN", "line": "A-01"}]


def _query_inventory(**_: Any) -> dict:
    return MOCK_DATA["inventory"]


def _query_equipment_status(**_: Any) -> dict:
    return MOCK_DATA["equipment"]


def _query_schedule_plan(**_: Any) -> dict:
    return {"today_planned": 6, "delayed": 1, "on_track": 5}


def _query_report_summary(**_: Any) -> dict:
    return MOCK_DATA["quality_analysis"]


def _create_andon_event(title: str = "电机异响", line: str = "A-01", **_: Any) -> dict:
    return {"event_id": "ANDON-MOCK-001", "title": title, "line": line, "status": "CREATED"}


def _submit_production_report(**kwargs: Any) -> dict:
    return {"report_id": "RPT-MOCK-001", **kwargs}


def _confirm_material_issue(**kwargs: Any) -> dict:
    return {"issue_id": "ISS-MOCK-001", **kwargs}


def _submit_quality_result(**kwargs: Any) -> dict:
    return {"record_id": "QC-MOCK-001", **kwargs}


TOOL_REGISTRY: dict[str, ToolDef] = {
    "query_system_overview": ToolDef(
        "query_system_overview", "系统总览", "read", "low", False,
        "查询系统总览 KPI", _query_system_overview,
    ),
    "query_schedule_plan": ToolDef(
        "query_schedule_plan", "计划调度", "read", "low", False,
        "查询排产计划", _query_schedule_plan,
    ),
    "query_work_order": ToolDef(
        "query_work_order", "计划调度", "read", "low", False,
        "查询工单", _query_work_order,
    ),
    "query_material_kitting": ToolDef(
        "query_material_kitting", "计划调度", "read", "low", False,
        "查询齐套", _query_material_kitting,
    ),
    "query_inventory": ToolDef(
        "query_inventory", "仓储管理", "read", "low", False,
        "查询库存", _query_inventory,
    ),
    "query_production_progress": ToolDef(
        "query_production_progress", "生产管理", "read", "low", False,
        "查询生产进度", _query_production_progress,
    ),
    "query_quality_record": ToolDef(
        "query_quality_record", "质量管理", "read", "low", False,
        "查询质检记录", _query_quality_record,
    ),
    "query_andon_events": ToolDef(
        "query_andon_events", "安灯中心", "read", "low", False,
        "查询安灯", _query_andon_events,
    ),
    "query_equipment_status": ToolDef(
        "query_equipment_status", "设备管理", "read", "low", False,
        "查询设备状态", _query_equipment_status,
    ),
    "query_report_summary": ToolDef(
        "query_report_summary", "分析集成", "read", "low", False,
        "查询报表摘要", _query_report_summary,
    ),
    "create_andon_event": ToolDef(
        "create_andon_event", "安灯中心", "write", "medium", True,
        "创建安灯事件", _create_andon_event,
    ),
    "submit_production_report": ToolDef(
        "submit_production_report", "生产管理", "write", "high", True,
        "提交报工", _submit_production_report,
    ),
    "confirm_material_issue": ToolDef(
        "confirm_material_issue", "仓储管理", "write", "high", True,
        "确认发料", _confirm_material_issue,
    ),
    "submit_quality_result": ToolDef(
        "submit_quality_result", "质量管理", "write", "high", True,
        "提交质检结果", _submit_quality_result,
    ),
}


def list_tools() -> list[dict]:
    return [
        {
            "name": t.name,
            "module": t.module,
            "action_type": t.action_type,
            "risk_level": t.risk_level,
            "require_confirmation": t.require_confirmation,
            "description": t.description,
        }
        for t in TOOL_REGISTRY.values()
    ]


def run_tool(agent_name: str, tool_name: str, params: dict | None = None) -> tuple[Any, bool]:
    from app.agents.registry import agent_can_use_tool

    if not agent_can_use_tool(agent_name, tool_name):
        raise PermissionError(f"{agent_name} 无权调用 {tool_name}")
    tool = TOOL_REGISTRY.get(tool_name)
    if not tool:
        raise ValueError(f"未知工具: {tool_name}")
    params = params or {}
    result = tool.handler(**params)
    return result, tool.require_confirmation
