import assert from "node:assert/strict";
import test from "node:test";

const model = await import("../src/yunshu-ui/planning/orders/model.js").catch(() => ({}));
const {
  filterOrders,
  getOrderMetrics,
  paginateOrders,
  normalizeOrderResponse,
} = model;

const orders = [
  { orderId: 1, orderNo: "SO-001", customerName: "华东客户", status: "CREATED", orderQty: 100, deliveryDate: "2026-07-12" },
  { orderId: 2, orderNo: "SO-002", customerName: "南方客户", status: "CONFIRMED", orderQty: 200, deliveryDate: "2026-07-15" },
  { orderId: 3, orderNo: "SO-003", customerName: "北方客户", status: "COMPLETED", orderQty: 300, deliveryDate: "2026-08-01" },
];

test("filters order number, customer, status and delivery range", () => {
  assert.equal(typeof filterOrders, "function");
  assert.deepEqual(
    filterOrders(orders, {
      orderNo: "002",
      customerName: "",
      status: "CONFIRMED",
      deliveryRange: ["2026-07-01", "2026-07-31"],
    }),
    [orders[1]],
  );
});

test("calculates command center metrics from current orders", () => {
  assert.equal(typeof getOrderMetrics, "function");
  assert.deepEqual(getOrderMetrics(orders, "2026-07-11"), {
    total: 3,
    pending: 1,
    executing: 1,
    urgent: 2,
  });
});

test("paginates filtered orders with stable total", () => {
  assert.equal(typeof paginateOrders, "function");
  assert.deepEqual(paginateOrders(orders, 2, 2), {
    rows: [orders[2]],
    total: 3,
    page: 2,
    limit: 2,
  });
});

test("normalizes API response safely", () => {
  assert.equal(typeof normalizeOrderResponse, "function");
  assert.deepEqual(normalizeOrderResponse({ data: orders }), orders);
  assert.deepEqual(normalizeOrderResponse({ rows: orders }), orders);
  assert.deepEqual(normalizeOrderResponse(null), []);
});
