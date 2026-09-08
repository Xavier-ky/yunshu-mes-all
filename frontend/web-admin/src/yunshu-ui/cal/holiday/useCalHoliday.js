import { computed, onMounted, ref } from "vue";
import { listCalendars } from "@/yunshu-ui/api/mes/cal/calendar";
import { addCalholiday, delCalholiday, listCalholiday } from "@/yunshu-ui/api/mes/cal/calholiday";
import {
  addMonths,
  createLatestRequestGuard,
  formatDate,
} from "../calendar/calScheduleModel";
import {
  computeHolidayKpis,
  findEnrichedDay,
  mergeHolidayDays,
  normalizeHolidayOverrides,
} from "./calHolidayModel";

const EMPTY_KPIS = {
  restCount: 0,
  workdayCount: 0,
  explicitHoliday: 0,
  adjustedWorkday: 0,
  defaultSunday: 0,
  festivalCount: 0,
};

const DEFAULT_CALENDAR_TYPE = "ZZ";

export function useCalHoliday() {
  const anchorDate = ref(new Date());
  const days = ref([]);
  const kpis = ref({ ...EMPTY_KPIS });
  const loading = ref(false);
  const error = ref("");
  const drawerOpen = ref(false);
  const drawerLoading = ref(false);
  const drawerError = ref("");
  const selectedDay = ref(null);
  const remarkDraft = ref("");
  const requestGuard = createLatestRequestGuard();
  const mutationGuard = createLatestRequestGuard();

  const periodLabel = computed(() => {
    const date = anchorDate.value instanceof Date ? anchorDate.value : new Date();
    return `${date.getFullYear()}年${date.getMonth() + 1}月`;
  });

  async function refresh() {
    const sequence = requestGuard.begin();
    loading.value = true;
    error.value = "";
    try {
      const date = formatDate(anchorDate.value);
      const [calendarResponse, holidayResponse] = await Promise.all([
        listCalendars({
          queryType: "TYPE",
          calendarType: DEFAULT_CALENDAR_TYPE,
          date,
        }),
        listCalholiday(),
      ]);
      if (!requestGuard.isCurrent(sequence)) return;
      const overrideMap = normalizeHolidayOverrides(holidayResponse);
      const enriched = mergeHolidayDays(calendarResponse, overrideMap);
      days.value = enriched;
      kpis.value = computeHolidayKpis(enriched);
    } catch (cause) {
      if (!requestGuard.isCurrent(sequence)) return;
      days.value = [];
      kpis.value = { ...EMPTY_KPIS };
      error.value = cause?.message || "节假日日历加载失败";
    } finally {
      if (requestGuard.isCurrent(sequence)) loading.value = false;
    }
  }

  function navigate(direction) {
    if (direction === 0) {
      anchorDate.value = new Date();
    } else {
      anchorDate.value = addMonths(anchorDate.value, direction);
    }
    refresh();
  }

  function openDay(value) {
    const theDay = formatDate(value?.theDay || value);
    if (!theDay) return;
    const day = findEnrichedDay(days.value, theDay) || { theDay };
    selectedDay.value = day;
    remarkDraft.value = day.override?.remark || "";
    drawerError.value = "";
    drawerOpen.value = true;
  }

  function closeDrawer() {
    mutationGuard.invalidate();
    drawerOpen.value = false;
    drawerLoading.value = false;
    drawerError.value = "";
  }

  async function applyStatus(holidayType) {
    const day = selectedDay.value;
    if (!day?.theDay) return;
    const sequence = mutationGuard.begin();
    drawerLoading.value = true;
    drawerError.value = "";
    try {
      await addCalholiday({
        theDay: day.theDay,
        holidayType,
        remark: remarkDraft.value.trim(),
        startTime: new Date(day.theDay).setHours(0, 0, 0, 0),
        endTime: new Date(day.theDay).setHours(23, 59, 59, 999),
      });
      await refresh();
      if (!mutationGuard.isCurrent(sequence)) return;
      if (drawerOpen.value) openDay(day.theDay);
    } catch (cause) {
      if (mutationGuard.isCurrent(sequence)) {
        drawerError.value = cause?.message || "节假日设置失败";
      }
      throw cause;
    } finally {
      if (mutationGuard.isCurrent(sequence)) drawerLoading.value = false;
    }
  }

  async function resetToDefault() {
    const day = selectedDay.value;
    const holidayId = day?.override?.holidayId;
    if (!holidayId) return;
    const sequence = mutationGuard.begin();
    drawerLoading.value = true;
    drawerError.value = "";
    try {
      await delCalholiday(holidayId);
      await refresh();
      if (!mutationGuard.isCurrent(sequence)) return;
      if (drawerOpen.value) openDay(day.theDay);
    } catch (cause) {
      if (mutationGuard.isCurrent(sequence)) {
        drawerError.value = cause?.message || "恢复默认失败";
      }
      throw cause;
    } finally {
      if (mutationGuard.isCurrent(sequence)) drawerLoading.value = false;
    }
  }

  onMounted(refresh);

  return {
    anchorDate,
    days,
    kpis,
    loading,
    error,
    periodLabel,
    drawerOpen,
    drawerLoading,
    drawerError,
    selectedDay,
    remarkDraft,
    refresh,
    navigate,
    openDay,
    closeDrawer,
    applyStatus,
    resetToDefault,
  };
}
