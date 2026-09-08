<template>
  <div class="qc-analytics-main">
    <div class="operator-workbench-head">
      <div class="operator-action-row qc-analytics-action-row">
        <el-button class="operator-ui-btn" type="primary" :auto-insert-space="false" @click="$emit('workbench')">
          <span class="operator-ui-btn__label">进入检验工作台</span>
        </el-button>
        <el-button class="operator-ui-btn" :auto-insert-space="false" @click="$emit('records')">
          <span class="operator-ui-btn__label">检验记录</span>
        </el-button>
        <el-button class="operator-ui-btn" :auto-insert-space="false" @click="$emit('config')">
          <span class="operator-ui-btn__label">标准配置</span>
        </el-button>
        <el-button class="operator-ui-btn" :auto-insert-space="false" @click="$emit('batch-trace')">
          <span class="operator-ui-btn__label">批次追溯</span>
        </el-button>
      </div>
    </div>

    <div class="qc-analytics-section qc-analytics-section--charts">
      <QcChartStrip
        :trend="trend"
        :type-distribution="typeDistribution"
        :defect-top="defectTop"
        :type-volume="typeVolume"
      />
    </div>

    <div class="qc-analytics-section qc-analytics-section--type-insight">
      <QcTypeInsightRow :type-stats="typeStats" :active-type="activeTypeFilter" @filter-type="$emit('filter-type', $event)" />
    </div>

    <div class="qc-analytics-section qc-analytics-section--middle">
      <QcDefectLevelPanel :levels="defectLevels" :defect-top="defectTop" />
      <QcActivityFeed :items="activityFeed" @open="onActivityOpen" />
    </div>

    <div class="qc-analytics-section qc-analytics-section--bottom">
      <RecentFinishedTable :rows="recentFinished" />
      <PendingDispositionTable :rows="pendingDisposition" @inspect="onDispositionInspect" />
    </div>
  </div>
</template>

<script>
import QcChartStrip from "./QcChartStrip.vue";
import QcTypeInsightRow from "./QcTypeInsightRow.vue";
import QcDefectLevelPanel from "./QcDefectLevelPanel.vue";
import QcActivityFeed from "./QcActivityFeed.vue";
import RecentFinishedTable from "./RecentFinishedTable.vue";
import PendingDispositionTable from "./PendingDispositionTable.vue";

export default {
  name: "QcAnalyticsMain",
  components: {
    QcChartStrip,
    QcTypeInsightRow,
    QcDefectLevelPanel,
    QcActivityFeed,
    RecentFinishedTable,
    PendingDispositionTable,
  },
  props: {
    trend: { type: Array, default: () => [] },
    typeDistribution: { type: Array, default: () => [] },
    defectTop: { type: Array, default: () => [] },
    typeVolume: { type: Array, default: () => [] },
    typeStats: { type: Object, default: () => ({}) },
    defectLevels: { type: Array, default: () => [] },
    activityFeed: { type: Array, default: () => [] },
    recentFinished: { type: Array, default: () => [] },
    pendingDisposition: { type: Array, default: () => [] },
    activeTypeFilter: { type: String, default: "all" },
  },
  emits: ["workbench", "records", "config", "batch-trace", "inspect", "filter-type"],
  methods: {
    onDispositionInspect(row) {
      this.$emit("inspect", row);
    },
    onActivityOpen(item) {
      this.$emit("inspect", item);
    },
  },
};
</script>
