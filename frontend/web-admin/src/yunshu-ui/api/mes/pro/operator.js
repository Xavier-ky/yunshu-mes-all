import { fetchDispatchTasks, updateDispatchTask } from "@/api/planning";
import { request } from "@/api/request";
import { fetchProductSns } from "@/api/domain";
import yunshuRequest from "@/yunshu-ui/api/request";
import { listFeedback } from "@/yunshu-ui/api/mes/pro/feedback";
import { listProcesscontent } from "@/yunshu-ui/api/mes/pro/processcontent";
import { listMdItem } from "@/yunshu-ui/api/mes/md/mdItem";
import { listBom } from "@/yunshu-ui/api/mes/md/bom";
import { isSuperAdminUser } from "@/constants/super-admin";

/** 演示账号 worker / worker01 互为别名，派工均归属 worker (user_id=6) */
const DEMO_OPERATOR_ACCOUNTS = new Set(["worker", "worker01"]);
const DEMO_OPERATOR_USER_ID = "6";

export function shouldViewAllDispatchTasks(authUser) {
  return isSuperAdminUser(authUser);
}

export function filterDispatchTasksForUser(tasks, authUser) {
  if (shouldViewAllDispatchTasks(authUser)) return tasks || [];
  const uid = String(authUser?.userId ?? "");
  const uname = (authUser?.username || "").trim();
  const linked = new Set([uname]);
  if (DEMO_OPERATOR_ACCOUNTS.has(uname)) {
    DEMO_OPERATOR_ACCOUNTS.forEach((n) => linked.add(n));
    return (tasks || []).filter((t) => {
      const assignee = (t.assigneeName || "").trim();
      return (
        String(t.operatorId) === uid ||
        String(t.operatorId) === DEMO_OPERATOR_USER_ID ||
        linked.has(assignee)
      );
    });
  }
  return (tasks || []).filter(
    (t) => String(t.operatorId) === uid || linked.has((t.assigneeName || "").trim())
  );
}

function parseDispatchTime(value) {
  if (value == null || value === "") return 0;
  const text = String(value).trim();
  const ms = Date.parse(text.includes("T") ? text : text.replace(" ", "T"));
  return Number.isFinite(ms) ? ms : 0;
}

/** 取任务「创建/派工时间」用于最近优先排序（不用计划开始时间，避免未来排期排到最前） */
function dispatchTaskRecentTime(task) {
  return parseDispatchTime(task?.createdAt) || Number(task?.dispatchId) || 0;
}

export function sortDispatchTasksRecentFirst(tasks) {
  return [...(tasks || [])].sort((a, b) => {
    const timeDiff = dispatchTaskRecentTime(b) - dispatchTaskRecentTime(a);
    if (timeDiff !== 0) return timeDiff;
    return (Number(b.dispatchId) || 0) - (Number(a.dispatchId) || 0);
  });
}

export function fetchMyDispatchTasks() {
  return fetchDispatchTasks();
}

export function startDispatchTask(dispatchId, task) {
  return updateDispatchTask(dispatchId, {
    dispatchNo: task.dispatchNo,
    taskId: task.taskId,
    stepId: task.stepId,
    stationId: task.stationId,
    assigneeId: task.operatorId,
    status: "RUNNING",
  });
}

export async function fetchTodayFeedback(userName, altUserName, viewAll = false) {
  if (viewAll) {
    const res = await listFeedback({ pageNum: 1, pageSize: 100 }).catch(() => ({ rows: [] }));
    return { rows: res?.rows || res?.data || [] };
  }
  const names = [...new Set([userName, altUserName].filter(Boolean))];
  const results = await Promise.all(
    names.map((name) =>
      listFeedback({ pageNum: 1, pageSize: 50, userName: name }).catch(() => ({ rows: [] }))
    )
  );
  const merged = [];
  const seen = new Set();
  results.forEach((res) => {
    (res?.rows || res?.data || []).forEach((row) => {
      const key = row.feedbackCode || row.recordId;
      if (key && !seen.has(key)) {
        seen.add(key);
        merged.push(row);
      }
    });
  });
  return { rows: merged };
}

export function fetchProcessSop(processId) {
  return listProcesscontent({ processId, pageNum: 1, pageSize: 50 });
}

export function fetchProcessByCode(processCode) {
  return yunshuRequest.get("/mes/pro/process/list", {
    params: { processCode, pageNum: 1, pageSize: 1 },
  });
}

export async function fetchProductBomByProductCode(productCode) {
  if (!productCode) return { rows: [] };
  const itemRes = await listMdItem({ itemCode: productCode, pageNum: 1, pageSize: 1 });
  const items = itemRes?.rows || itemRes?.data || [];
  const item = Array.isArray(items) ? items[0] : null;
  if (!item?.itemId) return { rows: [] };
  return listBom({ itemId: item.itemId, pageNum: 1, pageSize: 50 });
}

export async function fetchWorkOrderBindings(workOrderId, workOrderNo) {
  const snRes = await fetchProductSns();
  const sns = (snRes?.data || []).filter(
    (s) =>
      (workOrderId && String(s.workOrderId) === String(workOrderId)) ||
      (workOrderNo && s.workOrderNo === workOrderNo)
  );
  if (!sns.length) return [];
  const rows = [];
  for (const sn of sns.slice(0, 5)) {
    try {
      const res = await request.get(`/production/binding-ops/material-bindings/${sn.snId}`);
      (res?.data || []).forEach((b) => {
        rows.push({ ...b, snCode: sn.snCode });
      });
    } catch {
      /* skip */
    }
  }
  return rows.sort((a, b) => String(b.bind_time || b.bindTime || "").localeCompare(String(a.bind_time || a.bindTime || "")));
}

export function traceProductSn(code) {
  return request.get("/traceability/product", { params: { code } });
}

export function traceMaterialBatch(batchNo) {
  return request.get(`/traceability/batch/${encodeURIComponent(batchNo)}`);
}

export function bindMaterial(payload) {
  return request.post("/production/binding-ops/material-bindings", payload);
}

export function listMaterialBindings(snId) {
  return request.get(`/production/binding-ops/material-bindings/${snId}`);
}

export function fetchActiveAndonCount() {
  return yunshuRequest.get("/mes/pro/andonrecord/list", {
    params: { status: "ACTIVE", pageNum: 1, pageSize: 200 },
  });
}
