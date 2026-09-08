"""Schema-validated, JWT-forwarding real MES read tools for the P0 Agent."""

from __future__ import annotations

import re
from datetime import datetime, timezone
from typing import Any

from app.core.security import Principal
from app.integrations.spring_boot_client import SpringBootClient
from app.rag.retrieval import search_knowledge


_WORK_ORDER_RE = re.compile(r"^[A-Za-z0-9_-]{1,64}$")

TOOL_CATALOG: dict[str, dict[str, Any]] = {
    "query_work_order_pipeline": {
        "description": "查询工单从订单到入库的当前流程快照。",
        "required": ("work_order_no",),
        "resource": "agent_work_order_pipeline",
        "path": "work_order_no",
    },
    "query_kitting_snapshot": {
        "description": "纯只读计算工单 BOM 齐套、合格可用库存和欠料明细。",
        "required": ("work_order_id",),
        "resource": "agent_kitting_snapshot",
        "path": "work_order_id",
    },
    "query_production_task_snapshot": {
        "description": "查询工单的生产任务、工序、工位与计划/完成数量。",
        "required": ("work_order_id",),
        "resource": "agent_task_snapshot",
        "path": "work_order_id",
    },
    "query_dispatch_snapshot": {
        "description": "查询工单现场派工；操作工角色仅返回本人派工。",
        "required": ("work_order_id",),
        "resource": "agent_dispatch_snapshot",
        "path": "work_order_id",
    },
    "query_work_order_trace": {
        "description": "查询工单的领料、报工、质量、入库、批次与 SN 追溯快照。",
        "required": ("work_order_no",),
        "resource": "agent_work_order_trace",
        "path": "work_order_no",
    },
    "search_knowledge": {
        "description": "检索已审核、角色过滤后的 MES SOP 与业务规则，并返回引用。",
        "required": ("query",),
        "resource": "rag",
    },
}


class MesReadToolGateway:
    """The sole FastAPI gateway for real P0 Agent facts."""

    def __init__(self, client: SpringBootClient | None = None) -> None:
        self._client = client or SpringBootClient()

    def list_tools(self) -> list[dict[str, Any]]:
        return [{"tool_name": name, **spec} for name, spec in TOOL_CATALOG.items()]

    def invoke(
        self,
        tool_name: str,
        arguments: dict[str, Any],
        principal: Principal,
        trace_id: str,
    ) -> dict[str, Any]:
        spec = TOOL_CATALOG.get(tool_name)
        if not spec:
            raise ValueError("未登记的 Agent 工具")
        args = self._validate(spec, arguments)
        queried_at = datetime.now(timezone.utc).isoformat()
        if spec["resource"] == "rag":
            data = search_knowledge(
                args["query"],
                roles=list(principal.roles),
                factory_id=args.get("factory_id"),
                product_code=args.get("product_code"),
                process_code=args.get("process_code"),
                lifecycle_state=args.get("lifecycle_state"),
                limit=int(args.get("limit", 5)),
            )
            return {
                "tool_name": tool_name,
                "source": "QDRANT_RAG",
                "read_only": True,
                "queried_at": queried_at,
                "trace_id": trace_id,
                "data": data,
            }
        path_key = spec["path"]
        data = self._client.read(
            spec["resource"],
            principal,
            trace_id,
            path_params={path_key: args[path_key]},
        )
        return {
            "tool_name": tool_name,
            "source": "MES_AGENT_READ_FACADE",
            "read_only": True,
            "queried_at": queried_at,
            "trace_id": trace_id,
            "data": data,
        }

    @staticmethod
    def _validate(spec: dict[str, Any], arguments: dict[str, Any]) -> dict[str, Any]:
        if not isinstance(arguments, dict):
            raise ValueError("工具参数必须是对象")
        args = dict(arguments)
        required = spec["required"]
        for key in required:
            if args.get(key) is None or str(args[key]).strip() == "":
                raise ValueError(f"缺少必填参数: {key}")
        if "work_order_no" in args:
            args["work_order_no"] = str(args["work_order_no"]).strip().upper()
            if not _WORK_ORDER_RE.fullmatch(args["work_order_no"]):
                raise ValueError("work_order_no 格式非法")
        if "work_order_id" in args:
            try:
                args["work_order_id"] = int(args["work_order_id"])
            except (TypeError, ValueError) as exc:
                raise ValueError("work_order_id 必须是正整数") from exc
            if args["work_order_id"] <= 0:
                raise ValueError("work_order_id 必须是正整数")
        if "query" in args:
            args["query"] = str(args["query"]).strip()
            if not 1 <= len(args["query"]) <= 1000:
                raise ValueError("query 长度必须在 1 到 1000 之间")
        if "limit" in args:
            args["limit"] = max(1, min(int(args["limit"]), 8))
        return args


mes_read_tools = MesReadToolGateway()
