import calendar from "../../utils/calendar.js";
import {
  formatDate,
  getDayStatus,
  normalizeCalendarDays,
  parseLocalDate,
} from "../calendar/calScheduleModel.js";

export function normalizeHolidayOverrides(response) {
  const data = Array.isArray(response?.data)
    ? response.data
    : Array.isArray(response)
      ? response
      : [];
  const map = new Map();
  data.forEach((row) => {
    const theDay = formatDate(row?.theDay);
    if (!theDay) return;
    map.set(theDay, {
      holidayId: row.holidayId,
      holidayType: String(row.holidayType || "").toUpperCase(),
      remark: String(row.remark || "").trim(),
    });
  });
  return map;
}

export function getRestKind(day = {}, override = null) {
  const status = getDayStatus(day);
  if (status.code === "ADJUSTED") return null;
  if (status.code !== "REST") return null;
  if (override?.holidayType === "HOLIDAY") return "explicit";
  const date = parseLocalDate(day.theDay);
  if (date.getDay() === 0) return "default";
  return override ? "explicit" : "default";
}

export function getStatusBadgeLabel(day, restKind) {
  const status = getDayStatus(day);
  if (status.code === "ADJUSTED") return "调";
  if (status.code === "REST") return restKind === "explicit" ? "假日" : "休";
  return "班";
}

export function getDayVisualClass(day, restKind) {
  const status = getDayStatus(day);
  if (status.code === "ADJUSTED") return "cal-workbench-day--adjusted";
  if (status.code === "REST") {
    return restKind === "explicit"
      ? "cal-holiday-day--rest-holiday"
      : "cal-holiday-day--rest-default";
  }
  return "cal-workbench-day--shift";
}

export function getLunarInfo(theDay) {
  const [year, month, day] = String(theDay || "").split("-").map(Number);
  if (!year || !month || !day) {
    return { text: "", festival: false, festivalText: "" };
  }
  try {
    const lunar = calendar.solar2lunar(year, month, day);
    const festivalText = [lunar.festival, lunar.lunarFestival, lunar.Term]
      .filter(Boolean)
      .join(" ");
    return {
      text: festivalText || `${lunar.IMonthCn}${lunar.IDayCn}`,
      festival: Boolean(festivalText),
      festivalText,
    };
  } catch {
    return { text: "", festival: false, festivalText: "" };
  }
}

export function getDaySourceLabel(day = {}, override = null) {
  if (override) return "手动设置";
  const status = getDayStatus(day);
  if (status.code === "REST") return "系统默认（周日休息）";
  return "系统默认（工作日）";
}

export function getDayTypeLabel(day = {}, override = null) {
  const status = getDayStatus(day);
  if (status.code === "ADJUSTED") return "调休上班";
  if (status.code === "REST") {
    return override?.holidayType === "HOLIDAY" || getRestKind(day, override) === "explicit"
      ? "法定/手动假日"
      : "默认休息日";
  }
  return "工作日";
}

export function mergeHolidayDays(calendarDays, overrideMap) {
  const overrides = overrideMap instanceof Map ? overrideMap : new Map();
  return normalizeCalendarDays(calendarDays).map((day) => {
    const override = overrides.get(day.theDay) || null;
    const restKind = getRestKind(day, override);
    return {
      ...day,
      override,
      restKind,
      status: getDayStatus(day),
      badgeLabel: getStatusBadgeLabel(day, restKind),
      visualClass: getDayVisualClass(day, restKind),
      lunar: getLunarInfo(day.theDay),
      sourceLabel: getDaySourceLabel(day, override),
      typeLabel: getDayTypeLabel(day, override),
    };
  });
}

export function computeHolidayKpis(days) {
  const list = Array.isArray(days) ? days : [];
  let workdayCount = 0;
  let restCount = 0;
  let explicitHoliday = 0;
  let adjustedWorkday = 0;
  let defaultSunday = 0;
  let festivalCount = 0;

  list.forEach((day) => {
    if (day.workday) workdayCount += 1;
    else restCount += 1;
    if (day.override?.holidayType === "HOLIDAY") explicitHoliday += 1;
    if (day.override?.holidayType === "WORKDAY") adjustedWorkday += 1;
    if (day.restKind === "default") defaultSunday += 1;
    if (day.lunar?.festival) festivalCount += 1;
  });

  return {
    restCount,
    workdayCount,
    explicitHoliday,
    adjustedWorkday,
    defaultSunday,
    festivalCount,
  };
}

export function findEnrichedDay(days, theDay) {
  const target = formatDate(theDay);
  return (Array.isArray(days) ? days : []).find((day) => day.theDay === target) || null;
}

export const HOLIDAY_LEGEND = [
  { key: "shift", label: "班", className: "cal-holiday-legend__sample is-shift" },
  { key: "rest-default", label: "休·默认", className: "cal-holiday-legend__sample is-rest-default" },
  { key: "rest-holiday", label: "休·假日", className: "cal-holiday-legend__sample is-rest-holiday" },
  { key: "adjusted", label: "调", className: "cal-holiday-legend__sample is-adjusted" },
  { key: "festival", label: "节", className: "cal-holiday-legend__sample is-festival" },
];
