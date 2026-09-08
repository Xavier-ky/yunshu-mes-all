"""Persisted companion chat backed by the local original Qwen3-0.6B model."""

from __future__ import annotations

import json
import uuid
from collections.abc import Iterator
from pathlib import Path
from typing import Any

from app.core.config import settings
from app.core.security import Principal
from app.integrations.spring_boot_client import SpringBootClient
from app.llm.local_qwen import LocalQwenError, LocalQwenResult, LocalQwenRuntime, get_local_model
from app.schemas import CompanionChatRequest, CompanionChatResponse
from app.storage.mysql_store import ConversationStoreError, conversation_store


class CompanionChatService:
    """Coordinates database history, approved MES reads, and local Qwen inference."""

    _LIVE_PRESET_RESOURCES = {
        "today-production-overview": "today_production_overview",
        "quality-task-backlog": "quality_task_backlog",
    }

    _prompt_path = (
        Path(__file__).resolve().parents[3] / "mymodel" / "config" / "companion_system_prompt.txt"
    )

    def __init__(self) -> None:
        if not self._prompt_path.is_file():
            raise RuntimeError(f"Companion prompt file is missing: {self._prompt_path}")
        self._system_prompt = self._prompt_path.read_text(encoding="utf-8").strip()
        self._mes_client = SpringBootClient()

    @staticmethod
    def _identity_answer(request: CompanionChatRequest, runtime: LocalQwenRuntime) -> str | None:
        """Return authoritative model metadata instead of letting a small model guess it."""
        question = request.message.strip().lower()
        asks_about_model = "model" in question or "\u6a21\u578b" in question or "\u57fa\u5ea7" in question
        if not asks_about_model:
            return None
        if runtime.key == "qwen3l":
            return (
                "\u6211\u662f Qwen3L\uff1a\u5f53\u524d\u52a0\u8f7d\u7684\u662f "
                "Qwen3-0.6B-MES-clean360-balanced-merged \u5fae\u8c03\u5408\u5e76\u6a21\u578b\uff0c"
                "\u57fa\u5ea7\u662f Qwen3-0.6B\u3002"
            )
        return "\u6211\u662f Qwen3\uff1a\u5f53\u524d\u52a0\u8f7d\u7684\u662f\u539f\u59cb Qwen3-0.6B \u57fa\u7840\u6a21\u578b\u3002"

    @staticmethod
    def _identity_chunks(answer: str, size: int = 8) -> Iterator[str]:
        for index in range(0, len(answer), size):
            yield answer[index : index + size]

    @staticmethod
    def _live_fact_guard(live_context: dict[str, Any] | None) -> str:
        """Translate fixed live contracts into unambiguous facts for small local models."""
        if not live_context:
            return ""
        backlog = live_context.get("backlog")
        if not isinstance(backlog, dict):
            return ""
        pending_total = int(backlog.get("pendingTotal") or 0)
        created_count = int(backlog.get("createdCount") or 0)
        inspecting_count = int(backlog.get("inspectingCount") or 0)
        in_progress_count = int(backlog.get("inProgressCount") or 0)
        aged_24h_count = int(backlog.get("agedOver24HoursCount") or 0)
        aged_72h_count = int(backlog.get("agedOver72HoursCount") or 0)
        return (
            "\n\nQUALITY BACKLOG FACT CHECK (authoritative, do not contradict): "
            f"pending_total={pending_total}; created={created_count}; inspecting={inspecting_count}; "
            f"in_progress={in_progress_count}; aged_over_24h={aged_24h_count}; "
            f"aged_over_72h={aged_72h_count}. "
            "Required reasoning: pending_total > 0 means backlog EXISTS. "
            "Do not say there is no backlog. State the above numbers in Chinese, then give concise actions. "
            "These tasks have no database due-time field, so call them aged pending tasks, not overdue tasks."
        )

    @staticmethod
    def _quality_backlog_summary(live_context: dict[str, Any] | None) -> str | None:
        """Build the non-negotiable factual conclusion from the live quality contract."""
        if not live_context or not isinstance(live_context.get("backlog"), dict):
            return None
        backlog = live_context["backlog"]
        pending_total = int(backlog.get("pendingTotal") or 0)
        created_count = int(backlog.get("createdCount") or 0)
        inspecting_count = int(backlog.get("inspectingCount") or 0)
        in_progress_count = int(backlog.get("inProgressCount") or 0)
        aged_24h_count = int(backlog.get("agedOver24HoursCount") or 0)
        aged_72h_count = int(backlog.get("agedOver72HoursCount") or 0)
        if pending_total == 0:
            return "\u5b9e\u65f6\u67e5\u8be2\u7ed3\u8bba\uff1a\u5f53\u524d\u6ca1\u6709\u5f85\u5904\u7406\u7684\u8d28\u91cf\u4efb\u52a1\u3002\n"
        lines = [
            "\u5b9e\u65f6\u67e5\u8be2\u7ed3\u8bba\uff1a\u5f53\u524d\u5b58\u5728\u8d28\u91cf\u4efb\u52a1\u79ef\u538b\u3002",
            (
                f"\u5171 {pending_total} \u6761\u5f85\u5904\u7406\uff1a\u65b0\u5efa {created_count} \u6761\uff0c"
                f"\u68c0\u9a8c\u4e2d {inspecting_count} \u6761\uff0c\u5904\u7406\u4e2d {in_progress_count} \u6761\u3002"
            ),
            f"\u5176\u4e2d {aged_24h_count} \u6761\u5df2\u7b49\u5f85\u8d85\u8fc7 24 \u5c0f\u65f6\uff0c{aged_72h_count} \u6761\u8d85\u8fc7 72 \u5c0f\u65f6\u3002",
            "\u8d28\u91cf\u4efb\u52a1\u8868\u6ca1\u6709\u5e94\u5b8c\u6210\u65f6\u95f4\u5b57\u6bb5\uff0c\u4ee5\u4e0a\u4e3a\u8001\u5316\u5f85\u5904\u7406\uff0c\u4e0d\u5c06\u5176\u5224\u5b9a\u4e3a\u903e\u671f\u3002",
        ]
        oldest_tasks = live_context.get("oldestPendingTasks")
        if isinstance(oldest_tasks, list) and oldest_tasks:
            oldest = oldest_tasks[0]
            task_no = oldest.get("qualityTaskNo") or "-"
            pending_hours = oldest.get("pendingHours") or 0
            lines.append(f"\u6700\u65e9\u5f85\u5904\u7406\u4efb\u52a1\u4e3a {task_no}\uff0c\u5df2\u7b49\u5f85 {pending_hours} \u5c0f\u65f6\u3002")
        return "\n".join(lines)

    @staticmethod
    def _quality_action_only_instruction(live_context: dict[str, Any] | None) -> str:
        if not live_context or not isinstance(live_context.get("backlog"), dict):
            return ""
        return (
            "\n\nQUALITY RESPONSE MODE: the factual conclusion has already been shown to the user. "
            "Output only two short Chinese operational actions. Do not repeat any number, do not state whether "
            "backlog exists, and do not use the word overdue."
        )

    @staticmethod
    def _safe_quality_actions(model_answer: str) -> str:
        """Keep only advice that does not alter the live quality facts."""
        blocked_terms = (
            "overdue",
            "\u903e\u671f",
            "\u79ef\u538b",
            "\u5f85\u5904\u7406",
            "\u5c0f\u65f6",
            "pending_total",
        )
        normalized = model_answer.lower()
        has_number = any(character.isdigit() for character in model_answer)
        if not has_number and not any(term in normalized for term in blocked_terms):
            for marker in ("\u5efa\u8bae\uff1a", "\u5efa\u8bae:"):
                if marker in model_answer:
                    candidate = model_answer.rsplit(marker, 1)[-1].strip()
                    if candidate:
                        return "\u5efa\u8bae\uff1a\n" + candidate
        return (
            "\u5efa\u8bae\uff1a\n"
            "1. \u4f18\u5148\u5b89\u6392\u8d28\u68c0\u4eba\u5458\u5904\u7406\u6700\u65e9\u7684\u5f85\u5904\u7406\u4efb\u52a1\uff0c\u5e76\u6838\u5b9e\u5de5\u5355\u73b0\u573a\u72b6\u6001\u3002\n"
            "2. \u5b8c\u6210\u68c0\u9a8c\u540e\u53ca\u65f6\u66f4\u65b0 MES \u4efb\u52a1\u72b6\u6001\uff0c\u518d\u590d\u6838\u5269\u4f59\u4efb\u52a1\u6e05\u5355\u3002"
        )

    @staticmethod
    def _production_overview_summary(live_context: dict[str, Any] | None) -> str | None:
        """Render the final-product MES facts before requesting model advice."""
        if not live_context or not isinstance(live_context.get("overview"), dict):
            return None
        overview = live_context["overview"]

        def whole(key: str) -> int:
            try:
                return round(float(overview.get(key) or 0))
            except (TypeError, ValueError):
                return 0

        try:
            achievement_rate = float(overview.get("achievementRate") or 0)
        except (TypeError, ValueError):
            achievement_rate = 0.0

        lines = [
            "\u5b9e\u65f6\u751f\u4ea7\u6982\u89c8\uff08\u6570\u636e\u6e90\uff1aMES\uff09\uff1a",
            f"- \u4eca\u65e5\u8ba1\u5212\u4ea7\u91cf\uff1a{whole('todayPlanQty')} \u4ef6",
            f"- \u4eca\u65e5\u5b8c\u6210\u5408\u683c\u54c1\uff1a{whole('todayCompletedQty')} \u4ef6",
            f"- \u4eca\u65e5\u8fbe\u6210\u7387\uff1a{achievement_rate:.1f}%",
        ]
        products = live_context.get("todayCompletedProducts")
        if isinstance(products, list) and products:
            product_lines: list[str] = []
            for product in products[:5]:
                if not isinstance(product, dict):
                    continue
                name = str(product.get("productName") or "\u672a\u547d\u540d\u4ea7\u54c1").strip()
                try:
                    qty = round(float(product.get("completedQty") or 0))
                except (TypeError, ValueError):
                    qty = 0
                work_orders = str(product.get("workOrderNos") or "").strip()
                suffix = f"\uff08\u5de5\u5355\uff1a{work_orders}\uff09" if work_orders else ""
                product_lines.append(f"{name} {qty} \u4ef6{suffix}")
            if product_lines:
                lines.append("- \u4eca\u65e5\u5b8c\u6210\u4ea7\u54c1\uff1a" + "\uff1b".join(product_lines))
        else:
            lines.append("- \u4eca\u65e5\u5b8c\u6210\u4ea7\u54c1\uff1a\u6682\u65e0\u5df2\u5b8c\u6210\u6210\u54c1\u62a5\u5de5")

        lines.extend([
            f"- \u5f53\u524d\u8fdb\u884c\u5de5\u5355\uff1a{whole('activeWorkOrderCount')} \u4e2a",
            f"- \u672a\u5173\u95ed\u5b89\u706f\uff1a{whole('openAndonCount')} \u6761",
            f"- \u5f85\u5904\u7406\u8d28\u91cf\u4efb\u52a1\uff1a{whole('pendingQualityTaskCount')} \u6761",
            f"- \u4f4e\u5e93\u5b58\u6279\u6b21\uff1a{whole('lowInventoryBatchCount')} \u6279",
            f"- \u6545\u969c/\u505c\u673a\u8bbe\u5907\uff1a{whole('faultDeviceCount')} \u53f0",
        ])
        return "\n".join(lines)

    @staticmethod
    def _production_action_only_instruction(live_context: dict[str, Any] | None) -> str:
        if not live_context or not isinstance(live_context.get("overview"), dict):
            return ""
        return (
            "\n\nPRODUCTION RESPONSE MODE: the factual production overview has already been shown to the user. "
            "Output exactly two short Chinese operational recommendations only. Do not repeat or invent any "
            "number, product name, work order, or status; do not call quality tasks pending orders."
        )

    @staticmethod
    def _safe_production_actions(model_answer: str) -> str:
        """Keep a small model from changing the deterministic MES snapshot."""
        blocked_terms = (
            "\u5de5\u5355", "\u8ba2\u5355", "\u4ea7\u54c1", "\u5b89\u706f", "\u5e93\u5b58", "\u8bbe\u5907",
            "\u8d28\u91cf\u4efb\u52a1", "\u5b8c\u6210\u7387", "\u8fbe\u6210\u7387", "\u4ef6",
        )
        if not any(character.isdigit() for character in model_answer) and not any(
            term in model_answer for term in blocked_terms
        ):
            candidate = model_answer.strip()
            if candidate:
                return "\u5efa\u8bae\uff1a\n" + candidate
        return (
            "\u5efa\u8bae\uff1a\n"
            "1. \u7ee7\u7eed\u6838\u5bf9\u73b0\u573a\u62a5\u5de5\u4e0e\u5de5\u5e8f\u6d41\u8f6c\u662f\u5426\u53ca\u65f6\u540c\u6b65\u3002\n"
            "2. \u6309\u5f02\u5e38\u4f18\u5148\u7ea7\u5b89\u6392\u73b0\u573a\u8ddf\u8fdb\uff0c\u5e76\u5728\u5904\u7406\u540e\u590d\u6838\u751f\u4ea7\u770b\u677f\u3002"
        )

    def _messages(
        self,
        history: list[dict[str, str]],
        request: CompanionChatRequest,
        runtime: LocalQwenRuntime,
        live_context: dict[str, Any] | None = None,
    ) -> list[dict[str, str]]:
        page_context = (
            f"\n\n当前 MES 页面：{request.route_title or '未命名页面'}"
            f"（路径：{request.route_path}）。页面信息仅用于辅助理解，不代表已查询到业务数据。"
        )
        live_data_instruction = ""
        if live_context:
            live_data_instruction = (
                "\n\n本次查询已读取 MES 实时数据，数据源为 LIVE。"
                "只能根据以下 JSON 事实分析，必须引用其中的具体数字，不能补造任何指标。"
                "若今日计划数量为 0，应说明暂无当天计划，不能计算达成率。\n"
                + json.dumps(live_context, ensure_ascii=False, separators=(",", ":"))
            )
        return [
            {
                "role": "system",
                "content": (
                    self._system_prompt
                    + f"\n\n当前回答模型：{runtime.model_name}。"
                    + page_context
                    + live_data_instruction
                    + self._live_fact_guard(live_context)
                    + self._quality_action_only_instruction(live_context)
                    + self._production_action_only_instruction(live_context)
                ),
            },
            *history,
            {"role": "user", "content": request.message.strip()},
        ]

    def _prepare(
        self, request: CompanionChatRequest, principal: Principal
    ) -> tuple[int, int, int, str, list[dict[str, str]]]:
        question = request.message.strip()
        user_id, agent_id, db_session_id, public_session_id = conversation_store.prepare_conversation(
            principal.user_id, request.session_id, question
        )
        history = conversation_store.load_history(db_session_id, settings.local_qwen_max_history_turns)
        # Store the question before inference: model failures must never discard a user request.
        conversation_store.append_message(db_session_id, "USER", user_id, question)
        return user_id, agent_id, db_session_id, public_session_id, history

    def _read_live_preset(
        self, request: CompanionChatRequest, principal: Principal, trace_id: str
    ) -> dict[str, Any] | None:
        if not request.preset_id:
            return None
        resource = self._LIVE_PRESET_RESOURCES.get(request.preset_id)
        if resource is None:
            raise RuntimeError("Unsupported companion preset.")
        try:
            payload = self._mes_client.read(resource, principal, trace_id)
        except Exception as exc:
            raise RuntimeError("实时生产概览数据暂不可用，未使用模拟数据。") from exc
        if not isinstance(payload, dict) or payload.get("dataSource") != "LIVE":
            raise RuntimeError("实时生产概览接口未返回 LIVE 数据，已拒绝生成分析。")
        return payload

    @staticmethod
    def _source(live_context: dict[str, Any] | None, runtime: LocalQwenRuntime) -> str:
        prefix = "live-mes" if live_context else "local"
        return f"{prefix}-{runtime.key}"

    @staticmethod
    def _record_live_context(session_id: int, agent_id: int, live_context: dict[str, Any]) -> None:
        conversation_store.append_message(
            session_id,
            "TOOL",
            agent_id,
            "LIVE MES companion data: " + json.dumps(live_context, ensure_ascii=False),
        )

    def reply(
        self,
        request: CompanionChatRequest,
        principal: Principal,
        trace_id: str | None,
    ) -> CompanionChatResponse:
        request_trace_id = trace_id or str(uuid.uuid4())
        try:
            _, agent_id, db_session_id, public_session_id, history = self._prepare(request, principal)
            live_context = self._read_live_preset(request, principal, request_trace_id)
            if live_context:
                self._record_live_context(db_session_id, agent_id, live_context)
            runtime = get_local_model(request.model_key)
            identity_answer = self._identity_answer(request, runtime)
            result = (
                LocalQwenResult(identity_answer, 0, 0, runtime.model_name)
                if identity_answer
                else runtime.generate(self._messages(history, request, runtime, live_context))
            )
            quality_summary = self._quality_backlog_summary(live_context)
            production_summary = self._production_overview_summary(live_context)
            live_summary = quality_summary or production_summary
            if live_summary and not identity_answer:
                actions = (
                    self._safe_quality_actions(result.answer)
                    if quality_summary
                    else self._safe_production_actions(result.answer)
                )
                result = LocalQwenResult(
                    f"{live_summary}\n\n{actions}",
                    result.input_tokens,
                    result.output_tokens,
                    result.model,
                )
            conversation_store.append_message(
                db_session_id, "AGENT", agent_id, result.answer, result.output_tokens
            )
        except (ConversationStoreError, LocalQwenError) as exc:
            raise RuntimeError(str(exc)) from exc

        return CompanionChatResponse(
            answer=result.answer,
            session_id=public_session_id,
            source=self._source(live_context, runtime),
            model=result.model,
            trace_id=request_trace_id,
            follow_ups=["可以补充现场现象或工单号", "也可以继续描述质量、物料或设备问题"],
        )

    def stream_reply(
        self,
        request: CompanionChatRequest,
        principal: Principal,
        trace_id: str | None,
    ) -> Iterator[dict[str, Any]]:
        """Yield SSE-ready payloads and persist the completed answer."""
        request_trace_id = trace_id or str(uuid.uuid4())
        try:
            _, agent_id, db_session_id, public_session_id, history = self._prepare(request, principal)
            live_context = self._read_live_preset(request, principal, request_trace_id)
            runtime = get_local_model(request.model_key)
            source = self._source(live_context, runtime)
            if live_context:
                self._record_live_context(db_session_id, agent_id, live_context)
            yield {
                "type": "meta",
                "session_id": public_session_id,
                "source": source,
                "model": runtime.model_name,
                "trace_id": request_trace_id,
            }
            quality_summary = self._quality_backlog_summary(live_context)
            production_summary = self._production_overview_summary(live_context)
            live_summary = quality_summary or production_summary
            if live_summary:
                for chunk in self._identity_chunks(live_summary):
                    yield {"type": "delta", "content": chunk}
            identity_answer = self._identity_answer(request, runtime)
            if identity_answer:
                for chunk in self._identity_chunks(identity_answer):
                    yield {"type": "delta", "content": chunk}
                conversation_store.append_message(db_session_id, "AGENT", agent_id, identity_answer, 0)
                yield {
                    "type": "done",
                    "session_id": public_session_id,
                    "source": source,
                    "model": runtime.model_name,
                    "trace_id": request_trace_id,
                    "follow_ups": [],
                }
                return
            for event in runtime.stream_generate(self._messages(history, request, runtime, live_context)):
                if event.delta and not live_summary:
                    yield {"type": "delta", "content": event.delta}
                if event.result:
                    completed_answer = event.result.answer
                    if quality_summary:
                        safe_actions = self._safe_quality_actions(event.result.answer)
                        completed_answer = f"{quality_summary}\n\n{safe_actions}"
                        for chunk in self._identity_chunks("\n\n" + safe_actions):
                            yield {"type": "delta", "content": chunk}
                    elif production_summary:
                        safe_actions = self._safe_production_actions(event.result.answer)
                        completed_answer = f"{production_summary}\n\n{safe_actions}"
                        for chunk in self._identity_chunks("\n\n" + safe_actions):
                            yield {"type": "delta", "content": chunk}
                    conversation_store.append_message(
                        db_session_id,
                        "AGENT",
                        agent_id,
                        completed_answer,
                        event.result.output_tokens,
                    )
                    yield {
                        "type": "done",
                        "session_id": public_session_id,
                        "source": source,
                        "model": event.result.model,
                        "trace_id": request_trace_id,
                        "follow_ups": ["可以补充现场现象或工单号", "也可以继续描述质量、物料或设备问题"],
                    }
        except (ConversationStoreError, LocalQwenError) as exc:
            raise RuntimeError(str(exc)) from exc
