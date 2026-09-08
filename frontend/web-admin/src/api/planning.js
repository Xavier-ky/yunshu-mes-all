import { request } from "./request";

export function fetchWorkOrders(params) {
  return request.get("/planning/work-orders", { params });
}
export function getWorkOrder(id) {
  return request.get(`/planning/work-orders/${id}`);
}
export function createWorkOrder(payload) {
  return request.post("/planning/work-orders", payload);
}
export function updateWorkOrder(id, payload) {
  return request.put(`/planning/work-orders/${id}`, payload);
}
export function deleteWorkOrder(id) {
  return request.delete(`/planning/work-orders/${id}`);
}

export function fetchOrders(params) {
  return request.get("/planning/orders", { params });
}
export function getOrder(id) {
  return request.get(`/planning/orders/${id}`);
}
export function createOrder(payload) {
  return request.post("/planning/orders", payload);
}
export function updateOrder(id, payload) {
  return request.put(`/planning/orders/${id}`, payload);
}
export function deleteOrder(id) {
  return request.delete(`/planning/orders/${id}`);
}

export function fetchProductionTasks(params) {
  return request.get("/planning/production-tasks", { params });
}
export function createProductionTask(payload) {
  return request.post("/planning/production-tasks", payload);
}
export function updateProductionTask(id, payload) {
  return request.put(`/planning/production-tasks/${id}`, payload);
}
export function deleteProductionTask(id) {
  return request.delete(`/planning/production-tasks/${id}`);
}

export function fetchDispatchTasks(params) {
  return request.get("/planning/dispatch-tasks", { params });
}
export function createDispatchTask(payload) {
  return request.post("/planning/dispatch-tasks", payload);
}
export function updateDispatchTask(id, payload) {
  return request.put(`/planning/dispatch-tasks/${id}`, payload);
}
export function deleteDispatchTask(id) {
  return request.delete(`/planning/dispatch-tasks/${id}`);
}

export function fetchKitting(workOrderId) {
  return request.get(`/planning/kitting/${workOrderId}`);
}
export function reserveKitting(workOrderId) {
  return request.post(`/planning/kitting/${workOrderId}/reserve`);
}
export function releaseKitting(workOrderId) {
  return request.post(`/planning/kitting/${workOrderId}/release`);
}
export function fetchWorkOrderProgress(id) {
  return request.get(`/planning/work-orders/${id}/progress`);
}
export function cancelWorkOrder(id) {
  return request.post(`/planning/work-orders/${id}/cancel`);
}
export function fetchMaterialShortages() {
  return request.get("/planning/material-shortages");
}
export function fetchTaskLogs(taskId) {
  return request.get(`/planning/task-logs/${taskId}`);
}
export function fetchPlanningSummary() {
  return request.get("/planning/summary");
}
export function fetchOrderItems(orderId) {
  return request.get(`/planning/orders/${orderId}/items`);
}
export function fetchSchedulingOverview() {
  return request.get("/planning/scheduling/overview");
}
export function createTaskLog(payload) {
  return request.post("/planning/task-logs", payload);
}
