<template>
  <div class="andon-analytics">
    <div class="chart-row">
      <div ref="pieRef" class="chart-box" />
      <div ref="barRef" class="chart-box" />
      <div ref="levelRef" class="chart-box" />
    </div>
    <el-table class="yunshu-data-table" stripe border :data="stats.reasonTop || []" max-height="280">
      <el-table-column type="index" width="50" align="center" />
      <el-table-column label="呼叫原因" prop="reason" min-width="200" show-overflow-tooltip />
      <el-table-column label="次数" prop="cnt" width="100" align="center" />
    </el-table>
  </div>
</template>

<script>
import * as echarts from "echarts";

export default {
  name: "AndonAnalyticsPanel",
  props: {
    stats: { type: Object, default: () => ({}) },
  },
  data() {
    return { charts: [] };
  },
  watch: {
    stats: {
      deep: true,
      handler() { this.$nextTick(() => this.renderCharts()); },
    },
  },
  mounted() {
    this.renderCharts();
    window.addEventListener("resize", this.onResize);
  },
  beforeUnmount() {
    window.removeEventListener("resize", this.onResize);
    this.charts.forEach((c) => c?.dispose());
  },
  methods: {
    onResize() { this.charts.forEach((c) => c?.resize()); },
    renderCharts() {
      const s = this.stats || {};
      this.renderPie(s);
      this.renderBar(s.reasonTop || []);
      this.renderLevel(s.levelTop || []);
    },
    renderPie(s) {
      if (!this.$refs.pieRef) return;
      const chart = echarts.getInstanceByDom(this.$refs.pieRef) || echarts.init(this.$refs.pieRef);
      if (!this.charts.includes(chart)) this.charts.push(chart);
      const dist = s.statusDistribution || [
        { status: "ACTIVE", cnt: s.activeRecords || 0 },
        { status: "HANDLED", cnt: s.handledRecords || 0 },
      ];
      chart.setOption({
        title: { text: "处置状态", left: "center", textStyle: { fontSize: 13 } },
        tooltip: { trigger: "item" },
        color: ["#f59e0b", "#10b981"],
        series: [{
          type: "pie",
          radius: ["42%", "68%"],
          data: dist.map((d) => ({ name: d.status === "ACTIVE" ? "待处置" : "已处置", value: d.cnt || 0 })),
        }],
      });
    },
    renderBar(rows) {
      if (!this.$refs.barRef) return;
      const chart = echarts.getInstanceByDom(this.$refs.barRef) || echarts.init(this.$refs.barRef);
      if (!this.charts.includes(chart)) this.charts.push(chart);
      chart.setOption({
        title: { text: "原因 TOP10", left: "center", textStyle: { fontSize: 13 } },
        tooltip: { trigger: "axis" },
        grid: { left: 40, right: 16, bottom: 60, top: 40 },
        xAxis: { type: "category", data: rows.map((r) => r.reason), axisLabel: { rotate: 30, fontSize: 10 } },
        yAxis: { type: "value", minInterval: 1 },
        series: [{ type: "bar", data: rows.map((r) => r.cnt), itemStyle: { color: "#409eff" } }],
      });
    },
    renderLevel(rows) {
      if (!this.$refs.levelRef) return;
      const chart = echarts.getInstanceByDom(this.$refs.levelRef) || echarts.init(this.$refs.levelRef);
      if (!this.charts.includes(chart)) this.charts.push(chart);
      chart.setOption({
        title: { text: "级别分布", left: "center", textStyle: { fontSize: 13 } },
        tooltip: { trigger: "axis" },
        grid: { left: 40, right: 16, bottom: 30, top: 40 },
        xAxis: { type: "category", data: rows.map((r) => r.level || r.andon_level) },
        yAxis: { type: "value", minInterval: 1 },
        series: [{ type: "bar", data: rows.map((r) => r.cnt), itemStyle: { color: "#e6a23c" } }],
      });
    },
  },
};
</script>

<style scoped>
.andon-analytics { display: flex; flex-direction: column; gap: 12px; min-height: 55vh; }
.chart-row { display: grid; grid-template-columns: repeat(3, 1fr); gap: 10px; }
.chart-box { height: 260px; border: 1px solid #ebeef5; border-radius: 4px; }
</style>
