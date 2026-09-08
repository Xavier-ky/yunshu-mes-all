<template>
  <div class="operator-hero-bar">
    <div class="operator-hero-clock-zone">
      <span class="operator-hero-clock__time">{{ clockTime }}</span>
      <span class="operator-hero-clock-divider" aria-hidden="true"></span>
      <div class="operator-hero-clock-info">
        <span class="operator-hero-clock__date">{{ clockDate }}</span>
        <span class="operator-hero-clock__shift">{{ shiftLabel }} · 08:00–17:00</span>
      </div>
    </div>
    <div class="operator-hero-kpi-strip">
      <div v-for="(item, idx) in cards" :key="item.key" class="operator-hero-kpi-item" :class="{ 'is-last': idx === cards.length - 1 }">
        <span class="operator-hero-kpi-item__label">{{ item.label }}</span>
        <strong class="operator-hero-kpi-item__value" :class="item.tone">{{ item.display }}</strong>
      </div>
    </div>
    <div class="operator-hero-actions">
      <el-button class="operator-ui-btn operator-hero-btn" :auto-insert-space="false" :loading="loading" @click="$emit('refresh')">
        <span class="operator-ui-btn__label">刷新</span>
      </el-button>
      <el-button class="operator-ui-btn operator-hero-btn" type="warning" :auto-insert-space="false" @click="$emit('open-andon')">
        <span class="operator-ui-btn__label">发起安灯</span>
      </el-button>
      <el-button v-if="showAnalyticsLink" class="operator-ui-btn operator-hero-btn" :auto-insert-space="false" @click="$emit('go-analytics')">
        <span class="operator-ui-btn__label">设备分析</span>
      </el-button>
    </div>
  </div>
</template>

<script>
export default {
  name: "DvHeroBar",
  props: {
    summary: { type: Object, default: () => ({}) },
    loading: { type: Boolean, default: false },
    shiftLabel: { type: String, default: "白班" },
    showAnalyticsLink: { type: Boolean, default: true },
  },
  emits: ["refresh", "open-andon", "go-analytics"],
  data() {
    return { clockTime: "", clockDate: "", timer: null };
  },
  computed: {
    cards() {
      const s = this.summary || {};
      return [
        { key: "check", label: "待点检", display: s.pendingCheck ?? 0, tone: "tone-blue" },
        { key: "mainten", label: "待保养", display: s.pendingMainten ?? 0, tone: "tone-cyan" },
        { key: "repair", label: "待维修", display: s.pendingRepair ?? 0, tone: "tone-amber" },
        { key: "fault", label: "故障设备", display: s.faultCount ?? 0, tone: "tone-red" },
        { key: "oee", label: "平均OEE", display: `${s.avgOee ?? 0}%`, tone: "tone-green" },
      ];
    },
  },
  mounted() {
    this.tickClock();
    this.timer = setInterval(this.tickClock, 1000);
  },
  beforeUnmount() {
    if (this.timer) clearInterval(this.timer);
  },
  methods: {
    tickClock() {
      const now = new Date();
      this.clockTime = now.toLocaleString("zh-CN", { hour: "2-digit", minute: "2-digit", second: "2-digit", hour12: false });
      this.clockDate = now.toLocaleString("zh-CN", { year: "numeric", month: "2-digit", day: "2-digit", weekday: "short" });
    },
  },
};
</script>
