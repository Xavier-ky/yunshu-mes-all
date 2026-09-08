<template>
  <div class="bin-telemetry-rail">
    <div class="bin-telemetry-scope">
      <span class="bin-telemetry-scope-label">库区态势</span>
      <span class="bin-telemetry-scope-value">{{ zoneLabel }}</span>
      <span v-if="gridLabel" class="bin-telemetry-scope-meta">{{ gridLabel }} 网格</span>
    </div>
    <div class="bin-telemetry-metrics">
      <div v-for="item in metricItems" :key="item.key" class="bin-telemetry-seg">
        <span class="bin-telemetry-seg-value" :style="{ color: item.color }">{{ item.value }}</span>
        <span class="bin-telemetry-seg-label">{{ item.label }}</span>
      </div>
    </div>
    <div class="bin-telemetry-occupy">
      <span class="bin-telemetry-occupy-label">占用率</span>
      <div class="bin-telemetry-occupy-track">
        <div
          class="bin-telemetry-occupy-fill"
          :style="{ width: occupyPct + '%' }"
        ></div>
        <span class="bin-telemetry-occupy-pct">{{ occupyPct }}%</span>
      </div>
    </div>
    <div class="bin-telemetry-actions">
      <button
        type="button"
        class="bin-telemetry-toggle"
        :class="{ active: alertOnly }"
        @click="$emit('toggle-alert')"
      >仅异常</button>
      <button type="button" class="bin-telemetry-toggle" @click="$emit('reset-filter')">重置</button>
    </div>
  </div>
</template>

<script>
import { formatQty } from "./binMapUtils"

export default {
  name: "BinMapKpiBar",
  props: {
    summary: { type: Object, default: () => ({}) },
    alertOnly: { type: Boolean, default: false },
    zoneName: { type: String, default: "" },
    gridLabel: { type: String, default: "" },
  },
  emits: ["toggle-alert", "reset-filter"],
  computed: {
    zoneLabel() {
      return this.zoneName || "—"
    },
    occupyPct() {
      return formatQty(this.summary?.occupancyRate ?? 0)
    },
    metricItems() {
      const s = this.summary || {}
      return [
        { key: "total", label: "库位", value: formatQty(s.totalBins), color: "#1e3a5f" },
        { key: "occupied", label: "在库", value: formatQty(s.occupiedBins), color: "#2563eb" },
        { key: "empty", label: "空闲", value: formatQty(s.emptyBins), color: "#64748b" },
        { key: "frozen", label: "冻结", value: formatQty(s.frozenBins), color: "#d97706" },
        { key: "expiring", label: "临期", value: formatQty(s.expiringBins), color: "#dc2626" },
      ]
    },
  },
}
</script>
