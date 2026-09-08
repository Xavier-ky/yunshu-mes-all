export const TOKEN_KEY = "mes_token";
export const AUTH_USER_KEY = "mes_auth_user";
export const VISITED_TABS_KEY = "mes_visited_tabs";

let legacyPurged = false;

function authStorage() {
  return globalThis.sessionStorage;
}

function safelyRemove(storage, key) {
  try {
    storage?.removeItem(key);
  } catch {
    // Storage may be unavailable in privacy-restricted browser contexts.
  }
}

function purgeLegacyLocalAuthOnce() {
  if (legacyPurged) return;
  legacyPurged = true;
  safelyRemove(globalThis.localStorage, TOKEN_KEY);
  safelyRemove(globalThis.localStorage, AUTH_USER_KEY);
}

function clearLegacyAuthStorage() {
  safelyRemove(globalThis.localStorage, TOKEN_KEY);
  safelyRemove(globalThis.localStorage, AUTH_USER_KEY);
}

export function getToken() {
  purgeLegacyLocalAuthOnce();
  return authStorage()?.getItem(TOKEN_KEY) || "";
}

export function hasAuthToken() {
  return !!getToken();
}

export function setToken(token) {
  authStorage()?.setItem(TOKEN_KEY, token);
  clearLegacyAuthStorage();
}

export function getAuthUser() {
  purgeLegacyLocalAuthOnce();
  try {
    const raw = authStorage()?.getItem(AUTH_USER_KEY);
    return raw ? JSON.parse(raw) : null;
  } catch {
    return null;
  }
}

export function setAuthUser(user) {
  authStorage()?.setItem(AUTH_USER_KEY, JSON.stringify(user));
  clearLegacyAuthStorage();
}

export function clearAuthSession() {
  safelyRemove(authStorage(), TOKEN_KEY);
  safelyRemove(authStorage(), AUTH_USER_KEY);
  safelyRemove(globalThis.sessionStorage, VISITED_TABS_KEY);
  clearLegacyAuthStorage();
}
