"""OpenAI 兼容大模型客户端（DeepSeek / 通义千问 / 智谱 GLM）"""

from __future__ import annotations

import json
from collections.abc import Iterator
from typing import Any

import httpx

from app.core.config import settings


def chat_completion(
    provider_key: str,
    messages: list[dict[str, str]],
    *,
    stream: bool = False,
    timeout_seconds: float = 120.0,
) -> str:
    provider = settings.get_provider(provider_key)
    if not provider:
        raise ValueError(f"未知模型: {provider_key}")

    api_key = provider.get("api_key", "")
    if not api_key:
        raise ValueError(f"模型 {provider_key} 未配置 API Key")

    base = provider["base_url"].rstrip("/")
    url = f"{base}/chat/completions"
    payload: dict[str, Any] = {
        "model": provider["model"],
        "messages": messages,
        "stream": stream,
    }

    with httpx.Client(timeout=timeout_seconds) as client:
        resp = client.post(
            url,
            headers={
                "Authorization": f"Bearer {api_key}",
                "Content-Type": "application/json",
            },
            json=payload,
        )
        if resp.status_code != 200:
            raise RuntimeError(f"LLM 返回 {resp.status_code}: {resp.text[:500]}")
        data = resp.json()
        return (
            data.get("choices", [{}])[0]
            .get("message", {})
            .get("content", "")
            .strip()
        )


def stream_chat_completion(
    provider_key: str,
    messages: list[dict[str, str]],
    *,
    timeout_seconds: float = 120.0,
) -> Iterator[str]:
    """Yield OpenAI-compatible SSE deltas without buffering the whole answer."""
    provider = settings.get_provider(provider_key)
    if not provider:
        raise ValueError(f"Unknown model: {provider_key}")
    api_key = provider.get("api_key", "")
    if not api_key:
        raise ValueError(f"Model {provider_key} has no API key configured")

    url = f"{provider['base_url'].rstrip('/')}/chat/completions"
    payload: dict[str, Any] = {
        "model": provider["model"],
        "messages": messages,
        "stream": True,
    }
    with httpx.Client(timeout=timeout_seconds) as client:
        with client.stream(
            "POST",
            url,
            headers={"Authorization": f"Bearer {api_key}", "Content-Type": "application/json"},
            json=payload,
        ) as response:
            if response.status_code != 200:
                detail = response.read().decode("utf-8", errors="replace")[:500]
                raise RuntimeError(f"LLM returned {response.status_code}: {detail}")
            for line in response.iter_lines():
                if not line or not line.startswith("data:"):
                    continue
                raw = line[5:].strip()
                if raw == "[DONE]":
                    break
                try:
                    packet = json.loads(raw)
                except json.JSONDecodeError:
                    continue
                choices = packet.get("choices") or []
                if not choices:
                    continue
                delta = choices[0].get("delta") or {}
                content = delta.get("content")
                if content:
                    yield str(content)


def list_public_models() -> list[dict[str, Any]]:
    default = settings.default_provider
    out = []
    for key, p in settings.providers.items():
        if not p.get("api_key"):
            continue
        out.append(
            {
                "key": key,
                "label": p.get("label", key),
                "short": p.get("short", p.get("label", key)),
                "desc": p.get("desc", ""),
                "tier": p.get("tier", "高"),
                "default": key == default,
            }
        )
    return out
