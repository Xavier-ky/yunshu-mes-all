<template>
  <CalPlanWorkbench @navigate-tab="$emit('navigate-tab', $event)" />
</template>

<script setup>
import CalPlanWorkbench from "./CalPlanWorkbench.vue";

defineEmits(["navigate-tab"]);
</script>
