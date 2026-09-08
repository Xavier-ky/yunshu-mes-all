import { request } from "./request";

export function fetchUsers(params = {}) {
  return request.get("/system/users", { params });
}

export function fetchUser(id) {
  return request.get(`/system/users/${id}`);
}

export function fetchRoles() {
  return request.get("/system/roles");
}

export function createUser(payload) {
  return request.post("/system/users", payload);
}

export function updateUser(id, payload) {
  return request.put(`/system/users/${id}`, payload);
}

export function deleteUser(id) {
  return request.delete(`/system/users/${id}`);
}
