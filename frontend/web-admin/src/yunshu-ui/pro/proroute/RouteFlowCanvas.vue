<template>
  <div class="route-flow-canvas" :class="{ 'route-flow-canvas--mini': mini }">
    <template v-if="sortedSteps.length">
      <div
        v-for="(step, index) in visibleSteps"
        :key="`${step.recordId || step.processId || index}-${step.orderNum}`"
        class="route-flow-canvas__cell"
      >
        <span v-if="index > 0" class="route-flow-link" aria-hidden="true">
          <span class="route-flow-link__line"></span>
          <span v-if="!mini && sortedSteps[index - 1].linkType" class="route-flow-link__label">{{ linkLabel(sortedSteps[index - 1].linkType) }}</span>
        </span>
        <div
          class="route-flow-node"
          :class="{ 'route-flow-node--key': step.keyFlag === 'Y' }"
          :style="{ '--node-color': step.colorCode || '#1769e0' }"
        >
          <span v-if="step.keyFlag === 'Y'" class="route-flow-node__badge">关键</span>
          <span v-if="step.isCheck === 'Y'" class="route-flow-node__badge route-flow-node__badge--qc">质检</span>
          <b>{{ step.processName || step.processCode || "工序" }}</b>
          <small v-if="!mini">{{ step.processCode }}</small>
        </div>
      </div>
      <span v-if="overflowCount > 0" class="route-flow-canvas__more">+{{ overflowCount }}</span>
    </template>
    <span v-else class="route-flow-canvas__empty">暂无工序</span>
  </div>
</template>

<script>
export default {
  name: "RouteFlowCanvas",
  props: {
    steps: { type: Array, default: () => [] },
    linkTypes: { type: Array, default: () => [] },
    mini: { type: Boolean, default: false },
    maxNodes: { type: Number, default: 0 },
  },
  computed: {
    sortedSteps() {
      return [...(this.steps || [])].sort((a, b) => (a.orderNum || 0) - (b.orderNum || 0));
    },
    visibleSteps() {
      const limit = this.maxNodes > 0 ? this.maxNodes : this.sortedSteps.length;
      return this.sortedSteps.slice(0, limit);
    },
    overflowCount() {
      if (this.maxNodes <= 0) return 0;
      return Math.max(0, this.sortedSteps.length - this.maxNodes);
    },
  },
  methods: {
    linkLabel(value) {
      const hit = (this.linkTypes || []).find((d) => d.value === value);
      return hit?.label || value || "";
    },
  },
};
</script>
