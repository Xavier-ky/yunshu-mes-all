/**
 * UReport 设计器嵌入桥接。
 * 只通过设计器已有 DOM 命令触发保存/预览，避免绕过 UReport 内部状态。
 */
const PATCH_STYLE_ID = "yunshu-ureport-workbench-patch";
const PANEL_HEAD_ID = "yunshu-ureport-prop-head";
/** UReport 1-based B5 → Handsontable 0-based row 4, col 1 */
const DEFAULT_CELL = Object.freeze({ row: 4, col: 1, cellName: "B5", label: "5行B列" });

export const UREPORT_EMBED_CSS = `
html, body {
  width: 100% !important;
  height: 100% !important;
  margin: 0 !important;
  overflow: hidden !important;
  color: #26354d;
  background: #e9eef5 !important;
  font-family: "Microsoft YaHei", "PingFang SC", sans-serif;
}
.ureport-right-hr-for-print { display: none !important; }
#container {
  box-sizing: border-box;
  display: grid !important;
  grid-template-rows: auto 1fr;
  grid-template-columns: minmax(0, 1fr) 340px;
  width: 100% !important;
  height: 100% !important;
  overflow: hidden !important;
  position: relative;
  background: #e9eef5 !important;
}
#container > .ud-toolbar {
  grid-column: 1 / -1;
  grid-row: 1;
  position: sticky !important;
  top: 0 !important;
  z-index: 30;
  width: 100% !important;
  min-height: 40px;
  padding: 3px 8px !important;
  border-bottom: 1px solid #cfd8e5 !important;
  background: #f8fafc !important;
  box-shadow: 0 2px 8px rgba(31, 47, 72, .05);
}
#container > .ud-toolbar .btn {
  color: #33445f !important;
  background: transparent !important;
}
#container > .ud-toolbar .btn:hover {
  background: #e8eff9 !important;
}
#container > .ud-property-panel {
  grid-column: 2;
  grid-row: 2;
  position: relative !important;
  inset: auto !important;
  width: 100% !important;
  height: 100% !important;
  max-height: none !important;
  margin: 0 !important;
  border: 0 !important;
  border-left: 1px solid #cfd8e5 !important;
  border-radius: 0 !important;
  box-shadow: none !important;
  overflow: hidden;
  display: flex;
  flex-direction: column;
  z-index: 20;
  cursor: default !important;
  background: #fff !important;
}
#container > .ud-property-panel .yunshu-prop-head {
  flex-shrink: 0;
  display: grid;
  grid-template-columns: minmax(0, 1fr) auto;
  align-items: center;
  column-gap: 8px;
  min-height: 34px;
  padding: 0 10px;
  border-bottom: 1px solid #cfd8e5;
  background: #f5f7fa;
  box-sizing: border-box;
}
#container > .ud-property-panel .yunshu-prop-head__title {
  margin: 0;
  color: #172033;
  font-size: 13px;
  font-weight: 700;
  line-height: 34px;
  letter-spacing: 0.02em;
}
#container > .ud-property-panel .yunshu-prop-head__badge {
  display: inline-flex;
  align-items: center;
  justify-content: center;
  box-sizing: border-box;
  min-width: 48px;
  height: 22px;
  padding: 0 8px;
  color: #1769e0;
  font-size: 12px;
  font-weight: 700;
  font-family: Consolas, "Courier New", monospace;
  line-height: 1;
  border: 1px solid #cfd8e5;
  border-radius: 0;
  background: #fff;
}
#container > .ud-property-panel .yunshu-prop-head__badge.is-empty {
  color: #94a3b8;
  background: #fafafa;
}
#container > .ud-property-panel .nav-tabs {
  flex-shrink: 0;
  display: grid !important;
  grid-template-columns: repeat(2, minmax(0, 1fr));
  gap: 0;
  width: 100%;
  margin: 0 !important;
  padding: 0 !important;
  list-style: none;
  border-bottom: 1px solid #cfd8e5 !important;
  background: #fff !important;
  box-sizing: border-box;
}
#container > .ud-property-panel .nav-tabs::before,
#container > .ud-property-panel .nav-tabs::after {
  display: none !important;
  content: none !important;
}
#container > .ud-property-panel .nav-tabs > li {
  float: none !important;
  display: block;
  width: 100%;
  margin: 0 !important;
}
#container > .ud-property-panel .nav-tabs > li > a {
  display: block;
  width: 100%;
  margin: 0 !important;
  padding: 9px 0 !important;
  color: #51627a !important;
  font-size: 14px;
  font-weight: 600;
  line-height: 1.25;
  text-align: center;
  border: 0 !important;
  border-right: 1px solid #cfd8e5 !important;
  border-radius: 0 !important;
  background: #fafafa !important;
  box-sizing: border-box;
}
#container > .ud-property-panel .nav-tabs > li:last-child > a {
  border-right: 0 !important;
}
#container > .ud-property-panel .nav-tabs > li.active > a,
#container > .ud-property-panel .nav-tabs > li.active > a:hover,
#container > .ud-property-panel .nav-tabs > li.active > a:focus {
  color: #172033 !important;
  border-bottom: 0 !important;
  background: #fff !important;
  box-shadow: inset 0 -2px 0 #1769e0;
}
#container > .ud-property-panel .nav-tabs .glyphicon-circle-arrow-down {
  display: none !important;
}
#container > .ud-property-panel .tab-content {
  flex: 1 1 auto;
  min-height: 0;
  overflow-x: hidden;
  overflow-y: auto;
  display: block !important;
  padding: 0 !important;
  background: #fff !important;
}
#container > .ud-property-panel .tab-pane {
  padding: 0 !important;
}
#container > .ud-property-panel #_prop_container,
#container > .ud-property-panel #_datasource_container {
  padding: 8px !important;
  box-sizing: border-box;
}
#container > .ud-property-panel #_prop_container > div,
#container > .ud-property-panel #_datasource_container > div {
  margin: 0 !important;
  padding: 0 !important;
}
#container > .ud-property-panel #_prop_container .form-group,
#container > .ud-property-panel #_datasource_container .form-group {
  display: block !important;
  margin: 0 0 8px !important;
  white-space: normal !important;
}
#container > .ud-property-panel #_prop_container .form-group > label:first-child,
#container > .ud-property-panel #_datasource_container .form-group > label:first-child {
  display: block;
  width: 100%;
  margin: 0 0 4px !important;
  padding: 0 !important;
  color: #334155;
  font-size: 13px;
  font-weight: 600;
  line-height: 1.35;
  text-align: left !important;
}
#container > .ud-property-panel #_prop_container .yunshu-label-hint {
  display: block;
  margin-top: 2px;
  color: #94a3b8;
  font-size: 11px;
  font-weight: 400;
  line-height: 1.35;
}
#container > .ud-property-panel #_prop_container .yunshu-parent-field {
  padding-bottom: 8px;
  margin-bottom: 8px !important;
  border-bottom: 1px solid #edf1f6;
}
#container > .ud-property-panel #_prop_container .yunshu-parent-head {
  display: flex;
  align-items: center;
  gap: 12px;
  margin-bottom: 6px;
}
#container > .ud-property-panel #_prop_container .yunshu-parent-head > label {
  flex: 0 0 auto;
  width: auto !important;
  margin: 0 !important;
  font-size: 13px !important;
  font-weight: 700 !important;
  white-space: nowrap;
}
#container > .ud-property-panel #_prop_container .yunshu-parent-radios {
  display: flex;
  align-items: center;
  flex: 1 1 auto;
  gap: 14px;
  margin: 0 !important;
}
#container > .ud-property-panel #_prop_container .yunshu-parent-selects {
  display: grid;
  grid-template-columns: minmax(0, 1fr) minmax(0, 1fr);
  gap: 8px;
}
#container > .ud-property-panel #_prop_container .checkbox-inline,
#container > .ud-property-panel #_datasource_container .checkbox-inline {
  display: inline-flex !important;
  align-items: center;
  gap: 4px;
  margin: 0 !important;
  padding: 0 !important;
  color: #334155 !important;
  font-size: 13px;
  font-weight: 500;
  white-space: nowrap;
  border: 0 !important;
  border-radius: 0 !important;
  background: transparent !important;
}
#container > .ud-property-panel #_prop_container .checkbox-inline input,
#container > .ud-property-panel #_datasource_container .checkbox-inline input {
  margin: 0 !important;
  position: static !important;
}
#container > .ud-property-panel #_prop_container .form-control,
#container > .ud-property-panel #_datasource_container .form-control {
  display: block;
  width: 100% !important;
  max-width: 100% !important;
  min-height: 28px;
  height: 28px !important;
  margin: 0 !important;
  padding: 5px 8px !important;
  color: #1a1a1a !important;
  font-size: 13px !important;
  line-height: 1.3;
  border: 1px solid #cfd8e5 !important;
  border-radius: 0 !important;
  background: #fafafa !important;
  box-shadow: none !important;
  box-sizing: border-box;
}
#container > .ud-property-panel #_prop_container textarea.form-control,
#container > .ud-property-panel #_datasource_container textarea.form-control {
  min-height: 96px;
  height: auto !important;
  resize: vertical;
}
#container > .ud-property-panel #_prop_container textarea.yunshu-text-content,
#container > .ud-property-panel #_prop_container textarea[data-yunshu-text-content="1"] {
  width: 100% !important;
  min-height: 96px;
  height: auto !important;
  margin-bottom: 0 !important;
  resize: vertical;
  box-sizing: border-box;
  position: relative;
  z-index: 2;
  pointer-events: auto;
}
#container > .ud-property-panel #_prop_container .yunshu-text-content-fill {
  display: flex;
  flex-direction: column;
  min-height: 96px;
  margin-top: 2px;
  pointer-events: none;
}
#container > .ud-property-panel #_prop_container .yunshu-text-content-fill .yunshu-text-content-label,
#container > .ud-property-panel #_prop_container .yunshu-text-content-fill textarea {
  pointer-events: auto;
}
#container > .ud-property-panel #_prop_container .yunshu-text-content-fill textarea.yunshu-text-content,
#container > .ud-property-panel #_prop_container .yunshu-text-content-fill textarea[data-yunshu-text-content="1"] {
  flex: 1 1 auto;
  min-height: 96px;
}
#container > .ud-property-panel #_prop_container .yunshu-text-content-label {
  flex-shrink: 0;
  margin: 6px 0 4px !important;
}
#container > .ud-property-panel #_prop_container .yunshu-text-content-label label {
  margin: 0;
  color: #334155;
  font-size: 13px;
  font-weight: 600;
}
#container > .ud-property-panel #_prop_container .form-control:focus,
#container > .ud-property-panel #_datasource_container .form-control:focus {
  border-color: #1769e0 !important;
  background: #fff !important;
  box-shadow: none !important;
  outline: none !important;
}
#container > .ud-property-panel #_prop_container fieldset,
#container > .ud-property-panel #_datasource_container fieldset {
  width: 100% !important;
  margin: 0 0 8px !important;
  padding: 8px !important;
  border: 1px solid #cfd8e5 !important;
  border-radius: 0 !important;
  background: #fafafa;
  box-shadow: none;
  box-sizing: border-box;
}
#container > .ud-property-panel #_prop_container fieldset legend,
#container > .ud-property-panel #_datasource_container fieldset legend {
  width: auto !important;
  margin: 0 0 6px !important;
  padding: 0 !important;
  color: #334155 !important;
  font-size: 13px !important;
  font-weight: 700 !important;
  line-height: 1.3;
  border: 0 !important;
}
#container > .ud-property-panel #_prop_container fieldset .yunshu-field-row {
  margin-bottom: 8px !important;
}
#container > .ud-property-panel #_prop_container fieldset .yunshu-field-row:last-child {
  margin-bottom: 0 !important;
}
#container > .ud-property-panel #_prop_container .yunshu-inline-actions {
  display: grid;
  grid-template-columns: minmax(0, 1fr) auto;
  gap: 8px;
  align-items: center;
}
#container > .ud-property-panel #_prop_container .yunshu-inline-actions .form-control {
  width: 100% !important;
}
#container > .ud-property-panel #_prop_container .btn,
#container > .ud-property-panel #_datasource_container .btn {
  margin: 0 !important;
  min-height: 28px;
  height: 28px;
  padding: 0 12px !important;
  font-size: 12px !important;
  line-height: 26px;
  border-radius: 0 !important;
  white-space: nowrap;
  box-sizing: border-box;
}
#container > .ud-property-panel #_prop_container .btn-primary,
#container > .ud-property-panel #_datasource_container .btn-primary {
  color: #fff !important;
  border-color: #1769e0 !important;
  background: #1769e0 !important;
}
#container > .ud-property-panel #_prop_container .yunshu-hidden-advanced,
#container > .ud-property-panel #_prop_container .form-group.yunshu-render-bean {
  display: none !important;
}
#container > .handsontable,
#container > div:not(.ud-toolbar):not(.ud-property-panel) {
  grid-column: 1;
  grid-row: 2;
  min-width: 0;
  min-height: 0;
  width: 100% !important;
  height: 100% !important;
  padding: 0 !important;
  overflow: hidden !important;
  box-sizing: border-box;
  background: #e9eef5 !important;
}
#container > .handsontable,
#container > .handsontable .handsontable,
#container > div:not(.ud-toolbar):not(.ud-property-panel) .handsontable {
  font-size: 13px;
  filter: none !important;
}
`;

