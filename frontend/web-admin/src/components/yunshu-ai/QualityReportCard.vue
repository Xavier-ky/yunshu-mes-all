<template>
  <section class="quality-report-card" aria-label="质量分析报告">
    <header class="report-head">
      <div>
        <p class="report-kicker">MES · 实时质量快照</p>
        <h3>{{ report.title || "质量分析报告" }}</h3>
        <p class="report-meta">最近 {{ report.window_days || 7 }} 天 · {{ formatTime(report.queried_at) }}</p>
      </div>
      <span class="report-live"><i></i> 实时数据</span>
    </header>

    <div class="report-kpis">
      <div v-for="item in kpis" :key="item.label" class="report-kpi">
        <span>{{ item.label }}</span><strong>{{ item.value }}</strong>
      </div>
    </div>

    <section v-for="section in report.sections || []" :key="section.id" class="report-section">
      <h4>{{ section.title }}</h4>
      <p>{{ section.content || "正在整理分析内容…" }}<em v-if="section.streaming" aria-label="正在输出">▍</em></p>
    </section>

    <section v-if="(report.charts || []).length" class="report-section report-figures">
      <h4>质量数据图表</h4>
      <div class="chart-grid">
        <figure v-for="chart in report.charts" :key="chart.id" class="report-chart">
          <figcaption>{{ chart.title }}</figcaption>
          <div :ref="(el) => setChartEl(chart.id, el)" class="chart-canvas"></div>
          <p v-if="chartEmpty(chart)" class="chart-empty">当前统计窗口无可用记录</p>
        </figure>
      </div>
    </section>

    <section v-for="table in report.tables || []" :key="table.id" class="report-section report-table-wrap">
      <h4>{{ table.title }}</h4>
      <div class="report-table-scroll">
        <table>
          <thead><tr><th v-for="column in table.columns || []" :key="column">{{ column }}</th></tr></thead>
          <tbody>
            <tr v-for="(row, index) in table.rows || []" :key="index"><td v-for="(value, cell) in row" :key="cell">{{ value }}</td></tr>
            <tr v-if="!(table.rows || []).length"><td :colspan="Math.max((table.columns || []).length, 1)">当前统计窗口无可用记录</td></tr>
          </tbody>
        </table>
      </div>
    </section>

    <footer v-if="canExport" class="report-export">
      <span>{{ report.export_message || "报告已完成。是否需要导出 Word？" }}</span>
      <button type="button" :disabled="exporting" @click="$emit('export', report.report_id)">
        <span v-if="exporting" class="spin"></span><span v-else>⇩</span>{{ exporting ? "正在导出" : report.status === "EXPORTED" ? "再次导出 Word" : "导出 Word" }}
      </button>
    </footer>
  </section>
</template>

<script setup>
import { computed, nextTick, onBeforeUnmount, watch } from "vue";
import * as echarts from "echarts";

const props = defineProps({ report: { type: Object, required: true }, exporting: Boolean });
defineEmits(["export"]);
const chartElements = new Map();
const chartInstances = new Map();

const kpis = computed(() => {
  const summary = props.report?.facts?.summary || {};
  return [
    { label: "待检任务", value: number(summary.pendingCount) },
    { label: "今日完成", value: number(summary.todayFinished) },
    { label: "累计合格率", value: `${decimal(summary.passRate)}%` },
    { label: "待处置", value: number(summary.pendingDisposition) },
  ];
});
const canExport = computed(() => props.report?.export_ready || ["READY", "EXPORTED"].includes(props.report?.status));

function number(value) { return Number(value || 0).toLocaleString("zh-CN"); }
function decimal(value) { return Number(value || 0).toFixed(1); }
function formatTime(value) { return value ? String(value).replace("T", " ").replace("Z", "") : "刚刚"; }
function setChartEl(id, element) { if (element) chartElements.set(id, element); else chartElements.delete(id); }
function chartEmpty(chart) {
  if (chart.kind === "pie") return !(chart.items || []).some((item) => Number(item.value || 0) > 0);
  return !(chart.values || chart.series?.["检验量"] || []).some((value) => Number(value || 0) > 0);
}
function optionFor(chart) {
  const base = { animationDuration: 320, animationEasing: "cubicOut", textStyle: { fontFamily: "Microsoft YaHei, sans-serif" } };
  if (chart.kind === "trend") return {
    ...base, grid: { left: 38, right: 38, top: 24, bottom: 31 }, tooltip: { trigger: "axis" }, legend: { top: 0, textStyle: { color: "#72809a" } },
    xAxis: { type: "category", data: chart.labels || [], axisLine: { lineStyle: { color: "#d9e3f2" } }, axisLabel: { color: "#8b97aa", fontSize: 10 } },
    yAxis: [{ type: "value", name: "检验量", splitLine: { lineStyle: { color: "#eef3fa" } }, axisLabel: { color: "#8b97aa" } }, { type: "value", name: "%", min: 0, max: 100, axisLabel: { color: "#8b97aa", formatter: "{value}%" } }],
    series: [
      { name: "检验量", type: "bar", data: chart.series?.["检验量"] || [], itemStyle: { color: "#8daee9", borderRadius: [5, 5, 0, 0] }, barMaxWidth: 26 },
      { name: "合格率", type: "line", yAxisIndex: 1, data: chart.series?.["合格率"] || [], smooth: true, symbolSize: 6, lineStyle: { color: "#edaa79", width: 2.5 }, itemStyle: { color: "#edaa79" } },
    ],
  };
  if (chart.kind === "pie") return {
    ...base, tooltip: { trigger: "item", formatter: "{b}：{c}" }, legend: { bottom: 0, textStyle: { color: "#7b8799", fontSize: 10 } },
    series: [{ type: "pie", radius: ["44%", "70%"], center: ["50%", "45%"], label: { show: false }, data: (chart.items || []).map((item) => ({ name: item.name, value: item.value, itemStyle: { color: item.color } })) }],
  };
  return {
    ...base, grid: { left: 30, right: 18, top: 22, bottom: 42, containLabel: true }, tooltip: { trigger: "axis", axisPointer: { type: "shadow" } },
    xAxis: { type: "category", data: chart.labels || [], axisLabel: { color: "#7e8ba0", fontSize: 10, interval: 0, rotate: 20 }, axisLine: { lineStyle: { color: "#d9e3f2" } } },
    yAxis: { type: "value", splitLine: { lineStyle: { color: "#eef3fa" } }, axisLabel: { color: "#8b97aa" } },
    series: [{ type: "bar", data: (chart.values || []).map((value, index) => ({ value, itemStyle: { color: chart.colors?.[index] || "#8daee9", borderRadius: [5, 5, 0, 0] } })), barMaxWidth: 32 }],
  };
}
async function renderCharts() {
  await nextTick();
  for (const chart of props.report?.charts || []) {
    const element = chartElements.get(chart.id);
    if (!element) continue;
    let instance = chartInstances.get(chart.id);
    if (!instance) { instance = echarts.init(element, null, { renderer: "canvas" }); chartInstances.set(chart.id, instance); }
    instance.setOption(optionFor(chart), true);
    instance.resize();
  }
}
watch(() => props.report?.charts, renderCharts, { deep: true, immediate: true });
onBeforeUnmount(() => chartInstances.forEach((chart) => chart.dispose()));
</script>

