export function buildPreviewData(template, printData = {}) {
  const data = { ...printData };
  const panels = template?.getJson?.()?.panels || [];

  panels.forEach((panel) => {
    (panel?.printElements || []).forEach((element) => {
      const options = element?.options || {};
      const field = options.field;
      if (
        !field
        || (data[field] !== undefined && data[field] !== null && data[field] !== "")
      ) return;
      data[field] = options.testData || options.title || "";
    });
  });

  return data;
}
