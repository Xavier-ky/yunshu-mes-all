import { chromium } from "playwright";

const BASE = "http://127.0.0.1:5173";
const PAGES = [
  "/app/analytics/reports",
  "/app/analytics/report-designer",
  "/app/analytics/charts",
  "/app/analytics/product-trace",
  "/app/analytics/batch-trace",
  "/app/analytics/print-templates",
  "/app/analytics/print-clients",
  "/app/analytics/integration/systems",
  "/app/analytics/integration/endpoints",
  "/app/analytics/integration/sync-logs",
  "/app/reports",
  "/app/trace",
  "/app/integration",
];

const browser = await chromium.launch({ headless: true });
const page = await browser.newPage();
const errors = [];

page.on("pageerror", (err) => errors.push(err.message));
page.on("console", (msg) => {
  if (msg.type() === "error") errors.push(msg.text());
});

await page.goto(`${BASE}/login`, { waitUntil: "networkidle" });
await page.fill('input[type="text"], input[placeholder*="用户"], input:first-of-type', "admin");
const inputs = await page.locator("input").all();
for (const input of inputs) {
  const type = await input.getAttribute("type");
  const ph = await input.getAttribute("placeholder");
  if (type === "password" || (ph && ph.includes("密码"))) {
    await input.fill("admin123");
  }
}
await page.click('button[type="submit"], .login-btn, button:has-text("登录")').catch(() => {});
await page.waitForTimeout(2000);

let failed = 0;
for (const path of PAGES) {
  errors.length = 0;
  await page.goto(`${BASE}${path}`, { waitUntil: "networkidle" });
  await page.waitForTimeout(1500);
  const isDesigner = path.includes("report-designer");
  let visible;
  if (isDesigner) {
    const hasCommandBar = await page.locator(".studio-command").isVisible().catch(() => false);
    const hasTemplateDock = await page.locator(".template-dock").isVisible().catch(() => false);
    const hasTemplates = (await page.locator(".template-card").count()) >= 4;
    const hasFrame = await page.locator("iframe[src*='/ureport/designer']").isVisible().catch(() => false);
    await page.locator(".designer-loading").waitFor({ state: "hidden", timeout: 18000 }).catch(() => {});
    const designerReady = await page.locator(".save-state").innerText().catch(() => "");
    const frame = page.locator("iframe[src*='/ureport/designer']");
    const firstSrc = await frame.getAttribute("src");
    await page.locator(".template-card").nth(1).click();
    await page.waitForFunction(
      (src) => document.querySelector("iframe[src*='/ureport/designer']")?.getAttribute("src") !== src,
      firstSrc,
    ).catch(() => {});
    const switchWorked = (await frame.getAttribute("src")) !== firstSrc;
    await page.locator(".dock-search input").fill("质量");
    const searchWorked = await page.locator(".template-card").count() === 1;
    await page.locator(".dock-search input").fill("");
    const designerHealthy = /已保存|有未保存修改/.test(designerReady);
    visible = hasCommandBar
      && hasTemplateDock
      && hasTemplates
      && hasFrame
      && switchWorked
      && searchWorked
      && designerHealthy;
  } else {
    const text = await page.locator(".app-container, .yunshu-ui-root").first().innerText().catch(() => "");
    visible = text.trim().length > 20;
  }
  const textLen = isDesigner
    ? await page.locator(".report-studio").first().innerText().catch(() => "").then((t) => t.length)
    : await page.locator(".app-container, .yunshu-ui-root").first().innerText().catch(() => "").then((t) => t.length);
  console.log(`${path} visible=${visible} len=${textLen}`);
  if (!visible) {
    failed += 1;
    console.log("  errors:", errors.slice(0, 3).join(" | "));
  }
}

await browser.close();
process.exit(failed > 0 ? 1 : 0);
