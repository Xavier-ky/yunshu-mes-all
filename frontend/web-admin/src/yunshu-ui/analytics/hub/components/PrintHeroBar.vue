<template>
  <div class="operator-hero-bar sys-hub-hero">
    <div class="sys-hub-hero__intro">
      <h2 class="sys-hub-hero__title">打印中心</h2>
      <p class="sys-hub-hero__desc">标签模板 · 打印客户端</p>
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
      <el-button class="operator-ui-btn operator-hero-btn" type="primary" :auto-insert-space="false" @click="$emit('create-template')">
        <span class="operator-ui-btn__label">新建模板</span>
      </el-button>
    </div>
  </div>
</template>

<script>
export default {
  name: "PrintHeroBar",
  props: {
    summary: { type: Object, default: () => ({}) },
    loading: { type: Boolean, default: false },
  },
  emits: ["refresh", "create-template"],
  computed: {
    cards() {
      const s = this.summary || {};
      return [
        { key: "templates", label: "模板总数", display: s.templateTotal ?? "—", tone: "kpi-blue" },
        { key: "enabled", label: "已启用", display: s.enabledTotal ?? "—", tone: "kpi-green" },
        { key: "clients", label: "客户端", display: s.clientTotal ?? "—", tone: "" },
      ];
    },
  },
};
</script>
