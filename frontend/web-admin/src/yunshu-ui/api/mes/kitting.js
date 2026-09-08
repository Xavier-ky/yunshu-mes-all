import request from "@/yunshu-ui/api/request";

export function fetchKitting(workOrderId) {
  return request({
    url: `/planning/kitting/${workOrderId}`,
    method: "get",
  });
}

export function reserveKitting(workOrderId) {
  return request({
    url: `/planning/kitting/${workOrderId}/reserve`,
    method: "post",
  });
}

export function releaseKitting(workOrderId) {
  return request({
    url: `/planning/kitting/${workOrderId}/release`,
    method: "post",
  });
}
