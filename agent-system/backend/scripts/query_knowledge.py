"""Run a local RAG retrieval check with a real MES question."""
from __future__ import annotations

import argparse
import json
import sys
from pathlib import Path

ROOT = Path(__file__).resolve().parents[1]
sys.path.insert(0, str(ROOT))

from app.rag.retrieval import search_knowledge  # noqa: E402


if __name__ == "__main__":
    parser = argparse.ArgumentParser()
    parser.add_argument("query")
    parser.add_argument("--role", default="PROD_SUPERVISOR")
    args = parser.parse_args()
    print(
        json.dumps(
            search_knowledge(args.query, roles=[args.role]),
            ensure_ascii=False,
            indent=2,
        )
    )
