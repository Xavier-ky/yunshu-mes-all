<template>
  <PlanningPageShell
    title="排产派工"
    description="已确认工单工序排产与甘特可视化"
    theme="scheduling"
    :breadcrumbs="planBreadcrumbs"
    :planning-nav="{ current: 'schedule', query: stepperQuery }"
    agent-context="new-scheduling"
  >
    <YunshuUiHost>
      <YunshuSchedulePage />
    </YunshuUiHost>
  </PlanningPageShell>
</template>

<script setup>
import { computed } from "vue";
import { useRoute } from "vue-router";
import PlanningPageShell from "@/components/planning/PlanningPageShell.vue";
import YunshuUiHost from "@/components/yunshu-ui/YunshuUiHost.vue";
import YunshuSchedulePage from "@/yunshu-ui/pro/schedule/index.vue";

const route = useRoute();
const planBreadcrumbs = [
  { label: "计划调度", to: "/app/planning/orders" },
  { label: "排产派工" },
];
const stepperQuery = computed(() => {
  const q = {};
  if (route.query.orderId) q.orderId = route.query.orderId;
  return q;
});
</script>
