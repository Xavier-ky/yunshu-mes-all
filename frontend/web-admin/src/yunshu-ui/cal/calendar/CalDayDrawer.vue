<template>
  <el-drawer
    :model-value="modelValue"
    size="420px"
    class="cal-workbench-drawer"
    modal-class="cal-workbench-drawer-portal"
    :with-header="false"
    @update:model-value="$emit('update:modelValue', $event)"
    @close="$emit('close')"
  >
    <div class="cal-workbench-drawer__head">
      <div>
        <small>排班详情</small>
        <h3>{{ day.theDay || "日期详情" }}</h3>
        <p>{{ dayLabel }} · {{ lunarText }}</p>
      </div>
      <div class="cal-workbench-drawer__head-actions">
        <span :class="`cal-workbench-drawer__status is-${status.code.toLowerCase()}`">
          {{ status.label }}
        </span>
        <el-button
          :icon="Close"
          circle
          text
          aria-label="关闭排班详情"
          @click="$emit('update:modelValue', false)"
        />
      </div>
    </div>

    <div v-if="error" class="cal-workbench-drawer__error">
      <span>{{ error }}</span>
      <el-button link type="primary" @click="$emit('retry', day)">重试</el-button>
    </div>

    <div v-loading="loading" class="cal-workbench-drawer__content">
      <section>
        <div class="cal-workbench-drawer__section-title">
          <h4>当日班次</h4>
          <span>{{ day.teamShifts?.length || 0 }} 条</span>
        </div>
        <article
          v-for="shift in day.teamShifts"
          :key="shift.recordId"
          class="cal-workbench-drawer__shift"
        >
          <div>
            <b>{{ shift.teamName || "未命名班组" }}</b>
            <span>{{ shift.shiftName || "未命名班次" }} · {{ timeRange(shift) }}</span>
          </div>
          <dl>
            <div><dt>成员</dt><dd>{{ shift.memberCount || shift.members?.length || 0 }} 人</dd></div>
            <div><dt>计划</dt><dd>{{ shift.planName || "未关联" }}</dd></div>
          </dl>
          <p v-if="shift.members?.length">
            {{ shift.members.map((member) => member.nickName || member.userName).filter(Boolean).join("、") }}
          </p>
        </article>
        <div v-if="!day.teamShifts?.length" class="cal-workbench-drawer__empty">
          {{ day.workday ? "该工作日尚未安排班次" : "该日为休息日，无排班" }}
        </div>
      </section>

      <section>
        <div class="cal-workbench-drawer__section-title"><h4>风险提示</h4></div>
        <ul v-if="risks.length" class="cal-workbench-drawer__risks">
          <li v-for="risk in risks" :key="risk">{{ risk }}</li>
        </ul>
        <p v-else class="cal-workbench-drawer__safe">未发现明显排班风险</p>
      </section>
    </div>

    <div class="cal-workbench-drawer__actions">
      <el-button @click="$emit('navigate-tab', 'team')">班组维护</el-button>
      <el-button @click="$emit('navigate-tab', 'plan')">编制计划</el-button>
      <el-button :loading="loading" @click="$emit('set-status', 'HOLIDAY')">设为休息日</el-button>
      <el-button type="primary" :loading="loading" @click="$emit('set-status', 'WORKDAY')">
        设为调休上班
      </el-button>
    </div>
  </el-drawer>
</template>

<script setup>
import { computed } from "vue";
import { Close } from "@element-plus/icons-vue";
import calendar from "@/yunshu-ui/utils/calendar";
import { getDayStatus } from "./calScheduleModel";

const props = defineProps({
  modelValue: Boolean,
  day: { type: Object, default: () => ({ teamShifts: [] }) },
  loading: Boolean,
  error: { type: String, default: "" },
});

defineEmits(["update:modelValue", "navigate-tab", "set-status", "retry", "close"]);

const status = computed(() => getDayStatus(props.day));
const dayLabel = computed(() => {
  const labels = {
    MONDAY: "星期一",
    TUESDAY: "星期二",
    WEDNESDAY: "星期三",
    THURSDAY: "星期四",
    FRIDAY: "星期五",
    SATURDAY: "星期六",
    SUNDAY: "星期日",
  };
  return labels[props.day.dayOfWeek] || "";
});

const lunarText = computed(() => {
  const [year, month, day] = String(props.day.theDay || "").split("-").map(Number);
  if (!year || !month || !day) return "";
  try {
    const lunar = calendar.solar2lunar(year, month, day);
    return [lunar.festival, lunar.lunarFestival, lunar.Term].filter(Boolean).join(" ")
      || `${lunar.IMonthCn}${lunar.IDayCn}`;
  } catch {
    return "";
  }
});

const risks = computed(() => {
  const result = [];
  const shifts = props.day.teamShifts || [];
  if (props.day.workday && !shifts.length) result.push("工作日未排班，可能造成产能缺口。");
  const teamIds = shifts.map((shift) => shift.teamId);
  if (new Set(teamIds).size < teamIds.length) result.push("同一班组存在多条当日排班，请核对是否冲突。");
  if (shifts.some((shift) => !(Number(shift.memberCount) || shift.members?.length))) {
    result.push("存在无成员班组，排班可能无法执行。");
  }
  return result;
});

function shortTime(value) {
  return value ? String(value).slice(0, 5) : "--:--";
}

function timeRange(shift) {
  return `${shortTime(shift.shiftStartTime)}-${shortTime(shift.shiftEndTime)}`;
}
</script>
