export function statusTone(status) {
  const normalized = String(status || "").toUpperCase();
  if (["启用", "ENABLED", "RUNNING", "COMPLETED", "合格", "正常", "完整"].includes(status) || normalized === "SUCCESS") {
    return "success";
  }
  if (["打开", "待检", "处理中", "暂停", "已分派", "WARNING"].includes(status) || normalized === "WARNING") {
    return "warning";
  }
  if (["停用", "失败", "异常", "维修中", "OPEN", "ALERT"].includes(status) || normalized === "ALERT") {
    return "danger";
  }
  return "normal";
}
