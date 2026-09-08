import assert from "node:assert/strict";
import test from "node:test";

const model = await import("../src/yunshu-ui/cal/calendar/calScheduleModel.js").catch(() => ({}));
const {
  formatDate,
  pickDefaultCalendarType,
  pickDefaultTeam,
  getDayStatus,
  splitShiftChips,
  normalizeCalendarDays,
  normalizeSummary,
  calculateCoverage,
  createLatestRequestGuard,
  getSelectionGuidance,
  normalizeHubMode,
  shouldReloadCalendarType,
} = model;

test("formats calendar request dates as padded YYYY-MM-DD", () => {
  assert.equal(typeof formatDate, "function");
  assert.equal(formatDate(new Date(2026, 0, 5)), "2026-01-05");
  assert.equal(formatDate("2026-7-9"), "2026-07-09");
});

test("defaults calendar type to ZZ when available and otherwise first value", () => {
  assert.equal(typeof pickDefaultCalendarType, "function");
  assert.equal(pickDefaultCalendarType([{ value: "BC" }, { value: "ZZ" }]), "ZZ");
  assert.equal(pickDefaultCalendarType([{ value: "BC" }, { value: "SB" }]), "BC");
  assert.equal(pickDefaultCalendarType([]), "ZZ");
});

test("defaults team mode to the first enabled team", () => {
  assert.equal(typeof pickDefaultTeam, "function");
  const teams = [
    { teamId: 1, teamName: "停用班组", enableFlag: "N" },
    { teamId: 2, teamName: "甲班", enableFlag: "Y" },
    { teamId: 3, teamName: "乙班", status: "0" },
  ];
  assert.equal(pickDefaultTeam(teams)?.teamId, 2);
  assert.equal(pickDefaultTeam([{ teamId: 4, status: "0" }])?.teamId, 4);
});

test("distinguishes normal workdays, holidays and WORKDAY overrides", () => {
  assert.equal(typeof getDayStatus, "function");
  assert.deepEqual(getDayStatus({ workday: true, holidayType: "WEEKDAY" }), {
    code: "SHIFT",
    label: "班",
  });
  assert.deepEqual(getDayStatus({ workday: false, holidayType: "HOLIDAY" }), {
    code: "REST",
    label: "休",
  });
  assert.deepEqual(getDayStatus({ workday: true, holidayType: "WORKDAY" }), {
    code: "ADJUSTED",
    label: "调",
  });
});

test("caps month chips at three and reports +N overflow", () => {
  assert.equal(typeof splitShiftChips, "function");
  const shifts = [1, 2, 3, 4, 5].map((recordId) => ({ recordId }));
  assert.deepEqual(splitShiftChips(shifts), {
    visible: shifts.slice(0, 3),
    overflow: 2,
  });
  assert.deepEqual(splitShiftChips(null), { visible: [], overflow: 0 });
});

test("normalizes list and summary API envelopes and coverage values", () => {
  assert.equal(typeof normalizeCalendarDays, "function");
  assert.equal(typeof normalizeSummary, "function");
  assert.equal(typeof calculateCoverage, "function");

  const rows = [{ theDay: "2026-07-01", teamShifts: null }];
  assert.deepEqual(normalizeCalendarDays({ data: rows }), [
    { theDay: "2026-07-01", teamShifts: [], workday: false, holidayType: "HOLIDAY" },
  ]);
  assert.deepEqual(normalizeCalendarDays(rows), [
    { theDay: "2026-07-01", teamShifts: [], workday: false, holidayType: "HOLIDAY" },
  ]);
  assert.deepEqual(normalizeCalendarDays(null), []);

  assert.equal(calculateCoverage(9, 10), 90);
  assert.equal(calculateCoverage(0, 0), 100);
  assert.equal(calculateCoverage(15, 10), 100);
  assert.equal(calculateCoverage(-2, 10), 0);
  assert.equal(normalizeSummary({ data: { coverageRate: 145, workdayCount: 10 } }).coverageRate, 100);
  assert.deepEqual(normalizeSummary({ data: { scheduledDays: 9, workdayCount: 10 } }), {
    todayOnDutyCount: 0,
    coverageRate: 90,
    unscheduledWorkdays: 1,
    conflictCount: 0,
    holidayCount: 21,
    workdayCount: 10,
  });
});

test("requires valid TEAM and USER selections before requesting schedules", () => {
  assert.equal(typeof getSelectionGuidance, "function");
  assert.equal(
    getSelectionGuidance("TEAM", { selectedTeamId: null, teams: [] }),
    "暂无可用班组，请先启用或创建班组。",
  );
  assert.equal(
    getSelectionGuidance("TEAM", {
      selectedTeamId: 9,
      teams: [{ teamId: 9, enableFlag: "N" }],
    }),
    "暂无可用班组，请先启用或创建班组。",
  );
  assert.equal(getSelectionGuidance("USER", { selectedUser: null }), "请选择人员查看排班。");
  assert.equal(
    getSelectionGuidance("TEAM", {
      selectedTeamId: 2,
      teams: [{ teamId: 2, enableFlag: "Y" }],
    }),
    "",
  );
  assert.equal(
    getSelectionGuidance("TEAM", {
      selectedTeamId: null,
      teams: [],
      teamLoadError: "班组列表加载失败",
    }),
    "",
  );
});

test("latest-request guards invalidate stale schedule and drawer responses", () => {
  assert.equal(typeof createLatestRequestGuard, "function");
  const scheduleGuard = createLatestRequestGuard();
  const january = scheduleGuard.begin();
  const guidance = scheduleGuard.begin();
  assert.equal(scheduleGuard.isCurrent(january), false);
  assert.equal(scheduleGuard.isCurrent(guidance), true);

  const drawerGuard = createLatestRequestGuard();
  const firstDay = drawerGuard.begin();
  const secondDay = drawerGuard.begin();
  assert.equal(drawerGuard.isCurrent(firstDay), false);
  assert.equal(drawerGuard.isCurrent(secondDay), true);
  drawerGuard.invalidate();
  assert.equal(drawerGuard.isCurrent(secondDay), false);
});

test("normalizes route mode and detects async calendar-type replacement", () => {
  assert.equal(typeof normalizeHubMode, "function");
  assert.equal(typeof shouldReloadCalendarType, "function");
  assert.equal(normalizeHubMode("plan"), "plan");
  assert.equal(normalizeHubMode("unknown"), "overview");
  assert.equal(normalizeHubMode(undefined), "overview");
  assert.equal(shouldReloadCalendarType("ZZ", "BC", "TYPE"), true);
  assert.equal(shouldReloadCalendarType("ZZ", "BC", "TEAM"), false);
  assert.equal(shouldReloadCalendarType("ZZ", "ZZ", "TYPE"), false);
});
