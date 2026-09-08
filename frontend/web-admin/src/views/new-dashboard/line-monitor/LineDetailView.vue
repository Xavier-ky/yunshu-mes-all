<template>
  <div class="detail-view">
    <div class="detail-top">
      <button type="button" class="back-btn" @click="$emit('back')">← 返回总览</button>
      <div class="line-info">
        <h2>{{ line?.lineName }}</h2>
        <span class="mono">{{ line?.lineCode }}</span>
        <span class="sep">·</span>
        <span>{{ line?.workshopName }}</span>
        <span class="sep">·</span>
        <span>额定 {{ line?.ratedCapacity }} {{ line?.capacityUnit }}</span>
      </div>
    </div>

    <div class="detail-main">
      <div class="detail-left">
        <h3>工位拓扑</h3>
        <StationTopology :stations="stations" />
      </div>
      <div class="detail-right">
        <section class="side-panel">
          <h3>在制工单</h3>
          <div v-if="workOrders.length" class="side-list">
            <div v-for="wo in workOrders" :key="wo.workOrderId" class="side-card">
              <strong>{{ wo.workOrderNo }}</strong>
              <span>{{ wo.productName }}</span>
              <div class="side-meta">{{ wo.completedQty }} / {{ wo.planQty }} · {{ wo.status }}</div>
            </div>
          </div>
          <div v-else class="side-empty">暂无在制工单</div>
        </section>

        <section class="side-panel">
          <h3>派工进度</h3>
          <div v-if="dispatches.length" class="side-list compact">
            <div v-for="d in dispatches" :key="d.dispatchId" class="side-row">
              <span>{{ d.stationName }}</span>
              <span>{{ d.completedQty }}/{{ d.plannedQty }}</span>
              <span class="mono">{{ d.operatorName }}</span>
            </div>
          </div>
          <div v-else class="side-empty">暂无派工</div>
        </section>

        <section class="side-panel">
          <div class="panel-head">
            <h3>开放安灯</h3>
            <router-link to="/app/andon" class="link">查看全部 →</router-link>
          </div>
          <div v-if="andons.length" class="side-list">
            <div v-for="a in andons" :key="a.andonId" class="side-card warn">
              <strong>{{ a.andonNo }}</strong>
              <span>{{ a.typeName }} · {{ a.stationName }}</span>
              <div class="side-meta">{{ a.exceptionDesc || "—" }}</div>
            </div>
          </div>
          <div v-else class="side-empty">暂无开放安灯</div>
        </section>
      </div>
    </div>

    <LineMonitorCharts
      mode="detail"
      :hourly-output="hourlyOutput"
      :oee="oee"
      :events="events"
    />
  </div>
</template>

<script setup>
import StationTopology from "./StationTopology.vue";
import LineMonitorCharts from "./LineMonitorCharts.vue";

defineProps({
  line: { type: Object, default: null },
  stations: { type: Array, default: () => [] },
  workOrders: { type: Array, default: () => [] },
  dispatches: { type: Array, default: () => [] },
  andons: { type: Array, default: () => [] },
  hourlyOutput: { type: Array, default: () => [] },
  oee: { type: Object, default: null },
  events: { type: Array, default: () => [] },
});
defineEmits(["back"]);
</script>

<style scoped>
.detail-top {
  display: flex;
  align-items: center;
  gap: 16px;
  margin-bottom: 14px;
}
.back-btn {
  background: #fff;
  border: 1px solid #e7e7e7;
  padding: 6px 12px;
  font-size: 13px;
  cursor: pointer;
  color: #2563eb;
}
.line-info {
  display: flex;
  align-items: baseline;
  flex-wrap: wrap;
  gap: 8px;
}
.line-info h2 {
  margin: 0;
  font-size: 18px;
  font-weight: 600;
}
.mono {
  font-family: "JetBrains Mono", monospace;
  font-size: 12px;
  color: #999;
}
.sep {
  color: #ccc;
}
.detail-main {
  display: grid;
  grid-template-columns: 1fr 340px;
  gap: 12px;
  margin-bottom: 12px;
}
.detail-left,
.side-panel {
  background: #fff;
  border: 1px solid #e7e7e7;
  padding: 14px 16px;
}
.detail-left h3,
.side-panel h3 {
  margin: 0 0 12px;
  font-size: 13px;
  font-weight: 600;
}
.detail-right {
  display: grid;
  gap: 12px;
  align-content: start;
}
.panel-head {
  display: flex;
  justify-content: space-between;
  align-items: center;
  margin-bottom: 12px;
}
.panel-head h3 {
  margin: 0;
}
.link {
  font-size: 12px;
  color: #2563eb;
  text-decoration: none;
}
.side-list {
  display: grid;
  gap: 8px;
}
.side-card {
  padding: 8px 10px;
  background: #fafafa;
  border: 1px solid #f0f0f0;
  font-size: 12px;
  display: grid;
  gap: 2px;
}
.side-card.warn {
  border-color: #fecaca;
  background: #fef2f2;
}
.side-card strong {
  font-size: 13px;
}
.side-meta {
  color: #888;
  font-size: 11px;
}
.side-row {
  display: grid;
  grid-template-columns: 1fr auto auto;
  gap: 8px;
  font-size: 12px;
  padding: 4px 0;
  border-bottom: 1px solid #f5f5f5;
}
.side-empty {
  color: #ccc;
  font-size: 12px;
  text-align: center;
  padding: 16px;
}
@media (max-width: 960px) {
  .detail-main {
    grid-template-columns: 1fr;
  }
}
</style>