<style scoped>
.quality-report-card{margin-top:4px;padding:20px;border:1px solid rgba(173,194,232,.72);border-radius:22px;background:linear-gradient(145deg,rgba(255,255,255,.98),rgba(244,248,255,.94) 58%,rgba(255,249,245,.92));box-shadow:0 14px 34px rgba(90,118,170,.11);color:#314462}.report-head{display:flex;align-items:flex-start;justify-content:space-between;gap:12px;border-bottom:1px solid #e6edf8;padding-bottom:14px}.report-kicker{margin:0 0 5px;color:#7c91b9;font-size:11px;letter-spacing:.08em}.report-head h3{margin:0;font-size:20px;letter-spacing:.02em;color:#405f9d}.report-meta{margin:6px 0 0;font-size:12px;color:#8996aa}.report-live{flex:0 0 auto;padding:6px 9px;border-radius:999px;background:#edf7f1;color:#5b9475;font-size:11px}.report-live i{display:inline-block;width:6px;height:6px;border-radius:50%;margin-right:4px;background:#76c895}.report-kpis{display:grid;grid-template-columns:repeat(4,minmax(0,1fr));gap:9px;margin:15px 0}.report-kpi{padding:11px 10px;border-radius:14px;background:rgba(245,248,255,.8);border:1px solid rgba(219,229,247,.8)}.report-kpi span{display:block;color:#8795aa;font-size:11px}.report-kpi strong{display:block;margin-top:5px;color:#425f98;font-size:18px}.report-section{margin-top:17px}.report-section h4{margin:0 0 8px;font-size:14px;color:#506da4}.report-section p{margin:0;white-space:pre-wrap;line-height:1.82;font-size:13px;color:#42536f}.report-section em{font-style:normal;color:#7596d3;animation:blink .9s ease-in-out infinite}.chart-grid{display:grid;grid-template-columns:repeat(2,minmax(0,1fr));gap:12px}.report-chart{position:relative;min-width:0;margin:0;padding:11px;border:1px solid #e5ecf8;border-radius:15px;background:rgba(255,255,255,.7)}.report-chart figcaption{font-size:12px;color:#657896}.chart-canvas{height:205px;width:100%;opacity:0;animation:chart-in .32s ease-out forwards}.chart-empty{position:absolute;left:0;right:0;top:52%;text-align:center;color:#9ba7ba;font-size:12px}.report-table-scroll{overflow:auto;border:1px solid #e5ecf8;border-radius:13px;background:#fff}.report-table-scroll table{width:100%;border-collapse:collapse;min-width:430px;font-size:12px}.report-table-scroll th{padding:9px;background:#f0f5fd;color:#61759b;text-align:left;font-weight:600}.report-table-scroll td{padding:9px;color:#53657e;border-top:1px solid #edf1f7}.report-table-scroll tbody tr:nth-child(even){background:#fbfcff}.report-export{display:flex;align-items:center;justify-content:space-between;gap:14px;margin-top:19px;padding:13px 14px;border-radius:14px;background:linear-gradient(90deg,#eff5ff,#f8f4ff);color:#607393;font-size:13px}.report-export button{display:inline-flex;align-items:center;gap:6px;border:0;border-radius:10px;padding:8px 12px;background:linear-gradient(135deg,#7599dc,#8299d8);color:#fff;cursor:pointer;box-shadow:0 5px 12px rgba(94,126,190,.22)}.report-export button:disabled{opacity:.7;cursor:wait}.spin{width:12px;height:12px;border:2px solid rgba(255,255,255,.45);border-top-color:#fff;border-radius:50%;animation:spin .75s linear infinite}@keyframes chart-in{to{opacity:1}}@keyframes blink{50%{opacity:.25}}@keyframes spin{to{transform:rotate(360deg)}}@media(max-width:720px){.report-kpis,.chart-grid{grid-template-columns:repeat(2,minmax(0,1fr))}.report-head{display:block}.report-live{display:inline-block;margin-top:8px}.report-export{align-items:flex-start;flex-direction:column}}
</style>
