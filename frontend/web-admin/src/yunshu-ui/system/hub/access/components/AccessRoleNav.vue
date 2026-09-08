<template>
  <AccessScopeNav
    title="角色导航"
    :depth-hint="depthHint"
    :crumb-text="crumbText"
    :insight-items="insightItems"
    :insight-layout="selectedRole ? 'stack' : 'inline'"
    :search-text="roleFilter"
    search-placeholder="搜索角色名 / 权限字符"
    @update:search-text="roleFilter = $event"
  >
    <template #actions>
      <button type="button" class="is-reset" @click.stop="onReset">重置</button>
    </template>

    <div class="access-scope-nav__tree-wrap hub-tree-wrap access-org-picker">
      <div class="access-org-dist">
        <div class="access-org-dist__chart">
          <div ref="donutRef" class="access-org-dist__donut"></div>
          <div class="access-org-dist__center">
            <b>{{ assignedUserTotal }}</b>
            <span>已分配用户</span>
          </div>
        </div>

        <div class="access-org-dist__list">
          <button
            type="button"
            class="access-org-dist__row is-all"
            :class="{ 'is-active': !roleId }"
            @click="onRoleClick(null)"
          >
            <span class="access-org-dist__dot access-org-dist__dot--all"></span>
            <span class="access-org-dist__name">全部角色</span>
            <span class="access-org-dist__bar"><i style="width: 100%"></i></span>
            <span class="access-org-dist__val">{{ assignedUserTotal }}<em>100%</em></span>
          </button>

          <button
            v-for="role in distribution"
            :key="role.roleId"
            type="button"
            class="access-org-dist__row"
            :class="{ 'is-active': String(roleId) === String(role.roleId) }"
            @click="onRoleClick(role.roleId)"
          >
            <span class="access-org-dist__dot" :style="{ background: role.color }"></span>
            <span class="access-org-dist__name" :title="role.roleName">{{ role.roleName }}</span>
            <span class="access-org-dist__bar"><i :style="{ width: role.barWidth + '%', background: role.color }"></i></span>
            <span class="access-org-dist__val">{{ role.count }}<em>{{ role.pct }}%</em></span>
          </button>

          <div v-if="!distribution.length && roleFilter" class="access-scope-nav__empty">无匹配角色</div>
        </div>
      </div>
    </div>
  </AccessScopeNav>
</template>

<script>
import * as echarts from "echarts";
import AccessScopeNav from "./AccessScopeNav.vue";

const PALETTE = [
  "#1769e0",
  "#3b82f6",
  "#0ea5e9",
  "#6366f1",
  "#14b8a6",
  "#f59e0b",
  "#8b5cf6",
  "#64748b",
];

