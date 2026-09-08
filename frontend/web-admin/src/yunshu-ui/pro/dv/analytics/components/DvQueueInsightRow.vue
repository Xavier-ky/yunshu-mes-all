<template>
  <div class="dv-analytics-block dv-queue-insight-block">
    <div class="dv-analytics-block-head qc-analytics-bottom-panel__head">
      <span class="qc-analytics-bottom-panel__title">待办队列</span>
      <span class="qc-analytics-bottom-panel__meta">点击进入工作台</span>
    </div>
    <div class="dv-queue-insight-row">
      <button type="button" class="button--check" @click="$emit('queue', 'check')">
        <span>待点检</span>
        <em>{{ counts.check }}</em>
      </button>
    <button type="button" class="button--mainten" @click="$emit('queue', 'mainten')">
      <span>待保养</span>
      <em>{{ counts.mainten }}</em>
    </button>
    <button type="button" class="button--repair" @click="$emit('queue', 'repair')">
      <span>待维修</span>
      <em>{{ counts.repair }}</em>
    </button>
    </div>
  </div>
</template>

<script>
export default {
  name: "DvQueueInsightRow",
  props: {
    summary: { type: Object, default: () => ({}) },
  },
  emits: ["queue"],
  computed: {
    counts() {
      const s = this.summary || {};
      return {
        check: s.pendingCheck ?? 0,
        mainten: s.pendingMainten ?? 0,
        repair: s.pendingRepair ?? 0,
      };
    },
  },
};
</script>
