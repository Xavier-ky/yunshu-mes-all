<template>
  <span class="status-pill" :class="`tone-${tone}`">
    <span class="status-dot"></span>
    {{ label }}
  </span>
</template>

<script setup>
defineProps({
  label: {
    type: String,
    required: true,
  },
  tone: {
    type: String,
    default: "normal",
  },
});
</script>
