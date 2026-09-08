import { getAuthUser as readAuthUser } from "@/utils/auth-session";
import { resolveRoleCodeFromAuth } from "@/constants/role-access";

export function getAuthUser() {
  return readAuthUser();
}

export function getAuthRoleCode() {
  return resolveRoleCodeFromAuth(getAuthUser() || {});
}

export function getAuthPermissions() {
  const user = getAuthUser();
  return Array.isArray(user?.permissions) ? user.permissions : [];
}
