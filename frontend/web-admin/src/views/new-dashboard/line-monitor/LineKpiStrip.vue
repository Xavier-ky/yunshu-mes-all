<template>
  <div class="kpi-bar">
    <span
      v-for="m in metrics"
      :key="m.code"
      class="kpi-item"
      :class="toneClass(m.tone)"
    >
      <span class="kpi-text">
        <span class="kpi-label">{{ m.label }}</span>
        <strong class="kpi-value" :class="valueClass(m.code)">{{ m.value }}</strong>
        <span v-if="m.unit" class="kpi-unit">{{ m.unit }}</span>
      </span>
    </span>
  </div>
</template>

<script setup>
defineProps({
  metrics: { type: Array, default: () => [] },
});

function toneClass(tone) {
  if (tone === "ALERT") return "tone-alert";
  if (tone === "WARNING") return "tone-warn";
  return "";
}

function valueClass(code) {
  return {
    running_lines: "val-running",
    idle_lines: "val-idle",
    fault_lines: "val-fault",
    active_wo: "val-wo",
    today_output: "val-output",
    avg_oee: "val-oee",
    open_andons: "val-andon",
    fault_devices: "val-device",
  }[code] || "";
}
</script>

<style scoped>
.kpi-bar {
  display: flex;
  align-items: center;
  width: 100%;
  padding: 8px 0 2px;
  border-top: 1px solid #ececec;
  margin-top: 8px;
}
.kpi-item {
  flex: 1;
  display: flex;
  align-items: center;
  justify-content: center;
  min-width: 0;
  position: relative;
}
.kpi-item:not(:first-child)::before {
  content: "|";
  position: absolute;
  left: 0;
  color: #ddd;
  font-size: 13px;
  user-select: none;
}
.kpi-text {
  display: inline-flex;
  align-items: baseline;
  gap: 6px;
  font-size: 14px;
  white-space: nowrap;
}
.kpi-label {
  color: #666;
}
.kpi-value {
  font-size: 17px;
  font-weight: 700;
  color: #1a1a1a;
  line-height: 1;
}
.val-running { color: #059669; }
.val-idle { color: #64748b; }
.val-fault { color: #dc2626; }
.val-wo { color: #2563eb; }
.val-output { color: #7c3aed; }
.val-oee { color: #0891b2; }
.val-andon { color: #d97706; }
.val-device { color: #e11d48; }
.kpi-unit {
  font-size: 13px;
  color: #888;
}
.tone-warn .kpi-value.val-andon,
.kpi-value.val-andon {
  color: #d97706;
}
.tone-alert .kpi-value {
  color: #dc2626;
}
@media (max-width: 960px) {
  .kpi-bar {
    flex-wrap: wrap;
    gap: 8px 0;
  }
  .kpi-item {
    flex: 1 1 45%;
    justify-content: flex-start;
    padding-left: 8px;
  }
  .kpi-item::before {
    display: none;
  }
}
</style>
