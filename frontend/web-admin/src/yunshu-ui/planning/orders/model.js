function text(value) {
  return String(value ?? "").trim().toLowerCase();
}

export function normalizeOrderResponse(response) {
  if (Array.isArray(response)) return response;
  if (Array.isArray(response?.data)) return response.data;
  if (Array.isArray(response?.rows)) return response.rows;
  return [];
}

export function filterOrders(orders, filters = {}) {
  const orderNo = text(filters.orderNo);
  const customerName = text(filters.customerName);
  const status = filters.status || "";
  const range = Array.isArray(filters.deliveryRange) ? filters.deliveryRange : [];
  const start = range[0] || "";
  const end = range[1] || "";

  return (Array.isArray(orders) ? orders : []).filter((order) => {
    const deliveryDate = String(order.deliveryDate || "");
    return (
      (!orderNo || text(order.orderNo).includes(orderNo)) &&
      (!customerName || text(order.customerName).includes(customerName)) &&
      (!status || order.status === status) &&
      (!start || deliveryDate >= start) &&
      (!end || deliveryDate <= end)
    );
  });
}

export function getOrderMetrics(orders, today = new Date().toISOString().slice(0, 10)) {
  const list = Array.isArray(orders) ? orders : [];
  const urgentEnd = new Date(`${today}T23:59:59`);
  urgentEnd.setDate(urgentEnd.getDate() + 7);
  const urgentDate = urgentEnd.toISOString().slice(0, 10);

  return {
    total: list.length,
    pending: list.filter((order) => order.status === "CREATED").length,
    executing: list.filter((order) => order.status === "CONFIRMED").length,
    urgent: list.filter((order) =>
      order.status !== "COMPLETED" &&
      order.deliveryDate &&
      order.deliveryDate >= today &&
      order.deliveryDate <= urgentDate
    ).length,
  };
}

export function paginateOrders(orders, page = 1, limit = 10) {
  const rows = Array.isArray(orders) ? orders : [];
  const safeLimit = Math.max(1, Number(limit) || 10);
  const safePage = Math.max(1, Number(page) || 1);
  const start = (safePage - 1) * safeLimit;
  return {
    rows: rows.slice(start, start + safeLimit),
    total: rows.length,
    page: safePage,
    limit: safeLimit,
  };
}
