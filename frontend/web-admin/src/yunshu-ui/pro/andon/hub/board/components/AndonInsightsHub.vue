<template>
  <div class="andon-insights-main">
    <div class="andon-analytics-section andon-analytics-section--category">
      <AndonCategoryInsightRow
        :alerts="activeAlerts"
        :active-key="categoryFilter"
        @filter="$emit('filter', $event)"
      />
    </div>

    <div class="andon-analytics-section andon-analytics-section--middle">
      <ShiftTrendSpark :trend="trend" :loading="loadingTrend" />
      <AndonSlaPanel :alerts="activeAlerts" />
    </div>

    <div class="andon-analytics-section andon-analytics-section--bottom">
      <ActivityTimeline :items="timeline" :loading="loadingTimeline" @select="$emit('timeline-select', $event)" />
      <AndonReasonTopPanel :stats="analyticsStats" />
    </div>
  </div>
</template>

<script>
import AndonCategoryInsightRow from "./AndonCategoryInsightRow.vue";
import ShiftTrendSpark from "./ShiftTrendSpark.vue";
import AndonSlaPanel from "./AndonSlaPanel.vue";
import ActivityTimeline from "./ActivityTimeline.vue";
import AndonReasonTopPanel from "./AndonReasonTopPanel.vue";

export default {
  name: "AndonInsightsHub",
  components: {
    AndonCategoryInsightRow,
    ShiftTrendSpark,
    AndonSlaPanel,
    ActivityTimeline,
    AndonReasonTopPanel,
  },
  props: {
    activeAlerts: { type: Array, default: () => [] },
    categoryFilter: { type: String, default: "" },
    trend: { type: Object, default: () => ({}) },
    loadingTrend: { type: Boolean, default: false },
    timeline: { type: Array, default: () => [] },
    loadingTimeline: { type: Boolean, default: false },
    analyticsStats: { type: Object, default: () => ({}) },
  },
  emits: ["filter", "timeline-select"],
};
</script>
