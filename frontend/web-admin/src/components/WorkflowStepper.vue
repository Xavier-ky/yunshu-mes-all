<template>
  <div v-if="variant === 'compact'" class="plan-stepper-compact">
    <button
      v-for="step in steps"
      :key="step.key"
      type="button"
      class="plan-pill"
      :class="{ active: step.key === current }"
      @click="go(step)"
    >
      {{ step.label }}
    </button>
  </div>
  <div v-else class="plan-stepper">
    <span v-for="(step, i) in steps" :key="step.key" class="plan-step-group">
      <button
        type="button"
        class="plan-step"
        :class="stepClasses(step)"
        :disabled="!step.path"
        @click="go(step)"
      >
        <span class="step-num">{{ i + 1 }}</span>
        <span>{{ step.label }}</span>
      </button>
      <span v-if="i < steps.length - 1" class="plan-step-arrow">→</span>
    </span>
  </div>
</template>

<script setup>
import { useRouter } from "vue-router";

const props = defineProps({
  current: { type: String, default: "order" },
  query: { type: Object, default: () => ({}) },
  variant: { type: String, default: "default" },
});

const router = useRouter();

const steps = [
  { key: "order", label: "订单中心", path: "/app/planning/orders" },
  { key: "workorder", label: "工单中心", path: "/app/planning/work-orders" },
  { key: "schedule", label: "排产派工", path: "/app/planning/scheduling" },
];

function stepIndex(key) {
  return steps.findIndex((s) => s.key === key);
}

function stepClasses(step) {
  const idx = stepIndex(step.key);
  const cur = stepIndex(props.current);
  return {
    active: step.key === props.current,
    done: idx >= 0 && cur >= 0 && idx < cur,
    clickable: !!step.path,
  };
}

function go(step) {
  if (!step.path || step.key === props.current) return;
  router.push({ path: step.path, query: props.query });
}
</script>

<style scoped>
.plan-step-group {
  display: contents;
}
</style>
