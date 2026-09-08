import { chromium } from "playwright";

const browser = await chromium.launch({ headless: true });
const page = await browser.newPage();
const errors = [];
page.on("pageerror", (e) => errors.push(`pageerror: ${e.message}\n${e.stack}`));
page.on("console", (m) => {
  if (m.type() === "error") errors.push(`console: ${m.text()}`);
});

const loginRes = await fetch("http://127.0.0.1:8080/api/auth/login", {
  method: "POST",
  headers: { "Content-Type": "application/json" },
  body: JSON.stringify({ username: "supervisor", password: "123456" }),
});
const loginJson = await loginRes.json();
const token = loginJson.data?.token;
if (!token) throw new Error("login failed: " + JSON.stringify(loginJson));

await page.goto("http://127.0.0.1:5173/login");
await page.evaluate(({ t, user }) => {
  sessionStorage.setItem("mes_token", t);
  sessionStorage.setItem("mes_auth_user", JSON.stringify(user));
  localStorage.removeItem("mes_token");
  localStorage.removeItem("mes_auth_user");
}, {
  t: token,
  user: {
    userId: loginJson.data.userId,
    username: loginJson.data.username,
    realName: loginJson.data.realName,
    roleCodes: loginJson.data.roleCodes,
    roles: loginJson.data.roles,
    deptName: loginJson.data.deptName,
  },
});

await page.goto("http://127.0.0.1:5173/app/master-data/mditem");
await page.waitForTimeout(6000);

const info = await page.evaluate(() => ({
  url: location.href,
  title: document.title,
  appLen: document.querySelector("#app")?.innerHTML?.length || 0,
  hasContainer: !!document.querySelector(".app-container"),
  rowCount: document.querySelectorAll(".el-table__row").length,
  text: document.body.innerText.slice(0, 1000),
}));

console.log(JSON.stringify({ info, errors }, null, 2));
await page.screenshot({ path: "../../.playwright-mcp/mditem-debug.png", fullPage: true });
await browser.close();
