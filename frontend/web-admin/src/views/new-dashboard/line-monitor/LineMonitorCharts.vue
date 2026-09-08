<template>
  <div class="charts-wrap">
    <div v-if="mode === 'overview'" class="chart-row">
      <div class="chart-panel">
        <h4>各产线 OEE 对比</h4>
        <div ref="oeeCompareRef" class="chart-box"></div>
      </div>
      <div class="chart-panel alerts-panel">
        <h4>全厂开放安灯</h4>
        <div v-if="alerts.length" class="alert-list">
          <div v-for="a in alerts" :key="a.andonId" class="alert-item">
            <span class="alert-line">{{ a.lineName }}</span>
            <span class="alert-msg">{{ a.message }}</span>
            <span class="alert-time mono">{{ a.occurTime }}</span>
          </div>
        </div>
        <div v-else class="chart-empty">暂无开放安灯</div>
      </div>
    </div>

    <div v-else class="chart-row detail-row">
      <div class="chart-panel">
        <h4>今日小时产量</h4>
        <div ref="hourlyRef" class="chart-box"></div>
      </div>
      <div class="chart-panel">
        <h4>OEE 分解<span v-if="oee?.estimated" class="est-tag">估算</span></h4>
        <div ref="oeeDetailRef" class="chart-box"></div>
      </div>
      <div class="chart-panel events-panel">
        <h4>最近事件</h4>
        <div v-if="events.length" class="event-list">
          <div v-for="(ev, i) in events" :key="i" class="event-item">
            <span class="ev-type" :class="ev.eventType === 'ANDON' ? 'ev-andon' : ''">{{ ev.eventType }}</span>
            <span class="ev-msg">{{ ev.message }}</span>
            <span class="ev-time mono">{{ formatTime(ev.eventTime) }}</span>
          </div>
        </div>
        <div v-else class="chart-empty">暂无事件</div>
      </div>
    </div>
  </div>
</template>

<script setup>
import { ref, watch, onMounted, onBeforeUnmount, nextTick } from "vue";
import * as echarts from "echarts";

const props = defineProps({
  mode: { type: String, default: "overview" },
  lines: { type: Array, default: () => [] },
  alerts: { type: Array, default: () => [] },
  hourlyOutput: { type: Array, default: () => [] },
  oee: { type: Object, default: null },
  events: { type: Array, default: () => [] },
});

const COLORS = ["#059669", "#d97706", "#dc2626", "#2563eb", "#6b7280"];

const oeeCompareRef = ref(null);
const hourlyRef = ref(null);
const oeeDetailRef = ref(null);
let charts = [];

function formatTime(t) {
  if (!t) return "—";
  const s = String(t);
  return s.length > 16 ? s.slice(11, 16) : s;
}

function disposeCharts() {
  charts.forEach((c) => c?.dispose());
  charts = [];
}

function initOverview() {
  if (!oeeCompareRef.value) return;
  const chart = echarts.init(oeeCompareRef.value);
  charts.push(chart);
  const names = props.lines.map((l) => l.lineName);
  const values = props.lines.map((l) => Number(l.oee?.toFixed(1) || 0));
  chart.setOption({
    color: COLORS,
    grid: { left: 48, right: 16, top: 24, bottom: 32 },
    xAxis: { type: "category", data: names, axisLabel: { fontSize: 11, color: "#666" } },
    yAxis: { type: "value", max: 100, axisLabel: { formatter: "{value}%", color: "#666" } },
    series: [{
      type: "bar",
      data: values.map((v, i) => ({
        value: v,
        itemStyle: { color: props.lines[i]?.oeeEstimated ? "#d97706" : "#059669" },
      })),
      barMaxWidth: 48,
    }],
    tooltip: { trigger: "axis" },
  });
}

function initDetail() {
  if (hourlyRef.value) {
    const chart = echarts.init(hourlyRef.value);
    charts.push(chart);
    const hours = props.hourlyOutput.map((p) => `${p.hour}:00`);
    const qty = props.hourlyOutput.map((p) => p.qty);
    chart.setOption({
      color: ["#2563eb"],
      grid: { left: 40, right: 16, top: 24, bottom: 28 },
      xAxis: { type: "category", data: hours, axisLabel: { fontSize: 10, color: "#666" } },
      yAxis: { type: "value", axisLabel: { color: "#666" } },
      series: [{ type: "line", data: qty, smooth: true, areaStyle: { opacity: 0.08 } }],
      tooltip: { trigger: "axis" },
    });
  }

  if (oeeDetailRef.value && props.oee) {
    const chart = echarts.init(oeeDetailRef.value);
    charts.push(chart);
    chart.setOption({
      color: COLORS,
      series: [{
        type: "pie",
        radius: ["42%", "68%"],
        label: { fontSize: 11 },
        data: [
          { name: "可用率", value: props.oee.availability },
          { name: "表现率", value: props.oee.performance },
          { name: "质量率", value: props.oee.quality },
          { name: "OEE", value: props.oee.oee },
        ],
      }],
      tooltip: { trigger: "item", formatter: "{b}: {c}%" },
    });
  }
}

function render() {
  disposeCharts();
  nextTick(() => {
    if (props.mode === "overview") initOverview();
    else initDetail();
  });
}

function handleResize() {
  charts.forEach((c) => c?.resize());
}

watch(
  () => [props.mode, props.lines, props.hourlyOutput, props.oee, props.alerts],
  render,
  { deep: true }
);

onMounted(() => {
  render();
  window.addEventListener("resize", handleResize);
});

onBeforeUnmount(() => {
  window.removeEventListener("resize", handleResize);
  disposeCharts();
});
</script>

<style scoped>
.charts-wrap {
  margin-top: 12px;
}
.chart-row {
  display: grid;
  grid-template-columns: 1fr 1fr;
  gap: 12px;
}
.detail-row {
  grid-template-columns: 1fr 1fr 1fr;
}
.chart-panel {
  background: #fff;
  border: 1px solid #e7e7e7;
  padding: 12px 14px;
}
.chart-panel h4 {
  margin: 0 0 8px;
  font-size: 13px;
  font-weight: 600;
  color: #1a1a1a;
}
.chart-box {
  height: 200px;
}
.est-tag {
  font-size: 10px;
  color: #d97706;
  font-weight: 400;
  margin-left: 6px;
}
.alert-list,
.event-list {
  max-height: 200px;
  overflow-y: auto;
  display: grid;
  gap: 6px;
}
.alert-item,
.event-item {
  display: grid;
  grid-template-columns: 100px 1fr auto;
  gap: 8px;
  font-size: 12px;
  padding: 6px 0;
  border-bottom: 1px solid #f5f5f5;
}
.alert-line {
  color: #2563eb;
  font-weight: 500;
}
.alert-msg,
.ev-msg {
  color: #444;
}
.alert-time,
.ev-time {
  color: #999;
  font-size: 11px;
}
.ev-type {
  font-size: 10px;
  color: #059669;
  font-weight: 600;
}
.ev-type.ev-andon {
  color: #dc2626;
}
.chart-empty {
  color: #ccc;
  text-align: center;
  padding: 40px 0;
  font-size: 13px;
}
.mono {
  font-family: "JetBrains Mono", monospace;
}
@media (max-width: 960px) {
  .chart-row,
  .detail-row {
    grid-template-columns: 1fr;
  }
}
</style>