export default {
  name: "AccessRoleNav",
  components: { AccessScopeNav },
  props: {
    roleList: { type: Array, default: () => [] },
    roleId: { type: [Number, String], default: null },
    roleUserCounts: { type: Object, default: () => ({}) },
    summary: { type: Object, default: () => ({}) },
  },
  emits: ["update:roleId", "reset"],
  data() {
    return {
      roleFilter: "",
      donutChart: null,
    };
  },
  computed: {
    selectedRole() {
      if (!this.roleId) return null;
      return (this.roleList || []).find((r) => String(r.roleId) === String(this.roleId)) || null;
    },
    filteredRoles() {
      const q = (this.roleFilter || "").trim().toLowerCase();
      if (!q) return this.roleList || [];
      return (this.roleList || []).filter((role) => {
        const name = (role.roleName || "").toLowerCase();
        const key = (role.roleKey || role.roleCode || "").toLowerCase();
        return name.includes(q) || key.includes(q);
      });
    },
    depthHint() {
      const total = (this.roleList || []).length;
      return total ? `共 ${total} 个角色` : "";
    },
    crumbText() {
      if (!this.selectedRole) return "全部角色";
      const key = this.selectedRole.roleKey || this.selectedRole.roleCode || "—";
      return `${this.selectedRole.roleName} › ${key}`;
    },
    assignedUserTotal() {
      return Object.values(this.roleUserCounts || {}).reduce((sum, n) => sum + Number(n || 0), 0);
    },
    enabledRoleCount() {
      const s = this.summary || {};
      if (s.enabledRoleCount != null) return s.enabledRoleCount;
      return (this.roleList || []).filter((r) => r.status === "0" || r.status === "ENABLED").length;
    },
    distribution() {
      const total = this.assignedUserTotal || 0;
      const list = this.filteredRoles.map((role, idx) => {
        const count = this.roleUserCount(role.roleId);
        return {
          ...role,
          count,
          pct: total ? Math.round((count / total) * 100) : 0,
          color: PALETTE[idx % PALETTE.length],
        };
      });
      list.sort((a, b) => b.count - a.count);
      const maxCount = list.reduce((m, d) => Math.max(m, d.count), 0);
      return list.map((d) => ({
        ...d,
        barWidth: maxCount ? Math.round((d.count / maxCount) * 100) : 0,
      }));
    },
    insightItems() {
      if (this.selectedRole) {
        const role = this.selectedRole;
        const statusLabel = role.status === "0" || role.status === "ENABLED" ? "正常" : "停用";
        const roleKey = role.roleKey || role.roleCode || "—";
        return [
          { key: "users", label: "用户", value: this.roleUserCount(role.roleId) },
          { key: "key", label: "权限", value: roleKey, title: roleKey },
          { key: "sort", label: "顺序", value: role.roleSort ?? "—" },
          { key: "status", label: "状态", value: statusLabel },
        ];
      }
      const s = this.summary || {};
      return [
        { key: "total", label: "角色总数", value: s.roleTotal ?? (this.roleList || []).length },
        { key: "enabled", label: "启用", value: this.enabledRoleCount },
        { key: "assigned", label: "已分配用户", value: this.assignedUserTotal },
        { key: "online", label: "今日登录", value: s.onlineCount ?? "—" },
      ];
    },
  },
  watch: {
    distribution: {
      deep: true,
      handler() {
        this.$nextTick(() => this.renderDonut());
      },
    },
  },
  mounted() {
    this.$nextTick(() => this.renderDonut());
    window.addEventListener("resize", this.onDonutResize);
  },
  beforeUnmount() {
    window.removeEventListener("resize", this.onDonutResize);
    this.donutChart?.dispose();
    this.donutChart = null;
  },
  methods: {
    onDonutResize() {
      this.donutChart?.resize();
    },
    renderDonut() {
      const el = this.$refs.donutRef;
      if (!el) return;
      this.donutChart = echarts.getInstanceByDom(el) || echarts.init(el);
      const data = this.distribution.map((d) => ({
        name: d.roleName,
        value: d.count,
        itemStyle: { color: d.color },
      }));
      const hasData = this.assignedUserTotal > 0 && data.some((d) => d.value > 0);
      this.donutChart.setOption(
        {
          tooltip: {
            trigger: "item",
            formatter: (p) => `${p.name}<br/>用户 ${p.value} · ${p.percent}%`,
          },
          series: [
            {
              type: "pie",
              radius: ["62%", "86%"],
              center: ["50%", "50%"],
              avoidLabelOverlap: false,
              silent: !hasData,
              label: { show: false },
              labelLine: { show: false },
              itemStyle: { borderColor: "#fff", borderWidth: 2 },
              emphasis: { scale: hasData, scaleSize: 4 },
              data: hasData
                ? data
                : [{ name: "暂无数据", value: 1, itemStyle: { color: "#e8edf3" }, tooltip: { show: false } }],
            },
          ],
        },
        true
      );
    },
    roleUserCount(roleId) {
      return this.roleUserCounts[String(roleId)] ?? 0;
    },
    onRoleClick(id) {
      const next = id == null || String(this.roleId) === String(id) ? null : id;
      this.$emit("update:roleId", next);
    },
    onReset() {
      this.roleFilter = "";
      this.$emit("reset");
      this.$emit("update:roleId", null);
    },
  },
};
</script>
