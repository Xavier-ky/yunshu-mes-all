import assert from "node:assert/strict";
import { readFile } from "node:fs/promises";
import test from "node:test";

const previewUrl = new URL(
  "../src/yunshu-ui/print/template/preview.vue",
  import.meta.url,
);

test("print preview mounts generated hiPrint nodes instead of stringifying them", async () => {
  const source = await readFile(previewUrl, "utf8");

  assert.match(source, /const rendered = hiprintTemplateArg\.getHtml\(this\.printData\);/);
  assert.match(
    source,
    /previewEl\.replaceChildren\(\.\.\.Array\.from\(rendered\)\.map\(\(node\) => node\.cloneNode\(true\)\)\);/,
  );
  assert.doesNotMatch(source, /innerHTML\s*=\s*hiprintTemplateArg\.getHtml/);
});

test("print preview exposes its existing PDF export action", async () => {
  const source = await readFile(previewUrl, "utf8");

  assert.match(source, /<template #header>/);
  assert.match(source, /class="preview-dialog-header__export"/);
  assert.match(source, /class="preview-dialog-header__close"/);
  assert.match(source, /aria-label="关闭预览"/);
  assert.match(source, /class="preview-dialog-header__title"/);
  assert.match(source, /:show-close="false"/);
  assert.doesNotMatch(source, /<template #footer>/);
  assert.match(source, /导出 PDF/);
});

test("print preview exports a lossless PNG-based PDF to avoid faded text", async () => {
  const source = await readFile(previewUrl, "utf8");

  assert.match(source, /await Promise\.all\(\[\s*import\('html2canvas'\),\s*import\('jspdf'\),\s*\]\)/);
  assert.match(source, /canvas\.toDataURL\('image\/png'\)/);
  assert.match(source, /pdf\.addImage\(imgData, 'PNG'/);
  assert.doesNotMatch(source, /toDataURL\("image\/jpeg"\)/);
});

test("print studio passes the current template name to PDF preview", async () => {
  const source = await readFile(
    new URL("../src/yunshu-ui/print/template/index.vue", import.meta.url),
    "utf8",
  );

  assert.match(
    source,
    /this\.\$refs\.preView\.show\(\s*this\.hiprintTemplate,\s*printData,\s*width,\s*this\.form\.templateName \|\| this\.form\.templateCode \|\| '打印模板',?\s*\)/,
  );
});
