import request from "../request";

export function listWorkorderbom(query) {
  return request({ url: "/mes/pro/workorderbom/list", method: "get", params: query });
}

export function updateWorkorderbom(data) {
  return request({ url: "/mes/pro/workorderbom", method: "put", data });
}

export function delWorkorderbom(lineId) {
  return request({ url: `/mes/pro/workorderbom/${lineId}`, method: "delete" });
}
