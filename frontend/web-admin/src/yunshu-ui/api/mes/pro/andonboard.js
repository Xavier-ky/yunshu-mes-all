import { request } from "@/api/request";

export function getAndonBoardSummary() {
  return request.get("/andon/board/summary");
}

export function getAndonBoardActive() {
  return request.get("/andon/board/active");
}

export function getAndonBoardStations() {
  return request.get("/andon/board/stations");
}

export function getAndonBoardLines() {
  return request.get("/andon/board/lines");
}

export function getAndonBoardTrend() {
  return request.get("/andon/board/trend");
}

export function getAndonBoardTimeline(params) {
  return request.get("/andon/board/timeline", { params });
}
