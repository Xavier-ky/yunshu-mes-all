<template>
  <CalTeamWorkbench @navigate-tab="$emit('navigate-tab', $event)" />
</template>

<script setup>
import CalTeamWorkbench from "./CalTeamWorkbench.vue";

defineEmits(["navigate-tab"]);
</script>
