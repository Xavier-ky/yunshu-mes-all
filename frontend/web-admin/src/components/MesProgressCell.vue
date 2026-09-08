<template>
  <div class="mes-progress-cell">
    <div class="mes-progress-bar">
      <div class="mes-progress-fill" :style="{ width: pct + '%' }"></div>
    </div>
    <span class="mes-progress-text">{{ pct }}%</span>
  </div>
</template>

<script setup>
import { computed } from "vue";

const props = defineProps({
  completed: { type: [Number, String], default: 0 },
  total: { type: [Number, String], default: 0 },
});

const pct = computed(() => {
  const plan = Number(props.total) || 0;
  const done = Number(props.completed) || 0;
  if (plan <= 0) return 0;
  return Math.min(100, Math.round((done / plan) * 100));
});
</script>

<style scoped>
.mes-progress-cell {
  display: flex;
  align-items: center;
  gap: 8px;
  margin-top: 8px;
}
.mes-progress-bar {
  flex: 1;
  height: 4px;
  background: #e7e7e7;
  overflow: hidden;
}
.mes-progress-fill {
  height: 100%;
  background: #0052d9;
  transition: width 0.2s;
}
.mes-progress-text {
  font-size: 11px;
  color: #888;
  min-width: 32px;
  text-align: right;
}
</style>
