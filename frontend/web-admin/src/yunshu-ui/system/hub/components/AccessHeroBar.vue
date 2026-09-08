<template>
  <div class="operator-hero-bar sys-hub-hero">
    <div class="sys-hub-hero__intro">
      <h2 class="sys-hub-hero__title">权限与组织中心</h2>
      <p class="sys-hub-hero__desc">用户账号 · 角色权限 · 组织架构</p>
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
      <el-button class="operator-ui-btn operator-hero-btn" type="primary" :auto-insert-space="false" @click="$emit('create-user')">
        <span class="operator-ui-btn__label">新建用户</span>
      </el-button>
    </div>
  </div>
</template>

<script>
export default {
  name: "AccessHeroBar",
  props: {
    summary: { type: Object, default: () => ({}) },
    loading: { type: Boolean, default: false },
  },
  emits: ["refresh", "create-user"],
  computed: {
    cards() {
      const s = this.summary || {};
      return [
        { key: "users", label: "用户总数", display: s.userTotal ?? "—", tone: "kpi-blue" },
        { key: "roles", label: "角色数", display: s.roleTotal ?? "—", tone: "" },
        { key: "depts", label: "部门数", display: s.deptTotal ?? "—", tone: "" },
        { key: "online", label: "今日登录", display: s.onlineCount ?? "—", tone: "kpi-green" },
      ];
    },
  },
};
</script>
