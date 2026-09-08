<template>
  <button
    type="button"
    class="plan-list-card"
    :class="[active ? 'active' : '', borderClass]"
    @click="$emit('click')"
  >
    <div class="plan-list-card-head">
      <span class="plan-list-card-title">{{ title }}</span>
      <slot name="badge" />
    </div>
    <div v-if="subtitle" class="plan-list-card-sub">{{ subtitle }}</div>
    <div class="plan-list-card-meta">
      <slot name="meta" />
      <span v-for="chip in chips" :key="chip.text" class="plan-chip" :class="chip.tone || 'gray'">{{ chip.text }}</span>
    </div>
    <slot name="footer" />
  </button>
</template>

<script setup>
defineProps({
  title: { type: String, default: "" },
  subtitle: { type: String, default: "" },
  active: { type: Boolean, default: false },
  borderClass: { type: String, default: "" },
  chips: { type: Array, default: () => [] },
});

defineEmits(["click"]);
</script>
