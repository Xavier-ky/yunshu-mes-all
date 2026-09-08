<template>
  <div class="dv-analytics-kpi-bar">
    <div
      v-for="(item, idx) in cards"
      :key="item.key"
      class="dv-analytics-kpi-bar__item"
      :class="{ 'is-last': idx === cards.length - 1 }"
    >
      <span class="dv-analytics-kpi-bar__label">{{ item.label }}</span>
      <strong class="dv-analytics-kpi-bar__value" :class="item.tone">{{ item.display }}</strong>
    </div>
  </div>
</template>

<script>
export default {
  name: "DvAnalyticsKpiStrip",
  props: {
    overview: { type: Object, default: () => ({}) },
    summary: { type: Object, default: () => ({}) },
  },
  computed: {
    cards() {
      const o = this.overview || {};
      const s = this.summary || {};
      return [
        { key: "machinery", label: "设备总数", display: o.machineryTotal ?? s.machineryTotal ?? 0, tone: "tone-blue" },
        { key: "fault", label: "故障设备", display: o.faultCount ?? s.faultCount ?? 0, tone: "tone-red" },
        { key: "pending", label: "待处理合计", display: o.pendingTotal ?? 0, tone: "tone-amber" },
        { key: "check", label: "待点检", display: s.pendingCheck ?? 0, tone: "tone-blue" },
        { key: "mainten", label: "待保养", display: s.pendingMainten ?? 0, tone: "tone-green" },
        { key: "oee", label: "平均 OEE", display: `${o.avgOee ?? s.avgOee ?? 0}%`, tone: "tone-green" },
      ];
    },
  },
};
</script>
