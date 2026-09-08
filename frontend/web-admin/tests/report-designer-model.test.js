import assert from "node:assert/strict";
import test from "node:test";

const model = await import("../src/yunshu-ui/analytics/report/designer/model.js").catch(() => ({}));
const {
  normalizeReportFileName,
  buildTemplateCatalog,
  validateReportName,
  replaceTemplateTitle,
} = model;

test("normalizes report names to the UReport filename convention", () => {
  assert.equal(typeof normalizeReportFileName, "function");
  assert.equal(normalizeReportFileName(" 月度产量 "), "月度产量.ureport.xml");
  assert.equal(normalizeReportFileName("质量日报.ureport.xml"), "质量日报.ureport.xml");
});

test("rejects duplicate, invalid and overlong report names", () => {
  assert.equal(typeof validateReportName, "function");
  assert.equal(validateReportName("", []), "请输入模板名称");
  assert.equal(validateReportName("日报/夜班", []), "模板名称不能包含 \\/:*?\"<>|");
  assert.equal(validateReportName("产量统计", ["产量统计.ureport.xml"]), "模板名称已存在");
  assert.equal(validateReportName("A".repeat(90), []), "模板名称过长");
  assert.equal(validateReportName("周产量", []), "");
});

test("builds a categorized catalog and preserves custom reports", () => {
  assert.equal(typeof buildTemplateCatalog, "function");
  const catalog = buildTemplateCatalog([
    { id: 1, name: "产量统计报表.ureport.xml", updateTime: "2026-07-11" },
    { id: 2, name: "夜班日报.ureport.xml", updateTime: "2026-07-12" },
  ]);
  assert.equal(catalog.length, 2);
  assert.equal(catalog[0].category, "production");
  assert.equal(catalog[0].protected, true);
  assert.equal(catalog[1].category, "custom");
  assert.equal(catalog[1].protected, false);
  assert.equal(catalog[1].label, "夜班日报");
});

test("orders built-in templates by product priority before custom reports", () => {
  const catalog = buildTemplateCatalog([
    { id: 8, name: "demo_quality_report.ureport.xml" },
    { id: 7, name: "质量检验报告.ureport.xml" },
    { id: 6, name: "MES通用表格模板.ureport.xml" },
    { id: 5, name: "产量统计报表.ureport.xml" },
  ]);
  assert.deepEqual(
    catalog.map((item) => item.fileName),
    [
      "MES通用表格模板.ureport.xml",
      "产量统计报表.ureport.xml",
      "质量检验报告.ureport.xml",
      "demo_quality_report.ureport.xml",
    ],
  );
});

test("replaces the first static title without damaging XML", () => {
  assert.equal(typeof replaceTemplateTitle, "function");
  const xml = "<ureport><simple-value><![CDATA[旧标题]]></simple-value><simple-value><![CDATA[字段]]></simple-value></ureport>";
  assert.equal(
    replaceTemplateTitle(xml, "新标题"),
    "<ureport><simple-value><![CDATA[新标题]]></simple-value><simple-value><![CDATA[字段]]></simple-value></ureport>",
  );
});
