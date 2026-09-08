<template>
  <div class="plan-kpi-bento">
    <div
      v-for="(block, bi) in layout"
      :key="block.key ?? bi"
      class="plan-kpi-tile"
      :class="tileClasses(block)"
    >
      <!-- Hero: donut -->
      <template v-if="block.type === 'hero-donut'">
        <div class="plan-kpi-hero-inner">
          <div class="plan-kpi-hero-text">
            <span>{{ block.label }}</span>
            <strong>{{ block.value }}<small v-if="block.unit">{{ block.unit }}</small></strong>
          </div>
          <div :ref="(el) => setChartRef(block.key ?? bi, el)" class="plan-kpi-chart-slot" />
        </div>
      </template>

      <!-- Hero: ring -->
      <template v-else-if="block.type === 'hero-ring'">
        <div class="plan-kpi-hero-inner">
          <div class="plan-kpi-hero-text">
            <span>{{ block.label }}</span>
            <strong>{{ block.displayValue ?? `${block.value}%` }}</strong>
          </div>
          <div :ref="(el) => setChartRef(block.key ?? bi, el)" class="plan-kpi-chart-slot" />
        </div>
      </template>

      <!-- Hero: gauge -->
      <template v-else-if="block.type === 'hero-gauge'">
        <div class="plan-kpi-hero-inner">
          <div class="plan-kpi-hero-text">
            <span>{{ block.label }}</span>
            <strong>{{ block.value }}<small>%</small></strong>
          </div>
          <div :ref="(el) => setChartRef(block.key ?? bi, el)" class="plan-kpi-gauge-slot" />
        </div>
      </template>

      <!-- Status dots -->
      <template v-else-if="block.type === 'status-dots'">
        <p v-if="block.title" class="plan-kpi-aux-title">{{ block.title }}</p>
        <div class="plan-kpi-dots">
          <div v-for="(dot, di) in block.dots || []" :key="di" class="plan-kpi-dot-row">
            <span class="dot" :style="{ background: dot.color || '#ccc' }" />
            <span class="lbl">{{ dot.label }}</span>
            <strong class="val">{{ dot.value }}</strong>
          </div>
        </div>
      </template>

      <!-- Mini bars -->
      <template v-else-if="block.type === 'mini-bars'">
        <p class="plan-kpi-aux-title">{{ block.title || "趋势" }}</p>
        <div :ref="(el) => setChartRef(block.key ?? bi, el)" class="plan-kpi-mini-chart" />
      </template>

      <!-- Connected strip -->
      <template v-else-if="block.type === 'connected'">
        <div
          v-for="(item, ii) in block.items || []"
          :key="item.key ?? ii"
          class="plan-kpi-connected-item"
          :class="item.tone ? `tone-${item.tone}` : ''"
        >
          <span>{{ item.label }}</span>
          <strong>
            {{ item.value }}<small v-if="item.unit">{{ item.unit }}</small>
          </strong>
          <span v-if="item.delta" class="plan-kpi-delta" :class="item.deltaDir">{{ item.delta }}</span>
        </div>
      </template>
    </div>
  </div>
</template>

<script setup>
import { onBeforeUnmount, onMounted, ref, watch } from "vue";
import * as echarts from "echarts";

const props = defineProps({
  layout: { type: Array, default: () => [] },
  accentColor: { type: String, default: "#0052d9" },
});

const chartEls = ref({});
const chartInstances = {};

function tileClasses(block) {
  const classes = [`span-${block.span || (block.type === "connected" ? 12 : 5)}`];
  if (block.type === "connected") classes.push("plan-kpi-connected");
  else if (String(block.type).startsWith("hero-")) classes.push("plan-kpi-hero");
  return classes;
}

function setChartRef(key, el) {
  if (el) chartEls.value[key] = el;
}

