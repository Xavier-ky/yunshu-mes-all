import { isTeamEnabled } from "../calendar/calScheduleModel.js";

export function createEmptyTeamForm() {
  return {
    teamId: null,
    teamCode: "",
    teamName: "",
    calendarType: "ZZ",
    remark: "",
    enableFlag: "Y",
  };
}

export function normalizeTeamRow(row = {}) {
  return {
    teamId: row.teamId ?? null,
    teamCode: row.teamCode || "",
    teamName: row.teamName || "",
    calendarType: row.calendarType || "ZZ",
    remark: row.remark || "",
    enableFlag: row.enableFlag ?? "Y",
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

export function computeTeamKpis(allTeams = [], filteredTotal = 0, memberTotal = 0) {
  const teams = Array.isArray(allTeams) ? allTeams : [];
  const enabled = teams.filter(isTeamEnabled).length;
  const disabled = teams.length - enabled;
  const manufacturing = teams.filter((team) => String(team.calendarType) === "ZZ").length;
  return {
    totalCount: teams.length,
    enabledCount: enabled,
    disabledCount: disabled,
    manufacturingCount: manufacturing,
    filteredCount: filteredTotal,
    memberTotal,
  };
}
