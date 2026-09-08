import { getCurrentInstance, onMounted, ref } from "vue";
import { ElMessage, ElMessageBox } from "element-plus";
import {
  listCalplan,
  getCalplan,
  addCalplan,
  updateCalplan,
  delCalplan,
} from "@/yunshu-ui/api/mes/cal/calplan";
import { genCode } from "@/yunshu-ui/api/system/autocode/rule";
import { createLatestRequestGuard } from "../calendar/calScheduleModel.js";
import {
  computePlanKpis,
  createEmptyPlanForm,
  isPlanEditable,
  normalizePlanRow,
  unwrapRows,
  unwrapTotal,
} from "./calPlanModel.js";

const EMPTY_KPIS = {
  totalCount: 0,
  prepareCount: 0,
  confirmedCount: 0,
  monthActive: 0,
  shiftTwoCount: 0,
  singleCount: 0,
  filteredCount: 0,
};

export function useCalPlan() {
  const instance = getCurrentInstance();
  const proxy = instance?.proxy;

  const plans = ref([]);
  const allPlans = ref([]);
  const total = ref(0);
  const kpis = ref({ ...EMPTY_KPIS });
  const loading = ref(false);
  const saving = ref(false);
  const error = ref("");
  const selectedPlanId = ref(null);
  const editing = ref(false);
  const isNew = ref(false);
  const form = ref(createEmptyPlanForm());
  const autoGenFlag = ref(false);
  const queryParams = ref({
    pageNum: 1,
    pageSize: 10,
    planCode: "",
    planName: "",
    calendarType: "",
    startDate: "",
    endDate: "",
  });
  const requestGuard = createLatestRequestGuard();

  async function refreshKpis() {
    try {
      const allResponse = await listCalplan({ pageNum: 1, pageSize: 2000 });
      allPlans.value = unwrapRows(allResponse).map(normalizePlanRow);
      kpis.value = computePlanKpis(allPlans.value, total.value);
    } catch {
      kpis.value = computePlanKpis(allPlans.value, total.value);
    }
  }

  async function refresh() {
    const sequence = requestGuard.begin();
    loading.value = true;
    error.value = "";
    try {
      const params = {
        ...queryParams.value,
        planCode: queryParams.value.planCode || undefined,
        planName: queryParams.value.planName || undefined,
        calendarType: queryParams.value.calendarType || undefined,
        startDate: queryParams.value.startDate || undefined,
        endDate: queryParams.value.endDate || undefined,
      };
      const response = await listCalplan(params);
      if (!requestGuard.isCurrent(sequence)) return;
      plans.value = unwrapRows(response).map(normalizePlanRow);
      total.value = unwrapTotal(response, plans.value.length);
      await refreshKpis();
      if (isNew.value) {
        // keep create form while list refreshes
      } else if (plans.value.length) {
        const currentId = selectedPlanId.value;
        const inList = currentId != null
          && plans.value.some((p) => String(p.planId) === String(currentId));
        if (!inList) {
          await selectPlan(plans.value[0]);
        } else if (!form.value.planId) {
          await loadPlan(currentId);
        }
      } else {
        resetDetail();
      }
    } catch (cause) {
      if (!requestGuard.isCurrent(sequence)) return;
      error.value = cause?.message || "排班计划加载失败";
      plans.value = [];
      total.value = 0;
    } finally {
      if (requestGuard.isCurrent(sequence)) loading.value = false;
    }
  }

  function resetDetail() {
    selectedPlanId.value = null;
    editing.value = false;
    isNew.value = false;
    form.value = createEmptyPlanForm();
    autoGenFlag.value = false;
  }

  async function loadPlan(planId) {
    if (!planId) return;
    const response = await getCalplan(planId);
    form.value = normalizePlanRow(response?.data || response);
    selectedPlanId.value = form.value.planId;
    isNew.value = false;
    if (!isPlanEditable(form.value)) editing.value = false;
  }

  async function selectPlan(plan) {
    if (!plan?.planId) return;
    selectedPlanId.value = plan.planId;
    isNew.value = false;
    editing.value = false;
    try {
      await loadPlan(plan.planId);
    } catch (cause) {
      ElMessage.error(cause?.message || "计划详情加载失败");
    }
  }

  function startCreate() {
    resetDetail();
    isNew.value = true;
    editing.value = true;
    form.value = createEmptyPlanForm();
  }

  function startEdit() {
    if (!isPlanEditable(form.value)) return;
    editing.value = true;
  }

  function cancelEdit() {
    if (isNew.value) {
      resetDetail();
      if (plans.value.length) selectPlan(plans.value[0]);
      return;
    }
    editing.value = false;
    if (selectedPlanId.value) loadPlan(selectedPlanId.value);
  }

  async function savePlan() {
    if (!form.value.planCode?.trim() || !form.value.planName?.trim()) {
      ElMessage.warning("请填写计划编号和名称");
      return;
    }
    if (!form.value.startDate || !form.value.endDate || !form.value.calendarType) {
      ElMessage.warning("请完善日期和班组类型");
      return;
    }
    saving.value = true;
    try {
      if (form.value.planId) {
        await updateCalplan(form.value);
        ElMessage.success("计划已更新");
      } else {
        const response = await addCalplan(form.value);
        const newId = response?.data ?? response;
        if (newId) form.value.planId = newId;
        ElMessage.success("计划已创建");
      }
      isNew.value = false;
      selectedPlanId.value = form.value.planId;
      editing.value = isPlanEditable(form.value) ? editing.value : false;
      await refresh();
      if (selectedPlanId.value) await loadPlan(selectedPlanId.value);
    } catch (cause) {
      ElMessage.error(cause?.message || "保存失败");
      throw cause;
    } finally {
      saving.value = false;
    }
  }

  async function finishPlan() {
    if (!form.value.planId || !isPlanEditable(form.value)) return;
    await ElMessageBox.confirm("是否完成计划编制？完成后将不能更改。", "提示", { type: "warning" });
    saving.value = true;
    const previous = form.value.status;
    form.value.status = "CONFIRMED";
    try {
      await updateCalplan(form.value);
      ElMessage.success("计划已完成");
      editing.value = false;
      await refresh();
      await loadPlan(form.value.planId);
    } catch (cause) {
      form.value.status = previous;
      ElMessage.error(cause?.message || "完成计划失败");
      throw cause;
    } finally {
      saving.value = false;
    }
  }

  async function removePlan(planId) {
    const id = planId || selectedPlanId.value;
    if (!id) return;
    await ElMessageBox.confirm("是否确认删除该排班计划？", "提示", { type: "warning" });
    await delCalplan(id);
    ElMessage.success("删除成功");
    if (selectedPlanId.value === id) resetDetail();
    await refresh();
  }

  async function toggleAutoGen(value) {
    autoGenFlag.value = value;
    if (value) {
      try {
        form.value.planCode = await genCode("CAL_PLAN_CODE");
      } catch {
        ElMessage.error("自动生成编号失败");
        autoGenFlag.value = false;
      }
    } else {
      form.value.planCode = "";
    }
  }

  function setQueryField(field, value) {
    queryParams.value = { ...queryParams.value, [field]: value };
  }

  function search() {
    queryParams.value.pageNum = 1;
    refresh();
  }

  function resetQuery() {
    queryParams.value = {
      pageNum: 1,
      pageSize: queryParams.value.pageSize,
      planCode: "",
      planName: "",
      calendarType: "",
      startDate: "",
      endDate: "",
    };
    refresh();
  }

  function setPage(page) {
    queryParams.value.pageNum = page;
    refresh();
  }

  function setPageSize(limit) {
    queryParams.value.pageSize = limit;
    queryParams.value.pageNum = 1;
    refresh();
  }

  function exportPlans() {
    proxy?.download?.("cal/calplan/export", { ...queryParams.value }, `calplan_${Date.now()}.xlsx`);
  }

  onMounted(refresh);

  return {
    plans,
    total,
    kpis,
    loading,
    saving,
    error,
    selectedPlanId,
    editing,
    isNew,
    form,
    autoGenFlag,
    queryParams,
    refresh,
    search,
    resetQuery,
    setQueryField,
    setPage,
    setPageSize,
    selectPlan,
    startCreate,
    startEdit,
    cancelEdit,
    savePlan,
    finishPlan,
    removePlan,
    toggleAutoGen,
    exportPlans,
    isPlanEditable,
  };
}
