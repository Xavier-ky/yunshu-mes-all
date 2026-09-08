<template>
  <div
    class="qc-workbench-today qc-analytics-bottom-panel dv-recent-done-strip"
    :class="{ 'dv-recent-done-strip--embedded': embedded }"
  >
    <div v-if="!embedded || showHead" class="qc-analytics-bottom-panel__head dv-analytics-block-head">
      <span class="qc-analytics-bottom-panel__title">今日已完成</span>
      <span class="qc-analytics-bottom-panel__meta">{{ metaLabel }}</span>
    </div>
    <div class="inbound-table-frame qc-analytics-table-frame qc-workbench-today__frame">
      <span class="frame-corner frame-corner--tl" aria-hidden="true" />
      <span class="frame-corner frame-corner--tr" aria-hidden="true" />
      <span class="frame-corner frame-corner--bl" aria-hidden="true" />
      <span class="frame-corner frame-corner--br" aria-hidden="true" />
      <el-table
        v-loading="loading"
        class="yunshu-data-table inbound-table qc-analytics-table"
        stripe
        border
        height="100%"
        :data="displayRows"
        @row-click="onRowClick"
      >
        <el-table-column label="类型" width="76" align="center">
          <template #default="scope">
            <el-tag size="small" :type="tagType(scope.row.taskType)">{{ taskLabel(scope.row.taskType) }}</el-tag>
          </template>
        </el-table-column>
        <el-table-column label="设备编码" prop="machineryCode" min-width="110" show-overflow-tooltip />
        <el-table-column label="设备名称" prop="machineryName" min-width="120" show-overflow-tooltip />
        <el-table-column label="单号/计划" prop="planCode" min-width="120" show-overflow-tooltip />
        <el-table-column label="完成时间" prop="finishTime" width="140" show-overflow-tooltip />
      </el-table>
    </div>
  </div>
</template>

<script>
const LABEL = { CHECK: "点检", MAINTEN: "保养", REPAIR: "维修" };
const TAG = { CHECK: "primary", MAINTEN: "success", REPAIR: "warning" };

export default {
  name: "DvRecentDoneStrip",
  props: {
    rows: { type: Array, default: () => [] },
    loading: { type: Boolean, default: false },
    embedded: { type: Boolean, default: false },
    showHead: { type: Boolean, default: false },
  },
  emits: ["open-records"],
  computed: {
    todayStr() {
      return new Date().toISOString().slice(0, 10);
    },
    todayRows() {
      return (this.rows || []).filter((r) => String(r.finishTime || "").startsWith(this.todayStr));
    },
    displayRows() {
      const today = this.todayRows;
      if (today.length) return today.slice(0, 6);
      return (this.rows || []).slice(0, 6);
    },
    metaLabel() {
      const today = this.todayRows.length;
      if (today > 0) return `共 ${today} 条（今日）`;
      return `共 ${this.displayRows.length} 条（最近完成）`;
    },
  },
  methods: {
    taskLabel(t) {
      return LABEL[t] || t || "—";
    },
    tagType(t) {
      return TAG[t] || "info";
    },
    onRowClick() {
      this.$emit("open-records");
    },
  },
};
</script>
