import { chromium } from "playwright";

const BASE = "http://127.0.0.1:5173";
const PAGES = [
  "/app/system/users",
  "/app/system/roles",
  "/app/system/departments",
  "/app/system/posts",
  "/app/system/menus",
  "/app/system/dict",
  "/app/system/config",
  "/app/system/notices",
  "/app/system/autocode",
  "/app/system/message",
  "/app/system/operlog",
  "/app/system/logininfor",
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

for (const path of PAGES) {
  errors.length = 0;
  await page.goto(`${BASE}${path}`, { waitUntil: "networkidle" });
  await page.waitForTimeout(1500);
  const text = await page.locator(".app-container").first().innerText().catch(() => "");
  const visible = text.trim().length > 20;
  console.log(`${path} visible=${visible} len=${text.length}`);
  if (!visible) {
    console.log("  errors:", errors.slice(0, 3).join(" | "));
  }
  if (errors.length) {
    console.log("  console:", errors.slice(0, 2).join(" | "));
  }
}

await browser.close();
