<template>
  <div class="operator-task-queue qc-pending-aside">
    <div class="operator-aside-top">
      <div class="operator-worker-inline">
        <span class="operator-worker-inline__avatar">{{ avatarLetter }}</span>
        <div class="operator-worker-inline__text">
          <strong>{{ userLabel }}</strong>
          <span>{{ employeeNo || "—" }} · {{ shiftLabel }}</span>
        </div>
      </div>
      <div class="operator-aside-mini-stats">
        <span><b>{{ typeCount.IQC }}</b>IQC</span>
        <span><b>{{ typeCount.PQC }}</b>PQC</span>
        <span><b>{{ typeCount.OQC }}</b>OQC</span>
        <span><b>{{ typeCount.RQC }}</b>RQC</span>
      </div>
      <div class="operator-task-search">
        <el-input
          v-model="keyword"
          clearable
          size="small"
          placeholder="搜订单号 / 工单号 / 报工号 / 产品"
          prefix-icon="el-icon-search"
        />
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
          <button
            type="button"
            :class="{ active: typeFilter === 'all' }"
            @click="$emit('update:typeFilter', 'all')"
          >
            {{ allChip.label }}<em>{{ allChip.count }}</em>
          </button>
        </div>
      </div>
    </div>

    <div v-loading="loading" class="operator-task-list">
      <button
        v-for="row in visibleRows"
        :key="rowKey(row)"
        type="button"
        class="operator-task-item"
        :class="{ active: selectedKey === rowKey(row) }"
        @click="$emit('select', row)"
        @dblclick="$emit('inspect', row)"
      >
        <div class="operator-task-item__row">
          <span class="operator-task-item__no">{{ row.sourceDocCode || "—" }}</span>
          <dict-tag :options="dict.type.mes_qc_type" :value="row.qcType" />
          <span class="operator-task-item__qty">{{ row.quantityCheck ?? "—" }}</span>
        </div>
        <div class="operator-task-item__wo" v-if="row.workOrderCode">{{ row.workOrderCode }}</div>
        <div v-if="row.customerOrderNo" class="operator-task-item__co">{{ row.customerOrderNo }}</div>
        <div class="operator-task-item__product">{{ row.itemName || "—" }}</div>
        <div class="operator-task-item__step">{{ row.itemCode || "—" }} · {{ row.processName || row.workstationName || row.sourceDocName || "—" }}</div>
      </button>
      <div v-if="!loading && !visibleRows.length" class="operator-empty-hint">
        {{ keyword ? "未找到匹配的待检任务" : "暂无待检任务" }}
      </div>
    </div>

    <div class="operator-aside-footer">
      共 {{ allRows.length }} 条<template v-if="keyword"> · 筛选 {{ visibleRows.length }}</template
      ><template v-if="selectedDocCode"> · {{ selectedDocCode }}</template>
    </div>
  </div>
</template>

<script>
import { sortQcPendingRecentFirst } from "@/yunshu-ui/api/mes/qc/pending";

export default {
  name: "QcPendingAside",
  dicts: ["mes_qc_type"],
  props: {
    allRows: { type: Array, default: () => [] },
    filteredRows: { type: Array, default: () => [] },
    selectedKey: { type: String, default: "" },
    typeFilter: { type: String, default: "all" },
    loading: { type: Boolean, default: false },
    userLabel: { type: String, default: "—" },
    employeeNo: { type: String, default: "" },
    shiftLabel: { type: String, default: "白班" },
    initialKeyword: { type: String, default: "" },
  },
  emits: ["select", "inspect", "update:typeFilter"],
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
    visibleRows() {
      const q = (this.keyword || "").trim().toLowerCase();
      const base = !q
        ? this.filteredRows
        : this.filteredRows.filter((row) => {
        const haystack = [
          row.customerOrderNo,
          row.workOrderCode,
          row.sourceDocCode,
          row.sourceDocName,
          row.itemCode,
          row.itemName,
          row.workstationName,
          row.processName,
        ]
          .filter(Boolean)
          .join(" ")
          .toLowerCase();
        return haystack.includes(q);
      });
      return sortQcPendingRecentFirst(base);
    },
    avatarLetter() {
      return (this.userLabel || "?").slice(0, 1);
    },
    typeCount() {
      const c = { IQC: 0, PQC: 0, OQC: 0, RQC: 0 };
      this.allRows.forEach((r) => {
        if (c[r.qcType] !== undefined) c[r.qcType] += 1;
      });
      return c;
    },
    selectedDocCode() {
      const row = this.allRows.find((r) => this.rowKey(r) === this.selectedKey);
      return row?.sourceDocCode || "";
    },
    chipsWithCount() {
      const all = this.allRows;
      return [
        { value: "all", label: "全部", count: all.length },
        { value: "IQC", label: "IQC", count: all.filter((r) => r.qcType === "IQC").length },
        { value: "PQC", label: "PQC", count: all.filter((r) => r.qcType === "PQC").length },
        { value: "OQC", label: "OQC", count: all.filter((r) => r.qcType === "OQC").length },
        { value: "RQC", label: "RQC", count: all.filter((r) => r.qcType === "RQC").length },
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
      return `${row.qcType}-${row.sourceDocId}-${row.sourceLineId}-${row.itemId}`;
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
