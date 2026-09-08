from __future__ import annotations

import hashlib
import re
from dataclasses import asdict
from datetime import date
from uuid import NAMESPACE_URL, uuid5

from qdrant_client import models

from app.rag.embeddings import embed
from app.rag.initial_sources import KnowledgeSource, PROJECT_ROOT
from app.rag.qdrant_store import client, ensure_collection
from app.core.config import settings


_HEADING_RE = re.compile(r"^(#{1,6})\s+(.+?)\s*$")


def _chunks(markdown: str, max_chars: int = 1100) -> list[tuple[str, str]]:
    """Split Markdown on heading boundaries first, then on paragraphs with stable anchors."""
    headings: list[str] = []
    blocks: list[tuple[str, str]] = []
    current: list[str] = []

    def flush() -> None:
        text = "\n".join(current).strip()
        if text:
            blocks.append((" > ".join(headings) or "正文", text))
        current.clear()

    for line in markdown.splitlines():
        match = _HEADING_RE.match(line)
        if match:
            flush()
            level = len(match.group(1))
            headings[level - 1 :] = [match.group(2)]
        else:
            current.append(line)
    flush()

    result: list[tuple[str, str]] = []
    for heading, text in blocks:
        paragraphs = [p.strip() for p in re.split(r"\n\s*\n", text) if p.strip()]
        part = ""
        for paragraph in paragraphs:
            candidate = f"{part}\n\n{paragraph}".strip()
            if part and len(candidate) > max_chars:
                result.append((heading, part))
                part = paragraph
            else:
                part = candidate
        if part:
            result.append((heading, part))
    return result


def _payload(source: KnowledgeSource, heading: str, content: str, index: int) -> dict:
    content_hash = hashlib.sha256(content.encode("utf-8")).hexdigest()
    return {
        "document_id": source.document_id,
        "title": source.title,
        "domain": source.domain,
        "knowledge_type": source.knowledge_type,
        "authority_level": source.authority_level,
        "review_status": "approved",
        "role_scope": source.role_scope,
        "lifecycle_states": source.lifecycle_states,
        "version": "0.1",
        "effective_from": "2026-07-15",
        "source_path": str(source.source_path.relative_to(PROJECT_ROOT)),
        "anchor": heading,
        "chunk_index": index,
        "content": content,
        "content_hash": content_hash,
    }


def ingest_source(source: KnowledgeSource, *, replace: bool = True) -> int:
    if not source.source_path.exists():
        raise FileNotFoundError(source.source_path)
    ensure_collection()
    qdrant = client()
    if replace:
        qdrant.delete(
            collection_name=settings.qdrant_collection,
            points_selector=models.FilterSelector(
                filter=models.Filter(
                    must=[
                        models.FieldCondition(
                            key="document_id",
                            match=models.MatchValue(value=source.document_id),
                        )
                    ]
                )
            ),
            wait=True,
        )
    pieces = _chunks(source.source_path.read_text(encoding="utf-8"))
    payloads = [_payload(source, heading, content, i) for i, (heading, content) in enumerate(pieces)]
    vectors = embed([p["content"] for p in payloads])
    if len(payloads) != len(vectors):
        raise RuntimeError("Embedding output count does not match source chunk count")
    qdrant.upsert(
        collection_name=settings.qdrant_collection,
        points=[
            models.PointStruct(
                id=str(uuid5(NAMESPACE_URL, f"{p['document_id']}:{p['content_hash']}")),
                vector={"dense": vector},
                payload=p,
            )
            for p, vector in zip(payloads, vectors)
        ],
        wait=True,
    )
    return len(payloads)


def ingest_sources(sources: list[KnowledgeSource], *, replace: bool = True) -> dict[str, int]:
    return {source.document_id: ingest_source(source, replace=replace) for source in sources}
