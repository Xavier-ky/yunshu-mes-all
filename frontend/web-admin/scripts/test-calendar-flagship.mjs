import { chromium } from "playwright";

const BASE_URL = process.env.BASE_URL || "http://127.0.0.1:5173";
const allAccounts = [
  { username: "tester", password: "123456" },
  { username: "supervisor", password: "123456" },
];
const accounts = process.env.ACCOUNT
  ? allAccounts.filter((account) => account.username === process.env.ACCOUNT)
  : allAccounts;

async function moveToJuly2026(page) {
  for (let attempt = 0; attempt < 120; attempt += 1) {
    const label = await page.locator(".cal-workbench-toolbar__period strong").innerText();
    if (label === "2026年7月") return;
    const match = label.match(/(\d{4})年(\d{1,2})月/);
    if (!match) throw new Error(`Cannot parse calendar period: ${label}`);
    const current = Number(match[1]) * 12 + Number(match[2]);
    const target = 2026 * 12 + 7;
    await page.getByTitle(current < target ? "下一周期" : "上一周期").click();
    await page.waitForTimeout(80);
  }
  throw new Error("Could not navigate to July 2026");
}

async function calendarGet(page, path, params) {
  return page.evaluate(async ({ apiPath, query }) => {
    const token = localStorage.getItem("mes_token");
    const search = new URLSearchParams(query);
    const response = await fetch(`/api${apiPath}?${search}`, {
      headers: { Authorization: `Bearer ${token}` },
    });
    return { status: response.status, body: await response.json() };
  }, { apiPath: path, query: params });
}

async function setSundayStatusAndRestore(page, username) {
  const julyFifth = page.locator(".cal-workbench-day").filter({
    has: page.locator(".cal-workbench-day__date", { hasText: /^5$/ }),
  });
  await julyFifth.click();
  const drawer = page.locator(".cal-workbench-drawer");
  await drawer.waitFor({ state: "visible" });

  const workdayResponse = page.waitForResponse(
    (response) => response.url().includes("/api/mes/cal/calholiday") && response.request().method() === "POST",
  );
  await drawer.getByRole("button", { name: "设为调休上班" }).click();
  const workdayBody = await (await workdayResponse).json();
  if (workdayBody.code !== 200) {
    throw new Error(`${username}: WORKDAY mutation failed: ${JSON.stringify(workdayBody)}`);
  }
  await drawer.locator(".cal-workbench-drawer__status").getByText("调", { exact: true }).waitFor();

  const restoreResponse = page.waitForResponse(
    (response) => response.url().includes("/api/mes/cal/calholiday") && response.request().method() === "POST",
  );
  await drawer.getByRole("button", { name: "设为休息日" }).click();
  const restoreBody = await (await restoreResponse).json();
  if (restoreBody.code !== 200) {
    throw new Error(`${username}: HOLIDAY restoration failed: ${JSON.stringify(restoreBody)}`);
  }
  await drawer.locator(".cal-workbench-drawer__status").getByText("休", { exact: true }).waitFor();
  await drawer.getByRole("button", { name: "关闭排班详情" }).click();
}

const browser = await chromium.launch({ headless: true });

