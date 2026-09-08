<template>
  <div class="dv-analytics-main">
    <DvAnalyticsActionRow
      @workbench="$emit('workbench')"
      @machinery="$emit('machinery')"
      @check-maint="$emit('check-maint')"
      @repairs="$emit('repairs')"
    />

    <DvAnalyticsKpiStrip :overview="overview" :summary="summary" />

    <div class="dv-analytics-section dv-analytics-section--charts">
      <DvOeeCompactPanel :rows="oeeRows" :loading="oeeLoading" />
      <div class="dv-analytics-charts-right">
        <DvStatusGridPanel :rows="statusRows" />
        <DvMaintTrendPanel :rows="trendRows" />
      </div>
    </div>

    <div class="dv-analytics-section dv-analytics-section--middle">
      <DvQueueInsightRow :summary="summary" @queue="$emit('queue', $event)" />
      <DvActivityFeed :items="recentFinished" @open="$emit('activity-open', $event)" />
    </div>

    <div class="dv-analytics-section dv-analytics-section--bottom">
      <DvRecentDoneStrip
        :rows="recentFinished"
        :loading="loading"
        embedded
        show-head
        @open-records="$emit('records')"
      />
      <DvPendingTasksTable
        :rows="pendingRows"
        :loading="pendingLoading"
        @inspect="$emit('inspect', $event)"
      />
    </div>
  </div>
</template>

<script>
import DvAnalyticsActionRow from "./DvAnalyticsActionRow.vue";
import DvAnalyticsKpiStrip from "./DvAnalyticsKpiStrip.vue";
import DvOeeCompactPanel from "./DvOeeCompactPanel.vue";
import DvStatusGridPanel from "./DvStatusGridPanel.vue";
import DvMaintTrendPanel from "./DvMaintTrendPanel.vue";
import DvQueueInsightRow from "./DvQueueInsightRow.vue";
import DvActivityFeed from "./DvActivityFeed.vue";
import DvPendingTasksTable from "./DvPendingTasksTable.vue";
import DvRecentDoneStrip from "../../workbench/components/DvRecentDoneStrip.vue";

export default {
  name: "DvAnalyticsMain",
  components: {
    DvAnalyticsActionRow,
    DvAnalyticsKpiStrip,
    DvOeeCompactPanel,
    DvStatusGridPanel,
    DvMaintTrendPanel,
    DvQueueInsightRow,
    DvActivityFeed,
    DvPendingTasksTable,
    DvRecentDoneStrip,
  },
  props: {
    loading: { type: Boolean, default: false },
    pendingLoading: { type: Boolean, default: false },
    oeeLoading: { type: Boolean, default: false },
    overview: { type: Object, default: () => ({}) },
    summary: { type: Object, default: () => ({}) },
    statusRows: { type: Array, default: () => [] },
    trendRows: { type: Array, default: () => [] },
    oeeRows: { type: Array, default: () => [] },
    pendingRows: { type: Array, default: () => [] },
    recentFinished: { type: Array, default: () => [] },
  },
  emits: ["workbench", "machinery", "check-maint", "repairs", "queue", "inspect", "records", "activity-open"],
};
</script>
