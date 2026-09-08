import request from "../request";

export function listProductProcess(productId) {
  return request({ url: `/mes/pro/routeprocess/listProductProcess/${productId}`, method: "get" });
}
