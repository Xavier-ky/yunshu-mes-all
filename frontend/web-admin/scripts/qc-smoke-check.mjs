import { chromium } from "playwright";

const routes = [
  "/app/quality/pending",
  "/app/quality/iqc",
  "/app/quality/ipqc",
  "/app/quality/oqc",
  "/app/quality/rqc",
  "/app/quality/qctemplate",
  "/app/quality/qcindex",
  "/app/quality/qcdefect",
  "/app/quality/batchtrace",
];

const browser = await chromium.launch({ headless: true });
const page = await browser.newPage({ viewport: { width: 1440, height: 900 } });
const errors = [];
const failed = [];
page.on("pageerror", (e) => errors.push(e.message.split("\n")[0]));
page.on("console", (m) => {
  if (m.type() === "error") errors.push(m.text().slice(0, 240));
});
page.on("response", (r) => {
  if (r.url().includes("/api/") && r.status() >= 400) {
    failed.push(`${r.status()} ${r.url().replace("http://127.0.0.1:8080", "").replace("http://127.0.0.1:5173", "")}`);
  }
});

await page.goto("http://127.0.0.1:5173/login", { waitUntil: "domcontentloaded", timeout: 30000 });
await page.locator('input[placeholder="账号"]').first().fill("quality");
await page.locator('input[placeholder="密码"]').first().fill("123456");
await page.getByRole("button", { name: /登录|登 录/ }).first().click();
await page.waitForURL(/\/app/, { timeout: 15000 }).catch(() => {});
await page.waitForTimeout(1200);
console.log("after_login", page.url());

for (const route of routes) {
  errors.length = 0;
  failed.length = 0;
  await page.goto(`http://127.0.0.1:5173${route}`, { waitUntil: "networkidle", timeout: 30000 }).catch((e) => {
    errors.push(`NAV ${e.message}`);
  });
  await page.waitForTimeout(900);
  const info = await page.evaluate(() => {
    const root = document.querySelector(".yunshu-ui-root");
    const app = document.querySelector(".app-container");
    const table = document.querySelector(".el-table");
    const rows = document.querySelectorAll(".el-table__body tbody tr:not(.el-table__row--level-0-placeholder)");
    const empty = document.querySelector(".el-table__empty-text, .el-empty");
    const bodyText = (document.querySelector(".tc-content") || document.body).innerText.slice(0, 160).replace(/\s+/g, " ");
    const rect = (el) =>
      el
        ? {
            h: Math.round(el.getBoundingClientRect().height),
            w: Math.round(el.getBoundingClientRect().width),
          }
        : null;
    return {
      hasRoot: !!root,
      hasApp: !!app,
      hasTable: !!table,
      rowCount: rows.length,
      emptyText: empty ? empty.textContent.trim() : null,
      root: rect(root),
      app: rect(app),
      table: rect(table),
      bodyText,
    };
  });
  console.log(
    JSON.stringify({
      route,
      ...info,
      errors: [...errors].slice(0, 8),
      failed: [...failed].slice(0, 8),
    }),
  );
}

await browser.close();
