<template>
  <div ref="stripRoot" class="qc-chart-strip">
    <QcChartCard title="近7日合格率" :kpi="trendKpi" icon="📈" accent="blue">
      <div ref="trendRef" class="qc-chart-card__canvas" />
    </QcChartCard>
    <QcChartCard title="检验类型占比" :kpi="typeTotalKpi" icon="🎯" accent="cyan">
      <div ref="pieRef" class="qc-chart-card__canvas" />
    </QcChartCard>
    <QcChartCard title="不良原因 TOP" :kpi="defectKpi" icon="⚠" accent="red">
      <div ref="barRef" class="qc-chart-card__canvas" />
    </QcChartCard>
    <QcChartCard title="各类型检验量" :kpi="volumeKpi" icon="📦" accent="green">
      <div ref="volRef" class="qc-chart-card__canvas" />
    </QcChartCard>
  </div>
</template>

<script>
import * as echarts from "echarts";
import QcChartCard from "./QcChartCard.vue";

const TYPE_SHORT = { IQC: "IQC", PQC: "PQC", OQC: "OQC", RQC: "RQC" };
const LEVEL_COLOR = { CR: "#dc2626", MAJ: "#ea580c", MIN: "#ca8a04" };

export default {
  name: "QcChartStrip",
  components: { QcChartCard },
  props: {
    trend: { type: Array, default: () => [] },
    typeDistribution: { type: Array, default: () => [] },
    defectTop: { type: Array, default: () => [] },
    typeVolume: { type: Array, default: () => [] },
  },
  data() {
    return { charts: [], resizeObserver: null };
  },
  computed: {
    trendKpi() {
      const rows = this.trend || [];
      if (!rows.length) return "";
      const last = rows[rows.length - 1];
      return `今日 ${last.passRate ?? 0}%`;
    },
    typeTotalKpi() {
      const total = (this.typeDistribution || []).reduce((s, r) => s + (r.cnt || 0), 0);
      return total ? `共 ${total} 批` : "";
    },
    defectKpi() {
      const rows = this.defectTop || [];
      if (!rows.length) return "";
      return `TOP ${Math.min(rows.length, 10)}`;
    },
    volumeKpi() {
      const rows = this.typeVolume || [];
      const pass = rows.reduce((s, r) => s + (r.passCnt || 0), 0);
      const reject = rows.reduce((s, r) => s + (r.rejectCnt || 0), 0);
      if (!pass && !reject) return "";
      return `合格 ${pass} / 不合格 ${reject}`;
    },
  },
  watch: {
    trend: { deep: true, handler() { this.$nextTick(() => this.renderAll()); } },
    typeDistribution: { deep: true, handler() { this.$nextTick(() => this.renderAll()); } },
    defectTop: { deep: true, handler() { this.$nextTick(() => this.renderAll()); } },
    typeVolume: { deep: true, handler() { this.$nextTick(() => this.renderAll()); } },
  },
  mounted() {
    this.$nextTick(() => {
      this.renderAll();
      this.bindResizeObserver();
    });
    window.addEventListener("resize", this.onResize);
  },
  beforeUnmount() {
    window.removeEventListener("resize", this.onResize);
    this.resizeObserver?.disconnect();
    this.charts.forEach((c) => c?.dispose());
  },
  methods: {
    bindResizeObserver() {
      if (typeof ResizeObserver === "undefined" || !this.$refs.stripRoot) return;
      this.resizeObserver = new ResizeObserver(() => this.onResize());
      this.resizeObserver.observe(this.$refs.stripRoot);
    },
    onResize() {
      this.charts.forEach((c) => c?.resize());
    },
    initChart(refName) {
      const el = this.$refs[refName];
      if (!el) return null;
      const chart = echarts.getInstanceByDom(el) || echarts.init(el);
      if (!this.charts.includes(chart)) this.charts.push(chart);
      return chart;
    },
    renderAll() {
      this.renderTrend();
      this.renderPie();
      this.renderBar();
      this.renderVolume();
      this.$nextTick(() => this.onResize());
    },
    renderTrend() {
      const chart = this.initChart("trendRef");
      if (!chart) return;
      const rows = this.trend || [];
      const data = rows.map((r) => r.passRate ?? 0);
      const lastIdx = data.length - 1;
      chart.setOption({
        tooltip: { trigger: "axis", formatter: (p) => `${p[0]?.name}<br/>合格率 ${p[0]?.value}%` },
        grid: { left: 36, right: 8, bottom: 22, top: 8 },
        xAxis: {
          type: "category",
          data: rows.map((r) => String(r.dayLabel || "").slice(5, 10)),
          axisLabel: { fontSize: 12, color: "#64748b" },
          axisLine: { lineStyle: { color: "#e2e8f0" } },
        },
        yAxis: {
          type: "value",
          min: 0,
          max: 100,
          splitNumber: 4,
          axisLabel: { formatter: "{value}%", fontSize: 12, color: "#94a3b8" },
          splitLine: { lineStyle: { color: "#f1f5f9" } },
        },
        series: [
          {
            name: "目标",
            type: "line",
            data: rows.map(() => 95),
            symbol: "none",
            lineStyle: { type: "dashed", color: "#94a3b8", width: 1 },
            silent: true,
          },
          {
            name: "合格率",
            type: "line",
            smooth: true,
            data: data.map((v, i) => ({
              value: v,
              itemStyle: i === lastIdx ? { color: "#2563eb", borderWidth: 2, borderColor: "#fff" } : { color: "#2563eb" },
              symbolSize: i === lastIdx ? 8 : 4,
            })),
            areaStyle: {
              color: new echarts.graphic.LinearGradient(0, 0, 0, 1, [
                { offset: 0, color: "rgba(37, 99, 235, 0.25)" },
                { offset: 1, color: "rgba(37, 99, 235, 0.02)" },
              ]),
            },
            lineStyle: { color: "#2563eb", width: 2 },
          },
        ],
      }, true);
    },
    renderPie() {
      const chart = this.initChart("pieRef");
      if (!chart) return;
      const rows = this.typeDistribution || [];
      const total = rows.reduce((s, r) => s + (r.cnt || 0), 0);
      chart.setOption({
        tooltip: { trigger: "item" },
        color: ["#2563eb", "#0891b2", "#059669", "#6366f1"],
        graphic: total ? [{
          type: "text",
          left: "center",
          top: "42%",
          style: { text: String(total), fill: "#0f172a", fontSize: 18, fontWeight: 700, textAlign: "center" },
        }, {
          type: "text",
          left: "center",
          top: "52%",
          style: { text: "批次", fill: "#94a3b8", fontSize: 12, textAlign: "center" },
        }] : [],
        series: [{
          type: "pie",
          radius: ["48%", "72%"],
          center: ["50%", "50%"],
          data: rows.map((r) => ({ name: TYPE_SHORT[r.qcType] || r.qcType, value: r.cnt || 0 })),
          label: { show: false },
          labelLine: { show: false },
        }],
      }, true);
    },
    renderBar() {
      const chart = this.initChart("barRef");
      if (!chart) return;
      const rows = (this.defectTop || []).slice(0, 8);
      chart.setOption({
        tooltip: { trigger: "axis", axisPointer: { type: "shadow" } },
        grid: { left: 4, right: 28, bottom: 4, top: 4, containLabel: true },
        xAxis: { type: "value", minInterval: 1, axisLabel: { fontSize: 12 }, splitLine: { show: false } },
        yAxis: {
          type: "category",
          data: rows.map((r) => r.defectName).reverse(),
          axisLabel: { fontSize: 12, width: 64, overflow: "truncate" },
          axisLine: { show: false },
          axisTick: { show: false },
        },
        series: [{
          type: "bar",
          data: rows.map((r) => ({
            value: r.cnt,
            itemStyle: { color: LEVEL_COLOR[r.defectLevel] || "#dc2626", borderRadius: [0, 3, 3, 0] },
          })).reverse(),
          barWidth: 10,
        }],
      }, true);
    },
    renderVolume() {
      const chart = this.initChart("volRef");
      if (!chart) return;
      const order = ["IQC", "PQC", "OQC", "RQC"];
      const rows = order.map((t) => (this.typeVolume || []).find((r) => r.qcType === t) || { qcType: t, passCnt: 0, rejectCnt: 0 });
      chart.setOption({
        tooltip: { trigger: "axis", axisPointer: { type: "shadow" } },
        legend: { show: false },
        grid: { left: 32, right: 8, bottom: 22, top: 8 },
        xAxis: {
          type: "category",
          data: order,
          axisLabel: { fontSize: 12, color: "#64748b" },
        },
        yAxis: { type: "value", minInterval: 1, axisLabel: { fontSize: 12 }, splitLine: { lineStyle: { color: "#f1f5f9" } } },
        series: [
          { name: "合格", type: "bar", stack: "v", data: rows.map((r) => r.passCnt || 0), itemStyle: { color: "#059669", borderRadius: [0, 0, 0, 0] }, barWidth: 14 },
          { name: "不合格", type: "bar", stack: "v", data: rows.map((r) => r.rejectCnt || 0), itemStyle: { color: "#dc2626", borderRadius: [3, 3, 0, 0] }, barWidth: 14 },
        ],
      }, true);
    },
  },
};
</script>
