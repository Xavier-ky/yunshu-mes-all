<template>
  <div class="operator-hero-bar andon-command-hero">
    <div class="andon-hero-clock-zone">
      <SignalLamp
        class="andon-command-hero__lamp"
        :active-count="Number(summary.activeRecords || 0)"
        :sla-overdue="Number(summary.slaOverdue || 0)"
      />
      <span class="andon-hero-clock__time">{{ liveClock }}</span>
      <span class="andon-hero-clock-divider" aria-hidden="true" />
      <div class="andon-hero-clock-info">
        <strong class="andon-hero-clock__title">安灯看板</strong>
        <span class="andon-hero-clock__date">{{ liveDate }}</span>
      </div>
    </div>

    <div class="operator-hero-kpi-strip andon-hero-kpi-strip">
      <div
        v-for="(item, idx) in cards"
        :key="item.key"
        class="operator-hero-kpi-item"
        :class="{ 'is-last': idx === cards.length - 1 }"
      >
        <span class="operator-hero-kpi-item__label">{{ item.label }}</span>
        <strong class="operator-hero-kpi-item__value" :class="item.tone">
          {{ item.display }}
          <small v-if="item.badge" class="andon-kpi-badge">{{ item.badge }}</small>
        </strong>
      </div>
    </div>

    <div class="operator-hero-actions andon-hero-actions">
      <el-button class="operator-ui-btn operator-hero-btn" :auto-insert-space="false" :loading="loading" @click="$emit('refresh')">
        <span class="operator-ui-btn__label">刷新</span>
      </el-button>
      <el-button class="operator-ui-btn operator-hero-btn andon-btn-pull" type="warning" :auto-insert-space="false" @click="$emit('launch')">
        <span class="operator-ui-btn__label">发起安灯</span>
      </el-button>
      <el-button
        v-if="showHandle"
        class="operator-ui-btn operator-hero-btn"
        type="primary"
        :auto-insert-space="false"
        @click="$emit('handle')"
      >
        <span class="operator-ui-btn__label">进入处置</span>
      </el-button>
      <el-button
        v-if="showSettings"
        class="operator-ui-btn operator-hero-btn"
        :auto-insert-space="false"
        @click="$emit('settings')"
      >
        <span class="operator-ui-btn__label">设置</span>
      </el-button>
    </div>
  </div>
</template>

<script>
import SignalLamp from "./SignalLamp.vue";

export default {
  name: "AndonHeroBar",
  components: { SignalLamp },
  props: {
    summary: { type: Object, default: () => ({}) },
    loading: { type: Boolean, default: false },
    showSettings: { type: Boolean, default: false },
    showHandle: { type: Boolean, default: true },
    showMine: { type: Boolean, default: false },
  },
  emits: ["refresh", "launch", "handle", "settings"],
  data() {
    return { liveClock: "", liveDate: "", clockTimer: null };
  },
  computed: {
    cards() {
      const s = this.summary || {};
      const list = [
        {
          key: "active",
          label: "待处置",
          display: s.activeRecords ?? "—",
          tone: "tone-red",
          badge: s.slaOverdue > 0 ? `+${s.slaOverdue} 超时` : "",
        },
        { key: "sla", label: "SLA超时", display: s.slaOverdue ?? "—", tone: "tone-red" },
        { key: "avg", label: "平均响应(分)", display: s.avgResponseMinutes ?? "—", tone: "tone-blue" },
        { key: "closed", label: "本班已关闭", display: s.shiftClosed ?? "—", tone: "tone-green" },
      ];
      if (this.showMine) {
        list.push({ key: "mine", label: "待我处置", display: s.mineCount ?? "—", tone: "tone-indigo" });
      }
      return list;
    },
  },
  mounted() {
    this.tickClock();
    this.clockTimer = setInterval(this.tickClock, 1000);
  },
  beforeUnmount() {
    if (this.clockTimer) clearInterval(this.clockTimer);
  },
  methods: {
    tickClock() {
      const d = new Date();
      const p = (n) => String(n).padStart(2, "0");
      this.liveClock = `${p(d.getHours())}:${p(d.getMinutes())}:${p(d.getSeconds())}`;
      this.liveDate = d.toLocaleDateString("zh-CN", {
        year: "numeric",
        month: "2-digit",
        day: "2-digit",
        weekday: "short",
      });
    },
  },
};
</script>
