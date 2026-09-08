/** 属性面板：语义并排 + 复合区块（面板排列/水印） */
const PAIR_RULES = [
  ["打印规则", "分页规则"],
  ["首页页尾", "尾页页尾"],
  ["奇数页页尾", "偶数页页尾"],
  ["左偏移", "顶部偏移"],
  ["显示页码", "页码续排"],
];

function getItemLabel(item) {
  const label = item.querySelector(":scope > .hiprint-option-item-label");
  return label ? label.textContent.trim() : "";
}

function isPairableItem(item) {
  if (!item?.classList?.contains("hiprint-option-item")) return false;
  if (item.classList.contains("print-params-compound")) return false;
  const fields = item.querySelectorAll(":scope > .hiprint-option-item-field");
  return fields.length === 1 && !!item.querySelector(":scope > .hiprint-option-item-label");
}

function isCompoundRow(item) {
  const fields = item?.querySelectorAll(":scope > .hiprint-option-item-field");
  return fields && fields.length >= 2;
}

function resetCheckboxEnhancement(field) {
  if (!field || field.dataset.enhanced !== "1") return;
  const row = field.querySelector(".print-params-toggle-row");
  const checkbox = row?.querySelector('input[type="checkbox"]');
  if (checkbox && row) {
    field.appendChild(checkbox);
    row.remove();
  }
  delete field.dataset.enhanced;
}

function unwrapByClass(container, className) {
  container.querySelectorAll(`:scope > .${className}`).forEach((node) => {
    if (node.classList.contains("print-params-watermark-time")) {
      node.querySelectorAll(".print-params-checkbox-field").forEach(resetCheckboxEnhancement);
    }
    const parent = node.parentElement;
    if (!parent) return;
    while (node.firstChild) {
      parent.insertBefore(node.firstChild, node);
    }
    node.remove();
  });
}

function pairItems(container, left, right) {
  if (!left || !right || left === right) return;
  if (!isPairableItem(left) || !isPairableItem(right)) return;
  if (left.closest(".print-params-pair") || right.closest(".print-params-pair")) return;

  const wrapper = document.createElement("div");
  wrapper.className = "print-params-pair";
  container.insertBefore(wrapper, left);
  wrapper.appendChild(left);
  wrapper.appendChild(right);
}

function clearInlineLayoutStyles(node) {
  if (!node) return;
  if (node.closest?.(".minicolors")) return;
  if (node.classList?.contains("minicolors") || node.classList?.contains("minicolors-input")) return;
  if (node.style) {
    node.style.removeProperty("display");
    node.style.removeProperty("align-items");
    node.style.removeProperty("width");
    node.style.removeProperty("margin-top");
    node.style.removeProperty("margin-bottom");
  }
  node.querySelectorAll?.(
    ".hiprint-option-item-field, .hiprint-option-item-field > div:not(.minicolors), .hiprint-option-item-field > input:not(.minicolors-input), .hiprint-option-item-field > select",
  ).forEach(clearInlineLayoutStyles);
}

function enhanceCheckboxField(field) {
  if (!field || field.dataset.enhanced === "1") return;
  const checkbox = field.querySelector(':scope > input[type="checkbox"]');
  if (!checkbox) return;

  const row = document.createElement("div");
  row.className = "print-params-toggle-row";
  field.appendChild(row);
  row.appendChild(checkbox);

  const hint = document.createElement("span");
  hint.className = "print-params-toggle-hint";
  hint.textContent = "启用";
  row.appendChild(hint);
  field.dataset.enhanced = "1";
}

function layoutWatermarkCompound(item) {
  item.querySelectorAll(":scope > .hiprint-option-item-field").forEach((field) => {
    field.classList.remove("print-params-color-field", "print-params-checkbox-field");
    const text = field.textContent || "";
    if (text.includes("字体颜色")) field.classList.add("print-params-color-field");
    if (text.includes("水印时间")) field.classList.add("print-params-checkbox-field");
  });

  const timestampField = item.querySelector(":scope > .hiprint-option-item-field.print-params-checkbox-field");
  const formatField = [...item.querySelectorAll(":scope > .hiprint-option-item-field")].find((field) =>
    (field.textContent || "").includes("时间格式"),
  );
  if (!timestampField || !formatField) return;

  enhanceCheckboxField(timestampField);

  const wrapper = document.createElement("div");
  wrapper.className = "print-params-inline-gap print-params-watermark-time";
  item.insertBefore(wrapper, timestampField);
  wrapper.appendChild(timestampField);
  wrapper.appendChild(formatField);
}

function layoutCompoundRow(item) {
  unwrapByClass(item, "print-params-inline-gap");
  unwrapByClass(item, "print-params-watermark-time");
  item.classList.remove("print-params-compound");
  if (!isCompoundRow(item)) return;

  item.classList.add("print-params-compound");
  clearInlineLayoutStyles(item);

  const label = getItemLabel(item);
  const fields = [...item.querySelectorAll(":scope > .hiprint-option-item-field")];

  if (label === "面板排列" && fields.length === 3) {
    const wrapper = document.createElement("div");
    wrapper.className = "print-params-inline-gap";
    item.appendChild(wrapper);
    wrapper.appendChild(fields[1]);
    wrapper.appendChild(fields[2]);
    return;
  }

  if (label === "水印功能") {
    layoutWatermarkCompound(item);
  }
}

function layoutPairsInContainer(container) {
  unwrapByClass(container, "print-params-pair");

  container.querySelectorAll(":scope > .hiprint-option-item").forEach((item) => {
    clearInlineLayoutStyles(item);
    layoutCompoundRow(item);
  });

  const pairable = [...container.children].filter(
    (el) => el.classList?.contains("hiprint-option-item") && isPairableItem(el),
  );
  const byLabel = new Map(pairable.map((item) => [getItemLabel(item), item]));

  PAIR_RULES.forEach(([leftLabel, rightLabel]) => {
    pairItems(container, byLabel.get(leftLabel), byLabel.get(rightLabel));
  });
}

/**
 * @param {HTMLElement | null | undefined} root
 */
export function layoutPrintParamsPanel(root) {
  if (!root) return;
  root.querySelectorAll(".hiprint-option-items").forEach(layoutPairsInContainer);
}
