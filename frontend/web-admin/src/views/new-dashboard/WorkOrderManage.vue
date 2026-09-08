<template>
  <PlanningPageShell
    title="工单中心"
    description="生产工单创建、BOM 快照与确认下达"
    theme="workorders"
    :breadcrumbs="planBreadcrumbs"
    :planning-nav="{ current: 'workorder', query: stepperQuery }"
    agent-context="new-work-orders"
  >
    <YunshuUiHost>
      <YunshuWorkorderPage :initial-order-id="orderId" :initial-wo="initialWo" />
    </YunshuUiHost>
  </PlanningPageShell>
</template>

<script setup>
import { computed } from "vue";
import { useRoute } from "vue-router";
import PlanningPageShell from "@/components/planning/PlanningPageShell.vue";
import YunshuUiHost from "@/components/yunshu-ui/YunshuUiHost.vue";
import YunshuWorkorderPage from "@/yunshu-ui/pro/workorder/index.vue";

const route = useRoute();
const planBreadcrumbs = [
  { label: "计划调度", to: "/app/planning/orders" },
  { label: "工单中心" },
];
const stepperQuery = computed(() => {
  const q = {};
  if (route.query.orderId) q.orderId = route.query.orderId;
  return q;
});
const orderId = computed(() => route.query.orderId || null);
const initialWo = computed(() => route.query.wo || null);
</script>
