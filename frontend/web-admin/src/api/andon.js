import { request } from "./request";

export function fetchAndonEvents(params) { return request.get("/andon/events", { params }); }
export function fetchAndonTypes(params) { return request.get("/andon/types", { params }); }
export function fetchAndonReasons(params) { return request.get("/andon/reasons", { params }); }