try {
  for (const account of accounts) {
    const page = await browser.newPage({ viewport: { width: 1440, height: 960 } });
    const browserErrors = [];
    const calendarApiErrors = [];
    let calendarReady = false;

    page.on("pageerror", (error) => {
      if (calendarReady) browserErrors.push(error.stack || error.message);
    });
    page.on("console", (message) => {
      if (calendarReady && message.type() === "error") browserErrors.push(message.text());
    });
    page.on("response", (response) => {
      if (response.url().includes("/api/mes/cal/") && response.status() >= 400) {
        calendarApiErrors.push(`${response.status()} ${response.url()}`);
      }
    });

    await page.goto(`${BASE_URL}/login`, { waitUntil: "networkidle" });
    const loginForm = page.locator("form:visible").first();
    await loginForm.locator('input[type="text"]').first().fill(account.username);
    await loginForm.locator('input[type="password"]').first().fill(account.password);
    await loginForm.getByRole("button", { name: "登录" }).click();
    await page.waitForURL((url) => !url.pathname.includes("/login"), { timeout: 15_000 });

    browserErrors.length = 0;
    calendarApiErrors.length = 0;
    await page.goto(`${BASE_URL}/app/factory/calendar`, { waitUntil: "networkidle" });
    await page.locator(".cal-workbench").waitFor({ state: "visible" });
    calendarReady = true;
    await moveToJuly2026(page);
    await page.getByText("2026年7月", { exact: true }).waitFor();
    await page.locator(".cal-workbench-shift-chip").first().waitFor({ timeout: 10_000 });

    const scope = { queryType: "TYPE", calendarType: "ZZ", date: "2026-07-01" };
    const [listResult, summaryResult, weekResult, dayResult, invalidTeamResult] = await Promise.all([
      calendarGet(page, "/mes/cal/calendar/list", scope),
      calendarGet(page, "/mes/cal/calendar/summary", scope),
      calendarGet(page, "/mes/cal/calendar/week", { ...scope, date: "2026-07-06" }),
      calendarGet(page, "/mes/cal/calendar/day", { ...scope, date: "2026-07-06" }),
      calendarGet(page, "/mes/cal/calendar/list", { queryType: "TEAM", date: "2026-07-01" }),
    ]);
    for (const result of [listResult, summaryResult, weekResult, dayResult]) {
      if (result.status !== 200 || result.body.code !== 200) {
        throw new Error(`${account.username}: invalid calendar envelope ${JSON.stringify(result)}`);
      }
    }
    if (invalidTeamResult.body.code !== 400) {
      throw new Error(`${account.username}: TEAM query without teamId was not rejected`);
    }
    if (listResult.body.data?.length !== 31) {
      throw new Error(`${account.username}: July calendar did not contain 31 days`);
    }
    const shifts = listResult.body.data.flatMap((day) => day.teamShifts || []);
    if (shifts.length < 54) {
      throw new Error(`${account.username}: expected at least 54 assembly shifts, got ${shifts.length}`);
    }
    const sundayShifts = listResult.body.data
      .filter((day) => [5, 12, 19, 26].includes(Number(day.theDay.slice(-2))))
      .flatMap((day) => day.teamShifts || []);
    if (sundayShifts.length) {
      throw new Error(`${account.username}: Sunday seed contains ${sundayShifts.length} shifts`);
    }
    if (!weekResult.body.data?.teams?.length || !dayResult.body.data?.teamShifts?.length) {
      throw new Error(`${account.username}: week/day detail contracts returned no flagship data`);
    }

    const coverage = Number(
      await page.locator(".cal-workbench-kpis").getByText("排班覆盖率").locator("..").locator("b").innerText(),
    );
    if (!Number.isFinite(coverage) || coverage < 0 || coverage > 100) {
      throw new Error(`${account.username}: invalid coverage ${coverage}`);
    }

    await page.getByText("周", { exact: true }).click();
    await page.locator(".cal-workbench-week").waitFor({ state: "visible" });
    await page.locator(".cal-workbench-week__team").first().waitFor({ timeout: 10_000 });

    await page.getByText("月", { exact: true }).click();
    await page.locator(".cal-workbench-day").first().click();
    await page.locator(".cal-workbench-drawer").waitFor({ state: "visible" });
    await page.getByRole("button", { name: "班组维护" }).waitFor();
    await page.getByRole("button", { name: "编制计划" }).waitFor();
    await page.getByRole("button", { name: "设为休息日" }).waitFor();
    await page.getByRole("button", { name: "设为调休上班" }).waitFor();
    await page.getByRole("button", { name: "关闭排班详情" }).click();
    await setSundayStatusAndRestore(page, account.username);

    if (calendarApiErrors.length) {
      throw new Error(`${account.username}: calendar API errors: ${calendarApiErrors.join(", ")}`);
    }
    if (browserErrors.length) {
      throw new Error(`${account.username}: browser errors: ${browserErrors.join(" | ")}`);
    }

    console.log(`${account.username}: calendar month/week/drawer acceptance passed`);
    await page.close();
  }
} finally {
  await browser.close();
}
