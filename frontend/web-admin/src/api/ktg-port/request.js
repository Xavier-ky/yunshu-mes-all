import axios from "axios";
import { clearAuthSession, getToken } from "@/utils/auth-session";

const service = axios.create({
  baseURL: "/api",
  timeout: 30000,
});

service.interceptors.request.use((config) => {
  const token = getToken();
  if (token) {
    config.headers.Authorization = `Bearer ${token}`;
  }
  return config;
});

service.interceptors.response.use(
  (res) => {
    const data = res.data;
    if (data && typeof data.code === "number" && data.code !== 200) {
      return Promise.reject(new Error(data.msg || "请求失败"));
    }
    if (data && data.success === false) {
      return Promise.reject(new Error(data.message || "请求失败"));
    }
    return data;
  },
  (error) => {
    if (error?.response?.status === 401) {
      clearAuthSession();
      if (location.pathname !== "/login") location.href = "/login";
    }
    const msg = error?.response?.data?.msg || error?.response?.data?.message || error.message;
    return Promise.reject(new Error(msg || "接口请求失败"));
  },
);

export default service;
