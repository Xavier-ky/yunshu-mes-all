/**
 * hiprint 设计器运行时：formatDate、屏幕样式、全局插件注册。
 * 仅在 YunshuUiHost 内加载，避免污染非 yunshu-ui 页面。
 */
import { formatDate } from "@/yunshu-ui/utils/dateUtils";
import { hiPrintPlugin } from "vue-plugin-hiprint";
import webSite from "@/config/website";
import printLockCssUrl from "../../../public/print-lock.css?url";

if (typeof document !== "undefined" && !document.getElementById("yunshu-print-lock-css")) {
  const link = document.createElement("link");
  link.id = "yunshu-print-lock-css";
  link.rel = "stylesheet";
  link.href = printLockCssUrl;
  document.head.appendChild(link);
}

if (typeof window !== "undefined") {
  window.formatDate = formatDate;
}

let hiprintPluginInstalled = false;

/**
 * @param {import('vue').App} app
 */
export function installHiprintRuntime(app) {
  if (hiprintPluginInstalled || !app) return;
  app.use(hiPrintPlugin);
  if (typeof hiPrintPlugin.disAutoConnect === "function") {
    hiPrintPlugin.disAutoConnect();
  }
  app.config.globalProperties.website = {
    reportUrl: webSite.reportUrl || "/ureport",
    print_transfer_url: webSite.print_transfer_url || "",
    print_transfer_token: webSite.print_transfer_token || "",
  };
  hiprintPluginInstalled = true;
}

export { formatDate };
