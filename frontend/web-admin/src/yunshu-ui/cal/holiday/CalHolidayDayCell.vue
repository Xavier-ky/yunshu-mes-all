<template>
  <div
    role="button"
    tabindex="0"
    class="cal-workbench-day cal-holiday-day"
    :class="[
      day.visualClass,
      {
        'is-today': isToday,
        'is-festival-day': day.lunar?.festival,
        'is-explicit-holiday': isExplicitHoliday,
        'is-rest-day': isRestDay,
        'is-adjusted-day': isAdjusted,
      },
    ]"
    @click="$emit('open', day)"
    @contextmenu.prevent="$emit('open', day)"
    @keydown.enter="$emit('open', day)"
    @keydown.space.prevent="$emit('open', day)"
  >
    <span v-if="isExplicitHoliday" class="cal-holiday-day__accent" aria-hidden="true" />

    <span class="cal-workbench-day__head cal-holiday-day__head">
      <span class="cal-holiday-day__date-wrap">
        <span class="cal-workbench-day__date">{{ dayNumber }}</span>
        <span v-if="day.lunar?.festival" class="cal-holiday-day__fest-dot" aria-hidden="true" />
      </span>
      <span
        class="cal-workbench-day__lunar"
        :class="{ 'is-festival': day.lunar?.festival }"
      >
        {{ day.lunar?.text || "" }}
      </span>
      <span
        class="cal-workbench-day__status cal-holiday-day__status"
        :class="statusClass"
      >
        {{ day.badgeLabel }}
      </span>
    </span>

    <span class="cal-workbench-day__body cal-holiday-day__body">
      <span v-if="day.override?.remark" class="cal-holiday-day__remark">
        {{ day.override.remark }}
      </span>
      <span v-else-if="isExplicitHoliday" class="cal-holiday-day__remark is-muted">法定/手动假日</span>
      <span v-else-if="day.restKind === 'default'" class="cal-holiday-day__hint">周日休息</span>
      <span v-else-if="isAdjusted" class="cal-holiday-day__hint is-adjusted">调休上班</span>
      <span v-else-if="day.lunar?.festivalText" class="cal-holiday-day__festival">
        {{ day.lunar.festivalText }}
      </span>
    </span>
  </div>
</template>

<script setup>
import { computed } from "vue";
import { formatDate } from "../calendar/calScheduleModel";

const props = defineProps({
  day: { type: Object, required: true },
});

defineEmits(["open"]);

const dayNumber = computed(() => Number(String(props.day.theDay).slice(-2)));
const isToday = computed(() => props.day.theDay === formatDate(new Date()));
const isExplicitHoliday = computed(() => props.day.restKind === "explicit");
const isRestDay = computed(() => props.day.status?.code === "REST");
const isAdjusted = computed(() => props.day.status?.code === "ADJUSTED");

const statusClass = computed(() => {
  const code = props.day.status?.code?.toLowerCase();
  if (code === "rest") {
    return props.day.restKind === "explicit" ? "is-rest-holiday is-wide" : "is-rest-default";
  }
  if (code === "adjusted") return "is-adjusted";
  return "is-shift";
});
</script>
