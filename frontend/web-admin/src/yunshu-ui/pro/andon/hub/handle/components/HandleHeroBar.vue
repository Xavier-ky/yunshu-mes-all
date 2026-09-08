<template>
  <div class="operator-hero-bar sys-hub-hero">
    <div class="sys-hub-hero__intro">
      <h2 class="sys-hub-hero__title">呼叫处置</h2>
      <p class="sys-hub-hero__desc">待办响应 · 任务闭环</p>
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
      <el-button class="operator-ui-btn operator-hero-btn" type="primary" :auto-insert-space="false" @click="$emit('board')">
        <span class="operator-ui-btn__label">返回看板</span>
      </el-button>
    </div>
  </div>
</template>

<script>
export default {
  name: "HandleHeroBar",
  props: {
    summary: { type: Object, default: () => ({}) },
    loading: { type: Boolean, default: false },
  },
  emits: ["refresh", "board"],
  computed: {
    cards() {
      const s = this.summary || {};
      return [
        { key: "active", label: "待处置", display: s.activeCount ?? "—", tone: "tone-red" },
        { key: "mine", label: "待我处置", display: s.mineCount ?? "—", tone: "tone-indigo" },
        { key: "closed", label: "今日已关闭", display: s.todayClosed ?? "—", tone: "tone-green" },
        { key: "avg", label: "平均用时(分)", display: s.avgMinutes ?? "—", tone: "tone-blue" },
      ];
    },
  },
};
</script>