function normalizePropertyPanelLayout(doc) {
  const panel = doc.querySelector("#_prop_container > div");
  if (!panel) return;

  panel.querySelectorAll(":scope > fieldset .form-group").forEach((group) => {
    group.classList.add("yunshu-field-row");
    const label = group.querySelector(":scope > label");
    if (!label || label.dataset.yunshuNormalized) return;
    const span = label.querySelector("span");
    if (span && /URL/i.test(label.textContent || "")) {
      label.dataset.yunshuNormalized = "1";
      const hint = span.getAttribute("title") || span.textContent || "";
      label.innerHTML = 'URL<span class="yunshu-label-hint">（支持表达式，写法 ${...}）</span>';
      if (hint) label.title = hint;
    }
  });

  panel.querySelectorAll(":scope > fieldset .form-group").forEach((group) => {
    if (group.querySelector(".yunshu-inline-actions")) return;
    const select = group.querySelector("select.form-control");
    const button = group.querySelector(".btn");
    if (!select || !button) return;
    const wrap = doc.createElement("div");
    wrap.className = "yunshu-inline-actions";
    select.before(wrap);
    wrap.appendChild(select);
    wrap.appendChild(button);
  });

  panel.querySelectorAll(":scope > div").forEach((block) => {
    block.querySelectorAll(":scope > .form-group").forEach((group) => {
      if (!group.querySelector(".checkbox-inline")) return;
      group.classList.add("yunshu-parent-field");

      let radioWrap = group.querySelector(".yunshu-parent-radios");
      if (!radioWrap) {
        radioWrap = doc.createElement("div");
        radioWrap.className = "yunshu-parent-radios";
        const radios = [...group.querySelectorAll(":scope > .checkbox-inline")];
        if (radios.length) {
          radios[0].before(radioWrap);
          radios.forEach((node) => radioWrap.appendChild(node));
        }
      }

      const label = group.querySelector(":scope > label:first-child")
        || group.querySelector(".yunshu-parent-head > label");
      if (label && radioWrap && !group.querySelector(".yunshu-parent-head")) {
        const head = doc.createElement("div");
        head.className = "yunshu-parent-head";
        group.insertBefore(head, label);
        head.appendChild(label);
        head.appendChild(radioWrap);
      }

      const selects = [...group.querySelectorAll(":scope > select.form-control")];
      if (selects.length >= 2 && !group.querySelector(".yunshu-parent-selects")) {
        const selectWrap = doc.createElement("div");
        selectWrap.className = "yunshu-parent-selects";
        selects[0].before(selectWrap);
        selects.forEach((node) => selectWrap.appendChild(node));
      }
      group.dataset.yunshuParentFixed = "1";
    });
  });

  panel.querySelectorAll(":scope > .form-group").forEach((group) => {
    const label = group.querySelector(":scope > label");
    const text = label?.textContent || "";
    if (/渲染Bean|renderBean/i.test(text) || group.querySelector(".input-group-btn")) {
      group.classList.add("yunshu-render-bean", "yunshu-hidden-advanced");
    }
  });

  panel.querySelectorAll(":scope > div textarea.form-control, :scope > div > div textarea.form-control").forEach((textarea) => {
    const view = doc.defaultView;
    if (textarea.offsetParent === null && view?.getComputedStyle(textarea).display === "none") return;
    textarea.dataset.yunshuTextContent = "1";
    const labelWrap = textarea.previousElementSibling;
    if (labelWrap?.querySelector?.("label")) {
      labelWrap.classList.add("yunshu-text-content-label");
      if (!labelWrap.parentElement?.classList?.contains("yunshu-text-content-fill")) {
        const fill = doc.createElement("div");
        fill.className = "yunshu-text-content-fill";
        labelWrap.before(fill);
        fill.appendChild(labelWrap);
        fill.appendChild(textarea);
      }
    }
  });

  fitTextContentArea(doc);
  guardPropertyPanelInputs(doc);
}

