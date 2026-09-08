<template>
  <div class="andon-chart-card andon-chart-card--amber">
    <div class="andon-chart-card__head">
      <strong>本班趋势</strong>
      <span class="andon-chart-card__sub">发起 / 关闭</span>
    </div>
    <div ref="chartRef" class="andon-chart-card__body" v-loading="loading" />
  </div>
</template>

<script>
import * as echarts from "echarts";

export default {
  name: "ShiftTrendSpark",
  props: {
    trend: { type: Object, default: () => ({}) },
    loading: { type: Boolean, default: false },
  },
  data() {
    return { chart: null };
  },
  watch: {
    trend: {
      deep: true,
      handler() {
        this.$nextTick(() => this.render());
      },
    },
  },
  mounted() {
    this.render();
    window.addEventListener("resize", this.onResize);
  },
  beforeUnmount() {
    window.removeEventListener("resize", this.onResize);
    this.chart?.dispose();
  },
  methods: {
    onResize() {
      this.chart?.resize();
    },
    render() {
      if (!this.$refs.chartRef) return;
      this.chart = echarts.getInstanceByDom(this.$refs.chartRef) || echarts.init(this.$refs.chartRef);
      const hours = this.trend.hours || [];
      const created = this.trend.created || [];
      const closed = this.trend.closed || [];
      this.chart.setOption({
        tooltip: { trigger: "axis" },
        legend: {
          data: ["发起", "关闭"],
          bottom: 2,
          left: "center",
          itemWidth: 14,
          itemHeight: 8,
          itemGap: 16,
          textStyle: { fontSize: 10, color: "#64748b" },
        },
        grid: { left: 36, right: 12, top: 12, bottom: 40 },
        xAxis: { type: "category", data: hours.map((h) => `${h}:00`), axisLabel: { fontSize: 10, margin: 8 } },
        yAxis: { type: "value", minInterval: 1, axisLabel: { fontSize: 10 } },
        series: [
          { name: "发起", type: "line", smooth: true, data: created, itemStyle: { color: "#f59e0b" }, areaStyle: { color: "rgba(245,158,11,0.15)" } },
          { name: "关闭", type: "line", smooth: true, data: closed, itemStyle: { color: "#22c55e" }, areaStyle: { color: "rgba(34,197,94,0.12)" } },
        ],
      });
    },
  },
};
</script>
