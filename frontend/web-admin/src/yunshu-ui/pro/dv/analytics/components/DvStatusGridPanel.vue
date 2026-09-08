<template>
  <div class="dv-analytics-panel-card dv-analytics-block">
    <div class="dv-analytics-block-head qc-analytics-bottom-panel__head">
      <span class="qc-analytics-bottom-panel__title">设备状态分布</span>
      <span class="qc-analytics-bottom-panel__meta">共 {{ total }} 台</span>
    </div>
    <div class="dv-analytics-panel-card__body">
      <div class="machinery-status-grid">
        <button
          v-for="item in gridItems"
          :key="item.status"
          type="button"
          class="machinery-status-btn"
          :class="{ active: false }"
        >
          <span class="machinery-status-btn__dot" :style="{ background: item.color }" />
          <span class="machinery-status-btn__label">{{ item.label }}</span>
          <strong class="machinery-status-btn__count">{{ item.count }}</strong>
        </button>
      </div>
    </div>
  </div>
</template>

<script>
const STATUS_LABEL = { WORKING: "运行", STOP: "停机", REPAIR: "维修", IDLE: "闲置" };
const STATUS_COLOR = { WORKING: "#059669", STOP: "#64748b", REPAIR: "#dc2626", IDLE: "#94a3b8" };
const DEFAULT_STATUSES = ["WORKING", "STOP", "REPAIR", "IDLE"];

export default {
  name: "DvStatusGridPanel",
  props: {
    rows: { type: Array, default: () => [] },
  },
  computed: {
    countMap() {
      const m = {};
      (this.rows || []).forEach((r) => {
        m[r.status] = Number(r.count) || 0;
      });
      return m;
    },
    total() {
      return Object.values(this.countMap).reduce((s, n) => s + n, 0);
    },
    gridItems() {
      const seen = new Set();
      const items = (this.rows || []).map((r) => ({
        status: r.status,
        label: STATUS_LABEL[r.status] || r.status || "—",
        color: STATUS_COLOR[r.status] || "#2563eb",
        count: Number(r.count) || 0,
      }));
      items.forEach((i) => seen.add(i.status));
      DEFAULT_STATUSES.forEach((st) => {
        if (!seen.has(st)) {
          items.push({
            status: st,
            label: STATUS_LABEL[st],
            color: STATUS_COLOR[st],
            count: 0,
          });
        }
      });
      return items.slice(0, 4);
    },
  },
};
</script>
