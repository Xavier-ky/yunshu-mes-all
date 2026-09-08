<template>
  <div class="dv-analytics-block dv-activity-feed">
    <div class="dv-analytics-block-head qc-analytics-bottom-panel__head">
      <span class="qc-analytics-bottom-panel__title">维保动态</span>
      <span class="qc-analytics-bottom-panel__meta">最近完成</span>
    </div>
    <div v-if="!displayItems.length" class="dv-activity-feed__empty">暂无动态</div>
    <div v-else class="dv-activity-feed__list">
      <button
        v-for="(item, idx) in displayItems"
        :key="`${item.taskType}-${item.taskId}-${idx}`"
        type="button"
        class="dv-activity-feed__item"
        @click="$emit('open', item)"
      >
        <span class="dv-activity-feed__time">{{ formatTime(item.finishTime) }}</span>
        <span class="dv-activity-feed__type" :class="typeClass(item.taskType)">{{ taskLabel(item.taskType) }}</span>
        <span class="dv-activity-feed__main">{{ item.machineryCode }} · {{ item.machineryName || "—" }}</span>
        <span>{{ item.planCode || "—" }}</span>
      </button>
    </div>
  </div>
</template>

<script>
const LABEL = { CHECK: "点检", MAINTEN: "保养", REPAIR: "维修" };

export default {
  name: "DvActivityFeed",
  props: {
    items: { type: Array, default: () => [] },
  },
  emits: ["open"],
  computed: {
    displayItems() {
      return (this.items || []).slice(0, 6);
    },
  },
  methods: {
    taskLabel(t) {
      return LABEL[t] || t || "—";
    },
    typeClass(t) {
      return `dv-activity-feed__type--${(t || "").toLowerCase()}`;
    },
    formatTime(v) {
      if (!v) return "—";
      const s = String(v).replace("T", " ");
      return s.length > 11 ? s.slice(11, 16) : s.slice(0, 5);
    },
  },
};
</script>