function isPropertyPanelInputFocused(doc) {
  const active = doc?.activeElement;
  if (!active || active === doc.body) return false;
  return Boolean(active.closest?.("#_prop_container, #_datasource_container, .ud-property-panel"));
}

function stopPanelPointerBubble(event) {
  event.stopPropagation();
}

function guardPropertyPanel(doc) {
  const panel = doc.querySelector(".ud-property-panel");
  if (!panel || panel.dataset.yunshuPanelGuard === "1") return;
  panel.dataset.yunshuPanelGuard = "1";
  ["mousedown", "pointerdown", "click"].forEach((type) => {
    panel.addEventListener(type, stopPanelPointerBubble);
  });
}

function guardPropertyPanelInputs(doc) {
  const root = doc.querySelector("#_prop_container");
  if (!root) return;
  root.querySelectorAll("input, textarea, select, button, label, .checkbox-inline").forEach((el) => {
    if (el.dataset.yunshuInputGuard === "1") return;
    el.dataset.yunshuInputGuard = "1";
    ["mousedown", "pointerdown", "click"].forEach((type) => {
      el.addEventListener(type, stopPanelPointerBubble);
    });
  });
}

function fitTextContentArea(doc) {
  const win = doc.defaultView;
  const panel = doc.querySelector(".ud-property-panel");
  const tabContent = doc.querySelector(".ud-property-panel .tab-content");
  const fill = [...doc.querySelectorAll("#_prop_container .yunshu-text-content-fill")]
    .find((el) => el.offsetParent !== null && win?.getComputedStyle(el).display !== "none");
  const textarea = fill?.querySelector('textarea[data-yunshu-text-content="1"]');
  if (!panel || !tabContent || !fill || !textarea) return;

  const panelBottom = panel.getBoundingClientRect().bottom;
  const scrollBottom = tabContent.getBoundingClientRect().bottom;
  const anchorBottom = Math.min(panelBottom, scrollBottom) - 11;
  const fillTop = fill.getBoundingClientRect().top;
  const available = Math.floor(anchorBottom - fillTop);
  if (available < 96) return;

  fill.style.minHeight = `${available}px`;
  fill.style.height = `${available}px`;
  textarea.style.removeProperty("height");
  textarea.style.removeProperty("min-height");
}

