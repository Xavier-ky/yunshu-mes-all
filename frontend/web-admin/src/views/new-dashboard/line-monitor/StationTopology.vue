<template>
  <div class="topology">
    <div
      v-for="(st, idx) in stations"
      :key="st.stationId"
      class="station-block"
    >
      <div class="station-body">
        <div class="status-bar" :class="runClass(st.runStatus)"></div>
        <div class="st-name">{{ st.stationName }}</div>
        <div class="st-code mono">{{ st.stationCode }}</div>
        <div class="st-meta">
          <span class="run-tag" :class="runClass(st.runStatus)">{{ runLabel(st.runStatus) }}</span>
          <span v-if="st.dispatchNo" class="mono">{{ st.dispatchNo }}</span>
        </div>
        <div class="st-progress">
          {{ st.completedQty }} / {{ st.plannedQty }}
          <span v-if="st.operatorName"> · {{ st.operatorName }}</span>
        </div>
        <div v-if="st.deviceName" class="st-device">{{ st.deviceName }}</div>
      </div>
      <div v-if="idx < stations.length - 1" class="connector">→</div>
    </div>
    <div v-if="!stations.length" class="empty">暂无工位数据</div>
  </div>
</template>

<script setup>
defineProps({
  stations: { type: Array, default: () => [] },
});

function runClass(s) {
  return {
    RUNNING: "run",
    WARNING: "warn",
    FAULT: "fault",
    CHANGEOVER: "change",
    IDLE: "idle",
  }[s] || "idle";
}

function runLabel(s) {
  return {
    RUNNING: "运行",
    WARNING: "预警",
    FAULT: "故障",
    CHANGEOVER: "换型",
    IDLE: "待机",
  }[s] || s;
}
</script>

<style scoped>
.topology {
  display: flex;
  flex-wrap: wrap;
  align-items: stretch;
  gap: 8px;
}
.station-block {
  display: flex;
  align-items: center;
  gap: 8px;
}
.station-body {
  position: relative;
  background: #fff;
  border: 1px solid #e7e7e7;
  min-width: 140px;
  max-width: 180px;
  min-height: 72px;
  padding: 10px 10px 10px 14px;
  overflow: hidden;
}
.status-bar {
  position: absolute;
  left: 0;
  top: 0;
  bottom: 0;
  width: 4px;
}
.status-bar.run {
  background: #059669;
}
.status-bar.warn {
  background: #d97706;
}
.status-bar.fault {
  background: #dc2626;
}
.status-bar.change {
  background: #2563eb;
}
.status-bar.idle {
  background: #d1d5db;
}
.st-name {
  font-size: 13px;
  font-weight: 600;
}
.mono {
  font-family: "JetBrains Mono", monospace;
  font-size: 10px;
  color: #999;
}
.st-meta {
  display: flex;
  align-items: center;
  gap: 6px;
  margin-top: 4px;
}
.run-tag {
  font-size: 10px;
  padding: 1px 5px;
}
.run-tag.run {
  color: #059669;
  background: #f0fdf4;
}
.run-tag.warn {
  color: #d97706;
  background: #fffbeb;
}
.run-tag.fault {
  color: #dc2626;
  background: #fef2f2;
}
.run-tag.change {
  color: #2563eb;
  background: #eff6ff;
}
.run-tag.idle {
  color: #6b7280;
  background: #f3f4f6;
}
.st-progress {
  font-size: 11px;
  color: #666;
  margin-top: 4px;
}
.st-device {
  font-size: 10px;
  color: #888;
  margin-top: 2px;
}
.connector {
  color: #ccc;
  font-size: 18px;
  padding: 0 2px;
}
.empty {
  color: #ccc;
  padding: 24px;
  width: 100%;
  text-align: center;
}
@media (max-width: 960px) {
  .topology {
    flex-direction: column;
  }
  .connector {
    transform: rotate(90deg);
  }
}
</style>
