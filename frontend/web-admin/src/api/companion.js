import { agentRequest } from "@/api/agentRequest";
import { getToken } from "@/utils/auth-session";

const AGENT_BASE = "/yunshu-agent";

export function requestCompanionSuggestion(context) {
  return agentRequest("/companion/suggest", {
    method: "POST",
    body: JSON.stringify(context),
  });
}

/**
 * Read the companion SSE response incrementally. Each model segment is delivered to onDelta
 * instead of waiting for the complete answer.
 */
export async function streamCompanionReply(context, { onMeta, onDelta, onDone } = {}) {
  const token = getToken();
  const headers = {
    "Content-Type": "application/json",
    "X-Trace-Id": crypto.randomUUID?.() || String(Date.now()),
  };
  if (token) headers.Authorization = `Bearer ${token}`;

  const response = await fetch(`${AGENT_BASE}/companion/chat/stream`, {
    method: "POST",
    headers,
    body: JSON.stringify(context),
  });
  if (!response.ok) {
    const error = await response.json().catch(() => ({}));
    throw new Error(error.detail || "小精灵暂时无法响应");
  }
  if (!response.body) throw new Error("浏览器不支持流式响应");

  const reader = response.body.getReader();
  const decoder = new TextDecoder();
  let pending = "";

  const consumeEvent = (rawEvent) => {
    const line = rawEvent.split("\n").find((item) => item.startsWith("data:"));
    if (!line) return;
    const payload = JSON.parse(line.slice(5).trim());
    if (payload.type === "meta") onMeta?.(payload);
    if (payload.type === "delta") onDelta?.(payload.content || "");
    if (payload.type === "done") onDone?.(payload);
    if (payload.type === "error") throw new Error(payload.message || "本地模型暂时不可用");
  };

  try {
    while (true) {
      const { done, value } = await reader.read();
      pending += decoder.decode(value || new Uint8Array(), { stream: !done });
      const events = pending.split("\n\n");
      pending = events.pop() || "";
      events.forEach(consumeEvent);
      if (done) break;
    }
    if (pending.trim()) consumeEvent(pending);
  } finally {
    reader.releaseLock();
  }
}
