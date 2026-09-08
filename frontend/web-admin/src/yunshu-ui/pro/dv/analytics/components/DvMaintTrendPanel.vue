<template>
  <div class="dv-analytics-panel-card dv-analytics-block">
    <div class="dv-analytics-block-head qc-analytics-bottom-panel__head">
      <span class="qc-analytics-bottom-panel__title">近 30 天维保工单</span>
      <span class="qc-analytics-bottom-panel__meta">点检 + 保养 + 维修</span>
    </div>
    <div class="dv-analytics-panel-card__body">
      <div v-if="!trendRows.length" class="dv-activity-feed__empty">暂无趋势数据</div>
      <div v-else class="dv-trend-bars">
        <div v-for="b in trendRows" :key="b.dayLabel" class="dv-trend-bars__item">
          <span>{{ dayLabel(b.dayLabel) }}</span>
          <i :style="{ height: barHeight(b.total) + 'px' }" />
          <em>{{ b.total }}</em>
        </div>
      </div>
    </div>
  </div>
</template>

<script>
export default {
  name: "DvMaintTrendPanel",
  props: {
    rows: { type: Array, default: () => [] },
  },
  computed: {
    trendRows() {
      return this.rows || [];
    },
    trendMax() {
      return Math.max(1, ...this.trendRows.map((r) => Number(r.total) || 0));
    },
  },
  methods: {
    dayLabel(v) {
      if (!v) return "—";
      return String(v).slice(5);
    },
    barHeight(total) {
      return Math.max(4, Math.round(((Number(total) || 0) / this.trendMax) * 56));
    },
  },
};
</script>
