import axios from "axios";

const clientRequest = axios.create({
  baseURL: "/api/client",
  timeout: 15000,
});

clientRequest.interceptors.request.use((config) => {
  config.headers["X-Trace-Id"] = crypto.randomUUID?.() || String(Date.now());
  return config;
});

clientRequest.interceptors.response.use(
  (response) => response.data,
  (error) => {
    const message =
      error?.response?.data?.message || error?.message || "接口请求失败";
    return Promise.reject(new Error(message));
  },
);

export function fetchClientCatalog() {
  return clientRequest.get("/catalog");
}

export function submitClientOrder(payload) {
  return clientRequest.post("/orders", payload);
}

export function fetchClientOrders() {
  return clientRequest.get("/orders");
}
