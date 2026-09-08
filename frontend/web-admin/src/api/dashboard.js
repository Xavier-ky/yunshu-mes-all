import { request } from "./request";

export async function fetchDashboardSummary() {
  return request.get("/dashboard/summary");
}
