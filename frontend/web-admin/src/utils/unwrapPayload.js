/**
 * 解包 ApiResponse.data；兼容 mock 直出业务对象。
 */
export function unwrapPayload(res) {
  if (!res) return null;
  if (res.data !== undefined && res.data !== null) return res.data;
  if (Array.isArray(res.metrics) || Array.isArray(res.lines) || res.line) return res;
  if (Array.isArray(res.workOrders) || Array.isArray(res)) return res;
  return null;
}

/**
 * 解包列表型 ApiResponse（如 /equipment/devices）。
 */
export function unwrapList(res) {
  const payload = unwrapPayload(res);
  return Array.isArray(payload) ? payload : [];
}
