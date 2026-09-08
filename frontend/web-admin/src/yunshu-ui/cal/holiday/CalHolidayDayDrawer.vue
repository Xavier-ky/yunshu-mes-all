<template>
  <el-drawer
    :model-value="modelValue"
    size="420px"
    class="cal-workbench-drawer cal-holiday-drawer"
    modal-class="cal-workbench-drawer-portal"
    :with-header="false"
    @update:model-value="$emit('update:modelValue', $event)"
    @close="$emit('close')"
  >
    <div class="cal-workbench-drawer__head" :class="{ 'is-holiday-highlight': day?.restKind === 'explicit' }">
      <div>
        <small>节假日详情</small>
        <h3>{{ day?.theDay || "日期详情" }}</h3>
        <p>{{ dayLabel }} · {{ day?.lunar?.text || "" }}</p>
      </div>
      <div class="cal-workbench-drawer__head-actions">
        <span
          v-if="day"
          :class="[
            'cal-workbench-drawer__status',
            `is-${day.status?.code?.toLowerCase() || 'shift'}`,
            day.restKind === 'explicit' ? 'is-rest-holiday is-wide' : '',
          ]"
        >
          {{ day.badgeLabel || day.status?.label }}
        </span>
        <el-button
          :icon="Close"
          circle
          text
          aria-label="关闭节假日详情"
          @click="$emit('update:modelValue', false)"
        />
      </div>
    </div>

    <div v-if="error" class="cal-workbench-drawer__error">
      <span>{{ error }}</span>
      <el-button link type="primary" @click="$emit('retry')">重试</el-button>
    </div>

    <div v-loading="loading" class="cal-workbench-drawer__content">
      <section>
        <div class="cal-workbench-drawer__section-title"><h4>当前规则</h4></div>
        <dl class="cal-holiday-drawer__meta">
          <div><dt>日期类型</dt><dd>{{ day?.typeLabel || "—" }}</dd></div>
          <div><dt>规则来源</dt><dd>{{ day?.sourceLabel || "—" }}</dd></div>
          <div><dt>是否工作日</dt><dd>{{ day?.workday ? "是" : "否" }}</dd></div>
        </dl>
      </section>

      <section v-if="day?.lunar?.festivalText">
        <div class="cal-workbench-drawer__section-title"><h4>节日信息</h4></div>
        <p class="cal-holiday-drawer__festival">{{ day.lunar.festivalText }}</p>
      </section>

      <section>
        <div class="cal-workbench-drawer__section-title"><h4>备注</h4></div>
        <el-input
          :model-value="remark"
          type="textarea"
          :rows="3"
          maxlength="500"
          show-word-limit
          placeholder="例如：春节、国庆调休"
          @update:model-value="$emit('update:remark', $event)"
        />
      </section>
    </div>

    <div class="cal-workbench-drawer__actions cal-holiday-drawer__actions">
      <el-button
        :disabled="!day?.override?.holidayId"
        :loading="loading"
        @click="$emit('reset')"
      >
        恢复默认
      </el-button>
      <el-button :loading="loading" @click="$emit('apply', 'HOLIDAY')">设为休息日</el-button>
      <el-button type="primary" :loading="loading" @click="$emit('apply', 'WORKDAY')">
        设为调休上班
      </el-button>
    </div>
  </el-drawer>
</template>

<script setup>
import { computed } from "vue";
import { Close } from "@element-plus/icons-vue";
import { parseLocalDate } from "../calendar/calScheduleModel";

const props = defineProps({
  modelValue: Boolean,
  day: { type: Object, default: null },
  remark: { type: String, default: "" },
  loading: Boolean,
  error: { type: String, default: "" },
});

defineEmits(["update:modelValue", "update:remark", "apply", "reset", "retry", "close"]);

const dayLabel = computed(() => {
  if (!props.day?.theDay) return "";
  const date = parseLocalDate(props.day.theDay);
  const labels = ["星期日", "星期一", "星期二", "星期三", "星期四", "星期五", "星期六"];
  return labels[date.getDay()] || "";
});
</script>
