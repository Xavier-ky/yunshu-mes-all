"""Probe configured LLM providers — identity check without printing API keys."""
from __future__ import annotations

import json
import sys

import httpx

from app.core.config import settings
from app.llm.client import chat_completion

QUESTION = (
    "请直接回答：你是什么模型？你的官方模型名称/型号是什么？"
    "只回答模型名，不要解释。"
)


def probe_direct(key: str) -> dict:
    p = settings.providers[key]
    api_key = p.get("api_key", "")
    model = p.get("model", "")
    base = p["base_url"].rstrip("/")
    result = {
        "provider": key,
        "label": p.get("label"),
        "configured_model_id": model,
        "base_url": base,
        "api_key_set": bool(api_key),
        "http_status": None,
        "response_model_field": None,
        "self_identification": None,
        "error": None,
    }
    if not api_key:
        result["error"] = "API key not configured"
        return result

    url = f"{base}/chat/completions"
    payload = {
        "model": model,
        "messages": [{"role": "user", "content": QUESTION}],
        "max_tokens": 256,
    }
    try:
        with httpx.Client(timeout=120.0) as client:
            resp = client.post(
                url,
                headers={
                    "Authorization": f"Bearer {api_key}",
                    "Content-Type": "application/json",
                },
                json=payload,
            )
        result["http_status"] = resp.status_code
        if resp.status_code != 200:
            result["error"] = resp.text[:500]
            return result
        data = resp.json()
        result["response_model_field"] = data.get("model")
        result["self_identification"] = (
            data.get("choices", [{}])[0].get("message", {}).get("content", "").strip()
        )
    except Exception as exc:
        result["error"] = str(exc)
    return result


def probe_via_client(key: str) -> dict:
    try:
        text = chat_completion(
            key,
            [{"role": "user", "content": QUESTION}],
        )
        return {"provider": key, "via_client": True, "answer": text[:300], "error": None}
    except Exception as exc:
        return {"provider": key, "via_client": True, "answer": None, "error": str(exc)}


def main() -> int:
    print("=== Runtime config ===")
    print(f"MOCK_LLM={settings.mock_llm}")
    print(f"DEFAULT_LLM_PROVIDER={settings.default_provider}")
    print()

    results = []
    for key in ("deepseek", "qwen", "glm"):
        print(f"--- {key} ---")
        r = probe_direct(key)
        results.append(r)
        for field in (
            "configured_model_id",
            "base_url",
            "api_key_set",
            "http_status",
            "response_model_field",
            "self_identification",
            "error",
        ):
            print(f"  {field}: {r.get(field)}")
        print()

    print("=== Via app.llm.client.chat_completion ===")
    client_results = [probe_via_client(k) for k in ("deepseek", "qwen", "glm")]
    print(json.dumps(client_results, ensure_ascii=False, indent=2))
    return 0


if __name__ == "__main__":
    sys.exit(main())
