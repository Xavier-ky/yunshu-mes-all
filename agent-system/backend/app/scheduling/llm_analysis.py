"""Bounded external-model analysis for the scheduling Agent.

The MES remains authoritative for intent guards and every write.  This module
uses an OpenAI-compatible provider only to explain a user request or a
completed, already-verified scheduling result.
"""

from __future__ import annotations

import json
import re
from typing import Any

from app.core.config import settings
from app.llm.client import chat_completion
from app.rag.retrieval import search_knowledge


class SchedulingLlmAnalysisService:
    intent_prompt_version = "schedule-intent-v1"
    final_prompt_version = "schedule-final-summary-v1"

    @staticmethod
    def _rag_context(query: str, roles: list[str] | None) -> dict[str, Any]:
        """Read approved SOP knowledge without making RAG a business dependency.

        Retrieved passages are explanation-only context for the external model.
        Every MES decision and write remains in the bounded scheduling services.
        """
        try:
            hits = search_knowledge(query, roles=list(roles or ["PROD_SUPERVISOR"]), limit=3)
        except Exception:
            return {"source": "unavailable", "citations": [], "excerpts": []}

        excerpts: list[dict[str, str]] = []
        citations: list[dict[str, str | None]] = []
        for hit in hits:
            citation = hit.get("citation") or {}
            title = str(citation.get("title") or citation.get("document_id") or "MES 规则")
            citations.append({
                "document_id": citation.get("document_id"),
                "title": title,
                "anchor": citation.get("anchor"),
            })
            content = " ".join(str(hit.get("content") or "").split())
            if content:
                excerpts.append({"title": title, "content": content[:650]})
        return {"source": "QDRANT_RAG", "citations": citations, "excerpts": excerpts}

    @staticmethod
    def _rag_prompt_context(rag: dict[str, Any]) -> str:
        excerpts = rag.get("excerpts") or []
        if not excerpts:
            return "无可用规则片段；仅依据用户原话或事实包。"
        return "\n".join(f"[{item['title']}] {item['content']}" for item in excerpts)

    @staticmethod
    def _citation_titles(rag: dict[str, Any]) -> str:
        titles = [str(item.get("title") or "") for item in rag.get("citations") or []]
        return "、".join(dict.fromkeys(title for title in titles if title))

    @staticmethod
    def _provider(provider_key: str | None) -> tuple[str, str] | None:
        key = provider_key or settings.default_provider
        provider = settings.get_provider(key)
        if settings.mock_llm or not provider or not provider.get("api_key"):
            return None
        return key, str(provider.get("label") or key)

    @staticmethod
    def _json_object(text: str) -> dict[str, Any] | None:
        candidate = text.strip()
        candidate = re.sub(r"^```(?:json)?\s*|\s*```$", "", candidate, flags=re.IGNORECASE)
        try:
            value = json.loads(candidate)
            return value if isinstance(value, dict) else None
        except (TypeError, ValueError):
            match = re.search(r"\{.*\}", candidate, re.DOTALL)
            if not match:
                return None
            try:
                value = json.loads(match.group(0))
                return value if isinstance(value, dict) else None
            except (TypeError, ValueError):
                return None

    def analyze_intent(
        self, message: str, provider_key: str | None, roles: list[str] | None = None
    ) -> dict[str, Any]:
        """Call the selected model once, then validate its bounded JSON result."""
        provider = self._provider(provider_key)
        rag = self._rag_context(
            "新订单 智能排产 订单接收 订单确认 工单生成 BOM 齐套 甘特排产 派工 生产领料",
            roles,
        )
        fallback = {
            "intent": "scheduling_order_intake",
            "confidence": 0.0,
            "next_action": "collect_order_information",
            "reason": "模型 API 未返回可验证结果，已由排产规则确认该请求需要收集订单信息。",
            "source": "rule_fallback",
            "provider_key": provider_key or settings.default_provider,
            "provider_label": "规则校验",
            "prompt_version": self.intent_prompt_version,
            "rag": rag,
        }
        if not provider:
            return fallback
        key, label = provider
        prompt = (
            "你是制造 MES 的意图识别器。仅分析用户原话，不得创建订单，不得假设产品、数量或日期。"
            "只返回一个 JSON 对象，不要 Markdown、不要代码块。\n"
            "允许 intent 只有 scheduling_order_intake 或 other；"
            "next_action 只有 collect_order_information 或 ask_clarification。\n"
            "格式：{\"intent\":\"...\",\"confidence\":0.0,\"next_action\":\"...\",\"reason\":\"不超过45字\"}\n"
            f"用户原话：{message}\n"
            "以下是仅供流程理解的已批准 SOP 规则片段，不是订单事实，不能据此虚构订单字段：\n"
            f"{self._rag_prompt_context(rag)}"
        )
        try:
            raw = chat_completion(
                key,
                [{"role": "system", "content": "严格遵守输出 JSON 的格式约束。"}, {"role": "user", "content": prompt}],
                timeout_seconds=35.0,
            )
            parsed = self._json_object(raw)
            if not parsed:
                return {**fallback, "reason": "模型 API 返回格式无法验证，已由排产规则接管。"}
            intent = str(parsed.get("intent") or "")
            next_action = str(parsed.get("next_action") or "")
            try:
                confidence = float(parsed.get("confidence"))
            except (TypeError, ValueError):
                confidence = 0.0
            if intent != "scheduling_order_intake" or next_action != "collect_order_information":
                return {**fallback, "reason": "模型 API 结果未通过排产路由白名单校验，已由排产规则接管。"}
            return {
                "intent": intent,
                "confidence": max(0.0, min(1.0, confidence)),
                "next_action": next_action,
                "reason": str(parsed.get("reason") or "已识别为新订单智能排产需求。").strip()[:120],
                "source": "llm_api",
                "provider_key": key,
                "provider_label": label,
                "prompt_version": self.intent_prompt_version,
                "rag": rag,
            }
        except Exception:
            return {**fallback, "reason": "模型 API 调用失败，已由排产规则接管。"}

    def summarize_completed_schedule(
        self, facts: dict[str, Any], provider_key: str | None, roles: list[str] | None = None
    ) -> dict[str, Any]:
        """Ask the selected model for a concise, evidence-bounded final explanation."""
        provider = self._provider(provider_key)
        rag = self._rag_context(
            "智能排产 齐套 排产 派工 生产领料 生命周期 "
            f"{facts.get('lifecycle_status')} 风险 {facts.get('risk_level')}",
            roles,
        )
        fallback_text = self._fallback_final(facts)
        fallback = {
            "text": fallback_text,
            "source": "rule_fallback",
            "provider_key": provider_key or settings.default_provider,
            "provider_label": "规则总结",
            "prompt_version": self.final_prompt_version,
            "rag": rag,
        }
        if not provider:
            return fallback
        key, label = provider
        facts_json = json.dumps(facts, ensure_ascii=False, separators=(",", ":"))
        prompt = (
            "你是 MES 排产结果解读助手。只可使用下面 JSON 事实包中的字段；不允许编造库存、设备、人员、日期或状态，"
            "也不允许建议修改已经完成的数据库操作。输出纯中文文本，不要 Markdown。严格按五行输出：\n"
            "排产结论：...\n已完成事项：...\n风险建议：...\n现场下一步：...\n规则依据：...\n"
            "若 shortfall_count 大于 0、risk_level 不是 LOW、issue_ready_for_shop_floor 为 false，必须明确风险或阻断；"
            "否则说明当前没有来自事实包的阻断风险。\n事实包："
            f"{facts_json}\n"
            "以下为只读检索到的已批准 SOP 规则片段，仅可作为规则依据。若与事实包冲突，必须以事实包为准；不得将规则片段当作实时数据。\n"
            f"{self._rag_prompt_context(rag)}"
        )
        try:
            text = chat_completion(
                key,
                [{"role": "system", "content": "以事实包为准，SOP 仅作规则依据；输出五行中文业务摘要。"}, {"role": "user", "content": prompt}],
                timeout_seconds=45.0,
            ).strip()
            if not text:
                return {**fallback, "text": "模型 API 返回为空，以下为规则总结。\n" + fallback_text}
            return {
                "text": text[:1600],
                "source": "llm_api",
                "provider_key": key,
                "provider_label": label,
                "prompt_version": self.final_prompt_version,
                "rag": rag,
            }
        except Exception:
            return {**fallback, "text": "模型 API 调用失败，以下为规则总结。\n" + fallback_text}

    @staticmethod
    def _fallback_final(facts: dict[str, Any]) -> str:
        risk = facts.get("risk_level") or "UNKNOWN"
        ready = bool(facts.get("issue_ready_for_shop_floor"))
        risk_text = "当前事实包未显示阻断风险。" if risk == "LOW" and ready else "请关注物料、排产或领料节点的阻断信息。"
        return (
            f"排产结论：订单 {facts.get('order_no')} 已生成工单 {facts.get('work_order_no')}。\n"
            f"已完成事项：生产任务 {facts.get('production_task_count', 0)} 道，派工 {facts.get('dispatch_task_count', 0)} 条。\n"
            f"风险建议：{risk_text}\n"
            f"现场下一步：工单当前状态为 {facts.get('lifecycle_status') or '未知'}。"
        )


scheduling_llm_analysis_service = SchedulingLlmAnalysisService()
