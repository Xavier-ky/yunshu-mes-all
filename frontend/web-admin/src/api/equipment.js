import { request } from "./request";

export function fetchDevices(params) { return request.get("/equipment/devices", { params }); }
export function createDevice(payload) { return request.post("/equipment/devices", payload); }
export function updateDevice(id, payload) { return request.put(`/equipment/devices/${id}`, payload); }
export function deleteDevice(id) { return request.delete(`/equipment/devices/${id}`); }

export function fetchRepairOrders(params) { return request.get("/equipment/repair-orders", { params }); }
export function createRepairOrder(payload) { return request.post("/equipment/repair-orders", payload); }
export function updateRepairOrder(id, payload) { return request.put(`/equipment/repair-orders/${id}`, payload); }
