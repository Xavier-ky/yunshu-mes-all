import { getCurrentInstance, onMounted, ref } from "vue";
import { ElMessage, ElMessageBox } from "element-plus";
import { listTeam, listAllTeam, getTeam, addTeam, updateTeam, delTeam } from "@/yunshu-ui/api/mes/cal/team";
import { listTeammember } from "@/yunshu-ui/api/mes/cal/teammember";
import { genCode } from "@/yunshu-ui/api/system/autocode/rule";
import { createLatestRequestGuard } from "../calendar/calScheduleModel.js";
import {
  computeTeamKpis,
  createEmptyTeamForm,
  normalizeTeamRow,
  unwrapRows,
  unwrapTotal,
} from "./calTeamModel.js";

const EMPTY_KPIS = {
  totalCount: 0,
  enabledCount: 0,
  disabledCount: 0,
  manufacturingCount: 0,
  filteredCount: 0,
  memberTotal: 0,
};

export function useCalTeam() {
  const instance = getCurrentInstance();
  const proxy = instance?.proxy;

  const teams = ref([]);
  const allTeams = ref([]);
  const total = ref(0);
  const kpis = ref({ ...EMPTY_KPIS });
  const loading = ref(false);
  const saving = ref(false);
  const error = ref("");
  const selectedTeamId = ref(null);
  const editing = ref(false);
  const isNew = ref(false);
  const form = ref(createEmptyTeamForm());
  const autoGenFlag = ref(false);
  const queryParams = ref({
    pageNum: 1,
    pageSize: 10,
    teamCode: "",
    teamName: "",
    calendarType: "",
  });
  const requestGuard = createLatestRequestGuard();

  async function refreshKpis() {
    try {
      const [allResponse, memberResponse] = await Promise.all([
        listAllTeam(),
        listTeammember({ pageNum: 1, pageSize: 1 }),
      ]);
      allTeams.value = unwrapRows(allResponse);
      const memberTotal = unwrapTotal(memberResponse, 0);
      kpis.value = computeTeamKpis(allTeams.value, total.value, memberTotal);
    } catch {
      kpis.value = computeTeamKpis(allTeams.value, total.value, 0);
    }
  }

  async function refresh() {
    const sequence = requestGuard.begin();
    loading.value = true;
    error.value = "";
    try {
      const params = {
        ...queryParams.value,
        teamCode: queryParams.value.teamCode || undefined,
        teamName: queryParams.value.teamName || undefined,
        calendarType: queryParams.value.calendarType || undefined,
      };
      const response = await listTeam(params);
      if (!requestGuard.isCurrent(sequence)) return;
      teams.value = unwrapRows(response).map(normalizeTeamRow);
      total.value = unwrapTotal(response, teams.value.length);
      await refreshKpis();
      if (isNew.value) {
        // keep create form while list refreshes
      } else if (teams.value.length) {
        const currentId = selectedTeamId.value;
        const inList = currentId != null
          && teams.value.some((t) => String(t.teamId) === String(currentId));
        if (!inList) {
          await selectTeam(teams.value[0]);
        } else if (!form.value.teamId) {
          await loadTeam(currentId);
        }
      } else {
        resetDetail();
      }
    } catch (cause) {
      if (!requestGuard.isCurrent(sequence)) return;
      error.value = cause?.message || "班组列表加载失败";
      teams.value = [];
      total.value = 0;
    } finally {
      if (requestGuard.isCurrent(sequence)) loading.value = false;
    }
  }

  function resetDetail() {
    selectedTeamId.value = null;
    editing.value = false;
    isNew.value = false;
    form.value = createEmptyTeamForm();
    autoGenFlag.value = false;
  }

  async function loadTeam(teamId) {
    if (!teamId) return;
    const response = await getTeam(teamId);
    form.value = normalizeTeamRow(response?.data || response);
    selectedTeamId.value = form.value.teamId;
    isNew.value = false;
  }

  async function selectTeam(team) {
    if (!team?.teamId) return;
    selectedTeamId.value = team.teamId;
    editing.value = false;
    isNew.value = false;
    try {
      await loadTeam(team.teamId);
    } catch (cause) {
      ElMessage.error(cause?.message || "班组详情加载失败");
    }
  }

  function startCreate() {
    resetDetail();
    isNew.value = true;
    editing.value = true;
    form.value = createEmptyTeamForm();
  }

  function startEdit() {
    if (!selectedTeamId.value && !isNew.value) return;
    editing.value = true;
  }

  function cancelEdit() {
    if (isNew.value) {
      resetDetail();
      if (teams.value.length) selectTeam(teams.value[0]);
      return;
    }
    editing.value = false;
    if (selectedTeamId.value) loadTeam(selectedTeamId.value);
  }

  async function saveTeam() {
    if (!form.value.teamCode?.trim() || !form.value.teamName?.trim() || !form.value.calendarType) {
      ElMessage.warning("请填写班组编号、名称和类型");
      return;
    }
    saving.value = true;
    try {
      if (form.value.teamId) {
        await updateTeam(form.value);
        ElMessage.success("班组已更新");
      } else {
        const response = await addTeam(form.value);
        const newId = response?.data ?? response;
        if (newId) form.value.teamId = newId;
        ElMessage.success("班组已创建");
      }
      editing.value = false;
      isNew.value = false;
      selectedTeamId.value = form.value.teamId;
      await refresh();
      if (selectedTeamId.value) await loadTeam(selectedTeamId.value);
    } catch (cause) {
      ElMessage.error(cause?.message || "保存失败");
      throw cause;
    } finally {
      saving.value = false;
    }
  }

  async function removeTeam(teamId) {
    const id = teamId || selectedTeamId.value;
    if (!id) return;
    await ElMessageBox.confirm("是否确认删除该班组？", "提示", { type: "warning" });
    await delTeam(id);
    ElMessage.success("删除成功");
    if (selectedTeamId.value === id) resetDetail();
    await refresh();
  }

  async function toggleAutoGen(value) {
    autoGenFlag.value = value;
    if (value) {
      try {
        form.value.teamCode = await genCode("CAL_TEAM_CODE");
      } catch {
        ElMessage.error("自动生成编号失败");
        autoGenFlag.value = false;
      }
    } else {
      form.value.teamCode = "";
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
      teamCode: "",
      teamName: "",
      calendarType: "",
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

  function exportTeams() {
    proxy?.download?.("cal/team/export", { ...queryParams.value }, `team_${Date.now()}.xlsx`);
  }

  onMounted(refresh);

  return {
    teams,
    total,
    kpis,
    loading,
    saving,
    error,
    selectedTeamId,
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
    selectTeam,
    startCreate,
    startEdit,
    cancelEdit,
    saveTeam,
    removeTeam,
    toggleAutoGen,
    exportTeams,
  };
}
