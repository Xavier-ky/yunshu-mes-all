import request from "@/yunshu-ui/api/request";

export function getAccessSummary() {
  return request({
    url: "/system/access/summary",
    method: "get",
  });
}

export function getAccessOrgStats() {
  return request({
    url: "/system/access/org-stats",
    method: "get",
  });
}

export function getAccessRoleStats() {
  return request({
    url: "/system/access/role-stats",
    method: "get",
  });
}