function syncPropertyPanelBadge(doc) {
  const badge = doc?.querySelector?.(`#${PANEL_HEAD_ID} [data-role="cell-badge"]`);
  const propTab = doc?.querySelector?.("#__prop_tab_link");
  const text = propTab?.textContent || "";
  const match = text.match(/\[([A-Z]+\d+)\]/);
  if (match && badge) {
    badge.textContent = match[1];
    badge.classList.remove("is-empty");
  }
  if (propTab && match) {
    propTab.textContent = "属性";
  }
}

function enhancePropertyPanelShell(doc) {
  const panel = doc?.querySelector?.(".ud-property-panel");
  if (!panel) return;

  if (!panel.querySelector(`#${PANEL_HEAD_ID}`)) {
    const head = doc.createElement("div");
    head.id = PANEL_HEAD_ID;
    head.className = "yunshu-prop-head";
    head.innerHTML = `
    <strong class="yunshu-prop-head__title">单元格属性</strong>
    <span class="yunshu-prop-head__badge is-empty" data-role="cell-badge">未选</span>
  `;

    const tabs = panel.querySelector(".nav-tabs");
    if (tabs) panel.insertBefore(head, tabs);
    else panel.prepend(head);
  }

  guardPropertyPanel(doc);
  syncPropertyPanelBadge(doc);
  normalizePropertyPanelLayout(doc);

  if (!panel.dataset.yunshuObserversBound) {
    panel.dataset.yunshuObserversBound = "1";
    const propTab = doc.querySelector("#__prop_tab_link");
    if (propTab) {
      const observer = new MutationObserver(() => {
        syncPropertyPanelBadge(doc);
        normalizePropertyPanelLayout(doc);
      });
      observer.observe(propTab, { childList: true, characterData: true, subtree: true });
    }

    const propContainer = doc.querySelector("#_prop_container");
    if (propContainer) {
      let layoutTimer = null;
      const layoutObserver = new MutationObserver(() => {
        if (layoutTimer) clearTimeout(layoutTimer);
        layoutTimer = setTimeout(() => {
          normalizePropertyPanelLayout(doc);
          fitTextContentArea(doc);
        }, 60);
      });
      layoutObserver.observe(propContainer, { childList: true, subtree: true });
      doc.defaultView?.addEventListener?.("resize", () => fitTextContentArea(doc));
      setTimeout(() => fitTextContentArea(doc), 150);
      setTimeout(() => fitTextContentArea(doc), 500);
    }
  }
}

