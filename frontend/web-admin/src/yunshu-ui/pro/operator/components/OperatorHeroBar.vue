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
        <strong class="operator-hero-kpi-item__value" :class="item.tone">{{ item.value }}</strong>
      </div>
    </div>

    <div class="operator-hero-actions">
      <el-button class="operator-ui-btn operator-hero-btn" :auto-insert-space="false" @click="$emit('refresh')">
        <span class="operator-ui-btn__label">刷新</span>
      </el-button>
      <el-button class="operator-ui-btn operator-hero-btn" type="warning" :auto-insert-space="false" @click="$emit('open-andon')">
        <span class="operator-ui-btn__label">发起安灯</span>
      </el-button>
    </div>
  </div>
</template>

<script>
export default {
  name: "OperatorHeroBar",
  props: {
    kpi: {
      type: Object,
      default: () => ({ total: 0, running: 0, pending: 0, todayFeedback: 0, activeAndon: 0 }),
    },
    shiftLabel: { type: String, default: "白班" },
  },
  emits: ["refresh", "open-andon"],
  data() {
    return {
      clockTime: "",
      clockDate: "",
      timer: null,
    };
  },
  computed: {
    cards() {
      const k = this.kpi;
      return [
        { key: "total", label: "我的任务", value: k.total, tone: "tone-blue" },
        { key: "running", label: "进行中", value: k.running, tone: "tone-cyan" },
        { key: "pending", label: "待报工", value: k.pending, tone: "tone-indigo" },
        { key: "feedback", label: "今日报工", value: k.todayFeedback, tone: "tone-green" },
        { key: "andon", label: "工位安灯", value: k.activeAndon, tone: "tone-red" },
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
      this.clockTime = now.toLocaleString("zh-CN", {
        hour: "2-digit",
        minute: "2-digit",
        second: "2-digit",
      });
      this.clockDate = now.toLocaleString("zh-CN", {
        month: "2-digit",
        day: "2-digit",
        weekday: "short",
      });
    },
  },
};
</script>
