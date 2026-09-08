<template>
  <div class="line-grid">
    <button
      v-for="line in lines"
      :key="line.lineId"
      type="button"
      class="line-card"
      @click="$emit('select', line.lineId)"
    >
      <div class="card-head">
        <div>
          <strong>{{ line.lineName }}</strong>
          <span class="mono">{{ line.lineCode }}</span>
        </div>
        <span class="status-badge" :class="statusClass(line.lineStatus)">
          {{ statusLabel(line.lineStatus) }}
        </span>
      </div>

      <div class="wo-row">
        <span class="label">当前工单</span>
        <span>{{ line.currentWorkOrderNo || "—" }}</span>
      </div>
      <div class="wo-row">
        <span class="label">产品</span>
        <span>{{ line.productName || "—" }}</span>
      </div>

      <div class="progress-block">
        <div class="progress-meta">
          <span>{{ line.completedQty }} / {{ line.planQty }}</span>
          <span>{{ Math.round(line.progressPct) }}%</span>
        </div>
        <div class="progress-bar">
          <div class="progress-fill" :style="{ width: `${Math.min(100, line.progressPct)}%` }"></div>
        </div>
      </div>

      <div class="card-footer">
        <div class="oee">
          OEE <strong>{{ line.oee.toFixed(1) }}%</strong>
          <small v-if="line.oeeEstimated">估算</small>
        </div>
        <div class="station-bar" :title="stationTip(line.stationRatio)">
          <span
            v-for="(seg, i) in stationSegments(line.stationRatio)"
            :key="i"
            class="seg"
            :class="seg.cls"
            :style="{ flex: seg.count || 0.001 }"
          ></span>
        </div>
        <span v-if="line.openAndonCount" class="andon-badge">{{ line.openAndonCount }} 安灯</span>
      </div>
    </button>
  </div>
</template>

<script setup>
defineProps({
  lines: { type: Array, default: () => [] },
});
defineEmits(["select"]);

function statusClass(s) {
  return {
    RUNNING: "st-run",
    WARNING: "st-warn",
    FAULT: "st-fault",
    IDLE: "st-idle",
  }[s] || "st-idle";
}

function statusLabel(s) {
  return { RUNNING: "运行", WARNING: "预警", FAULT: "故障", IDLE: "待机" }[s] || s;
}

function stationSegments(ratio) {
  if (!ratio) return [];
  return [
    { cls: "seg-run", count: ratio.running },
    { cls: "seg-warn", count: ratio.warning },
    { cls: "seg-fault", count: ratio.fault },
    { cls: "seg-blue", count: ratio.changeover },
    { cls: "seg-idle", count: ratio.idle },
  ].filter((s) => s.count > 0);
}

function stationTip(ratio) {
  if (!ratio) return "";
  return `运行${ratio.running} 预警${ratio.warning} 故障${ratio.fault} 换型${ratio.changeover} 待机${ratio.idle}`;
}
</script>

<style scoped>
.line-grid {
  display: grid;
  grid-template-columns: repeat(auto-fill, minmax(300px, 1fr));
  gap: 12px;
}
.line-card {
  text-align: left;
  background: #fff;
  border: 1px solid #e7e7e7;
  padding: 16px;
  cursor: pointer;
  transition: border-color 0.15s;
}
.line-card:hover {
  border-color: #2563eb;
}
.card-head {
  display: flex;
  justify-content: space-between;
  align-items: flex-start;
  margin-bottom: 10px;
  padding-bottom: 10px;
  border-bottom: 1px solid #f0f0f0;
}
.card-head strong {
  display: block;
  font-size: 15px;
  font-weight: 600;
}
.mono {
  font-family: "JetBrains Mono", monospace;
  font-size: 11px;
  color: #999;
}
.status-badge {
  font-size: 11px;
  padding: 2px 8px;
  border: 1px solid #e7e7e7;
}
.st-run {
  color: #059669;
  border-color: #bbf7d0;
  background: #f0fdf4;
}
.st-warn {
  color: #d97706;
  border-color: #fde68a;
  background: #fffbeb;
}
.st-fault {
  color: #dc2626;
  border-color: #fecaca;
  background: #fef2f2;
}
.st-idle {
  color: #6b7280;
}
.wo-row {
  display: flex;
  justify-content: space-between;
  font-size: 12px;
  margin-bottom: 4px;
}
.wo-row .label {
  color: #888;
}
.progress-block {
  margin: 10px 0;
}
.progress-meta {
  display: flex;
  justify-content: space-between;
  font-size: 11px;
  color: #666;
  margin-bottom: 4px;
}
.progress-bar {
  height: 6px;
  background: #f0f0f0;
}
.progress-fill {
  height: 100%;
  background: #059669;
}
.card-footer {
  display: flex;
  align-items: center;
  gap: 10px;
  margin-top: 8px;
}
.oee {
  font-size: 11px;
  color: #666;
  white-space: nowrap;
}
.oee strong {
  color: #1a1a1a;
  font-size: 14px;
}
.oee small {
  color: #d97706;
  margin-left: 2px;
}
.station-bar {
  flex: 1;
  display: flex;
  height: 6px;
  gap: 1px;
  min-width: 60px;
}
.seg-run {
  background: #059669;
}
.seg-warn {
  background: #d97706;
}
.seg-fault {
  background: #dc2626;
}
.seg-blue {
  background: #2563eb;
}
.seg-idle {
  background: #d1d5db;
}
.andon-badge {
  font-size: 10px;
  color: #dc2626;
  background: #fef2f2;
  padding: 2px 6px;
  border: 1px solid #fecaca;
}
@media (max-width: 960px) {
  .line-grid {
    grid-template-columns: 1fr;
  }
}
</style>
