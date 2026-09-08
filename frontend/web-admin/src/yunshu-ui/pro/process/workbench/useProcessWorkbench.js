import { getCurrentInstance, onMounted, ref } from "vue";
import { ElMessageBox } from "element-plus";
import { listProcess, listAllProcess, getProcess, addProcess, updateProcess, delProcess } from "@/yunshu-ui/api/mes/pro/process";
import { genCode } from "@/yunshu-ui/api/system/autocode/rule";
import {
  computeProcessKpis,
  createEmptyProcessForm,
  normalizeProcessRow,
  pickDefaultProcess,
  sortProcessesAsc,
  unwrapRows,
  unwrapTotal,
} from "./processModel.js";

const EMPTY_KPIS = {
  totalCount: 0,
  enabledCount: 0,
  disabledCount: 0,
  filteredCount: 0,
};

export function useProcessWorkbench() {
  const instance = getCurrentInstance();
  const proxy = instance?.proxy;

  const processes = ref([]);
  const allProcesses = ref([]);
  const total = ref(0);
  const kpis = ref({ ...EMPTY_KPIS });
  const loading = ref(false);
  const saving = ref(false);
  const selectedProcessId = ref(null);
  const editing = ref(false);
  const isNew = ref(false);
  const form = ref(createEmptyProcessForm());
  const autoGenFlag = ref(false);
  const queryParams = ref({
    pageNum: 1,
    pageSize: 10,
    processCode: "",
    processName: "",
    enableFlag: "",
  });

  async function refreshKpis() {
    try {
      const allResponse = await listAllProcess();
      allProcesses.value = unwrapRows(allResponse);
      kpis.value = computeProcessKpis(allProcesses.value, total.value);
    } catch {
      kpis.value = computeProcessKpis(allProcesses.value, total.value);
    }
  }

  async function refresh() {
    loading.value = true;
    try {
      const params = {
        ...queryParams.value,
        processCode: queryParams.value.processCode || undefined,
        processName: queryParams.value.processName || undefined,
        enableFlag: queryParams.value.enableFlag || undefined,
      };
      const response = await listProcess(params);
      processes.value = sortProcessesAsc(unwrapRows(response).map(normalizeProcessRow));
      total.value = unwrapTotal(response, processes.value.length);
      await refreshKpis();
      if (isNew.value) {
        // keep create form
      } else if (processes.value.length) {
        const currentId = selectedProcessId.value;
        const inList = currentId != null && processes.value.some((p) => String(p.processId) === String(currentId));
        if (!inList) {
          await selectProcess(pickDefaultProcess(processes.value));
        } else if (!form.value.processId) {
          await loadProcess(currentId);
        }
      } else {
        resetDetail();
      }
    } catch (cause) {
      processes.value = [];
      total.value = 0;
      proxy?.$modal?.msgError?.(cause?.message || "工序列表加载失败");
    } finally {
      loading.value = false;
    }
  }

  function resetDetail() {
    selectedProcessId.value = null;
    editing.value = false;
    isNew.value = false;
    form.value = createEmptyProcessForm();
    autoGenFlag.value = false;
  }

  async function loadProcess(processId) {
    if (!processId) return;
    const response = await getProcess(processId);
    form.value = normalizeProcessRow(response?.data || response);
    selectedProcessId.value = form.value.processId;
    isNew.value = false;
  }

  async function selectProcess(row) {
    if (!row?.processId) return;
    editing.value = false;
    isNew.value = false;
    autoGenFlag.value = false;
    await loadProcess(row.processId);
  }

  function search() {
    queryParams.value.pageNum = 1;
    refresh();
  }

  function resetQuery() {
    queryParams.value = {
      pageNum: 1,
      pageSize: queryParams.value.pageSize,
      processCode: "",
      processName: "",
      enableFlag: "",
    };
    refresh();
  }

  function setQueryField(key, value) {
    queryParams.value[key] = value;
  }

  function setPage(page) {
    queryParams.value.pageNum = page;
    refresh();
  }

  function setPageSize(size) {
    queryParams.value.pageSize = size;
    queryParams.value.pageNum = 1;
    refresh();
  }

  function startCreate() {
    resetDetail();
    isNew.value = true;
    editing.value = true;
    form.value = createEmptyProcessForm();
  }

  function startEdit() {
    if (!form.value.processId && !isNew.value) return;
    editing.value = true;
  }

  function cancelEdit() {
    if (isNew.value) {
      resetDetail();
      if (processes.value.length) selectProcess(pickDefaultProcess(processes.value));
      return;
    }
    editing.value = false;
    autoGenFlag.value = false;
    loadProcess(selectedProcessId.value);
  }

  async function saveProcess() {
    saving.value = true;
    try {
      if (form.value.processId != null) {
        await updateProcess(form.value);
        proxy?.$modal?.msgSuccess?.("修改成功");
      } else {
        const response = await addProcess(form.value);
        const newId = response?.data?.processId ?? response?.processId;
        if (newId) form.value.processId = newId;
        proxy?.$modal?.msgSuccess?.("新增成功");
        isNew.value = false;
      }
      editing.value = false;
      autoGenFlag.value = false;
      selectedProcessId.value = form.value.processId;
      await refresh();
      if (form.value.processId) await loadProcess(form.value.processId);
    } catch (cause) {
      proxy?.$modal?.msgError?.(cause?.message || "保存失败");
    } finally {
      saving.value = false;
    }
  }

  async function removeProcess() {
    const id = form.value.processId;
    if (!id) return;
    await ElMessageBox.confirm("是否确认删除该生产工序？", "提示", { type: "warning" });
    await delProcess(id);
    proxy?.$modal?.msgSuccess?.("删除成功");
    resetDetail();
    await refresh();
  }

  function exportProcesses() {
    proxy?.download?.("mes/pro/process/export", { ...queryParams.value }, `process_${Date.now()}.xlsx`);
  }

  async function toggleAutoGen(flag) {
    autoGenFlag.value = flag;
    if (flag) {
      const code = await genCode("PROCESS_CODE");
      form.value.processCode = code;
    } else {
      form.value.processCode = "";
    }
  }

  onMounted(() => {
    refresh();
  });

  return {
    processes,
    total,
    kpis,
    loading,
    saving,
    selectedProcessId,
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
    selectProcess,
    startCreate,
    startEdit,
    cancelEdit,
    saveProcess,
    removeProcess,
    exportProcesses,
    toggleAutoGen,
  };
}
