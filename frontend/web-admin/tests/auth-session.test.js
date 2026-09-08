import assert from "node:assert/strict";
import test from "node:test";

const localValues = new Map();
const sessionValues = new Map();
globalThis.localStorage = {
  removeItem: (key) => localValues.delete(key),
};
globalThis.sessionStorage = {
  removeItem: (key) => sessionValues.delete(key),
};

const authModule = await import("../src/utils/auth-session.js").catch(() => ({}));
const { clearAuthSession } = authModule;

test("clears authentication and visited tabs together", () => {
  assert.equal(typeof clearAuthSession, "function");
  localValues.set("mes_token", "token");
  localValues.set("mes_auth_user", "{}");
  sessionValues.set("mes_visited_tabs", "[]");

  clearAuthSession();

  assert.equal(localValues.has("mes_token"), false);
  assert.equal(localValues.has("mes_auth_user"), false);
  assert.equal(sessionValues.has("mes_visited_tabs"), false);
});

test("does not throw when browser storage is unavailable", () => {
  globalThis.sessionStorage.removeItem = () => {
    throw new Error("storage denied");
  };

  assert.doesNotThrow(() => clearAuthSession());
});