function buildOption(block) {
  const color = props.accentColor;

  if (block.type === "hero-donut") {
    const segments = block.segments?.length
      ? block.segments
      : [{ name: "暂无", value: 1 }];
    return {
      color: block.colors || ["#0052d9", "#0abf5b", "#ff9d00", "#e34d59"],
      series: [{
        type: "pie",
        radius: ["58%", "82%"],
        center: ["50%", "50%"],
        label: { show: false },
        data: segments,
      }],
    };
  }

  if (block.type === "hero-ring") {
    const pct = Math.min(100, Math.max(0, Number(block.value) || 0));
    return {
      series: [{
        type: "pie",
        radius: ["58%", "82%"],
        center: ["50%", "50%"],
        label: { show: false },
        data: [
          { value: pct, itemStyle: { color } },
          { value: 100 - pct, itemStyle: { color: "#eee" } },
        ],
      }],
    };
  }

  if (block.type === "hero-gauge") {
    const val = Math.min(100, Math.max(0, Number(block.value) || 0));
    return {
      series: [{
        type: "gauge",
        min: 0,
        max: 100,
        startAngle: 200,
        endAngle: -20,
        progress: { show: true, width: 8, itemStyle: { color } },
        axisLine: { lineStyle: { width: 8, color: [[1, "#eee"]] } },
        axisTick: { show: false },
        splitLine: { show: false },
        axisLabel: { show: false },
        pointer: { show: false },
        detail: { show: false },
        data: [{ value: val }],
      }],
    };
  }

  if (block.type === "mini-bars") {
    const horizontal = block.horizontal !== false;
    if (horizontal) {
      return {
        color: [color],
        grid: { left: 4, right: 12, top: 4, bottom: 4, containLabel: true },
        xAxis: { type: "value", show: false },
        yAxis: {
          type: "category",
          data: (block.labels || []).slice().reverse(),
          axisLine: { show: false },
          axisTick: { show: false },
          axisLabel: { fontSize: 10, color: "#888" },
        },
        series: [{
          type: "bar",
          data: (block.values || []).slice().reverse(),
          barWidth: 10,
          itemStyle: { borderRadius: [0, 2, 2, 0] },
          label: {
            show: true,
            position: "right",
            fontSize: 10,
            color: "#666",
            formatter: (p) => (block.valueSuffix ? `${p.value}${block.valueSuffix}` : p.value),
          },
        }],
      };
    }
    return {
      color: [color],
      grid: { left: 28, right: 8, top: 8, bottom: 20 },
      xAxis: {
        type: "category",
        data: block.labels || [],
        axisLine: { show: false },
        axisTick: { show: false },
        axisLabel: { fontSize: 10, color: "#888" },
      },
      yAxis: { type: "value", show: false, minInterval: 1 },
      series: [{ type: "bar", data: block.values || [], barWidth: 16 }],
    };
  }

  return null;
}

function renderCharts() {
  props.layout.forEach((block, bi) => {
    const key = block.key ?? bi;
    if (!["hero-donut", "hero-ring", "hero-gauge", "mini-bars"].includes(block.type)) return;
    const el = chartEls.value[key];
    if (!el) return;
    if (!chartInstances[key]) chartInstances[key] = echarts.init(el);
    const opt = buildOption(block);
    if (opt) chartInstances[key].setOption(opt, true);
  });
}

function resizeCharts() {
  Object.values(chartInstances).forEach((c) => c?.resize());
}

function disposeCharts() {
  Object.keys(chartInstances).forEach((k) => {
    chartInstances[k]?.dispose();
    delete chartInstances[k];
  });
}

onMounted(() => {
  setTimeout(renderCharts, 0);
  window.addEventListener("resize", resizeCharts);
});

onBeforeUnmount(() => {
  window.removeEventListener("resize", resizeCharts);
  disposeCharts();
});

watch(() => props.layout, () => {
  disposeCharts();
  setTimeout(renderCharts, 50);
}, { deep: true });
</script>

<style scoped>
.plan-kpi-tile.plan-kpi-connected {
  display: flex;
  flex-direction: row;
  padding: 0;
  min-height: 72px;
}
</style>
