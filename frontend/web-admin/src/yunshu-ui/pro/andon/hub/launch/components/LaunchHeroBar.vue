<template>
  <div class="operator-hero-bar andon-command-hero andon-launch-hero">
    <div class="andon-hero-clock-zone">
      <span class="andon-launch-hero__icon" aria-hidden="true">⚠</span>
      <span class="andon-hero-clock-divider" aria-hidden="true" />
      <div class="andon-hero-clock-info">
        <strong class="andon-hero-clock__title">发起安灯</strong>
        <span class="andon-hero-clock__date">{{ userLabel }} · 一线拉绳</span>
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
        <strong class="operator-hero-kpi-item__value" :class="item.tone">{{ item.display }}</strong>
      </div>
    </div>

    <div class="operator-hero-actions andon-hero-actions">
      <el-button class="operator-ui-btn operator-hero-btn" :auto-insert-space="false" :loading="loading" @click="$emit('refresh')">
        <span class="operator-ui-btn__label">刷新</span>
      </el-button>
      <el-button class="operator-ui-btn operator-hero-btn" :auto-insert-space="false" @click="$emit('board')">
        <span class="operator-ui-btn__label">返回看板</span>
      </el-button>
      <el-button
        v-if="showBatch"
        class="operator-ui-btn operator-hero-btn"
        :auto-insert-space="false"
        @click="$emit('batch')"
      >
        <span class="operator-ui-btn__label">批量发起</span>
      </el-button>
      <el-button class="operator-ui-btn operator-hero-btn" type="primary" :auto-insert-space="false" @click="$emit('handle')">
        <span class="operator-ui-btn__label">进入处置</span>
      </el-button>
    </div>
  </div>
</template>

<script>
export default {
  name: "LaunchHeroBar",
  props: {
    loading: { type: Boolean, default: false },
    userLabel: { type: String, default: "—" },
    eligibleCount: { type: Number, default: 0 },
    activeStationCount: { type: Number, default: 0 },
    todayCount: { type: Number, default: 0 },
    showBatch: { type: Boolean, default: false },
  },
  emits: ["refresh", "board", "batch", "handle"],
  computed: {
    cards() {
      return [
        { key: "user", label: "发起人", display: this.userLabel, tone: "" },
        { key: "eligible", label: "可发起派工", display: this.eligibleCount, tone: "tone-blue" },
        { key: "active", label: "工位待处置", display: this.activeStationCount, tone: "tone-red" },
        { key: "today", label: "今日已发起", display: this.todayCount, tone: "tone-green" },
      ];
    },
  },
};
</script>
