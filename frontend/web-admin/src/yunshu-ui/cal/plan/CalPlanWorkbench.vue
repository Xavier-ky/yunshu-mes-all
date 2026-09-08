<template>
  <div class="cal-workbench cal-plan-workbench">
    <CalPlanToolbar
      :query-params="queryParams"
      :calendar-types="calendarTypes"
      :loading="loading"
      @update:calendar-type="setQueryField('calendarType', $event)"
      @update:plan-code="setQueryField('planCode', $event)"
      @update:plan-name="setQueryField('planName', $event)"
      @update:start-date="setQueryField('startDate', $event)"
      @update:end-date="setQueryField('endDate', $event)"
      @search="search"
      @reset="resetQuery"
      @create="startCreate"
      @export="exportPlans"
      @refresh="refresh"
      @navigate-tab="$emit('navigate-tab', $event)"
    />

    <div class="cal-workbench-kpis cal-plan-kpis" aria-label="排班计划关键指标">
      <div class="is-accent"><span>计划总数</span><b>{{ kpis.totalCount }}</b><small>个</small></div>
      <div><span>编制中</span><b>{{ kpis.prepareCount }}</b><small>个</small></div>
      <div><span>已确认</span><b>{{ kpis.confirmedCount }}</b><small>个</small></div>
      <div><span>本月生效</span><b>{{ kpis.monthActive }}</b><small>个</small></div>
      <div><span>双班计划</span><b>{{ kpis.shiftTwoCount }}</b><small>个</small></div>
      <div><span>单班计划</span><b>{{ kpis.singleCount }}</b><small>个</small></div>
    </div>

    <div class="cal-workbench-split">
      <CalPlanListPanel
        :plans="plans"
        :order-statuses="orderStatuses"
        :selected-plan-id="selectedPlanId"
        :loading="loading"
        :total="total"
        :page="queryParams.pageNum"
        :page-size="queryParams.pageSize"
        @select="selectPlan"
        @update:page="setPage"
        @update:page-size="setPageSize"
      />
      <CalPlanDetailPanel
        :form="form"
        :editing="editing"
        :is-new="isNew"
        :saving="saving"
        :auto-gen-flag="autoGenFlag"
        :calendar-types="calendarTypes"
        :shift-types="shiftTypes"
        :shift-methods="shiftMethods"
        :order-statuses="orderStatuses"
        @edit="startEdit"
        @save="savePlan"
        @finish="finishPlan"
        @cancel-edit="cancelEdit"
        @delete="removePlan()"
        @create="startCreate"
        @toggle-auto-gen="toggleAutoGen"
      />
    </div>
  </div>
</template>

<script setup>
import { computed, getCurrentInstance } from "vue";
import CalPlanToolbar from "./CalPlanToolbar.vue";
import CalPlanListPanel from "./CalPlanListPanel.vue";
import CalPlanDetailPanel from "./CalPlanDetailPanel.vue";
import { useCalPlan } from "./useCalPlan";

defineEmits(["navigate-tab"]);

const instance = getCurrentInstance();
const dict = computed(() => instance?.proxy?.dict?.type || {});

const calendarTypes = computed(() => dict.value.mes_calendar_type || []);
const shiftTypes = computed(() => dict.value.mes_shift_type || []);
const shiftMethods = computed(() => dict.value.mes_shift_method || []);
const orderStatuses = computed(() => dict.value.mes_order_status || []);

const {
  plans,
  total,
  kpis,
  loading,
  saving,
  selectedPlanId,
  editing,
  isNew,
  form,
  autoGenFlag,
  queryParams,
  refresh,
  search,
  resetQuery,
  setQueryField,
  setPage,
  setPageSize,
  selectPlan,
  startCreate,
  startEdit,
  cancelEdit,
  savePlan,
  finishPlan,
  removePlan,
  toggleAutoGen,
  exportPlans,
} = useCalPlan();
</script>

<script>
export default {
  dicts: ["mes_calendar_type", "mes_shift_type", "mes_shift_method", "mes_order_status"],
};
</script>
