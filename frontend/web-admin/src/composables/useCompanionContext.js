import { computed } from "vue";
import { useRoute } from "vue-router";

const IDENTIFIER_PATTERN = /^[A-Za-z0-9_-]{1,80}$/;

function readIdentifier(query, keys) {
  for (const key of keys) {
    const value = query[key];
    const text = Array.isArray(value) ? value[0] : value;
    if (typeof text === "string" && IDENTIFIER_PATTERN.test(text)) return text;
  }
  return undefined;
}

export function useCompanionContext() {
  const route = useRoute();
  return computed(() => ({
    route_path: route.path,
    route_title: String(route.meta.title || ""),
    work_order_no: readIdentifier(route.query, [
      "workOrderNo",
      "workorderNo",
      "wo",
    ]),
    line_id: readIdentifier(route.query, ["lineId", "line"]),
    device_id: readIdentifier(route.query, ["deviceId", "device"]),
    andon_id: readIdentifier(route.query, ["andonId", "eventId"]),
  }));
}
