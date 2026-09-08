import assert from "node:assert/strict";
import test from "node:test";

const model = await import("../src/yunshu-ui/cal/plan/calPlanModel.js");

const {
  createEmptyPlanForm,
  normalizePlanRow,
  isPlanEditable,
  planListStatusClass,
  computePlanKpis,
} = model;

test("creates empty plan form with prepare status", () => {
  const form = createEmptyPlanForm();
  assert.equal(form.status, "PREPARE");
  assert.equal(form.shiftType, "SHIFT_TWO");
});

test("detects editable prepare plans", () => {
  assert.equal(isPlanEditable({ status: "PREPARE" }), true);
  assert.equal(isPlanEditable({ status: "CONFIRMED" }), false);
});

test("maps plan list status classes", () => {
  assert.equal(planListStatusClass("PREPARE"), "is-prepare");
  assert.equal(planListStatusClass("CONFIRMED"), "is-confirmed");
});

test("computes plan KPI metrics including month overlap", () => {
  const plans = [
    { status: "PREPARE", shiftType: "SHIFT_TWO", startDate: "2026-07-01", endDate: "2026-07-31" },
    { status: "CONFIRMED", shiftType: "SINGLE", startDate: "2026-06-01", endDate: "2026-06-30" },
    { status: "PREPARE", shiftType: "SHIFT_TWO", startDate: "2026-08-01", endDate: "2026-08-31" },
  ];
  const kpis = computePlanKpis(plans, 2, new Date(2026, 6, 15));
  assert.equal(kpis.totalCount, 3);
  assert.equal(kpis.prepareCount, 2);
  assert.equal(kpis.confirmedCount, 1);
  assert.equal(kpis.monthActive, 1);
  assert.equal(kpis.shiftTwoCount, 2);
  assert.equal(kpis.singleCount, 1);
  assert.equal(kpis.filteredCount, 2);
});

test("normalizes plan rows", () => {
  assert.equal(normalizePlanRow({ planId: 9, shiftCount: "2" }).shiftCount, 2);
});
