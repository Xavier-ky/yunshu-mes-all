<template>
  <AgentCenterView :backend-status="statusText" />
</template>

<script setup>
import { onBeforeUnmount, onMounted, ref } from "vue";
import AgentCenterView from "./AgentCenterView.vue";

// 包装层：为移植的 AgentCenterView 提供 backendStatus（探活 /api/health），
// 不改动移植页面本身。
const statusText = ref("未连接");
let timer = null;

async function check() {
  try {
    const r = await fetch("/api/health");
    statusText.value = r.ok ? "运行中" : "未连接";
  } catch (e) {
    statusText.value = "未连接";
  }
}

onMounted(() => {
  check();
  timer = setInterval(check, 15000);
});

onBeforeUnmount(() => {
  if (timer) clearInterval(timer);
});
</script>
