export const REPORT_SUFFIX = ".ureport.xml";

export const PRESET_TEMPLATES = [
  {
    fileName: `MES通用表格模板${REPORT_SUFFIX}`,
    label: "通用表格",
    category: "general",
    categoryLabel: "通用",
    description: "标准数据清单与签核底稿",
    paper: "A4 · 纵向",
    preview: "table",
    protected: true,
  },
  {
    fileName: `产量统计报表${REPORT_SUFFIX}`,
    label: "产量统计",
    category: "production",
    categoryLabel: "生产",
    description: "计划、完成、差异与达成率",
    paper: "A4 · 横向",
    preview: "metrics",
    protected: true,
  },
  {
    fileName: `质量检验报告${REPORT_SUFFIX}`,
    label: "质量检验",
    category: "quality",
    categoryLabel: "质量",
    description: "来料检验与合格率分析",
    paper: "A4 · 横向",
    preview: "quality",
    protected: true,
  },
  {
    fileName: `生产工单打印模版${REPORT_SUFFIX}`,
    label: "工单打印",
    category: "print",
    categoryLabel: "打印",
    description: "车间生产流转与签核单",
    paper: "A4 · 纵向",
    preview: "form",
    protected: true,
  },
];

const presetByFile = Object.fromEntries(PRESET_TEMPLATES.map((item) => [item.fileName, item]));
const presetOrder = Object.fromEntries(PRESET_TEMPLATES.map((item, index) => [item.fileName, index]));

export function normalizeReportFileName(value) {
  const name = String(value || "").trim();
  if (!name) return "";
  return name.endsWith(REPORT_SUFFIX) ? name : `${name}${REPORT_SUFFIX}`;
}

export function reportDisplayName(fileName) {
  return String(fileName || "").replace(/\.ureport\.xml$/i, "");
}

export function validateReportName(value, existingNames = [], currentName = "") {
  const raw = String(value || "").trim();
  if (!raw) return "请输入模板名称";
  if (/[\\/:*?"<>|]/.test(raw)) return '模板名称不能包含 \\/:*?"<>|';
  const fileName = normalizeReportFileName(raw);
  if (fileName.length > 100) return "模板名称过长";
  const normalizedCurrent = normalizeReportFileName(currentName);
  const duplicated = existingNames
    .map(normalizeReportFileName)
    .some((name) => name === fileName && name !== normalizedCurrent);
  return duplicated ? "模板名称已存在" : "";
}

export function buildTemplateCatalog(rows = []) {
  return rows
    .map((row) => {
      const fileName = row.name || row.name_ || row.fileName;
      if (!fileName || !String(fileName).endsWith(REPORT_SUFFIX)) return null;
      const preset = presetByFile[fileName];
      return {
        id: row.id,
        fileName,
        label: preset?.label || reportDisplayName(fileName),
        category: preset?.category || "custom",
        categoryLabel: preset?.categoryLabel || "自定义",
        description: preset?.description || "团队自定义报表模板",
        paper: preset?.paper || "A4 · 自定义",
        preview: preset?.preview || "custom",
        protected: Boolean(preset?.protected),
        createTime: row.createTime || row.create_time_,
        updateTime: row.updateTime || row.update_time_,
      };
    })
    .filter(Boolean)
    .sort((left, right) => {
      const leftOrder = presetOrder[left.fileName] ?? 100;
      const rightOrder = presetOrder[right.fileName] ?? 100;
      return leftOrder - rightOrder;
    });
}

export function replaceTemplateTitle(xml, title) {
  const safeTitle = String(title || "").replaceAll("]]>", "");
  return String(xml || "").replace(
    /<simple-value><!\[CDATA\[[\s\S]*?\]\]><\/simple-value>/,
    `<simple-value><![CDATA[${safeTitle}]]></simple-value>`,
  );
}
