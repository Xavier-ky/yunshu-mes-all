export default {
  reportUrl: "/ureport",
  /** hiPrint 中转服务地址，如 http://127.0.0.1:17521；留空则仅浏览器打印 */
  print_transfer_url: import.meta.env.VITE_PRINT_TRANSFER_URL || "",
  print_transfer_token: import.meta.env.VITE_PRINT_TRANSFER_TOKEN || "",
};