function destroyPropertyPanelDrag(win) {
  if (!win?.$) return;
  const $ = win.$;
  const panel = $(".ud-property-panel");
  if (!panel.length) return;
  try {
    panel.draggable("destroy");
  } catch {
    // UReport ships a custom draggable plugin without a destroy implementation.
  }
  panel.off("mousedown mouseup");
  $(document).off("mousemove.drag");

  const originalDraggable = $.fn.draggable;
  if (typeof originalDraggable === "function" && !originalDraggable.__yunshuPatched) {
    $.fn.draggable = function patchedDraggable(options) {
      if (this.hasClass("ud-property-panel")) return this;
      return originalDraggable.call(this, options);
    };
    $.fn.draggable.__yunshuPatched = true;
  }
}

function triggerHotResize(win) {
  if (!win?.dispatchEvent) return;
  const resizeEvent = typeof Event === "function" ? new Event("resize") : { type: "resize" };
  win.dispatchEvent(resizeEvent);
  setTimeout(() => win.dispatchEvent(resizeEvent), 180);
}

function findTableHost(doc) {
  const root = doc?.getElementById?.("container");
  if (!root) return null;
  for (const child of root.children) {
    if (child.classList?.contains("ud-toolbar") || child.classList?.contains("ud-property-panel")) continue;
    if (child.querySelector?.(".ht_master") || child.classList?.contains("handsontable")) return child;
  }
  return doc.querySelector("#container .ht_master")?.closest?.(".handsontable, div") || null;
}

