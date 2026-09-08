import request from "@/yunshu-ui/api/request";

export function getQcSummary() {
  return request({ url: "/mes/qc/analytics/summary", method: "get" });
}

export function getQcTrend(days = 7) {
  return request({ url: "/mes/qc/analytics/trend", method: "get", params: { days } });
}

export function getQcTypeDistribution() {
  return request({ url: "/mes/qc/analytics/type-distribution", method: "get" });
}

export function getQcDefectTop(limit = 10) {
  return request({ url: "/mes/qc/analytics/defect-top", method: "get", params: { limit } });
}

export function getQcRecentFinished(limit = 8) {
  return request({ url: "/mes/qc/analytics/recent-finished", method: "get", params: { limit } });
}

export function getQcPendingDisposition(limit = 8) {
  return request({ url: "/mes/qc/analytics/pending-disposition", method: "get", params: { limit } });
}

export function getQcTypeVolume() {
  return request({ url: "/mes/qc/analytics/type-volume", method: "get" });
}

export function getQcTypeStats() {
  return request({ url: "/mes/qc/analytics/type-stats", method: "get" });
}

export function getQcDefectByLevel(days = 7) {
  return request({ url: "/mes/qc/analytics/defect-by-level", method: "get", params: { days } });
}

export function getQcActivityFeed(limit = 12) {
  return request({ url: "/mes/qc/analytics/activity-feed", method: "get", params: { limit } });
}
