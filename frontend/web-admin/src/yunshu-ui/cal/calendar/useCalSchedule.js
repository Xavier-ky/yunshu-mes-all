import { computed, onMounted, ref, unref, watch } from "vue";
import {
  getCalendarDay,
  getCalendarSummary,
  getCalendarWeek,
  listCalendars,
} from "@/yunshu-ui/api/mes/cal/calendar";
import { listAllTeam } from "@/yunshu-ui/api/mes/cal/team";
import { addCalholiday } from "@/yunshu-ui/api/mes/cal/calholiday";
import {
  addDays,
  addMonths,
  createLatestRequestGuard,
  formatDate,
  getSelectionGuidance,
  isTeamEnabled,
  normalizeCalendarDays,
  normalizeDay,
  normalizeSummary,
  normalizeWeek,
  pickDefaultCalendarType,
  pickDefaultTeam,
  shouldReloadCalendarType,
} from "./calScheduleModel";

const EMPTY_SUMMARY = {
  todayOnDutyCount: 0,
  coverageRate: 0,
  unscheduledWorkdays: 0,
  conflictCount: 0,
  holidayCount: 0,
  workdayCount: 0,
};

export function useCalSchedule(calendarTypes) {
  const anchorDate = ref(new Date());
  const displayMode = ref("month");
  const queryType = ref("TYPE");
  const selectedType = ref("ZZ");
  const selectedTeamId = ref(null);
  const selectedUser = ref(null);
  const teams = ref([]);
  const days = ref([]);
  const week = ref(normalizeWeek(null));
  const summary = ref({ ...EMPTY_SUMMARY });
  const loading = ref(false);
  const error = ref("");
  const teamLoadError = ref("");
  const drawerOpen = ref(false);
  const drawerLoading = ref(false);
  const drawerError = ref("");
  const selectedDay = ref(normalizeDay(null));
  const requestGuard = createLatestRequestGuard();
  const drawerRequestGuard = createLatestRequestGuard();
  let hasMounted = false;

  const selectionGuidance = computed(
    () => getSelectionGuidance(queryType.value, {
      selectedTeamId: selectedTeamId.value,
      selectedUser: selectedUser.value,
      teams: teams.value,
      teamLoadError: teamLoadError.value,
    }),
  );
  const needsUserSelection = computed(() => Boolean(selectionGuidance.value));
  const overviewError = computed(
    () => queryType.value === "TEAM" && teamLoadError.value
      ? teamLoadError.value
      : error.value,
  );
  const retryOverview = computed(
    () => queryType.value === "TEAM" && teamLoadError.value
      ? retryTeams
      : refresh,
  );

  const queryParams = computed(() => {
    const params = {
      queryType: queryType.value,
      date: formatDate(anchorDate.value),
    };
    if (queryType.value === "TYPE") params.calendarType = selectedType.value;
    if (queryType.value === "TEAM" && selectedTeamId.value) params.teamId = selectedTeamId.value;
    if (queryType.value === "USER" && selectedUser.value?.userId) {
      params.userId = selectedUser.value.userId;
    }
    return params;
  });

  function syncDefaultType(options) {
    const values = (options || []).map((item) => String(item?.value ?? item ?? ""));
    const previousType = selectedType.value;
    if (!selectedType.value || (values.length && !values.includes(selectedType.value))) {
      selectedType.value = pickDefaultCalendarType(options);
    }
    return shouldReloadCalendarType(previousType, selectedType.value, queryType.value);
  }

  async function loadTeams(refreshWhenChanged = true) {
    const previousTeamId = selectedTeamId.value;
    try {
      const response = await listAllTeam();
      teams.value = Array.isArray(response?.data)
        ? response.data
        : Array.isArray(response?.rows)
          ? response.rows
          : [];
      teamLoadError.value = "";
      if (!teams.value.some(
        (team) => String(team.teamId) === String(selectedTeamId.value) && isTeamEnabled(team),
      )) {
        selectedTeamId.value = pickDefaultTeam(teams.value)?.teamId ?? null;
      }
    } catch (cause) {
      teamLoadError.value = cause?.message || "班组列表加载失败";
    }
    if (
      refreshWhenChanged
      && hasMounted
      && queryType.value === "TEAM"
      && String(previousTeamId ?? "") !== String(selectedTeamId.value ?? "")
    ) {
      refresh();
    }
  }

  async function retryTeams() {
    await loadTeams(false);
    await refresh();
  }

  async function refresh() {
    const sequence = requestGuard.begin();
    const teamListFailed = queryType.value === "TEAM" && Boolean(teamLoadError.value);
    if (teamListFailed || selectionGuidance.value) {
      days.value = [];
      week.value = normalizeWeek(null);
      summary.value = { ...EMPTY_SUMMARY };
      loading.value = false;
      error.value = "";
      return;
    }

    loading.value = true;
    error.value = "";
    try {
      const params = queryParams.value;
      const [scheduleResponse, summaryResponse] = await Promise.all([
        displayMode.value === "month"
          ? listCalendars(params)
          : getCalendarWeek(params),
        getCalendarSummary(params),
      ]);
      if (!requestGuard.isCurrent(sequence)) return;
      if (displayMode.value === "month") {
        days.value = normalizeCalendarDays(scheduleResponse);
      } else {
        week.value = normalizeWeek(scheduleResponse);
      }
      summary.value = normalizeSummary(summaryResponse);
    } catch (cause) {
      if (!requestGuard.isCurrent(sequence)) return;
      error.value = cause?.message || "排班日历加载失败";
    } finally {
      if (requestGuard.isCurrent(sequence)) loading.value = false;
    }
  }

  function navigate(direction) {
    if (direction === 0) {
      anchorDate.value = new Date();
    } else if (displayMode.value === "month") {
      anchorDate.value = addMonths(anchorDate.value, direction);
    } else {
      anchorDate.value = addDays(anchorDate.value, direction * 7);
    }
    refresh();
  }

  function setDisplayMode(mode) {
    if (displayMode.value === mode) return;
    displayMode.value = mode;
    refresh();
  }

  function setQueryType(type) {
    queryType.value = type;
    if (type === "TYPE") syncDefaultType(unref(calendarTypes));
    if (type === "TEAM" && !selectedTeamId.value) {
      selectedTeamId.value = pickDefaultTeam(teams.value)?.teamId ?? null;
    }
    refresh();
  }

  function setType(value) {
    selectedType.value = value;
    refresh();
  }

  function setTeam(value) {
    selectedTeamId.value = value;
    refresh();
  }

  function setUser(user) {
    selectedUser.value = user || null;
    refresh();
  }

  async function openDay(value) {
    const sequence = drawerRequestGuard.begin();
    const date = formatDate(value?.theDay || value);
    if (!date) {
      drawerLoading.value = false;
      return;
    }
    drawerOpen.value = true;
    drawerLoading.value = true;
    drawerError.value = "";
    selectedDay.value = normalizeDay({ theDay: date });
    try {
      const response = await getCalendarDay({ ...queryParams.value, date });
      if (!drawerRequestGuard.isCurrent(sequence)) return;
      selectedDay.value = normalizeDay(response);
    } catch (cause) {
      if (!drawerRequestGuard.isCurrent(sequence)) return;
      selectedDay.value = normalizeDay({ theDay: date });
      drawerError.value = cause?.message || "日期详情加载失败";
    } finally {
      if (drawerRequestGuard.isCurrent(sequence)) drawerLoading.value = false;
    }
  }

  function closeDrawer() {
    drawerRequestGuard.invalidate();
    drawerOpen.value = false;
    drawerLoading.value = false;
  }

  async function setDayStatus(holidayType) {
    const date = selectedDay.value.theDay;
    if (!date) return;
    const sequence = drawerRequestGuard.begin();
    drawerLoading.value = true;
    drawerError.value = "";
    try {
      await addCalholiday({ theDay: date, holidayType });
      await refresh();
      if (drawerRequestGuard.isCurrent(sequence) && drawerOpen.value) await openDay(date);
    } catch (cause) {
      if (drawerRequestGuard.isCurrent(sequence)) {
        drawerError.value = cause?.message || "日期状态设置失败";
      }
      throw cause;
    } finally {
      if (drawerRequestGuard.isCurrent(sequence)) drawerLoading.value = false;
    }
  }

  watch(
    () => unref(calendarTypes),
    (options) => {
      if (syncDefaultType(options) && hasMounted) refresh();
    },
    { deep: true, immediate: true },
  );

  onMounted(async () => {
    hasMounted = true;
    const initialFetch = refresh();
    await loadTeams();
    await initialFetch;
  });

  return {
    anchorDate,
    displayMode,
    queryType,
    selectedType,
    selectedTeamId,
    selectedUser,
    teams,
    days,
    week,
    summary,
    loading,
    error,
    teamLoadError,
    overviewError,
    drawerOpen,
    drawerLoading,
    drawerError,
    selectedDay,
    selectionGuidance,
    needsUserSelection,
    refresh,
    retryOverview,
    retryTeams,
    navigate,
    setDisplayMode,
    setQueryType,
    setType,
    setTeam,
    setUser,
    openDay,
    closeDrawer,
    setDayStatus,
  };
}