function readHotFromElement(win, element) {
  if (!element) return null;
  if (win?.$) {
    const $el = win.$(element);
    const fromData = $el.data?.("handsontable");
    if (fromData?.context?.getCell) return fromData;
    try {
      const fromPlugin = $el.handsontable?.("getInstance");
      if (fromPlugin?.context?.getCell) return fromPlugin;
    } catch {
      // Handsontable 0.x jQuery plugin may not be mounted on this node.
    }
  }
  if (element.hot?.context?.getCell) return element.hot;
  if (element.__hotInstance?.context?.getCell) return element.__hotInstance;
  return null;
}

function resolveHotInstance(win, doc) {
  const host = findTableHost(doc);
  if (!host) return null;

  const candidates = [host];
  if (host.classList?.contains("handsontable")) {
    candidates.push(host);
  } else {
    const inner = host.querySelector?.(".handsontable");
    if (inner) candidates.push(inner);
  }

  for (const candidate of candidates) {
    const hot = readHotFromElement(win, candidate);
    if (hot) return hot;
  }

  const master = doc.querySelector(".ht_master");
  if (master) {
    let el = master.parentElement;
    while (el && el !== doc.body) {
      const hot = readHotFromElement(win, el);
      if (hot) return hot;
      el = el.parentElement;
    }
  }

  return null;
}

/** ht_master 首列为行号区，数据列 DOM 索引 = 逻辑列 + 1 */
const DOM_LEADING_COLS = 1;

function isDesignerGridReady(doc) {
  const tbody = doc?.querySelector?.(".ht_master tbody");
  const { row, col } = DEFAULT_CELL;
  const domCol = col + DOM_LEADING_COLS;
  return Boolean(tbody?.rows?.[row]?.cells?.[domCol]);
}

function isDesignerDataReady(hot, doc) {
  if (hot?.context?.getCell) {
    const { row, col } = DEFAULT_CELL;
    try {
      const rowCount = typeof hot.countRows === "function" ? hot.countRows() : 0;
      const colCount = typeof hot.countCols === "function" ? hot.countCols() : 0;
      if (rowCount <= row || colCount <= col) return false;
      return Boolean(hot.context.getCell(row, col));
    } catch {
      return false;
    }
  }
  return isDesignerGridReady(doc);
}

