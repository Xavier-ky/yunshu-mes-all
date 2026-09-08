<template>
  <div class="plan-metric-strip">
    <div
      v-for="item in items"
      :key="item.key || item.label"
      class="plan-metric-card"
      :class="item.tone ? `tone-${item.tone}` : ''"
    >
      <div v-if="item.icon" class="plan-metric-icon">{{ item.icon }}</div>
      <div class="plan-metric-body">
        <span>{{ item.label }}</span>
        <strong>{{ item.value }}<small v-if="item.unit">{{ item.unit }}</small></strong>
        <small v-if="item.hint">{{ item.hint }}</small>
      </div>
    </div>
  </div>
</template>

<script setup>
defineProps({
  items: { type: Array, default: () => [] },
});
</script>
