import { agentRequest } from "@/api/agentRequest";
import { getAuthUser, getToken } from "@/utils/auth-session";

const AGENT_BASE = "/yunshu-agent";

export function agentHealth() {
  return agentRequest("/health");
}

export function agentChat(payload) {
  return agentRequest("/agent/chat", {
    method: "POST",
    body: JSON.stringify(payload),
  });
}

/** Consume the main Agent SSE response without waiting for a complete answer. */
export async function streamAgentChat(payload, { onMeta, onDelta, onNode, onReport, onDone } = {}) {
  const token = getToken();
  const headers = {
    "Content-Type": "application/json",
    "X-Trace-Id": crypto.randomUUID?.() || String(Date.now()),
  };
  if (token) headers.Authorization = `Bearer ${token}`;

  const response = await fetch(`${AGENT_BASE}/agent/chat/stream`, {
    method: "POST",
    headers,
    body: JSON.stringify(payload),
  });
  if (!response.ok) {
    const error = await response.json().catch(() => ({}));
    throw new Error(error.detail || error.message || "云枢小智暂时无法响应");
  }
  if (!response.body) throw new Error("当前浏览器不支持流式响应");

  const reader = response.body.getReader();
  const decoder = new TextDecoder();
  let pending = "";
  let streamError = null;
  let receivedFinalDelta = false;

  const consumeEvent = async (rawEvent) => {
    const line = rawEvent.split("\n").find((item) => item.startsWith("data:"));
    if (!line) return;
    const event = JSON.parse(line.slice(5).trim());
    if (event.type === "meta") await onMeta?.(event);
    if (event.type === "delta") await onDelta?.(event.content || "");
    if (["node", "node_start", "node_delta", "node_complete"].includes(event.type)) await onNode?.(event);
    if (event.type === "final_delta") {
      receivedFinalDelta = true;
      await onDelta?.(event.content || "");
    }
    // When an external provider is unavailable, the quality workflow sends
    // its factual fallback as `final_complete` rather than token deltas.  Do
    // not let that valid result fall through to the UI's generic empty-answer
    // message. On successful streaming this event only closes the node, so it
    // must not duplicate already rendered final deltas.
    if (event.type === "final_complete" && !receivedFinalDelta) {
      await onDelta?.(event.content || "");
    }
    if (["report_start", "report_section", "report_section_delta", "report_chart", "report_table", "report_final_complete", "report_export_ready"].includes(event.type)) await onReport?.(event);
    if (event.type === "done") await onDone?.(event);
    if (event.type === "error") streamError = new Error(event.message || "云枢小智暂时无法响应");
  };

  try {
    while (true) {
      const { done, value } = await reader.read();
      pending += decoder.decode(value || new Uint8Array(), { stream: !done });
      const events = pending.split("\n\n");
      pending = events.pop() || "";
      for (const event of events) await consumeEvent(event);
      if (streamError) throw streamError;
      if (done) break;
    }
    if (pending.trim()) await consumeEvent(pending);
    if (streamError) throw streamError;
  } finally {
    reader.releaseLock();
  }
}

export function fetchAgentSessions() {
  return agentRequest("/agent/sessions");
}

export function fetchAgentSession(sessionId) {
  return agentRequest(`/agent/sessions/${encodeURIComponent(sessionId)}`);
}

export function fetchOrderIntakeProducts() {
  return agentRequest("/agent/scheduling/order-intake/products");
}

export function fetchLatestOrderForIntake() {
  return agentRequest("/agent/scheduling/order-intake/latest-order");
}

export function createOrderFromIntake(payload) {
  return agentRequest("/agent/scheduling/order-intake/orders", {
    method: "POST",
    body: JSON.stringify(payload),
  });
}

export function approveConfirmation(id, userId) {
  return agentRequest(`/confirmations/${id}/approve`, {
    method: "POST",
    body: JSON.stringify({ user_id: userId }),
  });
}

export function rejectConfirmation(id) {
  return agentRequest(`/confirmations/${id}/reject`, { method: "POST" });
}

export function fetchTrace(traceId) {
  return agentRequest(`/traces/${traceId}`);
}

export function fetchAgents() {
  return agentRequest("/agents");
}

export function fetchModels() {
  return agentRequest("/models");
}

/** Download a user-owned, server-generated quality report without exposing its snapshot to the browser. */
export async function exportQualityReport(reportId) {
  const token = getToken();
  const response = await fetch(`${AGENT_BASE}/agent/quality/reports/${encodeURIComponent(reportId)}/export`, {
    method: "POST",
    headers: token ? { Authorization: `Bearer ${token}` } : {},
  });
  if (!response.ok) {
    const error = await response.json().catch(() => ({}));
    throw new Error(error.detail || "质量报告导出失败");
  }
  const blob = await response.blob();
  const contentDisposition = response.headers.get("Content-Disposition") || "";
  const matched = contentDisposition.match(/filename\*=UTF-8''([^;]+)/i) || contentDisposition.match(/filename=\"?([^\";]+)/i);
  const filename = matched ? decodeURIComponent(matched[1]) : "质量分析报告.docx";
  const url = URL.createObjectURL(blob);
  const link = document.createElement("a");
  link.href = url;
  link.download = filename;
  document.body.appendChild(link);
  link.click();
  link.remove();
  URL.revokeObjectURL(url);
}

/** 兼容 MES JWT — 从当前标签页 session 取用户信息 */
export function currentAgentUser() {
  try {
    const u = getAuthUser() || {};
    return {
      user_id: u.username || u.userName || "guest",
      user_role: (u.roleCodes || [])[0] || "TESTER",
      display_name: u.realName || u.username || "用户",
      avatar: u.avatar || "/images/my-logo.jpg",
    };
  } catch {
    return { user_id: "guest", user_role: "TESTER", display_name: "用户", avatar: "/images/my-logo.jpg" };
  }
}
