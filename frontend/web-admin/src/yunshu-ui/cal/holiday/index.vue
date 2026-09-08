<template>
  <CalHolidayWorkbench @navigate-tab="$emit('navigate-tab', $event)" />
</template>

<script setup>
import CalHolidayWorkbench from "./CalHolidayWorkbench.vue";

defineEmits(["navigate-tab"]);
</script>
