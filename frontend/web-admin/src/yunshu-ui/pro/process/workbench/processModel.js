export function createEmptyProcessForm() {
  return {
    processId: null,
    processCode: "",
    processName: "",
    attention: "",
    enableFlag: "Y",
    remark: "",
    attr2: "",
  };
}

export function normalizeProcessRow(row = {}) {
  return {
    processId: row.processId ?? null,
    processCode: row.processCode || "",
    processName: row.processName || "",
    attention: row.attention || "",
    enableFlag: row.enableFlag ?? "Y",
    remark: row.remark || "",
    attr2: row.attr2 || "",
  };
}

export function unwrapRows(response) {
  if (Array.isArray(response?.rows)) return response.rows;
  if (Array.isArray(response?.data)) return response.data;
  return [];
}

export function unwrapTotal(response, fallback = 0) {
  const total = Number(response?.total);
  return Number.isFinite(total) ? total : fallback;
}

export function sortProcessesAsc(processes = []) {
  return [...processes].sort((a, b) => Number(a.processId) - Number(b.processId));
}

export function pickDefaultProcess(processes = []) {
  const sorted = sortProcessesAsc(processes);
  return sorted.find((p) => p.processCode === "STEP-MOTOR") || sorted[0] || null;
}

export function computeProcessKpis(allProcesses = [], filteredTotal = 0) {
  const list = Array.isArray(allProcesses) ? allProcesses : [];
  const enabled = list.filter((p) => p.enableFlag === "Y").length;
  return {
    totalCount: list.length,
    enabledCount: enabled,
    disabledCount: list.length - enabled,
    filteredCount: filteredTotal,
  };
}
