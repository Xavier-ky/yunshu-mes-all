import { getToken } from "@/utils/auth-session";

const BASE = "/yunshu-agent";

export async function agentRequest(path, options = {}) {
  const token = getToken();
  const headers = {
    "Content-Type": "application/json",
    "X-Trace-Id": crypto.randomUUID?.() || String(Date.now()),
    ...(options.headers || {}),
  };
  if (token) headers.Authorization = `Bearer ${token}`;

  const response = await fetch(`${BASE}${path}`, { ...options, headers });
  const data = await response.json().catch(() => ({}));
  if (!response.ok)
    throw new Error(data.detail || data.message || "小智暂时无法响应");
  return data;
}
