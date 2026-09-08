"""Streaming, read-only quality-management Agent workflow."""

from __future__ import annotations

import re
import uuid
from collections.abc import Iterator
from typing import Any

from app.business_agents.base import BusinessAgentState
from app.business_agents.quality_management import QualityManagementAgent
from app.core.config import settings
from app.core.security import Principal
from app.llm.client import stream_chat_completion
from app.llm.local_qwen import LocalQwenError, local_qwen
from app.quality.llm_analysis import quality_llm_analysis


def is_quality_analysis_request(message: str) -> bool:
    normalized = re.sub(r"\s+", "", message or "")
    quality = any(token in normalized for token in ("质量", "质检", "不良", "缺陷", "检验", "IQC", "IPQC", "PQC", "OQC", "RQC"))
    analysis = any(token in normalized for token in ("今天", "今日", "情况", "分析", "概览", "积压", "风险", "趋势", "看看"))
    return quality and analysis


class QualityWorkflow:
    """Coordinates real facts and explanations; it has no write capability."""

    selected_agents = ["IntentRoutingAgent", "QualityManagementAgent"]
    plan = [
        "识别质量分析意图并检索已批准的质量 SOP",
        "通过质量 Agent 只读门面读取待检、趋势、缺陷与处置事实",
        "按确定性规则评估积压、通过率与缺陷风险",
        "调用本地 Qwen3-0.6B 对压缩事实包做短分析",
        "调用外部模型生成仅基于事实的最终质量综述",
    ]

    def stream(
        self,
        *,
        message: str,
        principal: Principal,
        provider_key: str | None,
        days: int = 7,
    ) -> Iterator[dict[str, Any]]:
        trace_id = str(uuid.uuid4())
        events: list[dict[str, Any]] = []
        model_analysis: dict[str, Any] = {}
        roles = list(principal.roles or ("TESTER",))
        intent_rag = quality_llm_analysis.rag_context("质量管理 质量检验 不良 缺陷 待检 处置", roles)

        intent_event = self._event(
            "quality-intent-routing", "IntentRoutingAgent", "质量分析意图识别",
            "正在使用外部模型解释本次质量分析请求；不会据此修改任何质量记录。",
            self._sources("外部意图识别 API", intent_rag),
        )
        intent_text, intent_meta = yield from self._stream_external_card(
            intent_event,
            quality_llm_analysis.intent_messages(message, intent_rag),
            provider_key,
            fallback="已识别为质量管理分析请求。接下来将只读查询今天相关的待检任务、检验趋势、缺陷和处置事实，再给出分析建议。",
            prompt_version=quality_llm_analysis.intent_prompt_version,
        )
        events.append(intent_event)
        model_analysis["intent"] = {**intent_meta, "text": intent_text, "rag": intent_rag}

        agent = QualityManagementAgent()
        try:
            facts_envelope = agent.run(BusinessAgentState(principal=principal, trace_id=trace_id, params={"days": days}))
            overview = facts_envelope["facts"]["quality_overview"]
        except Exception as exc:
            failure = self._event(
                "quality-facts", "QualityManagementAgent", "实时质量事实读取失败",
                "质量只读门面当前不可用，已停止模型分析，未生成任何质量结论。",
                ["MES Agent 质量只读门面"], "blocked",
            )
            events.append(failure)
            yield {"type": "node", "content": self._card_text(failure), "workflow_event": failure, "agent_name": failure["agent"]}
            yield self._done(trace_id, events, model_analysis, "质量事实读取失败，请稍后重试。", "Error")
            return

        facts_event = self._event(
            "quality-facts", "QualityManagementAgent", "实时质量事实采集",
            self._facts_detail(overview), ["MES Agent 质量只读门面", "fan_mes 质量业务表"],
        )
        events.append(facts_event)
        yield {"type": "node", "content": self._card_text(facts_event), "workflow_event": facts_event, "agent_name": facts_event["agent"]}

        risk = self._risk(overview)
        risk_event = self._event(
            "quality-risk-rules", "QualityManagementAgent", "质量风险规则检核",
            risk["summary"], ["质量风险确定性规则", "MES Agent 质量只读门面"],
        )
        events.append(risk_event)
        yield {"type": "node", "content": self._card_text(risk_event), "workflow_event": risk_event, "agent_name": risk_event["agent"]}

        local_event = self._event(
            "quality-local-qwen", "QualityManagementAgent", "本地 Qwen3-0.6B 质量微分析",
            "正在基于压缩后的真实质量事实进行本地模型检核。", ["本地 Qwen3-0.6B", "MES Agent 质量只读门面"],
        )
        local_text, local_meta = yield from self._stream_local_card(local_event, overview, risk)
        events.append(local_event)
        model_analysis["local_qwen"] = local_meta | {"text": local_text}

        final_rag = quality_llm_analysis.rag_context("质量检验 待检 缺陷 不良 处置 质量风险", roles)
        final_facts = self._external_facts(overview, risk, local_text)
        final_event = self._event(
            "quality-final-summary", "MainAgent", "质量总体分析与风险建议",
            "正在调用外部模型，对已经完成的只读质量事实与本地检核结果进行最终总结。",
            self._sources("外部质量总结 API", final_rag),
        )
        final_text, final_meta = yield from self._stream_external_card(
            final_event,
            quality_llm_analysis.final_messages(final_facts, risk, final_rag),
            provider_key,
            fallback=self._fallback_final(final_facts, risk),
            prompt_version=quality_llm_analysis.final_prompt_version,
            final=True,
        )
        events.append(final_event)
        model_analysis["final"] = {**final_meta, "rag": final_rag}
        yield self._done(trace_id, events, model_analysis, final_text, "Done")

    def _stream_external_card(
        self,
        event: dict[str, Any],
        messages: list[dict[str, str]],
        provider_key: str | None,
        *,
        fallback: str,
        prompt_version: str,
        final: bool = False,
    ) -> Iterator[Any]:
        provider = quality_llm_analysis.provider(provider_key)
        if not provider:
            event["summary"] = fallback
            event["sources"] = ["质量规则回退"]
            payload_type = "final" if final else "node"
            yield {"type": payload_type, "content": fallback, "workflow_event": event, "agent_name": event["agent"]}
            return fallback, {"source": "rule_fallback", "provider_label": "质量规则回退", "prompt_version": prompt_version}
        key, label = provider
        yield {"type": "node_start", "node_id": event["event_id"], "workflow_event": event, "agent_name": event["agent"]}
        chunks: list[str] = []
        try:
            for delta in stream_chat_completion(key, messages, timeout_seconds=55.0):
                chunks.append(delta)
                yield {"type": "final_delta" if final else "node_delta", "node_id": event["event_id"], "content": delta, "agent_name": event["agent"]}
            text = self._clean("".join(chunks))
            if not text:
                raise RuntimeError("empty external-model answer")
            event["summary"] = text
            event["sources"] = [label, *event["sources"]]
            if final:
                yield {"type": "final_complete", "content": text, "workflow_event": event, "agent_name": event["agent"]}
            else:
                yield {"type": "node_complete", "node_id": event["event_id"], "content": text, "workflow_event": event, "agent_name": event["agent"]}
            return text, {"source": "llm_api", "provider_key": key, "provider_label": label, "prompt_version": prompt_version}
        except Exception:
            event["summary"] = fallback
            event["sources"] = ["质量规则回退", *event["sources"]]
            if final:
                yield {"type": "final_complete", "content": fallback, "workflow_event": event, "agent_name": event["agent"]}
            else:
                yield {"type": "node_complete", "node_id": event["event_id"], "content": fallback, "workflow_event": event, "agent_name": event["agent"]}
            return fallback, {"source": "rule_fallback", "provider_label": "质量规则回退", "prompt_version": prompt_version}

    def _stream_local_card(self, event: dict[str, Any], overview: dict[str, Any], risk: dict[str, Any]) -> Iterator[Any]:
        facts = self._local_facts(overview, risk)
        prompt = (
            "你是 MES 本地质量分析助手。只能依据给出的实时事实，不能编造检验结果或处理完成情况。"
            "使用三行纯中文：待检与节奏、缺陷与风险、最优先核查项。不要 Markdown。\n事实包：" + str(facts)
        )
        prompt += "\n请输出五行：待检队列、完成与合格率、缺陷等级与TOP、风险依据、优先核查动作。每行必须引用事实包中的数字或明确说明无对应数据。"
        yield {"type": "node_start", "node_id": event["event_id"], "workflow_event": event, "agent_name": event["agent"]}
        chunks: list[str] = []
        try:
            result_meta: dict[str, Any] = {}
            for item in local_qwen.stream_generate([
                {"role": "system", "content": "你必须遵守事实包边界，只输出简洁中文质量分析。"},
                {"role": "user", "content": prompt},
            ]):
                if item.delta:
                    chunks.append(item.delta)
                    yield {"type": "node_delta", "node_id": event["event_id"], "content": item.delta, "agent_name": event["agent"]}
                if item.result:
                    result_meta = {"model": item.result.model, "input_tokens": item.result.input_tokens, "output_tokens": item.result.output_tokens}
            text = self._clean("".join(chunks))
            if not text:
                raise LocalQwenError("empty local answer")
            event["summary"] = text
            yield {"type": "node_complete", "node_id": event["event_id"], "content": text, "workflow_event": event, "agent_name": event["agent"]}
            return text, {"source": "local_qwen", **result_meta}
        except Exception:
            text = self._fallback_local(facts, risk)
            event["summary"] = text
            event["sources"] = ["质量规则回退", "MES Agent 质量只读门面"]
            yield {"type": "node_complete", "node_id": event["event_id"], "content": text, "workflow_event": event, "agent_name": event["agent"]}
            return text, {"source": "rule_fallback", "model": local_qwen.model_name}

    @staticmethod
    def _event(event_id: str, agent: str, title: str, summary: str, sources: list[str], status: str = "completed") -> dict[str, Any]:
        return {"event_id": event_id, "agent": agent, "status": status, "title": title, "summary": summary, "sources": sources}

    @staticmethod
    def _sources(api_source: str, rag: dict[str, Any]) -> list[str]:
        titles = [str(item.get("title") or "") for item in rag.get("citations") or []]
        return [api_source, *( ["Qdrant RAG：" + "、".join(dict.fromkeys(t for t in titles if t))] if titles else [])]

    @staticmethod
    def _number(value: Any) -> float:
        try:
            return float(value or 0)
        except (TypeError, ValueError):
            return 0.0

    def _risk(self, overview: dict[str, Any]) -> dict[str, Any]:
        summary = overview.get("summary") or {}
        defects = overview.get("defectByLevel") or []
        by_level = {str(item.get("level") or "").upper(): self._number(item.get("cnt")) for item in defects if isinstance(item, dict)}
        pending = self._number(summary.get("pendingCount"))
        pass_rate = self._number(summary.get("passRate"))
        disposition = self._number(summary.get("pendingDisposition"))
        reasons: list[str] = []
        level = "LOW"
        if by_level.get("CR", 0) > 0:
            level = "HIGH"; reasons.append(f"近 {overview.get('windowDays', 7)} 天存在 {int(by_level['CR'])} 件 CR 级缺陷记录")
        if pending >= 10:
            level = "HIGH"; reasons.append(f"待检任务 {int(pending)} 条，达到积压预警阈值")
        if pass_rate and pass_rate < 95:
            level = "HIGH" if pass_rate < 90 else max(level, "MEDIUM", key=("LOW", "MEDIUM", "HIGH").index)
            reasons.append(f"累计合格率 {pass_rate:.1f}% 低于 95%")
        if by_level.get("MAJ", 0) > 0 or disposition > 0:
            if level == "LOW": level = "MEDIUM"
            if by_level.get("MAJ", 0) > 0: reasons.append(f"近 {overview.get('windowDays', 7)} 天存在 {int(by_level['MAJ'])} 件 MAJ 级缺陷记录")
            if disposition > 0: reasons.append(f"有 {int(disposition)} 条待处置质量记录")
        if not reasons:
            reasons.append("当前质量事实包未触发积压、合格率或缺陷等级预警规则")
        return {"level": level, "reasons": reasons, "summary": f"确定性规则评估：{level} 风险。" + "；".join(reasons) + "。"}

    def _facts_detail(self, overview: dict[str, Any]) -> str:
        s = overview.get("summary") or {}
        trend = [item for item in (overview.get("trend") or []) if isinstance(item, dict)]
        latest = trend[-1] if trend else {}
        defects = [item for item in (overview.get("defectTop") or []) if isinstance(item, dict)]
        defect_text = "、".join(
            f"{item.get('defectName') or '未命名缺陷'} {int(self._number(item.get('cnt')))}件/{item.get('defectLevel') or '未分级'}"
            for item in defects[:3]
        ) or "当前数据未显示缺陷 TOP 记录"
        pending_rows = [item for item in (overview.get("pendingTasks") or []) if isinstance(item, dict)]
        queue_by_type: dict[str, int] = {}
        for item in pending_rows:
            key = str(item.get("qcType") or "未分类")
            queue_by_type[key] = queue_by_type.get(key, 0) + 1
        queue_text = "、".join(f"{key} {value}条" for key, value in queue_by_type.items()) or "当前数据未显示待检明细"
        return (
            f"质量事实窗口：最近 {overview.get('windowDays', 7)} 天。\n"
            f"待检与完成：待检 {int(self._number(s.get('pendingCount')))} 条（{queue_text}）；今日完成 {int(self._number(s.get('todayFinished')))} 条。\n"
            f"质量结果：累计合格率 {self._number(s.get('passRate')):.1f}%，缺陷批次 {int(self._number(s.get('defectBatchCount')))}，待处置 {int(self._number(s.get('pendingDisposition')))} 条。\n"
            f"最新趋势：{latest.get('dayLabel') or '当前无趋势日期'} 共 {int(self._number(latest.get('total')))} 条，合格率 {self._number(latest.get('passRate')):.1f}%。\n"
            f"缺陷关注：{defect_text}。"
        )

    def _facts_summary(self, overview: dict[str, Any]) -> str:
        s = overview.get("summary") or {}
        return (
            f"已读取最近 {overview.get('windowDays', 7)} 天质量事实：待检 {int(self._number(s.get('pendingCount')))} 条，"
            f"今日完成 {int(self._number(s.get('todayFinished')))} 条，累计合格率 {self._number(s.get('passRate')):.1f}%，"
            f"缺陷批次 {int(self._number(s.get('defectBatchCount')))}，待处置 {int(self._number(s.get('pendingDisposition')))} 条。"
        )

    def _local_facts(self, overview: dict[str, Any], risk: dict[str, Any]) -> dict[str, Any]:
        s = overview.get("summary") or {}
        return {
            "window_days": overview.get("windowDays"), "pending_count": int(self._number(s.get("pendingCount"))),
            "today_finished": int(self._number(s.get("todayFinished"))), "pass_rate": self._number(s.get("passRate")),
            "defect_batches": int(self._number(s.get("defectBatchCount"))), "pending_disposition": int(self._number(s.get("pendingDisposition"))),
            "defect_by_level": overview.get("defectByLevel") or [], "top_defects": overview.get("defectTop") or [],
            "risk_level": risk["level"], "risk_reasons": risk["reasons"],
        }

    def _external_facts(self, overview: dict[str, Any], risk: dict[str, Any], local_text: str) -> dict[str, Any]:
        return {**self._local_facts(overview, risk), "trend": overview.get("trend") or [], "type_stats": overview.get("typeStats") or {}, "local_qwen_review": local_text}

    @staticmethod
    def _fallback_local(facts: dict[str, Any], risk: dict[str, Any]) -> str:
        return f"待检与节奏：当前待检 {facts['pending_count']} 条，今日完成 {facts['today_finished']} 条。\n缺陷与风险：风险等级为 {risk['level']}。\n最优先核查项：" + "；".join(risk["reasons"][:2])

    @staticmethod
    def _fallback_final(facts: dict[str, Any], risk: dict[str, Any]) -> str:
        return (
            f"今日质量概览：待检 {facts['pending_count']} 条，今日完成 {facts['today_finished']} 条，累计合格率 {facts['pass_rate']:.1f}%。\n"
            f"主要风险：{risk['level']}，" + "；".join(risk["reasons"]) + "。\n"
            "优先处理顺序：先核对待处置和高等级缺陷，再按待检队列推进检验。\n"
            "下一步建议：以上为只读分析，请由质量人员在质量管理页面复核后再执行任何处置操作。"
        )

    @staticmethod
    def _clean(text: str) -> str:
        text = re.sub(r"<think>.*?</think>", "", text, flags=re.I | re.S)
        text = re.sub(r"[*_`#]", "", text)
        return re.sub(r"\n{3,}", "\n\n", text).strip()

    @staticmethod
    def _card_text(event: dict[str, Any]) -> str:
        return f"调用结果 · {event['title']}\n{event['summary']}\n数据来源：{'、'.join(event['sources'])}\n节点状态：已完成"

    def _done(self, trace_id: str, events: list[dict[str, Any]], model_analysis: dict[str, Any], answer: str, status: str) -> dict[str, Any]:
        return {"type": "done", "answer": answer, "trace_id": trace_id, "status": status, "intent": "quality_daily_analysis", "agent_name": "MainAgent", "involved_modules": ["质量管理"], "selected_agents": self.selected_agents, "plan": self.plan, "tool_calls": [], "confirmations": [], "steps": [{"step": "QualityManagementAgent", "detail": "只读质量事实与模型解释完成"}], "workflow_events": events, "model_analysis": model_analysis}


quality_workflow = QualityWorkflow()
