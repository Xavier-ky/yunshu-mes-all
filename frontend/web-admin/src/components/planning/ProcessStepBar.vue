<template>
  <div class="plan-process-bar">
    <div
      v-for="(step, i) in steps"
      :key="step.key || i"
      class="plan-process-step"
      :class="stepState(step)"
    >
      <div class="plan-process-dot">{{ i + 1 }}</div>
      <span class="plan-process-label">{{ step.label }}</span>
      <span v-if="step.completed != null" class="plan-process-qty">{{ step.completed }}/{{ step.planned }}</span>
    </div>
  </div>
</template>

<script setup>
const props = defineProps({
  steps: { type: Array, default: () => [] },
});

function stepState(step) {
  if (step.status === "COMPLETED") return "done";
  if (step.status === "RUNNING" || step.status === "DISPATCHED") return "active";
  return "pending";
}
</script>
