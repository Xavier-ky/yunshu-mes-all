<template>
  <div
    role="button"
    tabindex="0"
    class="cal-workbench-day"
    :class="[
      `cal-workbench-day--${status.code.toLowerCase()}`,
      lunar.kind ? `is-${lunar.kind}-day` : '',
      { 'is-today': isToday, 'is-unassigned': isUnassigned },
    ]"
    @click="$emit('open', day)"
    @keydown.enter="$emit('open', day)"
    @keydown.space.prevent="$emit('open', day)"
  >
    <span class="cal-workbench-day__head">
      <span class="cal-workbench-day__date">{{ dayNumber }}</span>
      <span
        class="cal-workbench-day__lunar"
        :class="[
          { 'is-festival': lunar.festival },
          lunar.kind ? `cal-workbench-day__lunar--${lunar.kind}` : '',
        ]"
      >
        {{ lunar.text }}
      </span>
      <span class="cal-workbench-day__status">{{ status.label }}</span>
    </span>

    <span class="cal-workbench-day__body">
      <el-tooltip
        v-for="shift in chips.visible"
        :key="shift.recordId || `${shift.teamId}-${shift.shiftId}-${shift.orderNum}`"
        placement="top"
        :content="shiftTooltip(shift)"
      >
        <span
          class="cal-workbench-shift-chip"
          tabindex="0"
          role="button"
          @click.stop="$emit('open', day, shift)"
          @keydown.enter.stop="$emit('open', day, shift)"
          @keydown.space.stop.prevent="$emit('open', day, shift)"
        >
          <span>{{ shift.teamName || "未命名班组" }} · {{ shift.shiftName || "未命名班次" }}</span>
          <small>{{ timeRange(shift) }}</small>
        </span>
      </el-tooltip>
      <span v-if="chips.overflow" class="cal-workbench-day__overflow">+{{ chips.overflow }}</span>
      <span v-if="isUnassigned" class="cal-workbench-day__empty">工作日未排班</span>
      <span v-else-if="!day.teamShifts?.length" class="cal-workbench-day__rest">
        {{ day.workday ? "暂无班次" : "休息日" }}
      </span>
    </span>
  </div>
</template>

<script setup>
import { computed } from "vue";
import calendar from "@/yunshu-ui/utils/calendar";
import { formatDate, getDayStatus, splitShiftChips } from "./calScheduleModel";

const props = defineProps({
  day: { type: Object, required: true },
});

defineEmits(["open"]);

const status = computed(() => getDayStatus(props.day));
const chips = computed(() => splitShiftChips(props.day.teamShifts));
const dayNumber = computed(() => Number(String(props.day.theDay).slice(-2)));
const isToday = computed(() => props.day.theDay === formatDate(new Date()));
const isUnassigned = computed(
  () => props.day.workday && !(props.day.teamShifts || []).length,
);

const lunar = computed(() => {
  const [year, month, day] = String(props.day.theDay).split("-").map(Number);
  try {
    const value = calendar.solar2lunar(year, month, day);
    const festival = [value.festival, value.lunarFestival].filter(Boolean).join(" ");
    const term = value.Term || "";
    if (festival) {
      return { text: festival, festival: true, kind: "festival" };
    }
    if (term) {
      return { text: term, festival: false, kind: "term" };
    }
    return { text: `${value.IMonthCn}${value.IDayCn}`, festival: false, kind: "" };
  } catch {
    return { text: "", festival: false, kind: "" };
  }
});

function shortTime(value) {
  return value ? String(value).slice(0, 5) : "--:--";
}

function timeRange(shift) {
  return `${shortTime(shift.shiftStartTime)}-${shortTime(shift.shiftEndTime)}`;
}

function shiftTooltip(shift) {
  return `${shift.teamName || "未命名班组"} · ${shift.shiftName || "未命名班次"} · ${timeRange(shift)} · ${Number(shift.memberCount) || 0}人`;
}
</script>
