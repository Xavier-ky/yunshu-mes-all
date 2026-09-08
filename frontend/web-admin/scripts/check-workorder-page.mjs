import { chromium } from "playwright";

const base = "http://127.0.0.1:5173";
const errors = [];

const browser = await chromium.launch({ headless: true });
const page = await browser.newPage();
page.on("pageerror", (e) => errors.push(`pageerror: ${e.message}`));
page.on("console", (msg) => {
  if (msg.type() === "error") errors.push(`console: ${msg.text()}`);
});

await page.goto(`${base}/login?role=supervisor`, { waitUntil: "networkidle" });
const loginForm = page.locator("form").filter({ hasText: "登录" }).first();
await loginForm.locator('input[type="text"], input:not([type="password"])').first().fill("supervisor");
await loginForm.locator('input[type="password"]').first().fill("123456");
await loginForm.locator('button:has-text("登录")').click();
await page.waitForURL(/\/app/, { timeout: 15000 }).catch(() => {});

await page.goto(`${base}/app/planning/work-orders`, { waitUntil: "networkidle", timeout: 30000 });
await page.waitForTimeout(3000);

const url = page.url();
const title = await page.title();
const hasAppContainer = await page.locator(".app-container").count();
const hasTable = await page.locator(".el-table").count();
const bodyText = (await page.locator("body").innerText()).slice(0, 500);

console.log(JSON.stringify({ url, title, hasAppContainer, hasTable, errors, bodyText }, null, 2));
await browser.close();
