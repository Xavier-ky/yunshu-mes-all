<template>
  <div class="kpi-strip">
    <div
      v-for="item in items"
      :key="item.key || item.label"
      class="plan-kpi"
      :class="item.tone ? `tone-${item.tone}` : ''"
    >
      <span>{{ item.label }}</span>
      <strong>{{ item.value }}<small v-if="item.unit">{{ item.unit }}</small></strong>
    </div>
  </div>
</template>

<script setup>
defineProps({
  items: { type: Array, default: () => [] },
});
</script>

<style scoped>
.kpi-strip {
  display: grid;
  grid-template-columns: repeat(auto-fit, minmax(120px, 1fr));
  gap: 10px;
}
.plan-kpi strong small {
  font-size: 12px;
  font-weight: 500;
  margin-left: 2px;
  color: #888;
}
</style>
