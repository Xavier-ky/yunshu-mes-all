import request from "@/yunshu-ui/api/request";

export function getDvSummary() {
  return request.get("/mes/dv/workbench/summary");
}

export function listDvPending(params) {
  return request.get("/mes/dv/workbench/pending", { params });
}

export function getDvRecentFinished(limit = 8) {
  return request.get("/mes/dv/workbench/recent-finished", { params: { limit } });
}

export function getDvAnalyticsOverview() {
  return request.get("/mes/dv/workbench/analytics-overview");
}

export function createRepairFromAndon(recordId) {
  return request.post(`/mes/dv/workbench/repair-from-andon/${recordId}`);
}

export function getMachineryStationContext(machineryId) {
  return request.get("/mes/dv/workbench/station-context", { params: { machineryId } });
}
