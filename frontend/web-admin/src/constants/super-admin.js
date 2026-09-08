/**
 * 超级管理员（TESTER）— 全模块访问配置
 */

export { superAdminNav } from "./nav-modules.js";

export const SUPER_ADMIN_ROLE_CODES = ["TESTER"];

export function isSuperAdminRole(roleCode) {
  return SUPER_ADMIN_ROLE_CODES.includes(roleCode);
}

export function isSuperAdminUser(user = {}) {
  const codes = user.roleCodes || [];
  if (codes.some(isSuperAdminRole)) return true;
  const role = user.roles?.[0] || user.role || "";
  return role.includes("测试") || role === "TESTER";
}

/** 顶栏快捷入口（超级管理员） */
export const superAdminHeaderLinks = [
  { path: "/app/yunshu-ai", label: "云枢小智" },
  { path: "/app", label: "生产大屏" },
  { path: "/app/planning/scheduling", label: "排产派工" },
  { path: "/app/my-work", label: "我的工位" },
  { path: "/app/quality/analytics", label: "质检中心" },
  { path: "/app/system/access", label: "系统管理" },
  { path: "/app/andon", label: "安灯中心" },
];
