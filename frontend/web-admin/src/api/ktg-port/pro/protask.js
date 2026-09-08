import request from "../request";

export function listProtask(query) {
  return request({ url: "/mes/pro/protask/list", method: "get", params: query });
}

export function listGanttTaskList(query) {
  return request({ url: "/mes/pro/protask/listGanttTaskList", method: "get", params: query });
}

export function listTaskListByWorkorder(query) {
  return request({ url: "/mes/pro/protask/listTaskListByWorkorder", method: "get", params: query });
}

export function getProtask(taskId) {
  return request({ url: `/mes/pro/protask/${taskId}`, method: "get" });
}

export function addProtask(data) {
  return request({ url: "/mes/pro/protask", method: "post", data });
}

export function updateProtask(data) {
  return request({ url: "/mes/pro/protask", method: "put", data });
}

export function delProtask(taskId) {
  return request({ url: `/mes/pro/protask/${taskId}`, method: "delete" });
}
