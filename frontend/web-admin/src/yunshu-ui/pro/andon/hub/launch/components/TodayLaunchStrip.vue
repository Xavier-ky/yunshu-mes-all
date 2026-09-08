<template>
  <div class="andon-launch-today">
    <div class="andon-launch-today__head">
      <strong>今日发起</strong>
      <span class="andon-launch-today__sub">共 {{ records.length }} 条</span>
    </div>
    <div v-loading="loading" class="andon-launch-today__list">
      <div
        v-for="(item, idx) in records"
        :key="`${item.recordId}-${idx}`"
        class="andon-launch-today__item"
      >
        <span class="andon-launch-today__time">{{ fmtTime(item.createTime) }}</span>
        <span class="andon-launch-today__stamp" :class="statusClass(item)">{{ statusLabel(item) }}</span>
        <span class="andon-launch-today__text">{{ item.workstationName || item.workstationCode }} · {{ item.andonReason }}</span>
        <span class="andon-launch-today__level">{{ item.andonLevel || "—" }}</span>
      </div>
      <div v-if="!loading && !records.length" class="andon-launch-today__empty">今日尚未发起安灯</div>
    </div>
  </div>
</template>

<script>
export default {
  name: "TodayLaunchStrip",
  props: {
    records: { type: Array, default: () => [] },
    loading: { type: Boolean, default: false },
  },
  methods: {
    fmtTime(v) {
      return v ? String(v).substring(11, 16) : "—";
    },
    statusLabel(item) {
      return item.status === "HANDLED" ? "已处置" : "待处置";
    },
    statusClass(item) {
      return item.status === "HANDLED" ? "is-handled" : "is-active";
    },
  },
};
</script>
