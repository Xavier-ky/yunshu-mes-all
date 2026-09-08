import assert from "node:assert/strict";
import test from "node:test";

const model = await import("../src/yunshu-ui/cal/holiday/calHolidayModel.js");

const {
  normalizeHolidayOverrides,
  getRestKind,
  getStatusBadgeLabel,
  getDayVisualClass,
  mergeHolidayDays,
  computeHolidayKpis,
  findEnrichedDay,
} = model;

test("normalizes holiday override list into a day map", () => {
  const map = normalizeHolidayOverrides({
    data: [
      { holidayId: 1, theDay: "2026-07-05", holidayType: "HOLIDAY", remark: "周日休息" },
      { holidayId: 2, theDay: "2026-10-01", holidayType: "WORKDAY", remark: "国庆调休" },
    ],
  });
  assert.equal(map.size, 2);
  assert.deepEqual(map.get("2026-07-05"), {
    holidayId: 1,
    holidayType: "HOLIDAY",
    remark: "周日休息",
  });
});

test("merges calendar days with overrides and classifies rest kinds", () => {
  const overrides = normalizeHolidayOverrides({
    data: [
      { holidayId: 10, theDay: "2026-07-01", holidayType: "HOLIDAY", remark: "建党节" },
      { holidayId: 11, theDay: "2026-07-05", holidayType: "WORKDAY", remark: "调休上班" },
    ],
  });
  const days = mergeHolidayDays(
    [
      { theDay: "2026-07-01", workday: false, holidayType: "HOLIDAY" },
      { theDay: "2026-07-05", workday: true, holidayType: "WORKDAY" },
      { theDay: "2026-07-06", workday: true, holidayType: null },
      { theDay: "2026-07-12", workday: false, holidayType: "HOLIDAY" },
    ],
    overrides,
  );

  const explicitHoliday = days.find((day) => day.theDay === "2026-07-01");
  assert.equal(explicitHoliday.restKind, "explicit");
  assert.equal(explicitHoliday.badgeLabel, "假日");
  assert.equal(explicitHoliday.visualClass, "cal-holiday-day--rest-holiday");

  const adjusted = days.find((day) => day.theDay === "2026-07-05");
  assert.equal(adjusted.restKind, null);
  assert.equal(adjusted.badgeLabel, "调");
  assert.equal(adjusted.visualClass, "cal-workbench-day--adjusted");

  const defaultSunday = days.find((day) => day.theDay === "2026-07-12");
  assert.equal(defaultSunday.restKind, "default");
  assert.equal(defaultSunday.badgeLabel, "休");
  assert.equal(defaultSunday.visualClass, "cal-holiday-day--rest-default");
});

test("computes holiday KPI metrics from enriched days", () => {
  const days = mergeHolidayDays(
    [
      { theDay: "2026-07-01", workday: false, holidayType: "HOLIDAY" },
      { theDay: "2026-07-05", workday: true, holidayType: "WORKDAY" },
      { theDay: "2026-07-06", workday: true, holidayType: null },
      { theDay: "2026-07-12", workday: false, holidayType: "HOLIDAY" },
    ],
    normalizeHolidayOverrides({
      data: [
        { holidayId: 1, theDay: "2026-07-01", holidayType: "HOLIDAY", remark: "建党节" },
        { holidayId: 2, theDay: "2026-07-05", holidayType: "WORKDAY", remark: "调休" },
      ],
    }),
  );

  assert.deepEqual(computeHolidayKpis(days), {
    restCount: 2,
    workdayCount: 2,
    explicitHoliday: 1,
    adjustedWorkday: 1,
    defaultSunday: 1,
    festivalCount: days.filter((day) => day.lunar?.festival).length,
  });
});

test("finds enriched day by date string", () => {
  const days = mergeHolidayDays([{ theDay: "2026-07-06", workday: true, holidayType: null }], new Map());
  assert.equal(findEnrichedDay(days, "2026-07-06")?.badgeLabel, "班");
  assert.equal(findEnrichedDay(days, "2026-07-07"), null);
});

test("maps status badge labels for shift/rest/adjusted days", () => {
  assert.equal(getStatusBadgeLabel({ workday: true, holidayType: null }, null), "班");
  assert.equal(getStatusBadgeLabel({ workday: false, holidayType: "HOLIDAY" }, "default"), "休");
  assert.equal(
    getStatusBadgeLabel({ workday: false, holidayType: "HOLIDAY" }, "explicit"),
    "假日",
  );
  assert.equal(getDayVisualClass({ workday: true, holidayType: "WORKDAY" }, null), "cal-workbench-day--adjusted");
});
