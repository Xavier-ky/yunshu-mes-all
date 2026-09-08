"""Truthful, non-executing Agent tool catalogue.

The catalogue is a governance layer, not a second tool runner.  A listed tool
does not grant a model permission: current API role checks, service gates and
user confirmations stay authoritative.
"""

from __future__ import annotations

from dataclasses import asdict, dataclass
from typing import Optional


@dataclass(frozen=True)
class AgentToolDescriptor:
    tool_id: str
    name: str
    domain: str
    capability: str
    source: str
    access_mode: str
    execution_status: str
    approval_required: bool
    notes: str

    def to_dict(self) -> dict[str, object]:
        return asdict(self)


class AgentToolCatalog:
    """Read-only catalogue of real, currently governed MES Agent abilities."""

    _tools = (
        AgentToolDescriptor("rag_knowledge_search", "MES 知识检索", "knowledge", "按角色检索已审批 SOP、规则与 Agent 知识", "Qdrant / mes_knowledge_v1", "READ", "AVAILABLE", False, "知识解释，不作为实时业务事实或写入依据。"),
        AgentToolDescriptor("mes_work_order_snapshot", "工单流程快照", "planning", "读取工单生命周期、齐套、任务与派工摘要", "Spring Boot Agent Read API", "READ", "AVAILABLE", False, "JWT 与角色范围仍由后端强制执行。"),
        AgentToolDescriptor("mes_traceability_lookup", "工单追溯查询", "traceability", "读取工单的生产、质量、入库追溯链路", "Spring Boot Traceability API", "READ", "AVAILABLE", False, "仅查询当前授权范围内的真实 MES 数据。"),
        AgentToolDescriptor("mes_quality_snapshot", "质量数据快照", "quality", "读取检验任务、缺陷与合格率等质量指标", "Spring Boot Quality Read API", "READ", "AVAILABLE", False, "质量分析工作流当前使用此类真实数据。"),
        AgentToolDescriptor("local_qwen_analysis", "本地 Qwen 微分析", "llm", "对已获取的数据生成局部说明", "Local Qwen3-0.6B", "ANALYZE", "AVAILABLE", False, "不具备 MES 写入权限。"),
        AgentToolDescriptor("external_model_summary", "外部模型综合分析", "llm", "完成意图识别与最终风险总结", "Configured external LLM API", "ANALYZE", "AVAILABLE", False, "需结合 RAG 与实时事实，失败时有确定性降级。"),
        AgentToolDescriptor("scheduling_graph_execution", "智能排产图执行", "planning", "按既有服务顺序执行订单、BOM、齐套、排产、派工与领料", "LangGraph + existing MES services", "CONTROLLED_WRITE", "AVAILABLE", True, "只能通过既有确认订单入口；图本身不绕过业务事务与权限。"),
        AgentToolDescriptor("quality_report_export", "质量报告导出", "quality", "基于真实质量数据生成报告与 Word 文件", "Quality report workflow / Spring Boot export", "CONTROLLED_WRITE", "AVAILABLE", True, "当前由独立报告续接工作流处理。"),
        AgentToolDescriptor("calendar_schedule_read", "生产日历与班次读取", "calendar", "读取交期、班次及排程相关日历信息", "MES calendar / scheduling APIs", "READ", "RESERVED", False, "已有业务数据基础，尚未作为主控 Agent 的通用动态工具接入。"),
        AgentToolDescriptor("equipment_capacity_read", "设备与产能读取", "equipment", "读取设备状态与产能建议", "MES scheduling advisory", "READ", "AVAILABLE", False, "当前在排产链路中作为建议信息，不替代调度事务。"),
        AgentToolDescriptor("image_quality_analysis", "质量图像分析", "vision", "分析现场缺陷图片或视觉检测结果", "None", "ANALYZE", "PLANNED", False, "尚未接入，不能向模型宣称具备该能力。"),
        AgentToolDescriptor("sandboxed_script", "受控脚本运行", "automation", "运行审计过的批处理或数据诊断脚本", "None", "CONTROLLED_WRITE", "PLANNED", True, "当前不向 Agent 暴露任意脚本执行能力。"),
    )

    def list_tools(self, *, domain: Optional[str] = None) -> list[dict[str, object]]:
        tools = self._tools
        if domain:
            normalized = str(domain).strip().lower()
            tools = tuple(tool for tool in tools if tool.domain == normalized)
        return [tool.to_dict() for tool in tools]

    def get_tool(self, tool_id: str) -> Optional[dict[str, object]]:
        for tool in self._tools:
            if tool.tool_id == tool_id:
                return tool.to_dict()
        return None


agent_tool_catalog = AgentToolCatalog()
