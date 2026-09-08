"""Strictly bounded model prompts for the quality-management workflow."""

from __future__ import annotations

from typing import Any

from app.core.config import settings
from app.rag.retrieval import search_knowledge


class QualityLlmAnalysis:
    intent_prompt_version = "quality-intent-v1"
    final_prompt_version = "quality-final-summary-v1"

    @staticmethod
    def provider(provider_key: str | None) -> tuple[str, str] | None:
        key = provider_key or settings.default_provider
        config = settings.get_provider(key)
        if settings.mock_llm or not config or not config.get("api_key"):
            return None
        return key, str(config.get("label") or key)

    @staticmethod
    def rag_context(query: str, roles: list[str]) -> dict[str, Any]:
        try:
            hits = search_knowledge(query, roles=roles, limit=3)
        except Exception:
            return {"source": "unavailable", "citations": [], "excerpts": []}
        citations: list[dict[str, str | None]] = []
        excerpts: list[dict[str, str]] = []
        for hit in hits:
            citation = hit.get("citation") or {}
            title = str(citation.get("title") or citation.get("document_id") or "MES 质量规则")
            citations.append({"document_id": citation.get("document_id"), "title": title, "anchor": citation.get("anchor")})
            content = " ".join(str(hit.get("content") or "").split())
            if content:
                excerpts.append({"title": title, "content": content[:520]})
        return {"source": "QDRANT_RAG", "citations": citations, "excerpts": excerpts}

    @staticmethod
    def rag_prompt(rag: dict[str, Any]) -> str:
        excerpts = rag.get("excerpts") or []
        if not excerpts:
            return "没有可用 SOP 片段；只能根据用户原话或质量事实包回答。"
        return "\n".join(f"[{item['title']}] {item['content']}" for item in excerpts)

    def intent_messages(self, message: str, rag: dict[str, Any]) -> list[dict[str, str]]:
        return [
            {"role": "system", "content": "你是 MES 质量管理 Agent 的意图解释器。不能执行操作，不能虚构实时质量数据。只输出纯中文，不要 Markdown。"},
            {"role": "user", "content": (
                "请用不超过三行说明：是否应进入‘今日质量分析’流程、将读取哪些质量事实、分析边界是什么。"
                "用户原话：\n" + message + "\n"
                "SOP 仅用于说明流程，不能当成实时事实：\n" + self.rag_prompt(rag)
            )},
        ]

    def final_messages(self, facts: dict[str, Any], risk: dict[str, Any], rag: dict[str, Any]) -> list[dict[str, str]]:
        import json

        fact_json = json.dumps(facts, ensure_ascii=False, separators=(",", ":"))
        risk_json = json.dumps(risk, ensure_ascii=False, separators=(",", ":"))
        messages = [
            {"role": "system", "content": "你是 MES 质量总览的最终解释助手。只可使用提供的事实包与风险规则，不得编造检测结果、责任人、日期或处理完成情况。输出纯中文，不要 Markdown。"},
            {"role": "user", "content": (
                "基于以下质量事实包给出四段简洁结论：今日概览、主要风险、优先处理顺序、下一步建议。"
                "如果事实包为零，明确说明‘当前数据未显示对应记录’，不得推断无风险。\n"
                "质量事实包：" + fact_json + "\n"
                "确定性风险规则：" + risk_json + "\n"
                "可参考 SOP（不能替代事实包）：\n" + self.rag_prompt(rag)
            )},
        ]
        messages[1]["content"] += "\n请将内容扩展为六段：数据窗口与完成情况、待检队列、合格率与趋势、缺陷与分级、风险优先级、建议动作与人工复核边界。每段至少包含一个事实包数字或明确说明无对应数据。"
        return messages


quality_llm_analysis = QualityLlmAnalysis()
