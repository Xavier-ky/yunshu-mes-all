/**
 * 空模板默认示例布局：A4 纸张中央竖排 矩形 + 文字 + 时间 + 二维码
 */
export const starterPanel = {
  panels: [{
    index: 0,
    height: 297,
    width: 210,
    paperHeader: 0,
    paperFooter: 0,
    paperNumberDisabled: true,
    printElements: [
      {
        options: {
          left: 212.5,
          top: 100,
          height: 255,
          width: 170,
        },
        printElementType: { title: "矩形", type: "rect" },
      },
      {
        options: {
          left: 212.5,
          top: 118,
          height: 24,
          width: 170,
          title: "云枢智造标签",
          fontFamily: "Microsoft YaHei",
          fontSize: 12,
          fontWeight: "700",
          textAlign: "center",
          textContentVerticalAlign: "middle",
        },
        printElementType: { title: "自定义文本", type: "text" },
      },
      {
        options: {
          left: 212.5,
          top: 158,
          height: 18,
          width: 170,
          field: "date",
          type: "date",
          testData: "2026-07-14 09:00:00",
          formatter: "yyyy-MM-dd HH:mm:ss",
          fontFamily: "Microsoft YaHei",
          fontSize: 9,
          textAlign: "center",
          textContentVerticalAlign: "middle",
        },
        printElementType: { title: "日期时间", type: "text" },
      },
      {
        options: {
          left: 257.5,
          top: 210,
          height: 80,
          width: 80,
          title: "YS-MES-001",
          field: "qrcode",
          testData: "YS-MES-001",
          textType: "qrcode",
          textAlign: "center",
        },
        printElementType: { title: "二维码", type: "text" },
      },
    ],
  }],
};

export function isEmptyTemplate(json) {
  const panels = json?.panels;
  if (!panels?.length) return true;
  return panels.every((p) => !p.printElements?.length);
}

export function resolveTemplateJson(templateJson) {
  if (!templateJson) {
    return { data: starterPanel, isStarter: true };
  }
  try {
    const parsed = typeof templateJson === "string" ? JSON.parse(templateJson) : templateJson;
    if (isEmptyTemplate(parsed)) {
      return { data: starterPanel, isStarter: true };
    }
    return { data: parsed, isStarter: false };
  } catch {
    return { data: starterPanel, isStarter: true };
  }
}
