import { getAuthUser } from "@/utils/auth-context";
import {
  checkPermiForAuth,
  resolveRoleCodeFromAuth,
  normalizeRoleCode,
} from "@/constants/role-access";
import { isSuperAdminRole } from "@/constants/super-admin";

export function checkPermi(value) {
  if (value && Array.isArray(value) && value.length > 0) {
    return checkPermiForAuth(getAuthUser() || {}, value);
  }
  return false;
}

export function checkRole(value) {
  if (!value || !Array.isArray(value) || !value.length) {
    return false;
  }
  const roleCode = resolveRoleCodeFromAuth(getAuthUser() || {});
  if (isSuperAdminRole(roleCode)) {
    return true;
  }
  return value.some((role) => role === roleCode || normalizeRoleCode(role) === roleCode);
}
