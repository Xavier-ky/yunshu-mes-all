from __future__ import annotations

import re
import uuid
from typing import Any

from app.core.config import settings
from app.llm.client import chat_completion
from app.scheduling.agents.intent_routing import IntentRoutingAgent
from app.schemas import ChatResponse, ConfirmationAction, ToolCallRecord
from app.tools.registry import run_tool


def _extract_wo(msg: str) -> str | None:
    m = re.search(r"WO\d+", msg, re.I)
    return m.group(0).upper() if m else None


def _is_new_order_for_scheduling(msg: str) -> bool:
    normalized = re.sub(r"\s+", "", msg)
    asks_schedule = any(token in normalized for token in ("排产", "安排生产", "安排生产计划", "生产计划"))
    signals_new_order = any(token in normalized for token in (
        "新订单", "新增订单", "一笔订单", "一个订单", "有个订单", "客户订单", "创建订单", "订单需要",
    ))
    return asks_schedule and signals_new_order


def _step(name: str, detail: str) -> dict[str, Any]:
    return {"step": name, "detail": detail}


class AgentWorkflow:
    """MVP 多 Agent 编排（LangGraph 骨架占位，后续可替换为 StateGraph）"""

    SYSTEM_PROMPT = (
        "你是云枢智造 MES 的智能助手「云枢小智」，可协调计划调度、仓储、生产、质量、安灯、设备等子 Agent。"
        "回答请简洁专业，使用 Markdown。若涉及工单/安灯/质量等，可引导用户使用具体指令。"
    )

    def __init__(self) -> None:
        self.intent_routing_agent = IntentRoutingAgent()

    def _llm_reply(self, message: str, model: str | None) -> str | None:
        if settings.mock_llm:
            return None
        try:
            return chat_completion(
                model or settings.default_provider,
                [
                    {"role": "system", "content": self.SYSTEM_PROMPT},
                    {"role": "user", "content": message},
                ],
            )
        except Exception:
            return None

    def run(
        self,
        message: str,
        session_id: str | None,
        user_id: str,
        user_role: str,
        model: str | None = None,
        approval_mode: str = "risk_only",
    ) -> ChatResponse:
        sid = session_id or str(uuid.uuid4())
        trace_id = str(uuid.uuid4())
        steps: list[dict] = []
        tool_calls: list[ToolCallRecord] = []
        confirmations: list[ConfirmationAction] = []
        plan: list[str] = []
        agents: list[str] = []
        modules: list[str] = []
        msg = message.strip()
        lower = msg.lower()

        steps.append(_step("MainAgent", "接收用户请求"))
        steps.append(_step("安全检查", "通过"))
        steps.append(_step("批准模式", approval_mode))
        if model:
            steps.append(_step("模型", model))

        # --- 场景 0：智能排产的第一步，接收订单 ---
        if _is_new_order_for_scheduling(msg):
            intent_analysis = self.intent_routing_agent.run(msg, model, [user_role])
            intent = "scheduling_order_intake"
            agents = ["IntentRoutingAgent", "OrderIntakeAgent"]
            modules = ["订单中心", "计划调度"]
            plan = [
                "OrderIntakeAgent 从订单中心读取最新一条真实订单",
                "回填订单号、客户、产品、数量与交付日期并校验主数据",
                "等待用户确认后确认该订单，不重复创建订单",
                "转入订单确认与工单生成节点",
            ]
            steps.append(_step("意图识别", intent))
            steps.append(_step("OrderIntakeAgent", "读取订单中心最新订单并准备确认卡"))
            return ChatResponse(
                session_id=sid,
                answer=(
                    f"{intent_analysis['provider_label']} 意图识别 API 返回\n"
                    f"请求类型：新订单智能排产\n"
                    f"识别置信度：{intent_analysis['confidence']:.2f}\n"
                    f"模型判断：{intent_analysis['reason']}\n"
                    "下一步：从订单中心读取最新一条订单，并回填订单号、客户、产品、数量与交付日期。\n\n"
                    "已将请求交给订单接收 Agent。请确认下方自动回填的真实订单；确认后将直接确认该订单、生成工单并进入原有智能排产流程。"
                ),
                intent=intent,
                agent_name="IntentRoutingAgent",
                status="Waiting Approval",
                involved_modules=modules,
                selected_agents=agents,
                plan=plan,
                tool_calls=[],
                confirmations=[],
                trace_id=trace_id,
                steps=steps,
                interaction={"type": "scheduling_order_intake", "version": 1},
                model_analysis={"intent": intent_analysis},
                workflow_events=[{
                    "event_id": "intent-routing",
                    "agent": "IntentRoutingAgent",
                    "status": "completed",
                    "title": "新订单排产意图识别",
                    "summary": "已识别为新订单智能排产请求，下一节点为订单信息收集。",
                    "sources": [
                        f"{intent_analysis['provider_label']} Intent API",
                        *(
                            [
                                "Qdrant RAG · " + "、".join(
                                    str(item.get("title") or "")
                                    for item in intent_analysis.get("rag", {}).get("citations", [])
                                    if item.get("title")
                                )
                            ]
                            if intent_analysis.get("rag", {}).get("citations")
                            else []
                        ),
                    ],
                    "analysis_prompt": "仅根据用户原始请求识别业务意图、置信度与下一步；不得生成订单、不得编造业务数据。",
                }],
            )

        # --- 场景 1: 工单延期 ---
        if "延期" in msg or "wo2026" in lower or _extract_wo(msg):
            wo = _extract_wo(msg) or "WO20260708001"
            intent = "work_order_delay_analysis"
            agents = ["ScheduleAgent", "WarehouseAgent", "ProductionAgent", "QualityAgent", "AndonAgent"]
            modules = ["计划调度", "仓储管理", "生产管理", "质量管理", "安灯中心"]
            plan = [
                f"查询工单 {wo} 计划与状态",
                "检查物料齐套与库存",
                "查看生产进度瓶颈",
                "查看质检待办",
                "查看关联安灯事件",
                "汇总延期原因与建议",
            ]
            steps.append(_step("意图识别", intent))
            steps.append(_step("任务拆解", f"涉及 {len(agents)} 个子 Agent"))

            for agent, tool, params in [
                ("ScheduleAgent", "query_work_order", {"work_order_no": wo}),
                ("WarehouseAgent", "query_material_kitting", {"work_order_no": wo}),
                ("WarehouseAgent", "query_inventory", {}),
                ("ProductionAgent", "query_production_progress", {"work_order_no": wo}),
                ("QualityAgent", "query_quality_record", {"work_order_no": wo}),
                ("AndonAgent", "query_andon_events", {"work_order_no": wo}),
            ]:
                out, _ = run_tool(agent, tool, params)
                tool_calls.append(
                    ToolCallRecord(
                        tool_name=tool,
                        agent_name=agent,
                        module=agent.replace("Agent", ""),
                        action_type="read",
                        input=params,
                        output=out,
                    )
                )
                steps.append(_step(agent, f"执行 {tool}"))

            answer = (
                f"## 结论\n工单 **{wo}** 延期主因：**物料齐套不足（82%）** 叠加 **B-03 工位报工滞后**，另有 **2 项质检待复判**。\n\n"
                "## 子 Agent 执行情况\n"
                + "\n".join(f"- **{a}**：已完成查询" for a in agents)
                + "\n\n## 查询依据\n"
                "- 齐套缺：电机壳体、轴承组件\n"
                "- 生产进度 64%，瓶颈工位 B-03\n"
                "- 存在 OPEN 状态安灯（物料异常）\n\n"
                "## 风险点\n- 库存锁定导致发料延迟\n- 质检积压可能影响放行\n\n"
                "## 建议操作\n1. 优先跟进 WarehouseAgent 发料确认\n2. ProductionAgent 加派 B-03 人力\n3. QualityAgent 优先复判待检任务\n\n"
                "## 下一步\n可说「帮我创建电机异响安灯」或「确认发料申请」。"
            )
            return ChatResponse(
                session_id=sid,
                answer=answer,
                intent=intent,
                status="Done",
                involved_modules=modules,
                selected_agents=agents,
                plan=plan,
                tool_calls=tool_calls,
                confirmations=confirmations,
                trace_id=trace_id,
                steps=steps,
            )

        # --- 场景 2: 创建安灯 ---
        if "安灯" in msg and ("创建" in msg or "电机" in msg or "异响" in msg):
            intent = "create_andon"
            agents = ["AndonAgent"]
            modules = ["安灯中心"]
            plan = ["识别安灯类型与产线", "生成确认卡片", "等待用户批准后执行"]
            steps.append(_step("意图识别", intent))
            steps.append(_step("路由", "AndonAgent"))

            cid = str(uuid.uuid4())
            title = "电机异响" if "异响" in msg else "现场异常"
            confirmations.append(
                ConfirmationAction(
                    confirmation_id=cid,
                    agent_name="AndonAgent",
                    business_module="安灯中心",
                    action_name="create_andon_event",
                    target="产线 A-01",
                    params={"title": title, "line": "A-01"},
                    risk_level="medium",
                    risk_note="将通知班组长与维修员",
                    expected_result="创建安灯事件 ANDON-MOCK-001",
                    status="pending",
                )
            )
            steps.append(_step("确认流程", "写入操作等待批准"))

            answer = (
                "## 结论\n已为您准备 **安灯创建** 请求，请在下方确认卡片中批准执行。\n\n"
                "## 子 Agent 执行情况\n- **AndonAgent**：已生成待确认动作\n\n"
                "## 风险点\n- 中等风险：将触发安灯通知链\n\n"
                "## 下一步\n点击「批准执行」完成创建。"
            )
            return ChatResponse(
                session_id=sid,
                answer=answer,
                intent=intent,
                status="Waiting Approval",
                involved_modules=modules,
                selected_agents=agents,
                plan=plan,
                tool_calls=tool_calls,
                confirmations=confirmations,
                trace_id=trace_id,
                steps=steps,
            )

        # --- 场景 3: 系统总览 ---
        if "总览" in msg or "概况" in msg or "今天" in msg and "系统" in msg:
            intent = "system_overview"
            agents = ["OverviewAgent"]
            modules = ["系统总览"]
            plan = ["OverviewAgent 查询今日 KPI", "MainAgent 汇总"]
            out, _ = run_tool("OverviewAgent", "query_system_overview", {})
            tool_calls.append(
                ToolCallRecord(
                    tool_name="query_system_overview",
                    agent_name="OverviewAgent",
                    module="系统总览",
                    action_type="read",
                    input={},
                    output=out,
                )
            )
            steps.append(_step("OverviewAgent", "query_system_overview"))
            o = out
            answer = (
                f"## 结论\n今日系统运行 **总体平稳**，产量 **{o['today_output']}** 台，**{o['running_lines']}** 条产线运行中。\n\n"
                f"## 查询依据\n- 安灯待处理：{o['open_andon']} 条\n"
                f"- 质量合格率：{o['quality_pass_rate']}%\n"
                f"- 设备故障：{o['equipment_fault']} 台\n"
                f"- 库存预警：{o['inventory_alert']} 项\n\n"
                "## 建议操作\n优先处理 OPEN 安灯与库存预警。"
            )
            return ChatResponse(
                session_id=sid,
                answer=answer,
                intent=intent,
                status="Done",
                involved_modules=modules,
                selected_agents=agents,
                plan=plan,
                tool_calls=tool_calls,
                confirmations=confirmations,
                trace_id=trace_id,
                steps=steps,
            )

        # --- 场景 4: 质量分析 ---
        if "质量" in msg and ("分析" in msg or "异常" in msg):
            intent = "quality_analysis"
            agents = ["QualityAgent", "AnalyticsAgent"]
            modules = ["质量管理", "分析集成"]
            plan = ["QualityAgent 查询质检记录", "AnalyticsAgent 生成趋势分析", "MainAgent 汇总建议"]
            for agent, tool in [
                ("QualityAgent", "query_quality_record"),
                ("AnalyticsAgent", "query_report_summary"),
            ]:
                out, _ = run_tool(agent, tool, {})
                tool_calls.append(
                    ToolCallRecord(
                        tool_name=tool,
                        agent_name=agent,
                        module=agent.replace("Agent", ""),
                        action_type="read",
                        input={},
                        output=out,
                    )
                )
                steps.append(_step(agent, tool))
            r = tool_calls[-1].output
            answer = (
                "## 结论\n今日质量异常以 **异响、螺丝松动** 为主，建议加强 B 线末检工位巡检。\n\n"
                f"## 查询依据\n- TOP 缺陷：{', '.join(r['top_defects'])}\n"
                f"- 风险：{r['risk']}\n\n"
                "## 建议操作\n1. 末检工位加检扭矩\n2. 追溯 B-03 关联批次"
            )
            return ChatResponse(
                session_id=sid,
                answer=answer,
                intent=intent,
                status="Done",
                involved_modules=modules,
                selected_agents=agents,
                plan=plan,
                tool_calls=tool_calls,
                confirmations=confirmations,
                trace_id=trace_id,
                steps=steps,
            )

        # --- 默认：LLM 或静态引导 ---
        intent = "general_inquiry"
        agents = ["OverviewAgent"]
        modules = ["系统总览"]
        plan = ["MainAgent 理解请求", "调用大模型或 OverviewAgent 摘要"]
        llm_text = self._llm_reply(msg, model)
        if llm_text:
            steps.append(_step("LLM", f"provider={model or settings.default_provider}"))
            answer = llm_text
        else:
            out, _ = run_tool("OverviewAgent", "query_system_overview", {})
            tool_calls.append(
                ToolCallRecord(
                    tool_name="query_system_overview",
                    agent_name="OverviewAgent",
                    module="系统总览",
                    action_type="read",
                    input={},
                    output=out,
                )
            )
            answer = (
                "我是 **云枢小智**，可协调 9 大业务板块子 Agent 为您查询与操作 MES。\n\n"
                "**试试：**\n"
                "- 帮我查一下 WO20260708001 为什么延期\n"
                "- 帮我创建一个电机异响安灯\n"
                "- 查询今天系统总览\n"
                "- 生成今天的质量异常分析"
            )
        return ChatResponse(
            session_id=sid,
            answer=answer,
            intent=intent,
            status="Done",
            involved_modules=modules,
            selected_agents=agents,
            plan=plan,
            tool_calls=tool_calls,
            confirmations=confirmations,
            trace_id=trace_id,
            steps=steps,
        )


workflow = AgentWorkflow()
