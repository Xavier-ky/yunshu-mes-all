/**
 * 角色访问注册表 — 以 role_code 为唯一键，统一侧栏、路由、按钮权限前缀。
 */
import { isSuperAdminRole } from "./super-admin.js";
import {
  superAdminNav,
  navManager,
  navProdSupervisor,
  navWarehouse,
  navQualityRole,
  navEquipment,
  navOperator,
  collectRoutePrefixes,
} from "./nav-modules.js";

const ROLE_NAME_TO_CODE = {
  管理人员: "MANAGER",
  生产主管: "PROD_SUPERVISOR",
  仓库物料员: "WAREHOUSE_CLERK",
  质检员: "QUALITY_INSPECTOR",
  设备维修员: "EQUIPMENT_MAINTAINER",
  产线操作工人: "LINE_OPERATOR",
  测试人员: "TESTER",
};

const DEFAULT_HOME = "/app/line-monitor";

function roleEntry(nav, overrides = {}) {
  return {
    defaultHome: DEFAULT_HOME,
    permissionPrefixes: [],
    permissionExtras: [],
    routePrefixes: collectRoutePrefixes(nav, overrides.routeExtras || []),
    ...overrides,
    nav,
  };
}

/** @type {Record<string, import('./role-access-types').RoleAccessEntry>} */
export const ROLE_ACCESS_REGISTRY = {
  MANAGER: roleEntry(navManager, {
    label: "管理人员",
    permissionPrefixes: ["system:", "monitor:", "mes:"],
  }),
  PROD_SUPERVISOR: roleEntry(navProdSupervisor, {
    label: "生产主管",
    permissionPrefixes: [
      "mes:pro:",
      "mes:cal:",
      "mes:md:",
      "mes:wm:",
      "mes:qc:",
      "mes:dv:",
      "mes:pro:andon:",
    ],
  }),
  WAREHOUSE_CLERK: roleEntry(navWarehouse, {
    label: "仓库物料员",
    permissionPrefixes: ["mes:wm:", "mes:pro:progress:"],
  }),
  QUALITY_INSPECTOR: roleEntry(navQualityRole, {
    label: "质检员",
    permissionPrefixes: ["mes:qc:", "mes:wm:", "mes:pro:progress:"],
  }),
  EQUIPMENT_MAINTAINER: roleEntry(navEquipment, {
    label: "设备维修员",
    permissionPrefixes: ["mes:dv:", "mes:pro:progress:"],
  }),
  LINE_OPERATOR: roleEntry(navOperator, {
    label: "产线操作工人",
    permissionPrefixes: [
      "mes:pro:feedback:",
      "mes:pro:procard:",
      "mes:pro:protask:",
      "mes:pro:andon:",
    ],
  }),
  TESTER: {
    label: "测试人员",
    nav: superAdminNav,
    defaultHome: DEFAULT_HOME,
    routePrefixes: ["/"],
    allRoutes: true,
    permissionPrefixes: ["system:", "monitor:", "mes:"],
    permissionExtras: ["*:*:*:*"],
  },
};

export function normalizeRoleCode(role) {
  if (!role) return "";
  const raw = String(role).trim();
  if (ROLE_ACCESS_REGISTRY[raw]) return raw;
  if (ROLE_NAME_TO_CODE[raw]) return ROLE_NAME_TO_CODE[raw];
  return raw;
}

export function resolveRoleCodeFromAuth(authUser = {}) {
  if (authUser.roleCode) return normalizeRoleCode(authUser.roleCode);
  const codes = authUser.roleCodes || [];
  if (codes.length) return normalizeRoleCode(codes[0]);
  const role = authUser.roles?.[0] || authUser.role || "";
  return normalizeRoleCode(role);
}

export function getRoleAccess(roleCode) {
  const code = normalizeRoleCode(roleCode);
  if (ROLE_ACCESS_REGISTRY[code]) return ROLE_ACCESS_REGISTRY[code];
  return ROLE_ACCESS_REGISTRY.MANAGER;
}

export function getNavForRole(roleCode) {
  if (isSuperAdminRole(normalizeRoleCode(roleCode))) return superAdminNav;
  return getRoleAccess(roleCode).nav || navManager;
}

export function resolveDefaultHomePath(roleCode) {
  if (isSuperAdminRole(normalizeRoleCode(roleCode))) {
    return ROLE_ACCESS_REGISTRY.TESTER.defaultHome;
  }
  return getRoleAccess(roleCode).defaultHome || DEFAULT_HOME;
}

function normalizePath(path) {
  const base = String(path || "").split("?")[0].replace(/\/+$/, "") || "/";
  return base;
}

export function canRoleAccessPath(roleCode, path) {
  const code = normalizeRoleCode(roleCode);
  if (isSuperAdminRole(code)) return true;
  const access = getRoleAccess(code);
  if (access.allRoutes) return true;

  const normalized = normalizePath(path);
  for (const allowed of access.routePrefixes || []) {
    if (allowed === "/app" && normalized === "/app") return true;
    if (allowed !== "/app" && (normalized === allowed || normalized.startsWith(`${allowed}/`))) {
      return true;
    }
  }
  return false;
}

export function matchPermission(roleCode, permissions, requiredPerm) {
  if (!requiredPerm) return false;
  const code = normalizeRoleCode(roleCode);
  if (isSuperAdminRole(code)) return true;

  const perms = Array.isArray(permissions) ? permissions : [];
  if (perms.includes(requiredPerm)) return true;

  const access = getRoleAccess(code);
  if ((access.permissionExtras || []).includes(requiredPerm)) return true;
  if ((access.permissionExtras || []).includes("*:*:*:*")) return true;

  for (const prefix of access.permissionPrefixes || []) {
    if (requiredPerm.startsWith(prefix)) return true;
  }
  return false;
}

export function checkPermiForAuth(authUser, required) {
  if (!required || !Array.isArray(required) || !required.length) return false;
  const roleCode = resolveRoleCodeFromAuth(authUser);
  if (isSuperAdminRole(roleCode)) return true;
  const permissions = authUser?.permissions || [];
  return required.some((perm) => matchPermission(roleCode, permissions, perm));
}

/** @deprecated 使用 getNavForRole；保留兼容旧 import */
export const roleNavMap = {
  ...ROLE_NAME_TO_CODE,
  ...Object.fromEntries(
    Object.entries(ROLE_ACCESS_REGISTRY).flatMap(([code, entry]) => [
      [code, entry.nav],
      [entry.label, entry.nav],
    ]),
  ),
  TESTER: superAdminNav,
  测试人员: superAdminNav,
};
