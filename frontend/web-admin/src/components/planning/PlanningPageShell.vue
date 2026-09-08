<template>
  <div class="plan-page" :class="themeClass">
    <MesPageHero
      :title="title"
      :description="description"
      :breadcrumbs="breadcrumbs"
      :theme="theme"
      :refreshed-at="refreshedAt"
      :chips="chips"
      :planning-nav="planningNav"
      :agent-context="agentContext"
      :show-refresh="showRefresh"
      :refreshing="refreshing"
      @refresh="emit('refresh')"
    >
      <template v-if="$slots.actions" #actions>
        <slot name="actions" />
      </template>
    </MesPageHero>
    <div class="plan-page-body">
      <slot name="kpi" />
      <slot />
    </div>
  </div>
</template>

<script setup>
import { computed } from "vue";
import MesPageHero from "@/components/MesPageHero.vue";

const props = defineProps({
  title: { type: String, default: "" },
  description: { type: String, default: "" },
  refreshedAt: { type: String, default: "" },
  theme: { type: String, default: "" },
  breadcrumbs: { type: Array, default: () => [] },
  chips: { type: Array, default: () => [] },
  planningNav: { type: Object, default: null },
  agentContext: { type: String, default: "" },
  showRefresh: { type: Boolean, default: false },
  refreshing: { type: Boolean, default: false },
});

const emit = defineEmits(["refresh"]);

const themeClass = computed(() => {
  if (props.theme === "orders") return "plan-page--orders";
  if (props.theme === "workorders") return "plan-page--workorders";
  if (props.theme === "scheduling") return "plan-page--scheduling";
  return "";
});
</script>
