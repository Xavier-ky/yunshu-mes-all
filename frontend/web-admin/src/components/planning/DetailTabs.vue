<template>
  <div class="tabs-root">
    <div class="plan-tabs">
      <button
        v-for="t in tabs"
        :key="t.key"
        type="button"
        class="plan-tab"
        :class="{ active: modelValue === t.key }"
        @click="$emit('update:modelValue', t.key)"
      >
        {{ t.label }}
      </button>
    </div>
    <div class="tab-body">
      <slot />
    </div>
  </div>
</template>

<script setup>
defineProps({
  tabs: { type: Array, default: () => [] },
  modelValue: { type: String, default: "" },
});
defineEmits(["update:modelValue"]);
</script>

<style scoped>
.tabs-root {
  display: flex;
  flex-direction: column;
  min-height: 0;
  flex: 1;
}
.tab-body {
  flex: 1;
  overflow: auto;
}
</style>
