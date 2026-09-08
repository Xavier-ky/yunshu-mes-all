<template>
  <div class="qc-activity-feed">
    <div class="qc-activity-feed__head">
      <strong>检验动态</strong>
      <span class="qc-activity-feed__sub">实时滚动</span>
    </div>
    <div v-if="!items.length" class="qc-activity-feed__empty">暂无动态</div>
    <div
      v-else
      class="qc-activity-feed__viewport"
      @mouseenter="paused = true"
      @mouseleave="paused = false"
    >
      <div
        class="qc-activity-feed__track"
        :class="{ 'is-scrolling': shouldScroll, 'is-paused': paused }"
        :style="trackStyle"
      >
        <button
          v-for="(item, idx) in loopItems"
          :key="`${item.docCode}-${item.eventTime}-${idx}`"
          type="button"
          class="qc-activity-feed__item"
          @click="$emit('open', item)"
        >
          <span class="qc-activity-feed__time">{{ formatTime(item.eventTime) }}</span>
          <span class="qc-activity-feed__type" :class="`qc-activity-feed__type--${(item.qcType || '').toLowerCase()}`">{{ item.qcType }}</span>
          <span class="qc-activity-feed__main">
            <span class="qc-activity-feed__code">{{ item.docCode }}</span>
            <span v-if="item.itemName" class="qc-activity-feed__name">{{ item.itemName }}</span>
          </span>
          <span class="qc-activity-feed__stamp" :class="stampClass(item)">{{ stampLabel(item) }}</span>
        </button>
      </div>
    </div>
  </div>
</template>

<script>
export default {
  name: "QcActivityFeed",
  props: {
    items: { type: Array, default: () => [] },
  },
  emits: ["open"],
  data() {
    return { paused: false };
  },
  computed: {
    shouldScroll() {
      return (this.items || []).length >= 2;
    },
    loopItems() {
      const list = this.items || [];
      if (!list.length) return [];
      if (list.length === 1) return list;
      return [...list, ...list];
    },
    trackStyle() {
      const n = (this.items || []).length || 1;
      const duration = Math.max(n * 3.5, 20);
      return { "--qc-feed-duration": `${duration}s` };
    },
  },
  methods: {
    formatTime(v) {
      if (!v) return "—";
      const s = String(v).replace("T", " ");
      return s.length > 11 ? s.slice(11, 16) : s.slice(0, 5);
    },
    stampClass(item) {
      if (item.eventType === "PENDING") return "is-pending";
      if (item.checkResult === "REJECT") return "is-reject";
      if (item.checkResult === "ACCEPT") return "is-accept";
      return "is-neutral";
    },
    stampLabel(item) {
      if (item.eventType === "PENDING") return "待检";
      if (item.checkResult === "REJECT") return "不合格";
      if (item.checkResult === "ACCEPT") return "合格";
      return item.eventType || "—";
    },
  },
};
</script>
