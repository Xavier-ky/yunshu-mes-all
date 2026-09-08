import assert from "node:assert/strict";
import test from "node:test";

import { buildPreviewData } from "../src/yunshu-ui/print/template/preview-data.js";

test("preview data preserves supplied values and fills a QR field from template test data", () => {
  const template = {
    getJson() {
      return {
        panels: [{
          printElements: [
            { options: { field: "barcodeContent", testData: "YS-MES-QR-001" } },
            { options: { field: "qrcode", testData: "LEGACY-QR-001" } },
            { options: { field: "name", testData: "示例姓名" } },
          ],
        }],
      };
    },
  };

  const result = buildPreviewData(template, {
    barcodeContent: "ACTUAL-QR-001",
    name: "实际姓名",
  });

  assert.equal(result.barcodeContent, "ACTUAL-QR-001");
  assert.equal(result.qrcode, "LEGACY-QR-001");
  assert.equal(result.name, "实际姓名");
});
