"""Thin, typed wrapper around the existing real MES/RAG read gateway.

This module intentionally delegates every invocation to ``mes_read_tools``.
It does not add SQL access, HTTP allowlist entries, model permissions, or
write capabilities.  Existing scheduling and quality workflows do not import
this package; it is available for future dynamic routing only after separate
compatibility approval.
"""

from __future__ import annotations

from typing import Any, Optional

from app.core.security import Principal
from app.foundation.tools.contracts import PackagedReadToolDefinition
from app.tools.mes_read import TOOL_CATALOG, MesReadToolGateway, mes_read_tools


class PackagedReadToolError(RuntimeError):
    """Raised when a requested real Tool is not part of the read-only package."""


class FoundationReadToolRegistry:
    """One uniform contract for existing, authenticated real read tools."""

    _governance_ids = {
        "query_work_order_pipeline": "mes_work_order_snapshot",
        "query_kitting_snapshot": "mes_work_order_snapshot",
        "query_production_task_snapshot": "mes_work_order_snapshot",
        "query_dispatch_snapshot": "mes_work_order_snapshot",
        "query_work_order_trace": "mes_traceability_lookup",
        "search_knowledge": "rag_knowledge_search",
    }
    _names = {
        "query_work_order_pipeline": "工单流程快照",
        "query_kitting_snapshot": "BOM 齐套快照",
        "query_production_task_snapshot": "生产任务快照",
        "query_dispatch_snapshot": "派工快照",
        "query_work_order_trace": "工单全链路追溯",
        "search_knowledge": "MES 知识检索",
    }
    _domains = {
        "query_work_order_pipeline": "planning",
        "query_kitting_snapshot": "planning",
        "query_production_task_snapshot": "production",
        "query_dispatch_snapshot": "production",
        "query_work_order_trace": "traceability",
        "search_knowledge": "knowledge",
    }

    def __init__(self, gateway: Optional[MesReadToolGateway] = None) -> None:
        self._gateway = gateway or mes_read_tools
        self._definitions = self._build_definitions()

    def list_tools(self) -> list[dict[str, Any]]:
        return [self._definitions[name].to_dict() for name in sorted(self._definitions)]

    def get_tool(self, tool_name: str) -> Optional[dict[str, Any]]:
        definition = self._definitions.get(str(tool_name or ""))
        return definition.to_dict() if definition else None

    def invoke(
        self,
        tool_name: str,
        arguments: dict[str, Any],
        principal: Principal,
        trace_id: str,
    ) -> dict[str, Any]:
        normalized = str(tool_name or "").strip()
        definition = self._definitions.get(normalized)
        if definition is None:
            raise PackagedReadToolError("该工具未被包装为真实只读 Tool")

        # Delegate validation, role propagation, URL allowlisting and source
        # attribution to the proven P0 gateway.  No additional authority is
        # created by this wrapper.
        raw = self._gateway.invoke(normalized, arguments, principal, trace_id)
        if raw.get("read_only") is not True:
            raise PackagedReadToolError("只读 Tool 包装拒绝了非只读结果")
        return {
            "contract_version": definition.contract_version,
            "tool": definition.to_dict(),
            "trace_id": trace_id,
            "read_only": True,
            "result": raw,
        }

    @classmethod
    def _build_definitions(cls) -> dict[str, PackagedReadToolDefinition]:
        definitions: dict[str, PackagedReadToolDefinition] = {}
        for tool_name, governance_id in cls._governance_ids.items():
            spec = TOOL_CATALOG.get(tool_name)
            if not spec:
                raise RuntimeError(f"missing real read-tool specification: {tool_name}")
            source = "QDRANT_RAG" if spec.get("resource") == "rag" else "MES_AGENT_READ_FACADE"
            definitions[tool_name] = PackagedReadToolDefinition(
                tool_name=tool_name,
                governance_tool_id=governance_id,
                display_name=cls._names[tool_name],
                domain=cls._domains[tool_name],
                source=source,
                required_arguments=tuple(spec.get("required") or ()),
                description=str(spec.get("description") or ""),
            )
        return definitions


foundation_read_tools = FoundationReadToolRegistry()
