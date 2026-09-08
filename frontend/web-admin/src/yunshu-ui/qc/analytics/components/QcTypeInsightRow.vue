<template>
  <div class="qc-type-insight-row">
    <button
      v-for="card in cards"
      :key="card.code"
      type="button"
      class="qc-type-insight-card"
      :class="[`qc-type-insight-card--${card.code.toLowerCase()}`, { 'is-active': activeType === card.code }]"
      @click="$emit('filter-type', card.code)"
    >
      <div class="qc-type-insight-card__top">
        <span class="qc-type-insight-card__label">{{ card.label }}</span>
        <span class="qc-type-insight-card__stats">
          <span>待检 <b>{{ card.pending }}</b></span>
          <span>今日 <b>{{ card.finishedToday }}</b></span>
        </span>
        <span class="qc-type-insight-card__rate" :class="rateClass(card.passRate)">{{ formatRate(card.passRate) }}</span>
      </div>
      <div class="qc-type-insight-card__bar">
        <div class="qc-type-insight-card__bar-fill" :class="rateClass(card.passRate)" :style="{ width: barWidth(card.passRate) }" />
      </div>
    </button>
  </div>
</template>

<script>
const META = [
  { code: "IQC", label: "来料 IQC" },
  { code: "PQC", label: "过程 IPQC" },
  { code: "OQC", label: "出货 OQC" },
  { code: "RQC", label: "退料 RQC" },
];

export default {
  name: "QcTypeInsightRow",
  props: {
    typeStats: { type: Object, default: () => ({}) },
    activeType: { type: String, default: "all" },
  },
  emits: ["filter-type"],
  computed: {
    cards() {
      return META.map((m) => {
        const s = this.typeStats?.[m.code] || {};
        return {
          ...m,
          pending: s.pending ?? 0,
          finishedToday: s.finishedToday ?? 0,
          passRate: s.passRate ?? 0,
        };
      });
    },
  },
  methods: {
    formatRate(v) {
      if (v == null || v === 0) return "—";
      return `${Number(v).toFixed(1)}%`;
    },
    barWidth(v) {
      const n = Math.min(Math.max(Number(v) || 0, 0), 100);
      return `${n}%`;
    },
    rateClass(v) {
      const n = Number(v) || 0;
      if (n >= 95) return "is-good";
      if (n >= 90) return "is-warn";
      if (n > 0) return "is-bad";
      return "is-neutral";
    },
  },
};
</script>
