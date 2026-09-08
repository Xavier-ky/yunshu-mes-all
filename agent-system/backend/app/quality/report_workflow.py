"""Streaming, read-only continuation workflow for visual quality reports."""

from __future__ import annotations

import json
import re
import uuid
from collections import Counter
from collections.abc import Iterator
from datetime import datetime
from typing import Any

from app.business_agents.base import BusinessAgentState
from app.business_agents.quality_management import QualityManagementAgent
from app.core.security import Principal
from app.llm.client import stream_chat_completion
from app.llm.local_qwen import LocalQwenError, local_qwen
from app.quality.llm_analysis import quality_llm_analysis
from app.quality.report_store import quality_report_store
from app.quality.workflow import quality_workflow


def is_quality_report_request(message: str) -> bool:
    normalized = re.sub(r"\s+", "", message or "").lower()
    quality_terms = ("质量", "质检", "不良", "缺陷", "检验", "iqc", "ipqc", "pqc", "oqc", "rqc")
    report_terms = ("报告", "报表", "日报", "周报", "导出", "整理")
    return any(term in normalized for term in quality_terms) and any(term in normalized for term in report_terms)


class QualityReportWorkflow:
    """Creates a report from a fresh, JWT-scoped MES fact snapshot only."""

    selected_agents = ["IntentRoutingAgent", "QualityManagementAgent", "QualityReportAgent"]
    plan = [
        "识别质量报告意图并确认统计窗口",
        "读取 MES 实时质量报告快照",
        "核验待检、合格率、缺陷和待处置风险",
        "调用本地 Qwen3-0.6B 解读质量细项",
        "逐段生成图、文、表质量报告并提供 Word 导出",
    ]

    def stream(
        self,
        *,
        message: str,
        principal: Principal,
        provider_key: str | None,
        session_id: int,
        user_id: int,
        agent_id: int,
        days: int = 7,
    ) -> Iterator[dict[str, Any]]:
        trace_id = str(uuid.uuid4())
        report_id = f"QAR-{uuid.uuid4().hex[:20].upper()}"
        events: list[dict[str, Any]] = []
        model_analysis: dict[str, Any] = {}
        roles = list(principal.roles or ("TESTER",))

        intent_rag = quality_llm_analysis.rag_context("质量管理 质量报告 检验 缺陷 处置", roles)
        intent_event = self._event(
            "quality-report-intent", "IntentRoutingAgent", "质量报告意图与范围确认",
            "正在确认报告范围；本次只生成基于实时质量快照的分析，不会修改任何检验或放行记录。",
            self._sources("外部报告意图识别 API", intent_rag),
        )
        intent_text, intent_meta = yield from self._stream_external_card(
            intent_event,
            self._intent_messages(message, intent_rag),
            provider_key,
            fallback="已识别为质量分析报告请求。将读取最近 7 天的真实检验趋势、待检、缺陷和待处置数据，生成仅供辅助决策的图文报告。",
            prompt_version="quality-report-intent-v1",
        )
        events.append(intent_event)
        model_analysis["intent"] = {**intent_meta, "text": intent_text, "rag": intent_rag}

        try:
            agent = QualityManagementAgent()
            report_snapshot = agent._client.read(  # bounded GET-only allowlist
                "agent_quality_report_snapshot", principal, trace_id, params={"days": max(1, min(days, 30))}
            )
        except Exception as exc:
            failure = self._event(
                "quality-report-facts", "QualityManagementAgent", "实时质量报告数据读取失败",
                "质量报告只读门面暂时不可用，报告已停止，未生成任何质量结论或导出文件。",
                ["MES Agent 质量报告只读门面"], "blocked",
            )
            events.append(failure)
            yield self._node(failure)
            yield self._done(trace_id, events, model_analysis, "实时质量报告数据读取失败，请稍后再试。", "Error")
            return

        risk = quality_workflow._risk(report_snapshot)
        facts_event = self._event(
            "quality-report-facts", "QualityManagementAgent", "实时质量报告快照采集",
            self._facts_detail(report_snapshot),
            ["MES Agent 质量报告只读门面", "fan_mes 质量业务表"],
        )
        events.append(facts_event)
        yield self._node(facts_event)

        risk_event = self._event(
            "quality-report-risk", "QualityReportAgent", "质量指标与风险核验",
            risk["summary"],
            ["确定性质量风险规则", "MES Agent 质量报告只读门面"],
        )
        events.append(risk_event)
        yield self._node(risk_event)

        local_event = self._event(
            "quality-report-local-qwen", "QualityReportAgent", "本地 Qwen3-0.6B 质量细项解读",
            "正在基于压缩后的真实质量指标解读趋势、缺陷优先级和待处置事项。",
            ["本地 Qwen3-0.6B", "MES Agent 质量报告只读门面"],
        )
        local_text, local_meta = yield from self._stream_local_card(local_event, report_snapshot, risk)
        events.append(local_event)
        model_analysis["local_qwen"] = {**local_meta, "text": local_text}

        charts, tables = self._visual_payload(report_snapshot)
        snapshot: dict[str, Any] = {
            "report_id": report_id,
            "title": "质量分析报告",
            "window_days": self._int(report_snapshot.get("windowDays"), 7),
            "queried_at": report_snapshot.get("queriedAt") or datetime.now().isoformat(),
            "source": report_snapshot.get("source") or "MES_AGENT_QUALITY_READ_FACADE",
            "facts": report_snapshot,
            "risk": risk,
            "charts": charts,
            "tables": tables,
            "sections": [],
            "model_analysis": model_analysis,
            "status": "GENERATING",
        }
        try:
            quality_report_store.create(
                report_id=report_id,
                session_id=session_id,
                user_id=user_id,
                agent_id=agent_id,
                window_days=snapshot["window_days"],
                queried_at=self._parse_time(snapshot["queried_at"]),
                snapshot=snapshot,
            )
        except Exception as exc:
            failure = self._event(
                "quality-report-storage", "QualityReportAgent", "质量报告快照保存失败",
                "报告数据快照无法安全保存，因此不会展示不可导出的临时报告。",
                ["Agent 报告审计存储"], "blocked",
            )
            events.append(failure)
            yield self._node(failure)
            yield self._done(trace_id, events, model_analysis, "质量报告快照保存失败，请稍后重试。", "Error")
            return

        yield {
            "type": "report_start",
            "report_id": report_id,
            "title": snapshot["title"],
            "window_days": snapshot["window_days"],
            "queried_at": snapshot["queried_at"],
            "source": snapshot["source"],
            "facts": {"summary": report_snapshot.get("summary") or {}},
        }
        overview_text = self._overview_section(report_snapshot, risk)
        snapshot["sections"].append({"id": "overview", "title": "执行摘要", "content": overview_text, "source": "deterministic_rules"})
        yield {"type": "report_section", "report_id": report_id, "section": "overview", "title": "执行摘要", "content": overview_text}
        for chart in charts:
            yield {"type": "report_chart", "report_id": report_id, "chart": chart}
        for table in tables:
            yield {"type": "report_table", "report_id": report_id, "table": table}

        # A report must become exportable from its real MES snapshot even when
        # an optional third-party model is slow or unavailable.  The factual
        # rule conclusion is therefore the report's durable baseline; model
        # output is an enrichment rather than a delivery prerequisite.
        final_rag = quality_llm_analysis.rag_context("质量检验 缺陷 不良 处置 质量风险", roles)
        final_event = self._event(
            "quality-report-final", "MainAgent", "质量报告结论与行动建议",
            "外部模型可在报告交付后补充解读；它不能改写任何实时指标。",
            self._sources("外部质量报告分析 API", final_rag),
        )
        fallback_final = self._fallback_final(report_snapshot, risk, local_text)
        final_event["summary"] = fallback_final
        events.append(final_event)
        model_analysis["final"] = {
            "source": "deterministic_rules", "provider_label": "质量报告规则结论",
            "prompt_version": "quality-report-final-v1", "rag": final_rag, "text": fallback_final,
        }
        snapshot["sections"].append({"id": "analysis", "title": "风险与行动建议", "content": fallback_final, "source": "deterministic_rules"})
        snapshot["model_analysis"] = model_analysis
        snapshot["status"] = "READY"
        quality_report_store.mark_ready(report_id, snapshot)
        yield {"type": "report_section", "report_id": report_id, "section": "analysis", "title": "风险与行动建议", "content": fallback_final}
        yield {
            "type": "report_final_complete", "report_id": report_id, "section": "analysis", "title": "风险与行动建议",
            "content": fallback_final, "workflow_event": final_event, "agent_name": final_event["agent"],
        }
        yield {
            "type": "report_export_ready",
            "report_id": report_id,
            "message": "报告已完成。是否需要导出 Word？",
        }
        enrichment_text, enrichment_meta = yield from self._stream_report_final(
            report_id, final_event, report_snapshot, risk, local_text, final_rag, provider_key,
            section="model_insight", title="外部模型补充解读", announce=False, emit_fallback=False, timeout_seconds=12.0,
        )
        model_analysis["final_enrichment"] = {**enrichment_meta, "rag": final_rag, "text": enrichment_text}
        if enrichment_meta.get("source") == "llm_api" and enrichment_text:
            snapshot["sections"].append({"id": "model_insight", "title": "外部模型补充解读", "content": enrichment_text, "source": "llm_api"})
            snapshot["model_analysis"] = model_analysis
            # Preserve EXPORTED if the user exported during the optional
            # enrichment; a later re-export then receives the richer snapshot.
            quality_report_store.mark_ready(report_id, snapshot)
        yield self._done(trace_id, events, model_analysis, "质量分析报告已完成，可按需导出 Word。", "Done", report_id=report_id)

    def _stream_report_final(
        self, report_id: str, event: dict[str, Any], facts: dict[str, Any], risk: dict[str, Any], local_text: str,
        rag: dict[str, Any], provider_key: str | None, *, section: str = "analysis", title: str = "风险与行动建议",
        announce: bool = True, emit_fallback: bool = True, timeout_seconds: float = 18.0,
    ) -> Iterator[Any]:
        provider = quality_llm_analysis.provider(provider_key)
        fallback = self._fallback_final(facts, risk, local_text)
        if not provider:
            event["summary"] = fallback
            event["sources"] = ["质量报告规则回退"]
            if emit_fallback:
                yield {"type": "report_section", "report_id": report_id, "section": section, "title": title, "content": fallback}
            return fallback, {"source": "rule_fallback", "provider_label": "质量报告规则回退", "prompt_version": "quality-report-final-v1"}
        key, label = provider
        if announce:
            yield {"type": "node_start", "node_id": event["event_id"], "workflow_event": event, "agent_name": event["agent"]}
        chunks: list[str] = []
        try:
            for delta in stream_chat_completion(key, self._final_messages(facts, risk, local_text, rag), timeout_seconds=timeout_seconds):
                chunks.append(delta)
                yield {"type": "report_section_delta", "report_id": report_id, "section": section, "title": title, "content": delta}
            text = self._clean("".join(chunks))
            if not text:
                raise RuntimeError("empty report answer")
            event["summary"] = text
            event["sources"] = [label, *event["sources"]]
            yield {"type": "report_final_complete", "report_id": report_id, "section": section, "title": title, "content": text, "workflow_event": event, "agent_name": event["agent"]}
            return text, {"source": "llm_api", "provider_key": key, "provider_label": label, "prompt_version": "quality-report-final-v1"}
        except Exception:
            event["summary"] = fallback
            event["sources"] = ["质量报告规则回退", *event["sources"]]
            if emit_fallback:
                yield {"type": "report_section", "report_id": report_id, "section": section, "title": title, "content": fallback}
                yield {"type": "report_final_complete", "report_id": report_id, "section": section, "title": title, "content": fallback, "workflow_event": event, "agent_name": event["agent"]}
            return fallback, {"source": "rule_fallback", "provider_label": "质量报告规则回退", "prompt_version": "quality-report-final-v1"}

    def _stream_external_card(self, event: dict[str, Any], messages: list[dict[str, str]], provider_key: str | None, *, fallback: str, prompt_version: str) -> Iterator[Any]:
        provider = quality_llm_analysis.provider(provider_key)
        if not provider:
            event["summary"] = fallback
            yield self._node(event, fallback)
            return fallback, {"source": "rule_fallback", "provider_label": "质量报告规则回退", "prompt_version": prompt_version}
        key, label = provider
        yield {"type": "node_start", "node_id": event["event_id"], "workflow_event": event, "agent_name": event["agent"]}
        chunks: list[str] = []
        try:
            for delta in stream_chat_completion(key, messages, timeout_seconds=55.0):
                chunks.append(delta)
                yield {"type": "node_delta", "node_id": event["event_id"], "content": delta, "agent_name": event["agent"]}
            text = self._clean("".join(chunks))
            if not text:
                raise RuntimeError("empty external report intent")
            event["summary"] = text
            event["sources"] = [label, *event["sources"]]
            yield {"type": "node_complete", "node_id": event["event_id"], "content": text, "workflow_event": event, "agent_name": event["agent"]}
            return text, {"source": "llm_api", "provider_key": key, "provider_label": label, "prompt_version": prompt_version}
        except Exception:
            event["summary"] = fallback
            yield {"type": "node_complete", "node_id": event["event_id"], "content": fallback, "workflow_event": event, "agent_name": event["agent"]}
            return fallback, {"source": "rule_fallback", "provider_label": "质量报告规则回退", "prompt_version": prompt_version}

    def _stream_local_card(self, event: dict[str, Any], facts: dict[str, Any], risk: dict[str, Any]) -> Iterator[Any]:
        compact = self._compact_facts(facts)
        prompt = (
            "你是 MES 本地质量报告分析助手。只能引用下列事实包，不能虚构检验结果或处理完成情况。"
            "请用四行纯中文说明：趋势、缺陷优先级、待处置事项、最先核查动作。每行至少引用一个事实数字。\n事实包："
            + json.dumps(compact, ensure_ascii=False, separators=(",", ":"))
            + "\n确定性风险：" + json.dumps(risk, ensure_ascii=False, separators=(",", ":"))
        )
        yield {"type": "node_start", "node_id": event["event_id"], "workflow_event": event, "agent_name": event["agent"]}
        chunks: list[str] = []
        try:
            meta: dict[str, Any] = {}
            for item in local_qwen.stream_generate([
                {"role": "system", "content": "只输出简洁的中文质量解读，不要 Markdown。"},
                {"role": "user", "content": prompt},
            ]):
                if item.delta:
                    chunks.append(item.delta)
                    yield {"type": "node_delta", "node_id": event["event_id"], "content": item.delta, "agent_name": event["agent"]}
                if item.result:
                    meta = {"model": item.result.model, "input_tokens": item.result.input_tokens, "output_tokens": item.result.output_tokens}
            text = self._clean("".join(chunks))
            if not text:
                raise LocalQwenError("empty local report answer")
            event["summary"] = text
            yield {"type": "node_complete", "node_id": event["event_id"], "content": text, "workflow_event": event, "agent_name": event["agent"]}
            return text, {"source": "local_qwen", **meta}
        except Exception:
            text = self._fallback_local(facts, risk)
            event["summary"] = text
            yield {"type": "node_complete", "node_id": event["event_id"], "content": text, "workflow_event": event, "agent_name": event["agent"]}
            return text, {"source": "rule_fallback", "model": local_qwen.model_name}

    @staticmethod
    def _event(event_id: str, agent: str, title: str, summary: str, sources: list[str], status: str = "completed") -> dict[str, Any]:
        return {"event_id": event_id, "agent": agent, "status": status, "title": title, "summary": summary, "sources": sources}

    @staticmethod
    def _node(event: dict[str, Any], content: str | None = None) -> dict[str, Any]:
        text = content or f"调用结果 · {event['title']}\n{event['summary']}\n数据来源：{'；'.join(event['sources'])}\n节点状态：已完成"
        return {"type": "node", "content": text, "workflow_event": event, "agent_name": event["agent"]}

    @staticmethod
    def _sources(api_source: str, rag: dict[str, Any]) -> list[str]:
        titles = [str(item.get("title") or "") for item in rag.get("citations") or []]
        return [api_source, *(["Qdrant RAG：" + "；".join(dict.fromkeys(x for x in titles if x))] if any(titles) else [])]

    @staticmethod
    def _int(value: Any, default: int = 0) -> int:
        try:
            return int(float(value or 0))
        except (TypeError, ValueError):
            return default

    @staticmethod
    def _float(value: Any) -> float:
        try:
            return float(value or 0)
        except (TypeError, ValueError):
            return 0.0

    @staticmethod
    def _clean(text: str) -> str:
        return re.sub(r"\n{3,}", "\n\n", re.sub(r"(?:\*\*|__|`|#+\s*)", "", text or "")).strip()

    @staticmethod
    def _parse_time(value: Any) -> datetime | None:
        if not value:
            return None
        try:
            return datetime.fromisoformat(str(value).replace("Z", "+00:00")).replace(tzinfo=None)
        except ValueError:
            return None

    def _facts_detail(self, facts: dict[str, Any]) -> str:
        summary = facts.get("summary") or {}
        return (
            f"报告统计窗口：最近 {self._int(facts.get('windowDays'), 7)} 天。\n"
            f"待检 {self._int(summary.get('pendingCount'))} 条，今日完成 {self._int(summary.get('todayFinished'))} 条，"
            f"累计合格率 {self._float(summary.get('passRate')):.1f}%。\n"
            f"缺陷批次 {self._int(summary.get('defectBatchCount'))}，待处置 {self._int(summary.get('pendingDisposition'))} 条。\n"
            f"数据查询时间：{facts.get('queriedAt') or '系统当前时间'}。"
        )

    def _compact_facts(self, facts: dict[str, Any]) -> dict[str, Any]:
        return {
            "summary": facts.get("summary") or {},
            "trend": list(facts.get("trend") or [])[-7:],
            "defectTop": list(facts.get("defectTop") or [])[:5],
            "defectByLevel": list(facts.get("defectByLevel") or []),
            "pendingTasks": list(facts.get("pendingTasks") or [])[:12],
            "pendingDispositionRows": list(facts.get("pendingDispositionRows") or [])[:8],
        }

    def _overview_section(self, facts: dict[str, Any], risk: dict[str, Any]) -> str:
        s = facts.get("summary") or {}
        return (
            f"本报告基于最近 {self._int(facts.get('windowDays'), 7)} 天的 MES 实时质量快照生成。"
            f"当前待检 {self._int(s.get('pendingCount'))} 条，累计合格率 {self._float(s.get('passRate')):.1f}%，"
            f"待处置 {self._int(s.get('pendingDisposition'))} 条；确定性规则评估为 {risk.get('level', 'LOW')} 风险。"
        )

    def _visual_payload(self, facts: dict[str, Any]) -> tuple[list[dict[str, Any]], list[dict[str, Any]]]:
        trend = [item for item in facts.get("trend") or [] if isinstance(item, dict)]
        defects = [item for item in facts.get("defectTop") or [] if isinstance(item, dict)]
        levels = [item for item in facts.get("defectByLevel") or [] if isinstance(item, dict)]
        pending = [item for item in facts.get("pendingTasks") or [] if isinstance(item, dict)]
        queue = Counter(str(item.get("qcType") or "未分类") for item in pending)
        severity_colors = {"CR": "#e66a78", "MAJ": "#e9ae57", "MIN": "#7ca8e8"}
        charts = [
            {
                "id": "quality-trend", "title": "检验量与合格率趋势", "kind": "trend",
                "labels": [str(x.get("dayLabel") or "-") for x in trend],
                "series": {"检验量": [self._int(x.get("total")) for x in trend], "合格率": [self._float(x.get("passRate")) for x in trend]},
            },
            {
                "id": "defect-top", "title": "缺陷 TOP", "kind": "bar",
                "labels": [str(x.get("defectName") or "未命名缺陷") for x in defects],
                "values": [self._int(x.get("cnt")) for x in defects],
                "colors": [severity_colors.get(str(x.get("defectLevel") or "").upper(), "#8faee9") for x in defects],
            },
            {
                "id": "defect-level", "title": "缺陷严重度分布", "kind": "pie",
                "items": [{"name": str(x.get("level") or "MIN"), "value": self._int(x.get("cnt")), "color": severity_colors.get(str(x.get("level") or "").upper(), "#8faee9")} for x in levels],
            },
            {
                "id": "quality-queue", "title": "待检队列分布", "kind": "bar",
                "labels": list(queue.keys()), "values": list(queue.values()), "colors": ["#87a7df"] * len(queue),
            },
        ]
        tables = [
            {"id": "defect-table", "title": "缺陷 TOP 明细", "columns": ["排名", "缺陷名称", "数量", "等级"],
             "rows": [[index + 1, item.get("defectName") or "未命名缺陷", self._int(item.get("cnt")), item.get("defectLevel") or "未分级"] for index, item in enumerate(defects)]},
            {"id": "pending-table", "title": "待处置与近期检验", "columns": ["类型", "单号", "对象", "结果", "状态", "时间"],
             "rows": [[item.get("qcType") or "-", item.get("docCode") or "-", item.get("itemName") or "-", item.get("checkResult") or "-", item.get("status") or "-", str(item.get("inspectDate") or "-")] for item in (facts.get("pendingDispositionRows") or facts.get("recentFinished") or [])[:12]]},
        ]
        return charts, tables

    def _intent_messages(self, message: str, rag: dict[str, Any]) -> list[dict[str, str]]:
        return [
            {"role": "system", "content": "你是 MES 质量报告 Agent 的意图解释器。不得虚构实时数据，不得执行任何操作，只输出纯中文。"},
            {"role": "user", "content": "用户希望：" + message + "\n请用不超过三行说明将生成实时质量报告、使用哪些数据类别，以及报告仅辅助决策。SOP：" + quality_llm_analysis.rag_prompt(rag)},
        ]

    def _final_messages(self, facts: dict[str, Any], risk: dict[str, Any], local_text: str, rag: dict[str, Any]) -> list[dict[str, str]]:
        compact = json.dumps(self._compact_facts(facts), ensure_ascii=False, separators=(",", ":"))
        return [
            {"role": "system", "content": "你是 MES 质量分析报告撰写助手。只能使用事实包和规则结果。数字必须原样引用；无法判断时说数据不足；不得声称已处理、关闭、放行或修改任何记录。输出纯中文，不要 Markdown。"},
            {"role": "user", "content": "请为质量报告写四个连续段落：趋势解读、缺陷与风险、待处置优先级、建议动作与人工复核边界。每段至少引用一个事实数字。\n事实包：" + compact + "\n风险规则：" + json.dumps(risk, ensure_ascii=False) + "\n本地解读：" + local_text + "\nSOP（不可替代事实）：" + quality_llm_analysis.rag_prompt(rag)},
        ]

    def _fallback_local(self, facts: dict[str, Any], risk: dict[str, Any]) -> str:
        s = facts.get("summary") or {}
        return (f"趋势：当前累计合格率 {self._float(s.get('passRate')):.1f}%，需结合趋势图人工复核。\n"
                f"缺陷：最近窗口的缺陷批次为 {self._int(s.get('defectBatchCount'))}。\n"
                f"待处置：当前待检 {self._int(s.get('pendingCount'))} 条、待处置 {self._int(s.get('pendingDisposition'))} 条。\n"
                f"优先动作：按 {risk.get('level', 'LOW')} 风险规则先核查 CR/MAJ 缺陷与待处置单据。")

    def _fallback_final(self, facts: dict[str, Any], risk: dict[str, Any], local_text: str) -> str:
        s = facts.get("summary") or {}
        return (f"趋势解读：最近 {self._int(facts.get('windowDays'), 7)} 天累计合格率为 {self._float(s.get('passRate')):.1f}%，请结合图表核对每日变化。\n\n"
                f"缺陷与风险：当前缺陷批次 {self._int(s.get('defectBatchCount'))}，规则评估为 {risk.get('level', 'LOW')} 风险。\n\n"
                f"待处置优先级：待检 {self._int(s.get('pendingCount'))} 条、待处置 {self._int(s.get('pendingDisposition'))} 条，应先核验高等级缺陷与临近交付任务。\n\n"
                f"建议动作与边界：{local_text}\n以上为实时快照辅助分析，不代表已完成处置或质量放行。")

    @staticmethod
    def _done(trace_id: str, events: list[dict[str, Any]], model_analysis: dict[str, Any], answer: str, status: str, *, report_id: str | None = None) -> dict[str, Any]:
        return {"type": "done", "trace_id": trace_id, "status": status, "answer": answer, "workflow_events": events, "model_analysis": model_analysis, "report_id": report_id}


quality_report_workflow = QualityReportWorkflow()
