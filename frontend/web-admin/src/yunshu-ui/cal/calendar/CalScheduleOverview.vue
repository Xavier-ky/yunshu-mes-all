<template>
  <div class="cal-workbench">
    <CalScheduleToolbar
      :anchor-date="anchorDate"
      :display-mode="displayMode"
      :query-type="queryType"
      :calendar-types="calendarTypes"
      :teams="teams"
      :selected-type="selectedType"
      :selected-team-id="selectedTeamId"
      :selected-user="selectedUser"
      :week-start="week.startDate"
      :week-end="week.endDate"
      :loading="loading"
      @navigate="navigate"
      @display-mode="setDisplayMode"
      @query-type="setQueryType"
      @type-change="setType"
      @team-change="setTeam"
      @choose-user="chooseUser"
      @refresh="refresh"
      @navigate-tab="$emit('navigate-tab', $event)"
    />

    <div class="cal-workbench-kpis" aria-label="排班关键指标">
      <div><span>今日在岗</span><b>{{ summary.todayOnDutyCount }}</b><small>人</small></div>
      <div><span>排班覆盖率</span><b>{{ summary.coverageRate }}</b><small>%</small></div>
      <div :class="{ 'is-risk': summary.unscheduledWorkdays }">
        <span>未排工作日</span><b>{{ summary.unscheduledWorkdays }}</b><small>天</small>
      </div>
      <div :class="{ 'is-danger': summary.conflictCount }">
        <span>排班冲突</span><b>{{ summary.conflictCount }}</b><small>项</small>
      </div>
      <div><span>休息日</span><b>{{ summary.holidayCount }}</b><small>天</small></div>
      <div><span>工作日</span><b>{{ summary.workdayCount }}</b><small>天</small></div>
    </div>

    <CalMonthGrid
      v-if="displayMode === 'month'"
      :days="days"
      :anchor-date="anchorDate"
      :loading="loading"
      :error="overviewError"
      :guidance="selectionGuidance"
      :show-user-action="queryType === 'USER'"
      @open="openDay"
      @retry="retryOverview"
      @choose-user="chooseUser"
    />
    <CalWeekGrid
      v-else
      :week="week"
      :anchor-date="anchorDate"
      :loading="loading"
      :error="overviewError"
      :guidance="selectionGuidance"
      :show-user-action="queryType === 'USER'"
      @open="openDay"
      @retry="retryOverview"
      @choose-user="chooseUser"
    />

    <UserSingleSelect ref="userSelect" @on-selected="setUser" />
    <CalDayDrawer
      v-model="drawerOpen"
      :day="selectedDay"
      :loading="drawerLoading"
      :error="drawerError"
      @close="closeDrawer"
      @navigate-tab="$emit('navigate-tab', $event)"
      @set-status="updateDayStatus"
      @retry="openDay"
    />
  </div>
</template>

<script setup>
import { ref, toRef } from "vue";
import { ElMessage } from "element-plus";
import UserSingleSelect from "@/yunshu-ui/components/userSelect/single.vue";
import CalScheduleToolbar from "./CalScheduleToolbar.vue";
import CalMonthGrid from "./CalMonthGrid.vue";
import CalWeekGrid from "./CalWeekGrid.vue";
import CalDayDrawer from "./CalDayDrawer.vue";
import { useCalSchedule } from "./useCalSchedule";

const props = defineProps({
  calendarTypes: { type: Array, default: () => [] },
});

defineEmits(["navigate-tab"]);

const userSelect = ref(null);
const {
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
  overviewError,
  drawerOpen,
  drawerLoading,
  drawerError,
  selectedDay,
  selectionGuidance,
  refresh,
  retryOverview,
  navigate,
  setDisplayMode,
  setQueryType,
  setType,
  setTeam,
  setUser,
  openDay,
  closeDrawer,
  setDayStatus,
} = useCalSchedule(toRef(props, "calendarTypes"));

function chooseUser() {
  if (userSelect.value) userSelect.value.showFlag = true;
}

async function updateDayStatus(holidayType) {
  try {
    await setDayStatus(holidayType);
    ElMessage.success(holidayType === "HOLIDAY" ? "已设为休息日" : "已设为调休上班");
  } catch {
    // The drawer exposes the actionable API error and keeps retry available.
  }
}
</script>
