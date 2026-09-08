<template>
  <div class="access-aside">
    <div class="access-module-switch">
      <button
        v-for="item in modules"
        :key="item.key"
        type="button"
        :class="{ active: module === item.key }"
        @click="$emit('update:module', item.key)"
      >
        <span>{{ item.label }}</span>
        <b>{{ item.count }}</b>
      </button>
    </div>

    <div class="stock-aside-summary">
      <div class="aside-summary-head">
        <span>{{ summaryTitle }}</span>
        <button class="aside-reset" type="button" @click="resetScope">重置筛选</button>
      </div>
      <div class="aside-stat-grid">
        <div v-for="stat in summaryStats" :key="stat.label">
          <b>{{ stat.value }}</b>
          <span>{{ stat.label }}</span>
        </div>
      </div>
    </div>

    <AccessOrgPicker
      v-if="showDeptTree"
      :dept-tree="deptTree"
      :dept-id="deptId"
      :module="module"
      :dept-user-counts="deptUserCounts"
      :user-total="summary.userTotal ?? 0"
      :dept-total="summary.deptTotal ?? 0"
      @update:deptId="$emit('update:deptId', $event)"
      @reset="resetDeptScope"
    />

    <AccessRoleNav
      v-if="module === 'roles'"
      :role-list="roleList"
      :role-id="roleId"
      :role-user-counts="roleUserCounts"
      :summary="summary"
      @update:roleId="$emit('update:roleId', $event)"
      @reset="resetRoleScope"
    />
  </div>
</template>

<script>
import AccessOrgPicker from "./AccessOrgPicker.vue";
import AccessRoleNav from "./AccessRoleNav.vue";

const MODULES = [
  { key: "users", label: "用户" },
  { key: "roles", label: "角色" },
  { key: "departments", label: "部门" },
  { key: "posts", label: "岗位" },
];

export default {
  name: "AccessAside",
  components: { AccessOrgPicker, AccessRoleNav },
  props: {
    module: { type: String, default: "users" },
    summary: { type: Object, default: () => ({}) },
    deptTree: { type: Array, default: () => [] },
    deptId: { type: [Number, String], default: null },
    status: { type: String, default: null },
    roleId: { type: [Number, String], default: null },
    roleList: { type: Array, default: () => [] },
    roleUserCounts: { type: Object, default: () => ({}) },
    postTotal: { type: Number, default: 0 },
    deptUserCounts: { type: Object, default: () => ({}) },
  },
  emits: [
    "update:module",
    "update:deptId",
    "update:status",
    "update:roleId",
    "dept-expand",
    "dept-collapse",
  ],
  computed: {
    modules() {
      const s = this.summary || {};
      return MODULES.map((m) => ({
        ...m,
        count:
          m.key === "users"
            ? s.userTotal ?? "—"
            : m.key === "roles"
              ? s.roleTotal ?? "—"
              : m.key === "departments"
                ? s.deptTotal ?? "—"
                : this.postTotal ?? s.postTotal ?? "—",
      }));
    },
    summaryTitle() {
      const map = {
        users: "用户总览",
        roles: "角色总览",
        departments: "部门总览",
        posts: "岗位总览",
      };
      return map[this.module] || "组织总览";
    },
    summaryStats() {
      const s = this.summary || {};
      if (this.module === "users") {
        return [
          { label: "用户总数", value: s.userTotal ?? "—" },
          { label: "启用", value: s.enabledUserCount ?? "—" },
          { label: "停用", value: s.disabledUserCount ?? "—" },
          { label: "今日登录", value: s.onlineCount ?? "—" },
        ];
      }
      if (this.module === "roles") {
        const assigned = Object.values(this.roleUserCounts || {}).reduce(
          (sum, n) => sum + Number(n || 0),
          0
        );
        return [
          { label: "角色数", value: s.roleTotal ?? "—" },
          { label: "启用角色", value: s.enabledRoleCount ?? s.roleTotal ?? "—" },
          { label: "已分配用户", value: assigned || "—" },
          { label: "今日登录", value: s.onlineCount ?? "—" },
        ];
      }
      if (this.module === "departments") {
        return [
          { label: "部门数", value: s.deptTotal ?? "—" },
          { label: "启用", value: s.enabledDeptCount ?? "—" },
          { label: "选中节点", value: this.deptId ? 1 : 0 },
          { label: "用户总数", value: s.userTotal ?? "—" },
        ];
      }
      return [
        { label: "岗位数", value: this.postTotal ?? s.postTotal ?? "—" },
        { label: "角色数", value: s.roleTotal ?? "—" },
        { label: "部门数", value: s.deptTotal ?? "—" },
        { label: "用户总数", value: s.userTotal ?? "—" },
      ];
    },
    showDeptTree() {
      return ["users", "posts", "departments"].includes(this.module);
    },
  },
  methods: {
    resetDeptScope() {
      this.$emit("update:deptId", null);
    },
    resetRoleScope() {
      this.$emit("update:roleId", null);
    },
    resetScope() {
      this.$emit("update:deptId", null);
      this.$emit("update:status", null);
      this.$emit("update:roleId", null);
    },
  },
};
</script>
