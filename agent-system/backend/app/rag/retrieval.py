from __future__ import annotations

from typing import Any

from qdrant_client import models

from app.rag.embeddings import embed
from app.rag.qdrant_store import approved_knowledge_filter, client
from app.core.config import settings


def search_knowledge(
    query: str,
    *,
    roles: list[str],
    factory_id: str | None = None,
    product_code: str | None = None,
    process_code: str | None = None,
    lifecycle_state: str | None = None,
    limit: int = 5,
) -> list[dict[str, Any]]:
    vector = embed([query])[0]
    points = client().query_points(
        collection_name=settings.qdrant_collection,
        query=vector,
        using="dense",
        query_filter=approved_knowledge_filter(
            roles=roles,
            factory_id=factory_id,
            product_code=product_code,
            process_code=process_code,
            lifecycle_state=lifecycle_state,
        ),
        limit=limit,
        with_payload=True,
    ).points
    return [
        {
            "score": point.score,
            "content": point.payload.get("content", ""),
            "citation": {
                "document_id": point.payload.get("document_id"),
                "title": point.payload.get("title"),
                "source_path": point.payload.get("source_path"),
                "anchor": point.payload.get("anchor"),
                "version": point.payload.get("version"),
            },
        }
        for point in points
    ]
