"""Create and verify the local MES RAG collection.

Run from agent-system/backend after Qdrant is running:
    python scripts/init_qdrant_collection.py --verify
"""
from __future__ import annotations

import argparse
import sys
from pathlib import Path
from uuid import uuid4

ROOT = Path(__file__).resolve().parents[1]
sys.path.insert(0, str(ROOT))

from qdrant_client import models  # noqa: E402

from app.rag.qdrant_store import (  # noqa: E402
    client,
    collection_summary,
    ensure_collection,
    recreate_collection,
)
from app.core.config import settings  # noqa: E402


def verify_round_trip() -> None:
    qdrant = client()
    point_id = str(uuid4())
    vector = [0.0] * settings.rag_embedding_dimensions
    vector[0] = 1.0
    qdrant.upsert(
        collection_name=settings.qdrant_collection,
        points=[
            models.PointStruct(
                id=point_id,
                vector={"dense": vector},
                payload={
                    "review_status": "approved",
                    "authority_level": "system_derived",
                    "domain": "verification",
                    "role_scope": ["TESTER"],
                    "source_path": "verification-only",
                    "anchor": "round-trip",
                },
            )
        ],
        wait=True,
    )
    result = qdrant.query_points(
        collection_name=settings.qdrant_collection,
        query=vector,
        using="dense",
        query_filter=models.Filter(
            must=[
                models.FieldCondition(
                    key="review_status", match=models.MatchValue(value="approved")
                )
            ]
        ),
        limit=1,
        with_payload=True,
    ).points
    if not result or str(result[0].id) != point_id:
        raise RuntimeError("Qdrant round-trip verification failed")
    qdrant.delete(
        collection_name=settings.qdrant_collection,
        points_selector=models.PointIdsList(points=[point_id]),
        wait=True,
    )


def main() -> None:
    parser = argparse.ArgumentParser()
    parser.add_argument("--verify", action="store_true", help="verify upsert/query/delete")
    parser.add_argument(
        "--recreate",
        action="store_true",
        help="delete and recreate the empty development collection for a new embedding dimension",
    )
    args = parser.parse_args()
    if args.recreate:
        recreate_collection()
    else:
        ensure_collection()
    if args.verify:
        verify_round_trip()
    print(collection_summary())


if __name__ == "__main__":
    main()
