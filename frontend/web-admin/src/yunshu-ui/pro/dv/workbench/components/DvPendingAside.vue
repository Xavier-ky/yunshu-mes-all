<template>
  <div class="operator-task-queue dv-pending-aside">
    <div class="operator-aside-top">
      <div class="operator-worker-inline">
        <span class="operator-worker-inline__avatar">{{ avatarLetter }}</span>
        <div class="operator-worker-inline__text">
          <strong>{{ userLabel }}</strong>
          <span>{{ employeeNo || "—" }} · {{ shiftLabel }}</span>
        </div>
      </div>
      <div class="operator-aside-mini-stats">
        <span><b>{{ typeCount.CHECK }}</b>点检</span>
        <span><b>{{ typeCount.MAINTEN }}</b>保养</span>
        <span><b>{{ typeCount.REPAIR }}</b>维修</span>
      </div>
      <div class="operator-filter-stack">
        <div class="operator-filter-row operator-filter-row--types">
          <button
            v-for="chip in typeChips"
            :key="chip.value"
            type="button"
            :class="{ active: typeFilter === chip.value }"
            @click="$emit('update:typeFilter', chip.value)"
          >
            {{ chip.label }}<em>{{ chip.count }}</em>
          </button>
        </div>
        <div class="operator-filter-row operator-filter-row--all">
          <button type="button" :class="{ active: typeFilter === 'all' }" @click="$emit('update:typeFilter', 'all')">
            {{ allChip.label }}<em>{{ allChip.count }}</em>
          </button>
        </div>
      </div>
    </div>
    <div v-loading="loading" class="operator-task-list">
      <button
        v-for="row in filteredRows"
        :key="rowKey(row)"
        type="button"
        class="operator-task-item"
        :class="{ active: selectedKey === rowKey(row), [`operator-task-item--${(row.taskType || '').toLowerCase()}`]: true }"
        @click="$emit('select', row)"
        @dblclick="$emit('inspect', row)"
      >
        <div class="operator-task-item__row">
          <span class="operator-task-item__no">{{ row.machineryCode || "—" }}</span>
          <el-tag size="small" :type="tagType(row.taskType)">{{ taskLabel(row.taskType) }}</el-tag>
        </div>
        <div class="operator-task-item__product">{{ row.machineryName || "—" }}</div>
        <div class="operator-task-item__step">{{ row.planCode || row.planName || "—" }}</div>
        <div v-if="row.stationName || row.workOrderNo" class="operator-task-item__meta">
          <span v-if="row.stationName">工位 {{ row.stationName }}</span>
          <span v-if="row.workOrderNo">工单 {{ row.workOrderNo }}</span>
        </div>
      </button>
      <div v-if="!loading && !filteredRows.length" class="operator-empty-hint">暂无待办任务</div>
    </div>
    <div class="operator-aside-footer">共 {{ allRows.length }} 条</div>
  </div>
</template>

<script>
const LABEL = { CHECK: "点检", MAINTEN: "保养", REPAIR: "维修" };
const TAG = { CHECK: "primary", MAINTEN: "success", REPAIR: "warning" };

export default {
  name: "DvPendingAside",
  props: {
    allRows: { type: Array, default: () => [] },
    filteredRows: { type: Array, default: () => [] },
    selectedKey: { type: String, default: "" },
    typeFilter: { type: String, default: "all" },
    loading: { type: Boolean, default: false },
    userLabel: { type: String, default: "—" },
    employeeNo: { type: String, default: "" },
    shiftLabel: { type: String, default: "白班" },
  },
  emits: ["select", "inspect", "update:typeFilter"],
  computed: {
    avatarLetter() {
      return (this.userLabel || "?").slice(0, 1);
    },
    typeCount() {
      const c = { CHECK: 0, MAINTEN: 0, REPAIR: 0 };
      this.allRows.forEach((r) => {
        if (c[r.taskType] !== undefined) c[r.taskType] += 1;
      });
      return c;
    },
    chipsWithCount() {
      const all = this.allRows;
      return [
        { value: "all", label: "全部", count: all.length },
        { value: "CHECK", label: "点检", count: all.filter((r) => r.taskType === "CHECK").length },
        { value: "MAINTEN", label: "保养", count: all.filter((r) => r.taskType === "MAINTEN").length },
        { value: "REPAIR", label: "维修", count: all.filter((r) => r.taskType === "REPAIR").length },
      ];
    },
    typeChips() {
      return this.chipsWithCount.filter((c) => c.value !== "all");
    },
    allChip() {
      return this.chipsWithCount.find((c) => c.value === "all") || { value: "all", label: "全部", count: 0 };
    },
  },
  methods: {
    rowKey(row) {
      return `${row.taskType}-${row.taskId}`;
    },
    taskLabel(t) {
      return LABEL[t] || t || "—";
    },
    tagType(t) {
      return TAG[t] || "info";
    },
  },
};
</script>
