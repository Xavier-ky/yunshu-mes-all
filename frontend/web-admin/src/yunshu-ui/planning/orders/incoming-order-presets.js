function formatDate(date) {
  return date.toISOString().slice(0, 10);
}

function addDays(base, days) {
  const d = new Date(base);
  d.setDate(d.getDate() + days);
  return formatDate(d);
}

/** 演示用：模拟从外部系统推送的两条待接订单 */
export function buildIncomingOrderPresets(now = new Date()) {
  const stamp = formatDate(now).replace(/-/g, "");
  return [
    {
      id: "incoming-upc",
      refNo: `IN-${stamp}-UPC`,
      customerName: "中国石油大学（华东）",
      productCode: "FAN-FS40-A",
      productLabel: "40cm落地扇A型",
      orderQty: 12,
      deliveryDate: addDays(now, 14),
      receivedAt: new Date(now.getTime() - 95 * 60 * 1000).toISOString(),
      sourceLabel: "CRM推送",
    },
    {
      id: "incoming-midea",
      refNo: `IN-${stamp}-MD`,
      customerName: "美的（Midea）",
      productCode: "FAN-TS30-B",
      productLabel: "30cm台扇B型",
      orderQty: 8,
      deliveryDate: addDays(now, 21),
      receivedAt: new Date(now.getTime() - 38 * 60 * 1000).toISOString(),
      sourceLabel: "EDI推送",
    },
  ].sort((a, b) => new Date(b.receivedAt).getTime() - new Date(a.receivedAt).getTime());
}

export function presetToOrderForm(preset, productOptions = []) {
  const list = Array.isArray(productOptions) ? productOptions : [];
  const product =
    list.find((p) => p.productCode === preset.productCode) ||
    list.find((p) => String(p.productName || "").includes(preset.productLabel?.slice(0, 4) || ""));
  return {
    orderNo: preset.refNo,
    customerName: preset.customerName,
    productId: product?.productId ?? null,
    orderQty: preset.orderQty,
    deliveryDate: preset.deliveryDate,
    status: "CREATED",
    _incomingPresetId: preset.id,
  };
}

export const INCOMING_CONSUMED_KEY = "mes_consumed_incoming_orders";

export function loadConsumedIncomingIds() {
  try {
    const raw = sessionStorage.getItem(INCOMING_CONSUMED_KEY);
    const parsed = JSON.parse(raw || "[]");
    return Array.isArray(parsed) ? parsed : [];
  } catch {
    return [];
  }
}

export function saveConsumedIncomingIds(ids) {
  sessionStorage.setItem(INCOMING_CONSUMED_KEY, JSON.stringify(ids));
}
