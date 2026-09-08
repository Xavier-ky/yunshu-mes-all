import assert from "node:assert/strict";
import { readFile } from "node:fs/promises";
import test from "node:test";

const templateRoot = new URL(
  "../../../backend/mes-server/src/main/resources/ureport-templates/",
  import.meta.url,
);

async function template(name) {
  return readFile(new URL(name, templateRoot), "utf8");
}

test("general template includes document metadata, twenty rows and sign-off area", async () => {
  const xml = await template("MES通用表格模板.ureport.xml");
  assert.match(xml, /报表编号/);
  assert.match(xml, /生成日期/);
  assert.match(xml, /制表人/);
  assert.match(xml, /审核/);
  assert.match(xml, /批准/);
  assert.match(xml, /<!\[CDATA\[20\]\]>/);
  assert.match(xml, /row-number="25"/);
  assert.doesNotMatch(xml, /align="(?:left|right)"/);
});

test("general template migration replaces the unsupported now expression", async () => {
  const xml = await template("MES通用表格模板.ureport.xml");
  const migration = await readFile(
    new URL(
      "../../../backend/mes-server/src/main/resources/db/migration/V31__fix_general_report_export_date_expression.sql",
      import.meta.url,
    ),
    "utf8",
  );
  assert.doesNotMatch(xml, /\bnow\(\)/);
  assert.match(xml, /<expression-value><!\[CDATA\[date\('yyyy-MM-dd'\)\]\]><\/expression-value>/);
  assert.match(migration, /name_ = 'MES通用表格模板\.ureport\.xml'/);
  assert.match(migration, /<expression-value><!\[CDATA\[date\(''yyyy-MM-dd''\)\]\]><\/expression-value>/);
});

test("report studio offers a current-template Excel export action", async () => {
  const source = await readFile(
    new URL("../src/yunshu-ui/analytics/report/designer/index.vue", import.meta.url),
    "utf8",
  );
  assert.match(source, /import \{[\s\S]*?\bDownload,[\s\S]*?\} from "lucide-vue-next";/);
  assert.match(source, /<Download :size="16" \/> 导出/);
  assert.match(source, /function exportDesign\(\)/);
  assert.match(source, /\/ureport\/excel\?_u=/);
});

test("output template includes production summary and variance metrics", async () => {
  const xml = await template("产量统计报表.ureport.xml");
  for (const label of ["报表周期", "计划总量", "完成总量", "达成率", "差异"]) {
    assert.match(xml, new RegExp(label));
  }
  assert.match(xml, /FROM work_order/);
});

test("quality template includes inspection summary and acceptance metrics", async () => {
  const xml = await template("质量检验报告.ureport.xml");
  for (const label of ["检验批次", "接收数量", "合格数量", "不合格数量", "合格率", "供应商"]) {
    assert.match(xml, new RegExp(label));
  }
});

test("work-order print template keeps parameter support and shop-floor sign-off", async () => {
  const xml = await template("生产工单打印模版.ureport.xml");
  for (const label of ["产品规格", "计划开始", "计划结束", "生产签核", "条码", "二维码"]) {
    assert.match(xml, new RegExp(label));
  }
  assert.match(xml, /bind-parameter="id"/);
  assert.match(xml, /bind-parameter="code"/);
});
