<template>
  <div class="cal-workbench-month" v-loading="loading">
    <div v-for="label in weekLabels" :key="label" class="cal-workbench-month__weekday">
      {{ label }}
    </div>

    <template v-if="!guidance && !error">
      <div
        v-for="cell in cells"
        :key="cell.key"
        class="cal-workbench-month__cell"
        :class="{ 'is-placeholder': !cell.day }"
      >
        <CalDayCell v-if="cell.day" :day="cell.day" @open="(...args) => $emit('open', ...args)" />
      </div>
    </template>

    <div v-if="guidance" class="cal-workbench-state">
      <b>暂时无法加载排班</b>
      <span>{{ guidance }}</span>
      <el-button v-if="showUserAction" type="primary" @click="$emit('choose-user')">选择人员</el-button>
    </div>
    <div v-else-if="error" class="cal-workbench-state is-error">
      <b>日历加载失败</b>
      <span>{{ error }}</span>
      <el-button type="primary" plain @click="$emit('retry')">重试</el-button>
    </div>
  </div>
</template>

<script setup>
import { computed } from "vue";
import CalDayCell from "./CalDayCell.vue";
import { parseLocalDate } from "./calScheduleModel";

const props = defineProps({
  days: { type: Array, default: () => [] },
  anchorDate: { type: [Date, String], required: true },
  loading: Boolean,
  error: { type: String, default: "" },
  guidance: { type: String, default: "" },
  showUserAction: Boolean,
});

defineEmits(["open", "retry", "choose-user"]);

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
