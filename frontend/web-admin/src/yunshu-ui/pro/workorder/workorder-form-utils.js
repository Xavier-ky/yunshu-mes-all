/**
 * 工单编号：WO-YYYYMMDD-订单尾码/序号
 * 例：订单 CO-20260714-A01 → WO-20260714-A01
 */
export function buildWorkOrderCode(orderNo) {
  const now = new Date();
  const y = now.getFullYear();
  const m = String(now.getMonth() + 1).padStart(2, "0");
  const d = String(now.getDate()).padStart(2, "0");
  const datePart = `${y}${m}${d}`;

  if (orderNo) {
    const normalized = String(orderNo).trim().toUpperCase();
    const tail = normalized
      .replace(/^CO-/, "")
      .replace(/[^\dA-Z]/g, "")
      .slice(-8);
    if (tail) {
      return `WO-${datePart}-${tail}`;
    }
  }

  const seq = String(now.getTime()).slice(-4);
  return `WO-${datePart}-${seq}`;
}

export function defaultRequestDate(daysAhead = 14) {
  const dt = new Date();
  dt.setDate(dt.getDate() + daysAhead);
  return dt.toISOString().slice(0, 10);
}

/** 将 API 时间戳/ISO 字符串规范为 YYYY-MM-DD（供 Element Plus 日期控件） */
export function formatDateOnly(value) {
  if (value == null || value === "") return null;
  const d = new Date(value);
  if (Number.isNaN(d.getTime())) {
    const s = String(value);
    return /^\d{4}-\d{2}-\d{2}/.test(s) ? s.slice(0, 10) : s;
  }
  const y = d.getFullYear();
  const m = String(d.getMonth() + 1).padStart(2, "0");
  const day = String(d.getDate()).padStart(2, "0");
  return `${y}-${m}-${day}`;
}

export function orderOptionLabel(order) {
  if (!order) return "";
  const qty = Number(order.orderQty || 0).toLocaleString("zh-CN");
  const status = order.status === "CONFIRMED" ? "执行中" : order.status === "CREATED" ? "待确认" : order.status;
  return `${order.orderNo} · ${order.customerName || "—"} · ${order.productName || "—"} × ${qty}（${status}）`;
}

export function normalizeRecentOrders(orders, limit = 15) {
  const list = Array.isArray(orders) ? orders : [];
  return list
    .filter((o) => o.status === "CONFIRMED" || o.status === "CREATED")
    .sort((a, b) => (Number(b.orderId) || 0) - (Number(a.orderId) || 0))
    .slice(0, limit);
}
