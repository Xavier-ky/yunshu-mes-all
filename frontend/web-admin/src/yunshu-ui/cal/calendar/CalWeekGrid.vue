<template>
  <div class="cal-workbench-week" v-loading="loading">
    <div v-if="guidance" class="cal-workbench-state">
      <b>暂时无法加载排班</b>
      <span>{{ guidance }}</span>
      <el-button v-if="showUserAction" type="primary" @click="$emit('choose-user')">选择人员</el-button>
    </div>
    <div v-else-if="error" class="cal-workbench-state is-error">
      <b>周排班加载失败</b>
      <span>{{ error }}</span>
      <el-button type="primary" plain @click="$emit('retry')">重试</el-button>
    </div>
    <template v-else>
      <div class="cal-workbench-week__table">
        <div class="cal-workbench-week__head is-team">班组</div>
        <button
          v-for="day in weekDays"
          :key="day.theDay"
          type="button"
          class="cal-workbench-week__head"
          @click="$emit('open', day)"
        >
          <span>周{{ day.weekLabel }} {{ day.theDay.slice(5) }}</span>
          <small :class="`is-${day.status.code.toLowerCase()}`">{{ day.status.label }}</small>
        </button>

        <div
          v-for="team in week.teams"
          :key="team.teamId"
          class="cal-workbench-week__row"
        >
          <div class="cal-workbench-week__team">
            <b>{{ team.teamName }}</b>
            <small>{{ team.teamCode || `#${team.teamId}` }}</small>
          </div>
          <div
            v-for="day in weekDays"
            :key="`${team.teamId}-${day.theDay}`"
            class="cal-workbench-week__cell"
            role="button"
            tabindex="0"
            @click="$emit('open', day)"
            @keydown.enter="$emit('open', day)"
            @keydown.space.prevent="$emit('open', day)"
          >
            <el-tooltip
              v-for="shift in chipsFor(team, day.theDay).visible"
              :key="shift.recordId"
              :content="`${team.teamName} · ${shift.shiftName || '未命名班次'} · ${timeRange(shift)}`"
              placement="top"
            >
              <span
                class="cal-workbench-shift-chip"
                role="button"
                tabindex="0"
                @click.stop="$emit('open', day, shift)"
                @keydown.enter.stop="$emit('open', day, shift)"
                @keydown.space.stop.prevent="$emit('open', day, shift)"
              >
                <span>{{ shift.shiftName || "未命名班次" }}</span>
                <small>{{ timeRange(shift) }}</small>
              </span>
            </el-tooltip>
            <span v-if="chipsFor(team, day.theDay).overflow" class="cal-workbench-day__overflow">
              +{{ chipsFor(team, day.theDay).overflow }}
            </span>
            <span
              v-if="!shiftsFor(team, day.theDay).length"
              class="cal-workbench-week__empty"
              :class="{ 'is-unassigned': day.workday }"
            >
              {{ day.workday ? "未排班" : "休" }}
            </span>
          </div>
        </div>
      </div>
      <el-empty v-if="!week.teams.length && !loading" description="本周暂无班组排班" :image-size="68" />
    </template>
  </div>
</template>

<script setup>
import { computed } from "vue";
import {
  addDays,
  formatDate,
  getDayStatus,
  parseLocalDate,
  splitShiftChips,
} from "./calScheduleModel";

const props = defineProps({
  week: {
    type: Object,
    default: () => ({ startDate: "", endDate: "", dayTypes: {}, teams: [] }),
  },
  anchorDate: { type: [Date, String], required: true },
  loading: Boolean,
  error: { type: String, default: "" },
  guidance: { type: String, default: "" },
  showUserAction: Boolean,
});

defineEmits(["open", "retry", "choose-user"]);

const weekDays = computed(() => {
  let start;
  if (props.week.startDate) {
    start = parseLocalDate(props.week.startDate);
  } else {
    start = parseLocalDate(props.anchorDate);
    start.setDate(start.getDate() - ((start.getDay() + 6) % 7));
  }
  return ["一", "二", "三", "四", "五", "六", "日"].map((weekLabel, index) => {
    const theDay = formatDate(addDays(start, index));
    const type = props.week.dayTypes?.[theDay] || {};
    return { theDay, weekLabel, ...type, status: getDayStatus(type) };
  });
});

function shiftsFor(team, theDay) {
  return Array.isArray(team.days?.[theDay]) ? team.days[theDay] : [];
}

function chipsFor(team, theDay) {
  return splitShiftChips(shiftsFor(team, theDay));
}

function shortTime(value) {
  return value ? String(value).slice(0, 5) : "--:--";
}

function timeRange(shift) {
  return `${shortTime(shift.shiftStartTime)}-${shortTime(shift.shiftEndTime)}`;
}
</script>
