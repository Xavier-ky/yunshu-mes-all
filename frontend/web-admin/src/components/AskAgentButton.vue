<template>
  <RouterLink
    class="ask-agent-btn"
    :to="agentLink"
    title="向云枢小智提问"
  >
    <img src="/images/open-a-i.svg" alt="" />
    <span>问问小智</span>
  </RouterLink>
</template>

<script setup>
import { computed } from "vue";

const props = defineProps({
  context: { type: String, default: "" },
  pageTitle: { type: String, default: "" },
});

const agentLink = computed(() => ({
  path: "/app/yunshu-ai",
  query: {
    context: props.context || undefined,
    title: props.pageTitle || undefined,
  },
}));
</script>
