from __future__ import annotations

from functools import lru_cache
from pathlib import Path

from sentence_transformers import SentenceTransformer

from app.core.config import settings


@lru_cache(maxsize=1)
def model() -> SentenceTransformer:
    """Load the local Chinese embedding model lazily, never during FastAPI import."""
    model_ref = settings.rag_embedding_model
    local_path = settings.rag_embedding_model_path
    if local_path.exists():
        model_ref = str(local_path)
    else:
        local_path.parent.mkdir(parents=True, exist_ok=True)
        repo_cache = local_path.parent / f"models--{settings.rag_embedding_model.replace('/', '--')}"
        snapshots = repo_cache / "snapshots"
        if snapshots.exists():
            cached = sorted((p for p in snapshots.iterdir() if p.is_dir()), key=lambda p: p.name)
            if cached:
                model_ref = str(cached[-1])
    return SentenceTransformer(
        model_ref,
        cache_folder=str(local_path.parent),
        trust_remote_code=False,
    )


def embed(texts: list[str]) -> list[list[float]]:
    vectors = model().encode(
        texts,
        batch_size=settings.rag_embedding_batch_size,
        normalize_embeddings=True,
        show_progress_bar=False,
        convert_to_numpy=True,
    )
    output = vectors.tolist()
    if output and len(output[0]) != settings.rag_embedding_dimensions:
        raise ValueError(
            "Embedding dimension does not match the Qdrant collection: "
            f"expected={settings.rag_embedding_dimensions}, actual={len(output[0])}"
        )
    return output
