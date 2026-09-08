<template>
  <div class="operator-task-queue">
    <div class="operator-aside-top">
      <div class="operator-worker-inline">
        <span class="operator-worker-inline__avatar">{{ avatarLetter }}</span>
        <div class="operator-worker-inline__text">
          <strong>{{ userLabel }}</strong>
          <span>{{ employeeNo || "—" }} · {{ shiftLabel }}</span>
        </div>
      </div>
      <div class="operator-aside-mini-stats">
        <span><b>{{ statRunning }}</b>进行</span>
        <span><b>{{ statPending }}</b>待始</span>
        <span><b>{{ statCompleted }}</b>完成</span>
        <span><b>{{ todayQualified }}</b>良品</span>
      </div>
      <div class="operator-task-search">
        <el-input
          v-model="keyword"
          clearable
          size="small"
          placeholder="搜订单号 / 工单号 / 产品 / 工序"
          prefix-icon="el-icon-search"
        />
      </div>
      <div class="operator-filter-row">
        <button
          v-for="chip in chipsWithCount"
          :key="chip.value"
          type="button"
          :class="{ active: statusFilter === chip.value }"
          @click="$emit('update:statusFilter', chip.value)"
        >
          {{ chip.label }}<em>{{ chip.count }}</em>
        </button>
      </div>
    </div>

    <div v-loading="loading" class="operator-task-list">
      <button
        v-for="task in visibleTasks"
        :key="task.dispatchId"
        type="button"
        class="operator-task-item"
        :class="{ active: selectedId === task.dispatchId }"
        @click="$emit('select', task)"
      >
        <div class="operator-task-item__row">
          <span class="operator-task-item__no">{{ task.dispatchNo }}</span>
          <el-tag size="small" :type="statusTag(task.status)">{{ statusLabel(task.status) }}</el-tag>
          <span class="operator-task-item__qty">{{ task.completedQty || 0 }}/{{ task.plannedQty || 0 }}</span>
        </div>
        <div class="operator-task-item__wo">{{ task.workOrderNo || "—" }}</div>
        <div v-if="task.customerOrderNo" class="operator-task-item__co">{{ task.customerOrderNo }}</div>
        <div class="operator-task-item__product">{{ task.productName || "—" }}</div>
        <div class="operator-task-item__step">{{ task.stepName || "—" }} · {{ task.stationName || "—" }}</div>
        <el-progress :percentage="progressPct(task)" :stroke-width="4" :show-text="false" />
      </button>
      <div v-if="!loading && !visibleTasks.length" class="operator-empty-hint">
        {{ keyword ? "未找到匹配的派工" : "暂无派工任务" }}
      </div>
    </div>

    <div class="operator-aside-footer">
      共 {{ allTasks.length }} 条<template v-if="keyword"> · 筛选 {{ visibleTasks.length }}</template
      ><template v-if="selectedDispatchNo"> · {{ selectedDispatchNo }}</template>
    </div>
  </div>
</template>

<script>
import { sortDispatchTasksRecentFirst } from "@/yunshu-ui/api/mes/pro/operator";

export default {
  name: "OperatorTaskQueueAside",
  props: {
    tasks: { type: Array, default: () => [] },
    allTasks: { type: Array, default: () => [] },
    selectedId: { type: [Number, String], default: null },
    statusFilter: { type: String, default: "all" },
    loading: { type: Boolean, default: false },
    userLabel: { type: String, default: "—" },
    employeeNo: { type: String, default: "" },
    shiftLabel: { type: String, default: "白班" },
    todayQualified: { type: Number, default: 0 },
    initialKeyword: { type: String, default: "" },
  },
  emits: ["select", "update:statusFilter"],
  data() {
    return {
      keyword: this.initialKeyword || "",
    };
  },
  watch: {
    initialKeyword(value) {
      if (value && value !== this.keyword) {
        this.keyword = value;
      }
    },
  },
  computed: {
    visibleTasks() {
      const q = (this.keyword || "").trim().toLowerCase();
      const base = !q
        ? this.tasks
        : this.tasks.filter((task) => {
        const haystack = [
          task.customerOrderNo,
          task.workOrderNo,
          task.dispatchNo,
          task.productName,
          task.productCode,
          task.stepName,
          task.stationName,
        ]
              .filter(Boolean)
              .join(" ")
              .toLowerCase();
            return haystack.includes(q);
          });
      return sortDispatchTasksRecentFirst(base);
    },
    avatarLetter() {
      return (this.userLabel || "?").slice(0, 1);
    },
    statRunning() {
      return this.allTasks.filter((t) => t.status === "RUNNING").length;
    },
    statPending() {
      return this.allTasks.filter((t) => t.status === "CREATED" || t.status === "DISPATCHED").length;
    },
    statCompleted() {
      return this.allTasks.filter((t) => t.status === "COMPLETED").length;
    },
    selectedDispatchNo() {
      return this.allTasks.find((t) => t.dispatchId === this.selectedId)?.dispatchNo || "";
    },
    chipsWithCount() {
      const all = this.allTasks;
      return [
        { value: "all", label: "全部", count: all.length },
        { value: "RUNNING", label: "进行", count: all.filter((t) => t.status === "RUNNING").length },
        {
          value: "CREATED",
          label: "待始",
          count: all.filter((t) => t.status === "CREATED" || t.status === "DISPATCHED").length,
        },
        { value: "COMPLETED", label: "完成", count: all.filter((t) => t.status === "COMPLETED").length },
      ];
    },
  },
  methods: {
    progressPct(task) {
      const planned = Number(task.plannedQty) || 0;
      const done = Number(task.completedQty) || 0;
      if (planned <= 0) return 0;
      return Math.min(100, Math.round((done / planned) * 100));
    },
    statusLabel(s) {
      if (s === "CREATED" || s === "DISPATCHED") return "待始";
      if (s === "RUNNING") return "进行";
      if (s === "COMPLETED") return "完成";
      return s || "—";
    },
    statusTag(s) {
      if (s === "RUNNING") return "warning";
      if (s === "COMPLETED") return "success";
      return "info";
    },
  },
};
</script>

<style scoped>
.operator-task-item__co {
  font-family: "JetBrains Mono", ui-monospace, monospace;
  font-size: 10px;
  font-weight: 600;
  color: #64748b;
  margin-bottom: 2px;
  white-space: nowrap;
  overflow: hidden;
  text-overflow: ellipsis;
}
</style>
