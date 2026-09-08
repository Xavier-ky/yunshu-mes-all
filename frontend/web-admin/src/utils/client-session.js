export const CLIENT_SESSION_KEY = "mes_client_session";
export const FIXED_CLIENT_CUSTOMER = "软件2407隋若愚";

export function setClientSession() {
  localStorage.setItem(
    CLIENT_SESSION_KEY,
    JSON.stringify({ customerName: FIXED_CLIENT_CUSTOMER, loginAt: Date.now() }),
  );
}

export function getClientSession() {
  try {
    const raw = localStorage.getItem(CLIENT_SESSION_KEY);
    return raw ? JSON.parse(raw) : null;
  } catch {
    return null;
  }
}

export function clearClientSession() {
  localStorage.removeItem(CLIENT_SESSION_KEY);
}

export function hasClientSession() {
  return !!getClientSession()?.customerName;
}
