import assert from "node:assert/strict";
import { readFile } from "node:fs/promises";
import test from "node:test";

const root = new URL("../src/", import.meta.url);
const read = (path) => readFile(new URL(path, root), "utf8");

test("calendar API wrappers target list, summary, day and week contracts", async () => {
  const source = await read("yunshu-ui/api/mes/cal/calendar.js");
  for (const endpoint of ["/list", "/summary", "/day", "/week"]) {
    assert.match(source, new RegExp(`url:\\s*['\"]/mes/cal/calendar${endpoint}['\"]`));
  }
});

test("schedule and drawer requests both use latest-request guards", async () => {
  const source = await read("yunshu-ui/cal/calendar/useCalSchedule.js");
  assert.match(source, /const requestGuard = createLatestRequestGuard\(\)/);
  assert.match(
    source,
    /const sequence = requestGuard\.begin\(\);[\s\S]*?if \(teamListFailed \|\| selectionGuidance\.value\)/,
  );
  assert.match(source, /const drawerRequestGuard = createLatestRequestGuard\(\)/);
  assert.match(source, /drawerRequestGuard\.isCurrent\(sequence\)/);
});

test("team load failures retain their error and retry teams before schedules", async () => {
  const [source, overview] = await Promise.all([
    read("yunshu-ui/cal/calendar/useCalSchedule.js"),
    read("yunshu-ui/cal/calendar/CalScheduleOverview.vue"),
  ]);
  assert.match(source, /const teamLoadError = ref\(""\)/);
  assert.match(source, /catch \(cause\)[\s\S]*?teamLoadError\.value = cause\?\.message/);
  assert.match(
    source,
    /async function retryTeams\(\)[\s\S]*?await loadTeams\(false\)[\s\S]*?await refresh\(\)/,
  );
  assert.match(source, /const overviewError = computed/);
  assert.match(source, /const retryOverview = computed/);
  assert.match(overview, /:error="overviewError"/);
  assert.match(overview, /@retry="retryOverview"/);
});

test("successful day-status mutation refreshes overview before drawer staleness checks", async () => {
  const source = await read("yunshu-ui/cal/calendar/useCalSchedule.js");
  const mutation = source.slice(source.indexOf("async function setDayStatus"));
  const refreshIndex = mutation.indexOf("await refresh()");
  const staleDrawerIndex = mutation.indexOf("drawerRequestGuard.isCurrent(sequence)");
  assert.ok(refreshIndex > 0, "overview refresh is required after mutation");
  assert.ok(staleDrawerIndex > refreshIndex, "drawer guard must not suppress overview refresh");
  assert.match(
    mutation,
    /if \(drawerRequestGuard\.isCurrent\(sequence\) && drawerOpen\.value\) await openDay\(date\)/,
  );
});

test("calendar hub normalizes absent and invalid route modes", async () => {
  const source = await read("yunshu-ui/cal/calendar/index.vue");
  assert.match(source, /import \{ normalizeHubMode \}/);
  assert.match(source, /this\.hubMode = normalizeHubMode\(this\.\$route\.query\.mode\)/);
});

test("calendar views expose retry, drawer actions and keyboard-operable chips", async () => {
  const [month, week, day, drawer, userSelect] = await Promise.all([
    read("yunshu-ui/cal/calendar/CalMonthGrid.vue"),
    read("yunshu-ui/cal/calendar/CalWeekGrid.vue"),
    read("yunshu-ui/cal/calendar/CalDayCell.vue"),
    read("yunshu-ui/cal/calendar/CalDayDrawer.vue"),
    read("yunshu-ui/components/userSelect/single.vue"),
  ]);
  assert.match(month, /@click="\$emit\('retry'\)"/);
  assert.match(week, /splitShiftChips/);
  assert.match(week, /\+\{\{ chipsFor\(team, day\.theDay\)\.overflow \}\}/);
  assert.match(day, /class="cal-workbench-shift-chip"[\s\S]*?tabindex="0"/);
  assert.match(day, /@keydown\.(?:enter|space)/);
  assert.match(drawer, /aria-label="关闭排班详情"/);
  assert.match(drawer, /\$emit\('navigate-tab', 'team'\)/);
  assert.match(drawer, /\$emit\('set-status', 'HOLIDAY'\)/);
  assert.match(drawer, /\$emit\('set-status', 'WORKDAY'\)/);
  assert.match(drawer, /\$emit\('retry', day\)/);
  assert.match(userSelect, /<template #footer>/);
  assert.doesNotMatch(userSelect, /slot="footer"/);
  assert.match(userSelect, /<el-radio[^>]+:value="scope\.row\.userId"/);
  assert.doesNotMatch(userSelect, /<el-radio[^>]+:label=/);
});

test("new calendar radio controls use value instead of deprecated label values", async () => {
  const source = await read("yunshu-ui/cal/calendar/CalScheduleToolbar.vue");
  assert.doesNotMatch(source, /el-radio-button\s+label=/);
  for (const value of ["month", "week", "TYPE", "TEAM", "USER"]) {
    assert.match(source, new RegExp(`el-radio-button\\s+value="${value}"`));
  }
});

test("calendar styles remain host-scoped and drawer is mobile-safe", async () => {
  const source = await read("styles/yunshu-ui.css");
  const section = source.slice(source.indexOf(".yunshu-ui-root .cal-workbench {"));
  assert.ok(section.length > 100);
  assert.doesNotMatch(section, /^\.cal-workbench(?!-drawer-portal)/m);
  assert.match(section, /\.cal-workbench-drawer-portal \.cal-workbench-drawer/);
  assert.match(
    section,
    /@media \(max-width: 420px\)[\s\S]*?\.cal-workbench-drawer-portal \.cal-workbench-drawer\s*\{[\s\S]*?width: 100%/,
  );
});
