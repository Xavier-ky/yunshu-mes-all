<template>
  <div class="qc-chart-card" :class="[`qc-chart-card--${accent}`]">
    <div class="qc-chart-card__head">
      <span class="qc-chart-card__icon" aria-hidden="true">{{ icon }}</span>
      <div class="qc-chart-card__titles">
        <strong>{{ title }}</strong>
        <span v-if="kpi" class="qc-chart-card__kpi">{{ kpi }}</span>
      </div>
    </div>
    <div class="qc-chart-card__body">
      <slot />
    </div>
  </div>
</template>

<script>
export default {
  name: "QcChartCard",
  props: {
    title: { type: String, required: true },
    kpi: { type: String, default: "" },
    icon: { type: String, default: "📊" },
    accent: { type: String, default: "blue" },
  },
};
</script>
