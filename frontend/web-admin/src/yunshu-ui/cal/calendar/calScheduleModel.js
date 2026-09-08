const pad = (value) => String(value).padStart(2, "0");

export function formatDate(value) {
  if (value instanceof Date && !Number.isNaN(value.getTime())) {
    return `${value.getFullYear()}-${pad(value.getMonth() + 1)}-${pad(value.getDate())}`;
  }
  const match = String(value || "").match(/^(\d{4})-(\d{1,2})-(\d{1,2})/);
  return match ? `${match[1]}-${pad(match[2])}-${pad(match[3])}` : "";
}

export function parseLocalDate(value) {
  const formatted = formatDate(value);
  if (!formatted) return new Date();
  const [year, month, day] = formatted.split("-").map(Number);
  return new Date(year, month - 1, day);
}

export function pickDefaultCalendarType(options = []) {
  const values = options.map((item) => String(item?.value ?? item ?? "")).filter(Boolean);
  return values.find((value) => value === "ZZ") || values[0] || "ZZ";
}

export function isTeamEnabled(team) {
  if (!team) return false;
  if (team.enableFlag != null) return String(team.enableFlag).toUpperCase() !== "N";
  if (team.status != null) return !["1", "DISABLED", "N"].includes(String(team.status).toUpperCase());
  return true;
}

export function pickDefaultTeam(teams = []) {
  return teams.find(isTeamEnabled) || null;
}

export function getSelectionGuidance(queryType, selection = {}) {
  if (queryType === "USER" && !selection.selectedUser?.userId) {
    return "请选择人员查看排班。";
  }
  if (queryType === "TEAM") {
    if (selection.teamLoadError) return "";
    const selectedId = String(selection.selectedTeamId ?? "");
    const validTeam = (selection.teams || []).some(
      (team) => String(team.teamId) === selectedId && isTeamEnabled(team),
    );
    if (!validTeam) return "暂无可用班组，请先启用或创建班组。";
  }
  return "";
}

export function createLatestRequestGuard() {
  let sequence = 0;
  return {
    begin() {
      sequence += 1;
      return sequence;
    },
    invalidate() {
      sequence += 1;
    },
    isCurrent(candidate) {
      return candidate === sequence;
    },
  };
}

export function normalizeHubMode(mode) {
  return ["overview", "holiday", "team", "plan"].includes(mode) ? mode : "overview";
}

export function shouldReloadCalendarType(previousType, nextType, queryType) {
  return queryType === "TYPE" && Boolean(nextType) && previousType !== nextType;
}

export function getDayStatus(day = {}) {
  const holidayType = String(day.holidayType || "").toUpperCase();
  if (holidayType === "WORKDAY") return { code: "ADJUSTED", label: "调" };
  if (day.workday) return { code: "SHIFT", label: "班" };
  return { code: "REST", label: "休" };
}

export function splitShiftChips(shifts, limit = 3) {
  const rows = Array.isArray(shifts) ? shifts : [];
  return {
    visible: rows.slice(0, limit),
    overflow: Math.max(0, rows.length - limit),
  };
}

export function calculateCoverage(scheduledDays, workdayCount) {
  const scheduled = Math.max(0, Number(scheduledDays) || 0);
  const workdays = Math.max(0, Number(workdayCount) || 0);
  if (workdays === 0) return 100;
  return Math.min(100, Math.round((scheduled * 1000) / workdays) / 10);
}

export function unwrapData(response, fallback) {
  if (response?.data != null) return response.data;
  return response ?? fallback;
}

export function normalizeCalendarDays(response) {
  const data = unwrapData(response, []);
  if (!Array.isArray(data)) return [];
  return data.map((day) => ({
    ...day,
    teamShifts: Array.isArray(day?.teamShifts) ? day.teamShifts : [],
    workday: Boolean(day?.workday),
    holidayType: day?.holidayType || (day?.workday ? "WEEKDAY" : "HOLIDAY"),
  }));
}

export function normalizeSummary(response) {
  const data = unwrapData(response, {}) || {};
  const workdayCount = Number(data.workdayCount) || 0;
  const scheduledDays = Number(data.scheduledDays) || 0;
  const month = String(data.month || "");
  const [year, monthNumber] = month.split("-").map(Number);
  const monthDays = year && monthNumber ? new Date(year, monthNumber, 0).getDate() : 31;
  const reportedCoverage = Number(data.coverageRate);
  return {
    todayOnDutyCount: Number(data.todayOnDutyCount) || 0,
    coverageRate: Number.isFinite(reportedCoverage)
      ? Math.max(0, Math.min(100, reportedCoverage))
      : calculateCoverage(scheduledDays, workdayCount),
    unscheduledWorkdays: Number.isFinite(Number(data.unscheduledWorkdays))
      ? Number(data.unscheduledWorkdays)
      : Math.max(0, workdayCount - scheduledDays),
    conflictCount: Number(data.conflictCount) || 0,
    holidayCount: Math.max(0, monthDays - workdayCount),
    workdayCount,
  };
}

export function normalizeWeek(response) {
  const data = unwrapData(response, {}) || {};
  return {
    startDate: formatDate(data.startDate),
    endDate: formatDate(data.endDate),
    dayTypes: data.dayTypes && typeof data.dayTypes === "object" ? data.dayTypes : {},
    teams: Array.isArray(data.teams) ? data.teams : [],
  };
}

export function normalizeDay(response) {
  const data = unwrapData(response, {}) || {};
  return {
    ...data,
    theDay: formatDate(data.theDay),
    teamShifts: Array.isArray(data.teamShifts) ? data.teamShifts : [],
  };
}

export function addDays(value, amount) {
  const date = parseLocalDate(value);
  date.setDate(date.getDate() + amount);
  return date;
}

export function addMonths(value, amount) {
  const date = parseLocalDate(value);
  date.setDate(1);
  date.setMonth(date.getMonth() + amount);
  return date;
}
