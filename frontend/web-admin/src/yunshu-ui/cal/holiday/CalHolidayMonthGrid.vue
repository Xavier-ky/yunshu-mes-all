<template>
  <div class="cal-workbench-month cal-holiday-month" v-loading="loading">
    <div v-for="label in weekLabels" :key="label" class="cal-workbench-month__weekday">
      {{ label }}
    </div>

    <template v-if="!error">
      <div
        v-for="cell in cells"
        :key="cell.key"
        class="cal-workbench-month__cell"
        :class="{ 'is-placeholder': !cell.day }"
      >
        <CalHolidayDayCell v-if="cell.day" :day="cell.day" @open="(...args) => $emit('open', ...args)" />
      </div>
    </template>

    <div v-if="error" class="cal-workbench-state is-error">
      <b>节假日日历加载失败</b>
      <span>{{ error }}</span>
      <el-button type="primary" plain @click="$emit('retry')">重试</el-button>
    </div>
  </div>
</template>

<script setup>
import { computed } from "vue";
import CalHolidayDayCell from "./CalHolidayDayCell.vue";
import { parseLocalDate } from "../calendar/calScheduleModel";

const props = defineProps({
  days: { type: Array, default: () => [] },
  anchorDate: { type: [Date, String], required: true },
  loading: Boolean,
  error: { type: String, default: "" },
});

defineEmits(["open", "retry"]);

const weekLabels = ["一", "二", "三", "四", "五", "六", "日"];

const cells = computed(() => {
  const anchor = parseLocalDate(props.anchorDate);
  const first = new Date(anchor.getFullYear(), anchor.getMonth(), 1);
  const leading = (first.getDay() + 6) % 7;
  const result = Array.from({ length: leading }, (_, index) => ({
    key: `before-${index}`,
    day: null,
  }));
  props.days.forEach((day) => result.push({ key: day.theDay, day }));
  const trailing = (7 - (result.length % 7)) % 7;
  for (let index = 0; index < trailing; index += 1) {
    result.push({ key: `after-${index}`, day: null });
  }
  return result;
});
</script>
