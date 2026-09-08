<template>
  <article class="metric-card">
    <div class="metric-card-head">
      <span>{{ label }}</span>
      <StatusPill v-if="tone" :label="toneLabel" :tone="tone" />
    </div>
    <div class="metric-value">
      <strong>{{ value }}</strong>
      <em>{{ unit }}</em>
    </div>
    <p v-if="hint">{{ hint }}</p>
  </article>
</template>

<script setup>
import { computed } from "vue";
import StatusPill from "./StatusPill.vue";

const props = defineProps({
  label: { type: String, required: true },
  value: { type: [String, Number], required: true },
  unit: { type: String, default: "" },
  tone: { type: String, default: "normal" },
  hint: { type: String, default: "" },
});

const toneLabel = computed(() => {
  const map = {
    success: "稳定",
    warning: "关注",
    danger: "风险",
    normal: "正常",
  };
  return map[props.tone] || "正常";
});
</script>
