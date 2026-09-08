import { request } from "./request";

export function fetchWorkshops(params) { return request.get("/factory/workshops", { params }); }
export function createWorkshop(payload) { return request.post("/factory/workshops", payload); }
export function updateWorkshop(id, payload) { return request.put(`/factory/workshops/${id}`, payload); }
export function deleteWorkshop(id) { return request.delete(`/factory/workshops/${id}`); }

export function fetchLines(params) { return request.get("/factory/lines", { params }); }
export function createLine(payload) { return request.post("/factory/lines", payload); }
export function updateLine(id, payload) { return request.put(`/factory/lines/${id}`, payload); }
export function deleteLine(id) { return request.delete(`/factory/lines/${id}`); }

export function fetchWorkstations(params) { return request.get("/factory/workstations", { params }); }
export function createWorkstation(payload) { return request.post("/factory/workstations", payload); }
export function updateWorkstation(id, payload) { return request.put(`/factory/workstations/${id}`, payload); }
export function deleteWorkstation(id) { return request.delete(`/factory/workstations/${id}`); }

export function fetchShifts(params) { return request.get("/factory/shifts", { params }); }
