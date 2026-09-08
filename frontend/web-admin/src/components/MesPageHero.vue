<template>
  <header class="mes-page-hero" :class="themeClass">
    <div class="mes-page-hero-left">
      <nav v-if="breadcrumbs?.length" class="mes-page-hero-breadcrumb" aria-label="面包屑">
        <span v-for="(crumb, i) in breadcrumbs" :key="i" class="mes-page-hero-crumb">
          <span v-if="i > 0" class="sep">›</span>
          <RouterLink v-if="crumb.to" :to="crumb.to">{{ crumb.label }}</RouterLink>
          <span v-else class="current">{{ crumb.label }}</span>
        </span>
      </nav>
      <span v-else-if="title" class="mes-page-hero-breadcrumb">
        <span class="current">{{ title }}</span>
      </span>
      <span v-if="description" class="mes-page-hero-desc-inline">· {{ description }}</span>
    </div>

    <div v-if="visibleChips.length" class="mes-page-hero-chips">
      <span
        v-for="(chip, ci) in visibleChips"
        :key="ci"
        class="mes-page-hero-chip"
        :class="chip.tone ? `tone-${chip.tone}` : ''"
      >
        {{ chip.label }}
      </span>
    </div>

    <div class="mes-page-hero-spacer" />

    <div class="mes-page-hero-right">
      <WorkflowStepper
        v-if="planningNav"
        variant="compact"
        :current="planningNav.current"
        :query="planningNav.query || {}"
      />
      <AskAgentButton :context="agentContext" :page-title="title" />
      <span v-if="refreshedAt" class="mes-page-hero-meta">{{ refreshedAt }}</span>
      <button
        v-if="showRefresh"
        type="button"
        class="mes-page-hero-refresh"
        :disabled="refreshing"
        @click="emit('refresh')"
      >
        {{ refreshing ? "…" : "↻" }}
      </button>
      <slot name="actions" />
    </div>
  </header>
</template>

<script setup>
import { computed } from "vue";
import { useRoute } from "vue-router";
import AskAgentButton from "@/components/AskAgentButton.vue";
import WorkflowStepper from "@/components/WorkflowStepper.vue";

const props = defineProps({
  title: { type: String, default: "" },
  description: { type: String, default: "" },
  breadcrumbs: { type: Array, default: () => [] },
  theme: { type: String, default: "" },
  refreshedAt: { type: String, default: "" },
  chips: { type: Array, default: () => [] },
  planningNav: { type: Object, default: null },
  agentContext: { type: String, default: "" },
  showRefresh: { type: Boolean, default: false },
  refreshing: { type: Boolean, default: false },
});

const emit = defineEmits(["refresh"]);
const route = useRoute();

const themeClass = computed(() => {
  if (props.theme) return `theme-${props.theme}`;
  return "";
});

const visibleChips = computed(() =>
  (props.chips || []).filter((c) => c && c.label)
);

const agentContext = computed(
  () => props.agentContext || String(route.name || route.path || "")
);
</script>
