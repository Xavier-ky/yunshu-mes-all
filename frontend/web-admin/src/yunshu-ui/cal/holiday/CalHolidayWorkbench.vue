<template>
  <div class="cal-workbench cal-holiday-workbench">
    <CalHolidayToolbar
      :period-label="periodLabel"
      :loading="loading"
      @navigate="navigate"
      @refresh="refresh"
      @navigate-tab="$emit('navigate-tab', $event)"
    />

    <div class="cal-workbench-kpis cal-holiday-kpis" aria-label="节假日关键指标">
      <div><span>休息日</span><b>{{ kpis.restCount }}</b><small>天</small></div>
      <div><span>工作日</span><b>{{ kpis.workdayCount }}</b><small>天</small></div>
      <div class="is-accent"><span>法定/手动假日</span><b>{{ kpis.explicitHoliday }}</b><small>天</small></div>
      <div><span>调休上班</span><b>{{ kpis.adjustedWorkday }}</b><small>天</small></div>
      <div><span>默认周日休息</span><b>{{ kpis.defaultSunday }}</b><small>天</small></div>
      <div class="is-festival-kpi"><span>本月节日</span><b>{{ kpis.festivalCount }}</b><small>天</small></div>
    </div>

    <div class="cal-holiday-legend" aria-label="节假日图例">
      <span
        v-for="item in legendItems"
        :key="item.key"
        class="cal-holiday-legend__item"
      >
        <i :class="item.className" aria-hidden="true" />
        {{ item.label }}
      </span>
    </div>

    <CalHolidayMonthGrid
      :days="days"
      :anchor-date="anchorDate"
      :loading="loading"
      :error="error"
      @open="openDay"
      @retry="refresh"
    />

    <CalHolidayDayDrawer
      v-model="drawerOpen"
      :day="selectedDay"
      :remark="remarkDraft"
      :loading="drawerLoading"
      :error="drawerError"
      @update:remark="remarkDraft = $event"
      @close="closeDrawer"
      @apply="applyStatus"
      @reset="resetToDefault"
      @retry="() => selectedDay && openDay(selectedDay)"
    />
  </div>
</template>

<script setup>
import { ElMessage } from "element-plus";
import CalHolidayToolbar from "./CalHolidayToolbar.vue";
import CalHolidayMonthGrid from "./CalHolidayMonthGrid.vue";
import CalHolidayDayDrawer from "./CalHolidayDayDrawer.vue";
import { HOLIDAY_LEGEND } from "./calHolidayModel";
import { useCalHoliday } from "./useCalHoliday";

defineEmits(["navigate-tab"]);

const legendItems = HOLIDAY_LEGEND;

const {
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
  applyStatus: applyStatusRaw,
  resetToDefault: resetToDefaultRaw,
} = useCalHoliday();

async function applyStatus(holidayType) {
  try {
    await applyStatusRaw(holidayType);
    ElMessage.success("节假日设置已保存");
  } catch {
    ElMessage.error("节假日设置失败");
  }
}

async function resetToDefault() {
  try {
    await resetToDefaultRaw();
    ElMessage.success("已恢复系统默认");
  } catch {
    ElMessage.error("恢复默认失败");
  }
}
</script>
