import assert from "node:assert/strict";
import test from "node:test";

const patchModule = await import(
  "../src/yunshu-ui/analytics/report/designer/ureport-iframe-patch.js"
).catch(() => ({}));

test("embedded stylesheet preserves Handsontable overlay geometry", () => {
  assert.equal(typeof patchModule.UREPORT_EMBED_CSS, "string");
  assert.match(
    patchModule.UREPORT_EMBED_CSS,
    /#container > \.handsontable\s*\{[\s\S]*?padding:\s*0 !important;[\s\S]*?overflow:\s*hidden !important;/,
  );
  assert.doesNotMatch(
    patchModule.UREPORT_EMBED_CSS,
    /#container > div:not\([\s\S]*?padding:\s*22px 28px 34px/,
  );
});

test("embedded stylesheet gives property controls enough single-line space", () => {
  assert.match(patchModule.UREPORT_EMBED_CSS, /grid-template-columns:\s*minmax\(0, 1fr\) 392px/);
  assert.match(
    patchModule.UREPORT_EMBED_CSS,
    /\.ud-property-panel \.form-group\s*\{[\s\S]*?white-space:\s*nowrap;/,
  );
});

test("designer patch exposes controlled save, preview and dirty-state commands", () => {
  assert.equal(typeof patchModule.patchUreportDesignerIframe, "function");

  let saveClicks = 0;
  let previewClicks = 0;
  let reloaded = 0;
  const saveItem = {
    classList: { contains: (name) => name === "disabled" },
    querySelector: () => ({ click: () => { saveClicks += 1; } }),
  };
  const previewIcon = {
    closest: () => ({
      querySelector: () => ({ click: () => { previewClicks += 1; } }),
    }),
  };
  const doc = {
    readyState: "complete",
    head: { appendChild() {} },
    createElement: () => ({ id: "", textContent: "" }),
    getElementById: () => ({}),
    querySelector: (selector) => {
      if (selector === "#__save_btn") return saveItem;
      if (selector === ".ureport-preview") return previewIcon;
      if (selector === ".ud-toolbar" || selector === ".ud-property-panel") return {};
      return null;
    },
  };
  const win = {
    dispatchEvent() {},
    location: { reload: () => { reloaded += 1; } },
  };
  const iframe = {
    contentDocument: doc,
    contentWindow: win,
    addEventListener() {},
    removeEventListener() {},
  };

  const bridge = patchModule.patchUreportDesignerIframe(iframe, {});
  assert.equal(typeof bridge.save, "function");
  assert.equal(typeof bridge.preview, "function");
  assert.equal(typeof bridge.refresh, "function");
  assert.equal(bridge.isDirty(), false);
  assert.equal(bridge.save(), false);
  assert.equal(saveClicks, 0);
  assert.equal(bridge.preview(), true);
  assert.equal(previewClicks, 1);
  bridge.refresh();
  assert.equal(reloaded, 1);
  bridge.cleanup();
});
