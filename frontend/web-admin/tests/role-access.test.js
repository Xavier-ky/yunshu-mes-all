import assert from "node:assert/strict";
import test from "node:test";

const {
  canRoleAccessPath,
  matchPermission,
  checkPermiForAuth,
  resolveDefaultHomePath,
  normalizeRoleCode,
} = await import("../src/constants/role-access.js");

test("normalizeRoleCode maps Chinese labels", () => {
  assert.equal(normalizeRoleCode("生产主管"), "PROD_SUPERVISOR");
  assert.equal(normalizeRoleCode("MANAGER"), "MANAGER");
});

test("warehouse clerk has yunshu-ai and overview but not work-order editor", () => {
  assert.equal(canRoleAccessPath("WAREHOUSE_CLERK", "/app/yunshu-ai"), true);
  assert.equal(canRoleAccessPath("WAREHOUSE_CLERK", "/app/line-monitor"), true);
  assert.equal(canRoleAccessPath("WAREHOUSE_CLERK", "/app/inventory/wmstock"), true);
  assert.equal(canRoleAccessPath("WAREHOUSE_CLERK", "/app/planning/progress"), true);
  assert.equal(canRoleAccessPath("WAREHOUSE_CLERK", "/app/planning/work-orders"), false);
});

test("line operator has yunshu-ai and my-work", () => {
  assert.equal(canRoleAccessPath("LINE_OPERATOR", "/app/yunshu-ai"), true);
  assert.equal(canRoleAccessPath("LINE_OPERATOR", "/app/my-work"), true);
  assert.equal(canRoleAccessPath("LINE_OPERATOR", "/app/system/access"), false);
});

test("prod supervisor can access planning routes", () => {
  assert.equal(canRoleAccessPath("PROD_SUPERVISOR", "/app/planning/work-orders"), true);
  assert.equal(canRoleAccessPath("PROD_SUPERVISOR", "/app/system/access"), false);
});

test("matchPermission uses prefix rules and db permissions", () => {
  assert.equal(
    matchPermission("PROD_SUPERVISOR", [], "mes:pro:workorder:add"),
    true,
  );
  assert.equal(
    matchPermission("WAREHOUSE_CLERK", [], "mes:pro:workorder:add"),
    false,
  );
  assert.equal(
    matchPermission("MANAGER", ["system:user:list"], "system:user:list"),
    true,
  );
});

test("checkPermiForAuth respects stored permissions", () => {
  const authUser = {
    roleCode: "WAREHOUSE_CLERK",
    permissions: ["system:user:list"],
  };
  assert.equal(checkPermiForAuth(authUser, ["system:user:list"]), true);
  assert.equal(checkPermiForAuth(authUser, ["system:user:remove"]), false);
  assert.equal(checkPermiForAuth(authUser, ["mes:wm:stock:query"]), true);
});

test("resolveDefaultHomePath per role", () => {
  assert.equal(resolveDefaultHomePath("WAREHOUSE_CLERK"), "/app/line-monitor");
  assert.equal(resolveDefaultHomePath("PROD_SUPERVISOR"), "/app/line-monitor");
  assert.equal(resolveDefaultHomePath("LINE_OPERATOR"), "/app/line-monitor");
});
