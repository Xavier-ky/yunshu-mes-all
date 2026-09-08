<template>
  <div class="andon-activity-feed">
    <div class="andon-activity-feed__head">
      <strong>活动动态</strong>
      <span class="andon-activity-feed__sub">最近 24h</span>
    </div>
    <div v-loading="loading" class="andon-activity-feed__list">
      <button
        v-for="(item, idx) in items"
        :key="`${item.eventType}-${item.recordId}-${idx}`"
        type="button"
        class="andon-activity-feed__item"
        @click="$emit('select', item)"
      >
        <span class="andon-activity-feed__time">{{ fmtTime(item.eventTime) }}</span>
        <span class="andon-activity-feed__stamp" :class="stampClass(item)">{{ eventLabel(item) }}</span>
        <span class="andon-activity-feed__text">
          {{ item.workstationName || "—" }} · {{ item.andonReason || "—" }}
        </span>
        <span class="andon-activity-feed__actor">{{ item.actor || "—" }}</span>
      </button>
      <div v-if="!loading && !items.length" class="andon-activity-feed__empty">暂无动态</div>
    </div>
  </div>
</template>

<script>
export default {
  name: "ActivityTimeline",
  props: {
    items: { type: Array, default: () => [] },
    loading: { type: Boolean, default: false },
  },
  emits: ["select"],
  methods: {
    fmtTime(v) {
      return v ? String(v).substring(5, 16) : "—";
    },
    eventLabel(item) {
      if (item.eventType === "HANDLED") return "已处置";
      if (item.eventType === "SLA_OVERDUE") return "SLA超时";
      return "发起";
    },
    stampClass(item) {
      if (item.eventType === "HANDLED") return "is-handled";
      if (item.eventType === "SLA_OVERDUE") return "is-overdue";
      return "is-created";
    },
  },
};
</script>
