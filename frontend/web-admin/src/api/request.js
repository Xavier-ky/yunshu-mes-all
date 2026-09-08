import axios from "axios";
import { clearAuthSession, getToken } from "@/utils/auth-session";

export const request = axios.create({
  baseURL: "/api",
  timeout: 8000,
});

request.interceptors.request.use((config) => {
  config.headers["X-Trace-Id"] = crypto.randomUUID?.() || String(Date.now());
  const token = getToken();
  if (token) {
    config.headers["Authorization"] = `Bearer ${token}`;
  }
  return config;
});

request.interceptors.response.use(
  (response) => response.data,
  (error) => {
    if (error?.response?.status === 401) {
      clearAuthSession();
      if (location.pathname !== "/login") {
        location.href = "/login";
      }
    }
    const message =
      error?.response?.data?.message || error?.message || "接口请求失败";
    return Promise.reject(new Error(message));
  },
);