function isDefaultCellSelected(doc) {
  const link = doc?.querySelector?.("#__prop_tab_link");
  const text = link?.textContent || link?.innerHTML || "";
  return text.includes(`[${DEFAULT_CELL.cellName}]`);
}

function ensurePropertyPanelVisible(doc) {
  const panel = doc?.querySelector?.(".ud-property-panel");
  if (!panel) return;
  const tabContent = panel.querySelector(".tab-content");
  if (tabContent) {
    tabContent.style.display = "block";
    tabContent.classList?.add?.("in");
  }
  const propTab = doc.querySelector("#__prop_tab_link");
  propTab?.click?.();
  const propPane = doc.querySelector("#_prop_container");
  if (propPane) {
    propPane.classList?.add?.("active", "in");
    propPane.style.display = "";
  }
}

function dispatchCellPointer(win, td) {
  if (!td || !win) return;
  const eventInit = { bubbles: true, cancelable: true, view: win, button: 0 };
  ["mousedown", "mouseup", "click"].forEach((type) => {
    td.dispatchEvent(new MouseEvent(type, eventInit));
  });
}

function clickCellByDom(win, doc, row, col) {
  const tbody = doc?.querySelector?.(".ht_master tbody");
  const domCol = col + DOM_LEADING_COLS;
  const td = tbody?.rows?.[row]?.cells?.[domCol];
  if (!td) return false;
  dispatchCellPointer(win, td);
  if (win.$) {
    try {
      win.$(td).trigger("mousedown").trigger("mouseup").trigger("click");
    } catch {
      // jQuery trigger is optional; native events above are enough.
    }
  }
  return true;
}

function triggerSelectionHooks(hot, row, col) {
  if (typeof hot.runHooks === "function") {
    hot.runHooks("afterSelectionEnd", row, col, row, col);
    return true;
  }
  const Handsontable = hot.constructor;
  if (Handsontable?.hooks?.run) {
    Handsontable.hooks.run(hot, "afterSelectionEnd", row, col, row, col);
    return true;
  }
  return false;
}

function selectHotCell(hot, row, col) {
  if (typeof hot.selectCell === "function") {
    try {
      hot.selectCell(row, col, row, col, false);
      return true;
    } catch {
      try {
        hot.selectCell(row, col);
        return true;
      } catch {
        return false;
      }
    }
  }
  if (typeof hot.selectCells === "function") {
    hot.selectCells([[row, col, row, col]]);
    return true;
  }
  return false;
}

function selectDefaultDesignerCell(win, doc) {
  if (isPropertyPanelInputFocused(doc)) return isDefaultCellSelected(doc);
  if (!isDesignerGridReady(doc)) return false;

  const hot = resolveHotInstance(win, doc);
  const { row, col } = DEFAULT_CELL;
  ensurePropertyPanelVisible(doc);

  if (hot && isDesignerDataReady(hot, doc)) {
    selectHotCell(hot, row, col);
    triggerSelectionHooks(hot, row, col);
  }

  clickCellByDom(win, doc, row, col);
  if (hot) triggerSelectionHooks(hot, row, col);

  if (!isDefaultCellSelected(doc)) {
    const td = typeof hot?.getCell === "function" ? hot.getCell(row, col) : null;
    if (td) dispatchCellPointer(win, td);
    if (hot) triggerSelectionHooks(hot, row, col);
  }

  ensurePropertyPanelVisible(doc);
  normalizePropertyPanelLayout(doc);
  fitTextContentArea(doc);
  syncPropertyPanelBadge(doc);
  return isDefaultCellSelected(doc);
}

function scheduleDefaultCellSelection(win, doc, isCancelled) {
  let attempt = 0;
  const tick = () => {
    if (isCancelled()) return;
    if (selectDefaultDesignerCell(win, doc)) return;
    if (attempt++ < 120) setTimeout(tick, 120);
  };
  setTimeout(tick, 200);
}

function findPreviewAction(doc) {
  const icon = doc?.querySelector?.(".ureport-preview");
  return icon?.closest?.(".btn-group")?.querySelector?.(".dropdown-menu li a") || null;
}

