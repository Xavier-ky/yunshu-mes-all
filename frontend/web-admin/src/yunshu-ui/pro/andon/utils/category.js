const CATEGORIES = [
  { key: "material", label: "物料", tone: "tone-material", keywords: ["料", "物料"] },
  { key: "quality", label: "质量", tone: "tone-quality", keywords: ["质", "检"] },
  { key: "equipment", label: "设备", tone: "tone-equipment", keywords: ["设备", "机"] },
  { key: "process", label: "工艺", tone: "tone-process", keywords: ["工艺", "程"] },
];

export function categorizeReason(reason) {
  const t = String(reason || "").toLowerCase();
  for (const c of CATEGORIES) {
    if (c.keywords.some((k) => t.includes(k))) return c.key;
  }
  return "other";
}

export function reasonTone(reason) {
  const key = categorizeReason(reason);
  return CATEGORIES.find((c) => c.key === key)?.tone || "tone-default";
}

export function countByCategory(alerts) {
  const counts = { material: 0, quality: 0, equipment: 0, process: 0, other: 0 };
  (alerts || []).forEach((a) => {
    counts[categorizeReason(a.andonReason)] += 1;
  });
  return counts;
}

export { CATEGORIES };
