<template>
  <div class="plan-insight-panel">
    <div v-for="(panel, i) in panels" :key="i" class="plan-insight-card">
      <h4>{{ panel.title }}</h4>
      <div v-if="panel.type === 'chart'" :ref="(el) => setChartRef(el, i)" class="plan-insight-chart" />
      <div v-else-if="panel.type === 'list'" class="plan-insight-list">
        <div v-if="!panel.rows?.length" class="plan-empty">{{ panel.empty || "暂无数据" }}</div>
        <div v-for="(row, ri) in panel.rows" :key="ri" class="plan-insight-row">
          <span>{{ row.label }}</span>
          <strong :style="row.color ? { color: row.color } : undefined">{{ row.value }}</strong>
        </div>
      </div>
      <slot v-else :name="`panel-${i}`" />
    </div>
  </div>
</template>

<script setup>
import { onBeforeUnmount, onMounted, ref, watch } from "vue";
import * as echarts from "echarts";

const props = defineProps({
  panels: { type: Array, default: () => [] },
});

const chartRefs = ref([]);
const chartInstances = [];

function setChartRef(el, i) {
  if (el) chartRefs.value[i] = el;
}

function renderCharts() {
  props.panels.forEach((panel, i) => {
    if (panel.type !== "chart" || !chartRefs.value[i]) return;
    if (!chartInstances[i]) chartInstances[i] = echarts.init(chartRefs.value[i]);
    chartInstances[i].setOption(panel.option || {}, true);
  });
}

onMounted(() => {
  renderCharts();
  window.addEventListener("resize", renderCharts);
});

onBeforeUnmount(() => {
  window.removeEventListener("resize", renderCharts);
  chartInstances.forEach((c) => c?.dispose());
});

watch(() => props.panels, renderCharts, { deep: true });
</script>

<style scoped>
.plan-empty {
  padding: 40px 0;
  text-align: center;
  color: #ccc;
  font-size: 13px;
}
</style>
