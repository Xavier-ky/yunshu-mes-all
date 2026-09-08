/** auth 插件 — 云枢版默认放行 */
export default {
  hasPermi() {
    return true;
  },
  hasPermiOr() {
    return true;
  },
  hasPermiAnd() {
    return true;
  },
  hasRole() {
    return true;
  },
  hasRoleOr() {
    return true;
  },
  hasRoleAnd() {
    return true;
  },
};
