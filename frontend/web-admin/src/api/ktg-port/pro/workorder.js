import request from "../request";

export function listWorkorder(query) {
  return request({ url: "/mes/pro/workorder/list", method: "get", params: query });
}

export function getWorkorder(workorderId) {
  return request({ url: `/mes/pro/workorder/${workorderId}`, method: "get" });
}

export function addWorkorder(data) {
  return request({ url: "/mes/pro/workorder", method: "post", data });
}

export function updateWorkorder(data) {
  return request({ url: "/mes/pro/workorder", method: "put", data });
}

export function delWorkorder(workorderId) {
  return request({ url: `/mes/pro/workorder/${workorderId}`, method: "delete" });
}

export function listItems(query) {
  return request({ url: "/mes/pro/workorder/listItems", method: "get", params: query });
}

export function dofinish(workorderId) {
  return request({ url: `/mes/pro/workorder/finish/${workorderId}`, method: "put" });
}

export function doCancel(workorderId) {
  return request({ url: `/mes/pro/workorder/cancel/${workorderId}`, method: "put" });
}
