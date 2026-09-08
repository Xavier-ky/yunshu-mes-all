import request from "@/yunshu-ui/api/request";

export function getPrintSummary() {
  return request({ url: "/print/summary", method: "get" });
}