export function patchUreportDesignerIframe(iframeEl, callbacks = {}) {
  const options = typeof callbacks === "function" ? { onReady: callbacks } : callbacks;
  let cancelled = false;
  let ready = false;
  let timer = null;
  let dirtyObserver = null;

  const getDocument = () => iframeEl?.contentDocument || null;
  const getWindow = () => iframeEl?.contentWindow || null;
  const getSaveItem = () => getDocument()?.querySelector?.("#__save_btn") || null;
  const isDirty = () => {
    const item = getSaveItem();
    return Boolean(item && !item.classList?.contains?.("disabled"));
  };

  const emitDirty = () => options.onDirtyChange?.(isDirty());

  const attachDirtyObserver = () => {
    dirtyObserver?.disconnect?.();
    const saveItem = getSaveItem();
    const Observer = getWindow()?.MutationObserver || globalThis.MutationObserver;
    if (saveItem && Observer) {
      dirtyObserver = new Observer(emitDirty);
      dirtyObserver.observe(saveItem, { attributes: true, attributeFilter: ["class"] });
    }
    emitDirty();
  };

  const tryPatch = (attempt = 0) => {
    if (cancelled) return;
    const doc = getDocument();
    const win = getWindow();
    if (!doc || !win) {
      if (attempt < 80) timer = setTimeout(() => tryPatch(attempt + 1), 100);
      else options.onError?.("无法访问报表设计器");
      return;
    }

    if (!doc.getElementById?.(PATCH_STYLE_ID)) {
      const style = doc.createElement("style");
      style.id = PATCH_STYLE_ID;
      style.textContent = UREPORT_EMBED_CSS;
      doc.head?.appendChild?.(style);
    }

    const toolbar = doc.querySelector?.(".ud-toolbar");
    const panel = doc.querySelector?.(".ud-property-panel");
    const tableHost = findTableHost(doc);
    if (!toolbar || !panel || !tableHost) {
      if (attempt < 80) timer = setTimeout(() => tryPatch(attempt + 1), 100);
      else options.onError?.("设计器结构加载超时");
      return;
    }

    destroyPropertyPanelDrag(win);
    enhancePropertyPanelShell(doc);
    triggerHotResize(win);
    attachDirtyObserver();
    scheduleDefaultCellSelection(win, doc, () => cancelled);
    ready = true;
    options.onReady?.();
  };

  const onLoad = () => {
    ready = false;
    dirtyObserver?.disconnect?.();
    if (timer) clearTimeout(timer);
    timer = setTimeout(() => tryPatch(0), 40);
  };

  iframeEl?.addEventListener?.("load", onLoad);
  if (getDocument()?.readyState === "complete") tryPatch(0);

  const bridge = {
    get ready() {
      return ready;
    },
    isDirty,
    /**
     * 触发 UReport 内置保存。
     * @param {boolean} [force=true] 云枢工具栏显式保存时强制触发（即使 li.disabled）
     */
    save(force = true) {
      const win = getWindow();
      const item = getSaveItem();
      if (!item) return false;
      const disabled = item.classList?.contains?.("disabled");
      if (disabled && !force) return false;
      // Bootstrap .disabled > a { pointer-events:none } 会挡住原生 click；
      // jQuery triggerHandler 直接调用 SaveTool 绑在 li 上的处理器。
      if (force) item.classList.remove("disabled");
      const $ = win?.jQuery || win?.$;
      if ($) {
        try {
          $(item).triggerHandler("click");
          return true;
        } catch {
          /* fall through */
        }
      }
      const action = item.querySelector?.("a") || item;
      action.dispatchEvent?.(
        new MouseEvent("click", { bubbles: true, cancelable: true, view: win }),
      );
      return true;
    },
    preview() {
      const action = findPreviewAction(getDocument());
      if (!action) return false;
      action.click?.();
      return true;
    },
    refresh() {
      getWindow()?.location?.reload?.();
    },
    selectDefaultCell() {
      const win = getWindow();
      const doc = getDocument();
      if (!win || !doc) return false;
      return selectDefaultDesignerCell(win, doc);
    },
    cleanup() {
      cancelled = true;
      ready = false;
      if (timer) clearTimeout(timer);
      dirtyObserver?.disconnect?.();
      iframeEl?.removeEventListener?.("load", onLoad);
    },
  };

  return bridge;
}
