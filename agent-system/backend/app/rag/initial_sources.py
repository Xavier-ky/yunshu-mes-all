from __future__ import annotations

from dataclasses import dataclass
from pathlib import Path


PROJECT_ROOT = Path(__file__).resolve().parents[4]
ALL_BUSINESS_ROLES = [
    "MANAGER",
    "PROD_SUPERVISOR",
    "WAREHOUSE_CLERK",
    "LINE_OPERATOR",
    "QUALITY_INSPECTOR",
    "EQUIPMENT_MAINTAINER",
    "TESTER",
]


@dataclass(frozen=True)
class KnowledgeSource:
    document_id: str
    title: str
    source_path: Path
    domain: str
    knowledge_type: str
    authority_level: str
    role_scope: list[str]
    lifecycle_states: list[str]


INITIAL_SOURCES: tuple[KnowledgeSource, ...] = (
    KnowledgeSource(
        "RAG-WF-GOLDEN-001",
        "云枢智造 MES 黄金演示链（页面操作指南）",
        PROJECT_ROOT / "MES-GOLDEN-PATH.md",
        "workflow",
        "workflow_sop",
        "project_baseline",
        ALL_BUSINESS_ROLES,
        [
            "DRAFT", "RELEASED", "KITTING_OK", "SCHEDULED", "MATERIAL_ISSUED",
            "IN_PROGRESS", "QC_PENDING", "QC_PASSED", "QC_FAILED", "COMPLETED",
        ],
    ),
    KnowledgeSource(
        "RAG-WF-RULE-001",
        "MES 黄金演示链（接口、生命周期与桥接规则）",
        PROJECT_ROOT / "docs" / "workflow-golden-path.md",
        "workflow",
        "workflow_rule",
        "system_derived",
        ALL_BUSINESS_ROLES,
        [
            "DRAFT", "RELEASED", "KITTING_OK", "SCHEDULED", "MATERIAL_ISSUED",
            "IN_PROGRESS", "QC_PENDING", "QC_PASSED", "QC_FAILED", "COMPLETED",
        ],
    ),
    KnowledgeSource(
        "RAG-ROLE-SOP-001",
        "云枢智造 MES 角色视图与职责",
        PROJECT_ROOT / "docs" / "role-views.md",
        "role",
        "role_sop",
        "project_baseline",
        ALL_BUSINESS_ROLES,
        [],
    ),
    KnowledgeSource(
        "RAG-DATA-RULE-001",
        "云枢智造 MES 核心数据库设计说明",
        PROJECT_ROOT / "docs" / "core-database-design.md",
        "business_data",
        "business_data_rule",
        "system_derived",
        ["MANAGER", "PROD_SUPERVISOR", "TESTER"],
        [],
    ),
    KnowledgeSource(
        "RAG-COMPAT-RULE-001",
        "fan_mes Compat 主线数据库梳理",
        PROJECT_ROOT / "docs" / "database-landscape.md",
        "system",
        "system_rule",
        "system_derived",
        ["MANAGER", "TESTER"],
        [],
    ),
    KnowledgeSource(
        "RAG-LIFECYCLE-RULE-001",
        "工单生命周期与业务门禁规则",
        PROJECT_ROOT / "agent-system" / "knowledge" / "system_rules" / "WORK_ORDER_LIFECYCLE.md",
        "workflow",
        "workflow_rule",
        "system_derived",
        ALL_BUSINESS_ROLES,
        [
            "DRAFT", "RELEASED", "KITTING_OK", "SCHEDULED", "MATERIAL_ISSUED",
            "IN_PROGRESS", "QC_PENDING", "QC_PASSED", "QC_FAILED", "COMPLETED",
        ],
    ),
    KnowledgeSource(
        "RAG-KITTING-RULE-001",
        "齐套预留、排产与派工规则",
        PROJECT_ROOT / "agent-system" / "knowledge" / "system_rules" / "KITTING_AND_SCHEDULING.md",
        "planning",
        "workflow_rule",
        "system_derived",
        ["MANAGER", "PROD_SUPERVISOR", "WAREHOUSE_CLERK", "TESTER"],
        ["RELEASED", "KITTING_OK", "SCHEDULED"],
    ),
    KnowledgeSource(
        "RAG-DISPATCH-RULE-001",
        "排产任务到现场派工规则",
        PROJECT_ROOT / "agent-system" / "knowledge" / "system_rules" / "SCHEDULING_TO_DISPATCH.md",
        "planning",
        "workflow_rule",
        "system_derived",
        ["MANAGER", "PROD_SUPERVISOR", "LINE_OPERATOR", "TESTER"],
        ["KITTING_OK", "SCHEDULED"],
    ),
    KnowledgeSource(
        "RAG-ISSUE-RULE-001",
        "生产领料执行与物料消耗同步规则",
        PROJECT_ROOT / "agent-system" / "knowledge" / "system_rules" / "MATERIAL_ISSUE_EXECUTION.md",
        "inventory",
        "workflow_rule",
        "system_derived",
        ["MANAGER", "PROD_SUPERVISOR", "WAREHOUSE_CLERK", "TESTER"],
        ["RELEASED", "KITTING_OK", "SCHEDULED", "MATERIAL_ISSUED"],
    ),
    KnowledgeSource(
        "RAG-FEEDBACK-RULE-001",
        "现场报工、任务进度与生产报告同步规则",
        PROJECT_ROOT / "agent-system" / "knowledge" / "system_rules" / "SHOP_FLOOR_FEEDBACK.md",
        "production",
        "workflow_rule",
        "system_derived",
        ["MANAGER", "PROD_SUPERVISOR", "LINE_OPERATOR", "QUALITY_INSPECTOR", "TESTER"],
        ["MATERIAL_ISSUED", "IN_PROGRESS", "QC_PENDING"],
    ),
    KnowledgeSource(
        "RAG-IPQC-RULE-001",
        "IPQC 判定、模板与质量门禁规则",
        PROJECT_ROOT / "agent-system" / "knowledge" / "system_rules" / "IPQC_AND_QUALITY_GATE.md",
        "quality",
        "workflow_rule",
        "system_derived",
        ["MANAGER", "PROD_SUPERVISOR", "QUALITY_INSPECTOR", "TESTER"],
        ["QC_PENDING", "QC_PASSED", "QC_FAILED"],
    ),
    KnowledgeSource(
        "RAG-RECEIPT-RULE-001",
        "成品入库、完工与追溯同步规则",
        PROJECT_ROOT / "agent-system" / "knowledge" / "system_rules" / "FINISHED_GOODS_RECEIPT.md",
        "inventory",
        "workflow_rule",
        "system_derived",
        ["MANAGER", "PROD_SUPERVISOR", "WAREHOUSE_CLERK", "QUALITY_INSPECTOR", "TESTER"],
        ["QC_PASSED", "COMPLETED"],
    ),
    KnowledgeSource(
        "RAG-TRACE-RULE-001",
        "工单、批次、SN 与全过程追溯规则",
        PROJECT_ROOT / "agent-system" / "knowledge" / "system_rules" / "WORK_ORDER_TRACEABILITY.md",
        "traceability",
        "workflow_rule",
        "system_derived",
        ALL_BUSINESS_ROLES,
        ["MATERIAL_ISSUED", "IN_PROGRESS", "QC_PENDING", "QC_PASSED", "QC_FAILED", "COMPLETED"],
    ),
    KnowledgeSource(
        "RAG-AGENT-GOV-001",
        "云枢小智 Agent 运行与 RAG 治理规则",
        PROJECT_ROOT / "agent-system" / "knowledge" / "agent_runtime" / "AGENT_RUNTIME_GOVERNANCE.md",
        "agent_runtime",
        "agent_governance",
        "system_derived",
        ALL_BUSINESS_ROLES,
        [],
    ),
    KnowledgeSource(
        "RAG-AGENT-SCHED-001",
        "智能排产总 Agent 已落地运行链路",
        PROJECT_ROOT / "agent-system" / "knowledge" / "agent_runtime" / "INTELLIGENT_SCHEDULING_AGENT.md",
        "planning",
        "agent_runtime_sop",
        "system_derived",
        ["MANAGER", "PROD_SUPERVISOR", "WAREHOUSE_CLERK", "LINE_OPERATOR", "TESTER"],
        ["DRAFT", "RELEASED", "KITTING_OK", "SCHEDULED", "MATERIAL_ISSUED"],
    ),
    KnowledgeSource(
        "RAG-AGENT-QUALITY-001",
        "质量总 Agent 今日质量分析运行说明",
        PROJECT_ROOT / "agent-system" / "knowledge" / "agent_runtime" / "QUALITY_ANALYSIS_AGENT.md",
        "quality",
        "agent_runtime_sop",
        "system_derived",
        ["MANAGER", "PROD_SUPERVISOR", "QUALITY_INSPECTOR", "TESTER"],
        ["QC_PENDING", "QC_PASSED", "QC_FAILED"],
    ),
    KnowledgeSource(
        "RAG-AGENT-QUALITY-REPORT-001",
        "质量报告 Agent 图文报告与 Word 导出运行说明",
        PROJECT_ROOT / "agent-system" / "knowledge" / "agent_runtime" / "QUALITY_REPORT_AND_EXPORT.md",
        "quality",
        "agent_runtime_sop",
        "system_derived",
        ["MANAGER", "PROD_SUPERVISOR", "QUALITY_INSPECTOR", "TESTER"],
        ["QC_PENDING", "QC_PASSED", "QC_FAILED"],
    ),
    KnowledgeSource(
        "RAG-AGENT-CATALOG-001",
        "云枢小智业务子 Agent 清单与真实数据边界",
        PROJECT_ROOT / "agent-system" / "knowledge" / "agent_runtime" / "BUSINESS_SUBAGENTS.md",
        "agent_runtime",
        "agent_catalog",
        "system_derived",
        ALL_BUSINESS_ROLES,
        [],
    ),
    KnowledgeSource(
        "RAG-AGENT-DATA-001",
        "MES 实时数据工具与 Agent 合同",
        PROJECT_ROOT / "agent-system" / "knowledge" / "agent_runtime" / "REALTIME_DATA_AND_AGENT_CONTRACTS.md",
        "agent_runtime",
        "agent_data_contract",
        "system_derived",
        ALL_BUSINESS_ROLES,
        ["DRAFT", "RELEASED", "KITTING_OK", "SCHEDULED", "MATERIAL_ISSUED", "IN_PROGRESS", "QC_PENDING", "QC_PASSED", "QC_FAILED", "COMPLETED"],
    ),
    KnowledgeSource(
        "RAG-AGENT-FOUNDATION-001",
        "云枢小智 Agent 记忆、工具治理、复盘与人在回路边界",
        PROJECT_ROOT / "agent-system" / "knowledge" / "agent_runtime" / "AGENT_FOUNDATION_EXTENSIONS.md",
        "agent_runtime",
        "agent_foundation_governance",
        "system_derived",
        ALL_BUSINESS_ROLES,
        [],
    ),
)
