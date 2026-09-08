"""Run the first regression set for the governed MES knowledge base.

Run from agent-system/backend:
    D:\Anaconda2024.10\envs\pytorch\python.exe scripts\evaluate_knowledge.py
"""
from __future__ import annotations

import sys
from pathlib import Path

ROOT = Path(__file__).resolve().parents[1]
sys.path.insert(0, str(ROOT))

from app.rag.retrieval import search_knowledge  # noqa: E402


CASES = (
    (
        "排产任务保存后为什么没有派工？",
        ["PROD_SUPERVISOR"],
        "RAG-DISPATCH-RULE-001",
    ),
    (
        "领料单执行后会写入哪些物料消耗记录？",
        ["WAREHOUSE_CLERK"],
        "RAG-ISSUE-RULE-001",
    ),
    (
        "报工的 taskId 是派工单还是排产任务？",
        ["LINE_OPERATOR"],
        "RAG-DISPATCH-RULE-001",
    ),
    (
        "IPQC 不合格后为什么不能直接入库？",
        ["QUALITY_INSPECTOR"],
        "RAG-IPQC-RULE-001",
    ),
    (
        "成品入库执行前必须满足什么条件？",
        ["WAREHOUSE_CLERK"],
        "RAG-RECEIPT-RULE-001",
    ),
    (
        "怎样查看一张工单使用的批次和已生成的 SN？",
        ["PROD_SUPERVISOR"],
        "RAG-TRACE-RULE-001",
    ),
)


def main() -> int:
    failures: list[str] = []
    for question, roles, expected_document_id in CASES:
        items = search_knowledge(question, roles=roles, limit=5)
        document_ids = [str(item["citation"]["document_id"]) for item in items]
        passed = expected_document_id in document_ids
        print(
            {
                "question": question,
                "roles": roles,
                "expected_document_id": expected_document_id,
                "retrieved_document_ids": document_ids,
                "passed": passed,
            }
        )
        if not passed:
            failures.append(question)
    if failures:
        print({"status": "failed", "failed_questions": failures})
        return 1
    print({"status": "passed", "cases": len(CASES)})
    return 0


if __name__ == "__main__":
    raise SystemExit(main())
