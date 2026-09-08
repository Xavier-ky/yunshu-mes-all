<template>
  <aside class="cal-workbench-list">
    <div class="cal-workbench-list__body" v-loading="loading">
      <button
        v-for="plan in plans"
        :key="plan.planId"
        type="button"
        class="cal-workbench-list__item"
        :class="[
          statusClass(plan.status),
          { 'is-active': plan.planId === selectedPlanId },
        ]"
        @click="$emit('select', plan)"
      >
        <span class="cal-workbench-list__item-head">
          <b>{{ plan.planName || "未命名计划" }}</b>
          <small>{{ plan.planCode }}</small>
        </span>
        <span class="cal-workbench-list__item-meta">
          <dict-tag :options="orderStatuses" :value="plan.status" />
          <span>{{ plan.startDate }} ~ {{ plan.endDate }}</span>
        </span>
      </button>
      <div v-if="!loading && !plans.length" class="cal-workbench-list__empty">
        <b>暂无排班计划</b>
        <span>调整筛选条件或点击新增</span>
      </div>
    </div>
    <div v-if="total > 0" class="cal-workbench-list__footer">
      <span class="cal-workbench-list__footer-total">共 {{ total }} 条</span>
      <pagination
        :total="total"
        :page="page"
        :limit="pageSize"
        layout="prev, pager, next"
        :pager-count="5"
        :auto-scroll="false"
        @update:page="$emit('update:page', $event)"
        @update:limit="$emit('update:pageSize', $event)"
      />
    </div>
  </aside>
</template>

<script setup>
import { planListStatusClass } from "./calPlanModel.js";

defineProps({
  plans: { type: Array, default: () => [] },
  orderStatuses: { type: Array, default: () => [] },
  selectedPlanId: { type: [Number, String], default: null },
  loading: Boolean,
  total: { type: Number, default: 0 },
  page: { type: Number, default: 1 },
  pageSize: { type: Number, default: 10 },
});

defineEmits(["select", "update:page", "update:pageSize"]);

function statusClass(status) {
  return planListStatusClass(status);
}
</script>
