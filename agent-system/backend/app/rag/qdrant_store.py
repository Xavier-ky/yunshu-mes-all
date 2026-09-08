from __future__ import annotations

from collections.abc import Iterable
from typing import Any

from qdrant_client import QdrantClient, models

from app.core.config import settings


PAYLOAD_INDEXES: tuple[str, ...] = (
    "review_status",
    "authority_level",
    "domain",
    "role_scope",
    "factory_scope",
    "product_codes",
    "material_codes",
    "process_codes",
    "lifecycle_states",
    "version",
)


def client() -> QdrantClient:
    """Return the single vector-index client. Business data must never be queried here."""
    return QdrantClient(url=settings.qdrant_url, api_key=settings.qdrant_api_key or None)


def ensure_collection(qdrant: QdrantClient | None = None) -> None:
    qdrant = qdrant or client()
    if not qdrant.collection_exists(settings.qdrant_collection):
        qdrant.create_collection(
            collection_name=settings.qdrant_collection,
            vectors_config={
                "dense": models.VectorParams(
                    size=settings.rag_embedding_dimensions,
                    distance=models.Distance.COSINE,
                )
            },
        )
    else:
        existing = qdrant.get_collection(settings.qdrant_collection)
        vectors = existing.config.params.vectors
        dense = vectors.get("dense") if isinstance(vectors, dict) else None
        if dense and dense.size != settings.rag_embedding_dimensions:
            raise ValueError(
                f"Collection {settings.qdrant_collection} uses {dense.size} dimensions, "
                f"but configured embedding uses {settings.rag_embedding_dimensions}. "
                "Run init_qdrant_collection.py --recreate before ingesting."
            )
    for field_name in PAYLOAD_INDEXES:
        qdrant.create_payload_index(
            collection_name=settings.qdrant_collection,
            field_name=field_name,
            field_schema=models.PayloadSchemaType.KEYWORD,
            wait=True,
        )


def recreate_collection() -> None:
    qdrant = client()
    if qdrant.collection_exists(settings.qdrant_collection):
        qdrant.delete_collection(settings.qdrant_collection)
    ensure_collection(qdrant)


def approved_knowledge_filter(
    *,
    roles: Iterable[str] | None = None,
    factory_id: str | None = None,
    product_code: str | None = None,
    process_code: str | None = None,
    lifecycle_state: str | None = None,
) -> models.Filter:
    """Build a restrictive filter; unapproved content can never be retrieved."""
    must: list[models.FieldCondition] = [
        models.FieldCondition(
            key="review_status", match=models.MatchValue(value="approved")
        )
    ]
    if roles:
        must.append(
            models.FieldCondition(
                key="role_scope", match=models.MatchAny(any=list(roles))
            )
        )
    for key, value in (
        ("factory_scope", factory_id),
        ("product_codes", product_code),
        ("process_codes", process_code),
        ("lifecycle_states", lifecycle_state),
    ):
        if value:
            must.append(
                models.FieldCondition(key=key, match=models.MatchValue(value=value))
            )
    return models.Filter(must=must)


def collection_summary() -> dict[str, Any]:
    qdrant = client()
    info = qdrant.get_collection(settings.qdrant_collection)
    return {
        "collection": settings.qdrant_collection,
        "points_count": info.points_count,
        "status": str(info.status),
        "vector_size": settings.rag_embedding_dimensions,
    }
