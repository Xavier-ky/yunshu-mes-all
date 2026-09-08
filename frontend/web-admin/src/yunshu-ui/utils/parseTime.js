export function parseTime(time, pattern = "{y}-{m}-{d}") {
  if (!time) return "";
  const d = new Date(time);
  if (Number.isNaN(d.getTime())) return String(time);
  const map = {
    y: d.getFullYear(),
    m: String(d.getMonth() + 1).padStart(2, "0"),
    d: String(d.getDate()).padStart(2, "0"),
    h: String(d.getHours()).padStart(2, "0"),
    i: String(d.getMinutes()).padStart(2, "0"),
    s: String(d.getSeconds()).padStart(2, "0"),
  };
  return pattern.replace(/\{([ymdhis])\}/g, (_, k) => map[k]);
}
