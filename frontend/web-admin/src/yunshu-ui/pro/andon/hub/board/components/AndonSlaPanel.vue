<template>
  <div class="andon-sla-panel">
    <div class="andon-sla-panel__head">
      <strong>SLA 分布</strong>
      <span class="andon-sla-panel__sub">待处置 {{ total }} 条</span>
    </div>
    <div class="andon-sla-panel__body">
      <div v-if="!total" class="andon-sla-panel__empty">暂无待处置告警</div>
      <div v-else class="andon-sla-panel__rows">
        <div v-for="row in rows" :key="row.key" class="andon-sla-panel__row">
          <span class="andon-sla-panel__label">
            <i class="andon-sla-panel__dot" :class="row.tone" />{{ row.label }}
          </span>
          <div class="andon-sla-panel__track">
            <div class="andon-sla-panel__fill" :class="row.tone" :style="{ width: `${row.pct}%` }" />
          </div>
          <b class="andon-sla-panel__count">{{ row.count }}</b>
        </div>
      </div>
    </div>
  </div>
</template>

<script>
const SLA_MINUTES = 30;
const WARN_MINUTES = 15;

export default {
  name: "AndonSlaPanel",
  props: {
    alerts: { type: Array, default: () => [] },
  },
  data() {
    return { tick: 0, tickTimer: null };
  },
  computed: {
    total() {
      return (this.alerts || []).length;
    },
    buckets() {
      const b = { ok: 0, warn: 0, critical: 0 };
      (this.alerts || []).forEach((a) => {
        const m = this.elapsedMinutes(a);
        if (m >= SLA_MINUTES) b.critical += 1;
        else if (m >= WARN_MINUTES) b.warn += 1;
        else b.ok += 1;
      });
      return b;
    },
    rows() {
      const t = this.total || 1;
      return [
        { key: "ok", label: "正常", tone: "is-ok", count: this.buckets.ok, pct: (this.buckets.ok / t) * 100 },
        { key: "warn", label: "15分+", tone: "is-warn", count: this.buckets.warn, pct: (this.buckets.warn / t) * 100 },
        { key: "critical", label: "30分+", tone: "is-critical", count: this.buckets.critical, pct: (this.buckets.critical / t) * 100 },
      ];
    },
  },
  mounted() {
    this.tickTimer = setInterval(() => { this.tick += 1; }, 60000);
  },
  beforeUnmount() {
    if (this.tickTimer) clearInterval(this.tickTimer);
  },
  methods: {
    elapsedMinutes(alert) {
      void this.tick;
      if (alert?.createTime) {
        const start = new Date(String(alert.createTime).replace(" ", "T")).getTime();
        if (!Number.isNaN(start)) {
          return Math.max(0, Math.floor((Date.now() - start) / 60000));
        }
      }
      return Number(alert?.elapsedMinutes) || 0;
    },
  },
};
</script>
