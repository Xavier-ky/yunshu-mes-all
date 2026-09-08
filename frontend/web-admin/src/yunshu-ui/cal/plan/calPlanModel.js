export function createEmptyPlanForm() {
  return {
    planId: null,
    planCode: "",
    planName: "",
    calendarType: "ZZ",
    startDate: "",
    endDate: "",
    shiftType: "SHIFT_TWO",
    shiftMethod: "MONTH",
    shiftCount: 1,
    status: "PREPARE",
    remark: "",
  };
}

export function normalizePlanRow(row = {}) {
  return {
    planId: row.planId ?? null,
    planCode: row.planCode || "",
    planName: row.planName || "",
    calendarType: row.calendarType || "ZZ",
    startDate: row.startDate || "",
    endDate: row.endDate || "",
    shiftType: row.shiftType || "SHIFT_TWO",
    shiftMethod: row.shiftMethod || "MONTH",
    shiftCount: Number(row.shiftCount) || 1,
    status: row.status || "PREPARE",
    remark: row.remark || "",
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

export function isPlanEditable(plan = {}) {
  return String(plan.status || "").toUpperCase() === "PREPARE";
}

export function planListStatusClass(status) {
  return String(status || "").toUpperCase() === "CONFIRMED" ? "is-confirmed" : "is-prepare";
}

function monthKey(date = new Date()) {
  return `${date.getFullYear()}-${String(date.getMonth() + 1).padStart(2, "0")}`;
}

function overlapsMonth(plan, key) {
  const start = String(plan.startDate || "").slice(0, 7);
  const end = String(plan.endDate || "").slice(0, 7);
  if (!start || !end) return false;
  return start <= key && end >= key;
}

export function computePlanKpis(allPlans = [], filteredTotal = 0, anchorDate = new Date()) {
  const plans = Array.isArray(allPlans) ? allPlans : [];
  const key = monthKey(anchorDate);
  const prepareCount = plans.filter((plan) => String(plan.status).toUpperCase() === "PREPARE").length;
  const confirmedCount = plans.filter((plan) => String(plan.status).toUpperCase() === "CONFIRMED").length;
  const monthActive = plans.filter((plan) => overlapsMonth(plan, key)).length;
  const shiftTwoCount = plans.filter((plan) => String(plan.shiftType).toUpperCase() === "SHIFT_TWO").length;
  const singleCount = plans.filter((plan) => String(plan.shiftType).toUpperCase() === "SINGLE").length;
  return {
    totalCount: plans.length,
    prepareCount,
    confirmedCount,
    monthActive,
    shiftTwoCount,
    singleCount,
    filteredCount: filteredTotal,
  };
}
