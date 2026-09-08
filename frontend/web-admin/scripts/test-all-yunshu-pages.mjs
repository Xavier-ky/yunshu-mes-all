import fs from "fs";
import { chromium } from "playwright";

const BASE = "http://127.0.0.1:5173";
const routerSrc = fs.readFileSync(new URL("../src/router/index.js", import.meta.url), "utf8");

function extractRoutes() {
  const blocks = routerSrc.split(/\{ path:/).slice(1);
  const routes = [];
  for (const block of blocks) {
    const pathMatch = block.match(/^\s*"([^"]+)"/);
    if (!pathMatch) continue;
    const path = pathMatch[1];
    if (!block.includes("yunshuUi: true")) continue;
    if (block.includes("hidden: true")) continue;
    if (path.includes(":")) continue;
    routes.push(`/app/${path}`);
  }
  return [...new Set(routes)];
}

const PAGES = extractRoutes();
const failed = [];
const warnings = [];

const browser = await chromium.launch({ headless: true });
const page = await browser.newPage();
const pageErrors = [];
const apiErrors = [];

page.on("pageerror", (err) => pageErrors.push(err.message));
page.on("console", (msg) => {
  if (msg.type() === "error") pageErrors.push(msg.text());
});
page.on("response", (res) => {
  const url = res.url();
  if (url.includes("/api/") && res.status() >= 400) {
    apiErrors.push(`${res.status()} ${url.split("?")[0]}`);
  }
});

async function login() {
  await page.goto(`${BASE}/login`, { waitUntil: "networkidle" });
  await page.fill('input[type="text"], input:first-of-type', "tester");
  const inputs = await page.locator("input").all();
  for (const input of inputs) {
    const type = await input.getAttribute("type");
    const ph = await input.getAttribute("placeholder");
    if (type === "password" || (ph && ph.includes("密码"))) {
      await input.fill("123456");
    }
  }
  await page.click('button[type="submit"], .login-btn, button:has-text("登录")').catch(() => {});
  await page.waitForTimeout(2500);
}

await login();
console.log(`Testing ${PAGES.length} yunshu-ui routes as tester...\n`);

for (const path of PAGES) {
  pageErrors.length = 0;
  apiErrors.length = 0;

  await page.goto(`${BASE}${path}`, { waitUntil: "networkidle", timeout: 60000 }).catch((e) => {
    pageErrors.push(`navigation: ${e.message}`);
  });
  await page.waitForTimeout(1200);

  const url = page.url();
  const redirectedLogin = url.includes("/login");
  const shellCount = await page.locator(".app-container, .yunshu-ui-root").count();
  const text = await page.locator(".app-container, .yunshu-ui-root").first().innerText().catch(() => "");
  const tableRows = await page.locator(".yunshu-data-table tbody tr, .el-table__body tbody tr").count();
  const cardAdd = await page.locator(".card-add, .card-add-text").count();
  const chartCanvas = await page.locator("canvas").count();
  const emptyState = await page.locator(".el-empty, .el-table__empty-text").count();

  const hasContent =
    text.trim().length > 15 ||
    tableRows > 0 ||
    cardAdd > 0 ||
    chartCanvas > 0;

  const issues = [];
  if (redirectedLogin) issues.push("redirected to login");
  if (shellCount === 0) issues.push("no app shell");
  if (!hasContent && emptyState === 0) issues.push("shell empty");
  if (pageErrors.length) issues.push(`js: ${pageErrors[0].slice(0, 120)}`);
  if (apiErrors.length) issues.push(`api: ${apiErrors.slice(0, 2).join("; ")}`);

  const status = issues.length ? "FAIL" : tableRows === 0 && emptyState > 0 ? "WARN" : "OK";
  console.log(`${status.padEnd(4)} ${path} rows=${tableRows} cards=${cardAdd} charts=${chartCanvas} len=${text.length}`);

  if (status === "FAIL") {
    failed.push({ path, issues });
    console.log(`       -> ${issues.join(" | ")}`);
  } else if (status === "WARN") {
    warnings.push({ path, issues: ["empty table/list"] });
  }
}

console.log(`\n=== Summary ===`);
console.log(`Total: ${PAGES.length}, Failed: ${failed.length}, Warnings: ${warnings.length}`);

if (failed.length) {
  console.log("\nFailed pages:");
  for (const f of failed) {
    console.log(`  ${f.path}`);
    for (const i of f.issues) console.log(`    - ${i}`);
  }
}

if (warnings.length) {
  console.log("\nWarnings (empty data):");
  for (const w of warnings) console.log(`  ${w.path}`);
}

await browser.close();
process.exit(failed.length > 0 ? 1 : 0);
