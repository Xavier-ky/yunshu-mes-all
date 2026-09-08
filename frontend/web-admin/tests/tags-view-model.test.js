import assert from "node:assert/strict";
import test from "node:test";

const modelModule = await import("../src/stores/tags-view-model.js").catch(() => ({}));
const {
  createHomeTab,
  createTabFromRoute,
  addVisitedTab,
  closeVisitedTab,
  sanitizeVisitedTabs,
} = modelModule;

const home = {
  path: "/app",
  fullPath: "/app",
  name: "new-dashboard",
  meta: { title: "工作台" },
};

test("creates a fixed home tab", () => {
  assert.equal(typeof createHomeTab, "function");
  assert.deepEqual(createHomeTab(), {
    key: "/app",
    path: "/app",
    fullPath: "/app",
    name: "new-dashboard",
    title: "工作台",
    affix: true,
  });
});

test("collects all named app routes except Yunshu AI", () => {
  assert.equal(typeof createTabFromRoute, "function");
  assert.equal(createTabFromRoute(home)?.title, "工作台");
  assert.equal(
    createTabFromRoute({
      path: "/app/master-data/mditem/form/8",
      fullPath: "/app/master-data/mditem/form/8?mode=edit",
      name: "new-mditem-form",
      meta: { title: "物料详情", hidden: true },
    })?.key,
    "/app/master-data/mditem/form/8?mode=edit",
  );
  assert.equal(
    createTabFromRoute({
      path: "/app/yunshu-ai",
      fullPath: "/app/yunshu-ai",
      name: "yunshu-ai",
      meta: { title: "云枢小智" },
    }),
    null,
  );
  assert.deepEqual(
    createTabFromRoute({
      path: "/app",
      fullPath: "/app?from=header",
      name: "new-dashboard",
      meta: { title: "工作台" },
    }),
    createHomeTab(),
  );
});

test("deduplicates tabs by fullPath while keeping distinct query tabs", () => {
  assert.equal(typeof addVisitedTab, "function");
  const workOrders = {
    key: "/app/planning/work-orders",
    path: "/app/planning/work-orders",
    fullPath: "/app/planning/work-orders",
    name: "new-work-orders",
    title: "工单中心",
    affix: false,
  };
  const filtered = { ...workOrders, key: `${workOrders.key}?status=RUNNING`, fullPath: `${workOrders.fullPath}?status=RUNNING` };

  assert.deepEqual(addVisitedTab([createHomeTab(), workOrders], workOrders), [createHomeTab(), workOrders]);
  assert.deepEqual(addVisitedTab([createHomeTab(), workOrders], filtered), [createHomeTab(), workOrders, filtered]);
});

test("closing the active tab selects the nearest tab on its left", () => {
  assert.equal(typeof closeVisitedTab, "function");
  const tabs = [
    createHomeTab(),
    { key: "/app/a", fullPath: "/app/a", path: "/app/a", title: "A", affix: false },
    { key: "/app/b", fullPath: "/app/b", path: "/app/b", title: "B", affix: false },
  ];

  assert.deepEqual(closeVisitedTab(tabs, "/app/b", "/app/b"), {
    tabs: tabs.slice(0, 2),
    nextPath: "/app/a",
  });
  assert.deepEqual(closeVisitedTab(tabs, "/app", "/app"), {
    tabs,
    nextPath: null,
  });
});

test("restores only valid, authorized app tabs and always keeps home first", () => {
  assert.equal(typeof sanitizeVisitedTabs, "function");
  const saved = [
    { key: "/app/yunshu-ai", fullPath: "/app/yunshu-ai", path: "/app/yunshu-ai", title: "云枢小智" },
    { key: "/app/system/users", fullPath: "/app/system/users", path: "/app/system/users", title: "用户管理" },
    { key: "/app/equipment/machinery", fullPath: "/app/equipment/machinery", path: "/app/equipment/machinery", title: "设备台账" },
    { key: "/missing", fullPath: "/missing", path: "/missing", title: "失效页面" },
  ];
  const resolve = (fullPath) => ({
    matched: fullPath.startsWith("/app/") && fullPath !== "/missing" ? [{}] : [],
    path: fullPath.split("?")[0],
    fullPath,
    name: fullPath === "/app/system/users" ? "sys-users" : fullPath === "/app/equipment/machinery" ? "dv-machinery" : "yunshu-ai",
    meta: { title: fullPath === "/app/system/users" ? "用户管理" : fullPath === "/app/equipment/machinery" ? "设备台账" : "云枢小智" },
  });

  assert.deepEqual(sanitizeVisitedTabs(saved, resolve, (route) => route.name !== "dv-machinery"), [
    createHomeTab(),
    {
      key: "/app/system/users",
      fullPath: "/app/system/users",
      path: "/app/system/users",
      name: "sys-users",
      title: "用户管理",
      affix: false,
    },
  ]);
});
