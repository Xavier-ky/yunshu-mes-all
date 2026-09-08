"""Embed the approved, real MES baseline documents into local Qdrant.

Run from agent-system/backend:
    python scripts/ingest_initial_knowledge.py
"""
from __future__ import annotations

import sys
from pathlib import Path

ROOT = Path(__file__).resolve().parents[1]
sys.path.insert(0, str(ROOT))

from app.rag.ingestion import ingest_sources  # noqa: E402
from app.rag.initial_sources import INITIAL_SOURCES  # noqa: E402
from app.rag.qdrant_store import collection_summary  # noqa: E402


if __name__ == "__main__":
    print({"ingested_chunks": ingest_sources(list(INITIAL_SOURCES)), "collection": collection_summary()})
