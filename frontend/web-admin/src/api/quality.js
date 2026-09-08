import { request } from "./request";

export function fetchQualityTasks(params) { return request.get("/quality/tasks", { params }); }
export function createQualityTask(payload) { return request.post("/quality/tasks", payload); }
export function updateQualityTask(id, payload) { return request.put(`/quality/tasks/${id}`, payload); }
export function deleteQualityTask(id) { return request.delete(`/quality/tasks/${id}`); }

export function fetchDefects(params) { return request.get("/quality/defects", { params }); }
export function createDefect(payload) { return request.post("/quality/defects", payload); }
export function updateDefect(id, payload) { return request.put(`/quality/defects/${id}`, payload); }
export function deleteDefect(id) { return request.delete(`/quality/defects/${id}`); }
