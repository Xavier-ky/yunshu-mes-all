"""Stable workflow-event projections for the scheduling sub-Agent wrappers."""

from __future__ import annotations

from typing import Any


def build_scheduling_workflow_events(
    analysis: dict[str, Any],
    execution: dict[str, Any] | None,
    production_issue: dict[str, Any] | None,
    *,
    order_no: str = "本次订单",
    work_order_no: str = "本次工单",
) -> list[dict[str, Any]]:
    """Return the existing UI/persistence schema without changing its order."""
    bom = analysis["bom_route_agent"]
    kitting = analysis["kitting_risk_agent"]
    risk = analysis["summary"]["risk_level"]
    events: list[dict[str, Any]] = [
        {
            "event_id": "order-intake-execution",
            "agent": "OrderIntakeAgent",
            "status": "completed",
            "title": "订单确认与工单生成",
            "summary": f"订单 {order_no} 已确认，并生成关联工单 {work_order_no}。",
            "sources": [
                "POST /api/planning/orders",
                "PUT /api/planning/orders/{id}",
                "POST /api/planning/work-orders",
            ],
            "analysis_prompt": "根据订单中心返回的订单号、确认状态和工单号，说明是否完成订单接收；不得虚构订单或工单。",
        },
        {
            "event_id": "bom-route",
            "agent": "BomRouteAgent",
            "status": "completed" if bom["status"] == "READY" else "blocked",
            "title": "BOM 与工艺路线分析",
            "summary": f"BOM物料 {bom['material_count']} 项，工艺工序 {bom['process_step_count']} 道；"
            + ("路线可用。" if bom["status"] == "READY" else "缺少可执行BOM或路线。"),
            "sources": ["GET /api/agent/read/work-orders/{id}/bom-route"],
            "analysis_prompt": "仅依据 BOM 数量、工序数量与路线状态，判断该工单能否进入齐套分析；不要补充未返回的工艺细节。",
        },
        {
            "event_id": "kitting-risk",
            "agent": "KittingRiskAgent",
            "status": "completed" if kitting["all_sufficient"] else "blocked",
            "title": "齐套与物料风险分析",
            "summary": f"齐套物料 {kitting['sufficient_material_count']}/{kitting['material_count']} 项，"
            f"短缺 {kitting['shortage_material_count']} 项，风险等级 {risk}。",
            "sources": ["GET /api/agent/read/work-orders/{id}/kitting"],
            "analysis_prompt": "仅依据齐套数量、短缺数量和风险等级说明物料风险；短缺时必须明确阻断后续排产。",
        },
    ]
    if not execution:
        events.append({
            "event_id": "scheduling-blocked", "agent": "CapacitySchedulingAgent", "status": "blocked",
            "title": "智能排产未执行", "summary": analysis["summary"]["recommendation"],
            "sources": ["齐套风险结论"],
            "analysis_prompt": "基于齐套风险结论，说明排产被阻断的真实原因和建议动作；不得宣称已经写入排产数据。",
        })
        return events
    events.extend([
        {
            "event_id": "kitting-execution", "agent": "KittingExecutionAgent", "status": "completed",
            "title": "锁料执行", "summary": "已对齐套分析通过的物料执行库存锁定。",
            "sources": ["POST /api/agent/scheduling/work-orders/{id}/execute"],
            "analysis_prompt": "仅确认锁料事务是否成功；不得将锁料描述为已经出库。",
        },
        {
            "event_id": "capacity-scheduling", "agent": "CapacitySchedulingAgent", "status": "completed",
            "title": "甘特排产", "summary": f"已创建 {execution.get('productionTaskCount', 0)} 道生产任务，并写入计划工位与时间。",
            "sources": ["生产任务 / 工位产能 API"],
            "analysis_prompt": "根据生产任务数量、计划工位与时间写入结果，概述甘特排产结果；不要捏造设备或人员负荷。",
        },
        {
            "event_id": "dispatch-execution", "agent": "DispatchExecutionAgent", "status": "completed",
            "title": "派工同步", "summary": f"已同步 {execution.get('dispatchTaskCount', 0)} 条现场派工。",
            "sources": ["dispatch_task"],
            "analysis_prompt": "依据派工同步数量确认现场执行单元是否建立；不得杜撰具体操作员。",
        },
        {
            "event_id": "scheduling-validation", "agent": "SchedulingValidationAgent", "status": "completed",
            "title": "排产写回校验", "summary": f"工单生命周期已写回 {execution.get('lifecycleStatus')}。",
            "sources": ["work_order / production_task / dispatch_task"],
            "analysis_prompt": "核对工单、生产任务与派工三类记录，并只陈述实际返回的生命周期状态。",
        },
    ])
    if production_issue:
        ready = bool(production_issue.get("readyForShopFloor"))
        events.append({
            "event_id": "production-issue", "agent": "ProductionIssueAgent",
            "status": "completed" if ready else "blocked", "title": "生产领料与出库",
            "summary": (f"领料单 {production_issue.get('issueCode') or production_issue.get('issueId')} 已完成，"
                        f"工单进入 {production_issue.get('lifecycleStatus')}，可现场开工。") if ready
                       else str(production_issue.get("error") or production_issue.get("message")),
            "sources": ["POST /api/agent/production-issue/work-orders/{id}/execute"],
            "analysis_prompt": "根据领料执行结果、库存流水和工单生命周期判断能否进入现场作业；失败时如实说明阻断原因。",
        })
    return events
