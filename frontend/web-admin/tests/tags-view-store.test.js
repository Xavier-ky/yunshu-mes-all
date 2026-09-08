import assert from "node:assert/strict";
import test from "node:test";
import { createPinia, setActivePinia } from "pinia";

const values = new Map();
globalThis.sessionStorage = {
  getItem: (key) => values.get(key) ?? null,
  setItem: (key, value) => values.set(key, value),
  removeItem: (key) => values.delete(key),
  clear: () => values.clear(),
};

const storeModule = await import("../src/stores/tags-view.js").catch(() => ({}));
const { useTagsViewStore, TAGS_STORAGE_KEY } = storeModule;

function makeRoute(path, title = path) {
  return {
    path,
    fullPath: path,
    name: path === "/app" ? "new-dashboard" : path.slice(5).replaceAll("/", "-"),
    meta: { title },
  };
}

function createStore() {
  setActivePinia(createPinia());
  return useTagsViewStore();
}

test.beforeEach(() => values.clear());

test("persists added routes in session storage", () => {
  assert.equal(typeof useTagsViewStore, "function");
  const store = createStore();

  store.addRoute(makeRoute("/app/system/users", "用户管理"));

  assert.equal(store.tabs.length, 2);
  assert.equal(JSON.parse(sessionStorage.getItem(TAGS_STORAGE_KEY))[1].title, "用户管理");
});

test("closes other tabs while preserving home and active tab", () => {
  const store = createStore();
  store.addRoute(makeRoute("/app/a", "A"));
  store.addRoute(makeRoute("/app/b", "B"));

  store.closeOthers("/app/b");

  assert.deepEqual(store.tabs.map((tab) => tab.key), ["/app", "/app/b"]);
});

test("closing all tabs returns the fixed home destination", () => {
  const store = createStore();
  store.addRoute(makeRoute("/app/a", "A"));

  assert.equal(store.closeAll("/app/a"), "/app");
  assert.deepEqual(store.tabs.map((tab) => tab.key), ["/app"]);
});

test("restores valid tabs after refresh and clears them on logout", () => {
  sessionStorage.setItem(TAGS_STORAGE_KEY, JSON.stringify([
    { key: "/app", fullPath: "/app", path: "/app", title: "工作台", affix: true },
    { key: "/app/a", fullPath: "/app/a", path: "/app/a", title: "A", affix: false },
  ]));
  const store = createStore();
  const resolve = (fullPath) => ({
    ...makeRoute(fullPath, fullPath === "/app/a" ? "A" : "工作台"),
    matched: [{}],
  });

  store.restore(resolve);
  assert.deepEqual(store.tabs.map((tab) => tab.key), ["/app", "/app/a"]);

  store.clearSession();
  assert.deepEqual(store.tabs.map((tab) => tab.key), ["/app"]);
  assert.equal(sessionStorage.getItem(TAGS_STORAGE_KEY), null);
});
