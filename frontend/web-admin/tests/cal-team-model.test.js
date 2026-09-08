import assert from "node:assert/strict";
import test from "node:test";

const model = await import("../src/yunshu-ui/cal/team/calTeamModel.js");

const { createEmptyTeamForm, normalizeTeamRow, computeTeamKpis } = model;

test("creates empty team form with defaults", () => {
  assert.deepEqual(createEmptyTeamForm(), {
    teamId: null,
    teamCode: "",
    teamName: "",
    calendarType: "ZZ",
    remark: "",
    enableFlag: "Y",
  });
});

test("normalizes team rows", () => {
  assert.deepEqual(normalizeTeamRow({ teamId: 1, teamName: "甲班" }), {
    teamId: 1,
    teamCode: "",
    teamName: "甲班",
    calendarType: "ZZ",
    remark: "",
    enableFlag: "Y",
  });
});

test("computes team KPI metrics", () => {
  const teams = [
    { teamId: 1, calendarType: "ZZ", enableFlag: "Y" },
    { teamId: 2, calendarType: "BC", enableFlag: "N" },
    { teamId: 3, calendarType: "ZZ", enableFlag: "Y" },
  ];
  assert.deepEqual(computeTeamKpis(teams, 2, 15), {
    totalCount: 3,
    enabledCount: 2,
    disabledCount: 1,
    manufacturingCount: 2,
    filteredCount: 2,
    memberTotal: 15,
  });
});
