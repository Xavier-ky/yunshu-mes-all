<template>
  <div class="app-container access-command-center">
    <div class="access-hub-head">
      <h2 class="access-hub-title">权限与组织</h2>
      <div class="access-hub-actions">
        <el-button :loading="summaryLoading" size="default" @click="refreshAll">刷新</el-button>
        <el-button type="primary" size="default" @click="createUser">新建用户</el-button>
      </div>
    </div>

    <el-row :gutter="16" class="access-hub-row">
      <el-col :span="6" :xs="24" class="access-hub-aside">
        <AccessAside
          :module="activeModule"
          :summary="summary"
          :dept-tree="deptTree"
          :dept-id="scopeDeptId"
          :status="scopeStatus"
          :role-id="scopeRoleId"
          :role-list="roleList"
          :role-user-counts="roleUserCounts"
          :post-total="postTotal"
          :dept-user-counts="deptUserCounts"
          @update:module="onModuleChange"
          @update:deptId="onDeptChange"
          @update:status="onStatusChange"
          @update:roleId="onRoleChange"
          @dept-expand="onDeptExpand(true)"
          @dept-collapse="onDeptExpand(false)"
        />
      </el-col>
      <el-col :span="18" :xs="24" class="access-hub-main">
        <AccessMainPanel
          ref="mainPanel"
          :module="activeModule"
          :dept-id="scopeDeptId"
          :status="scopeStatus"
          :role-id="scopeRoleId"
          @changed="onPanelChanged"
        />
      </el-col>
    </el-row>
  </div>
</template>

<script>
import AccessAside from "./components/AccessAside.vue";
import AccessMainPanel from "./components/AccessMainPanel.vue";
import { getAccessSummary, getAccessOrgStats, getAccessRoleStats } from "@/yunshu-ui/api/system/access";
import { treeselect } from "@/yunshu-ui/api/system/dept";
import { listRole } from "@/yunshu-ui/api/system/role";
import { listPost } from "@/yunshu-ui/api/system/post";

const MODULES = new Set(["users", "roles", "departments", "posts"]);
const LEGACY_TAB_MAP = { users: "users", roles: "roles", departments: "departments", posts: "posts" };

export default {
  name: "SysAccessHub",
  components: { AccessAside, AccessMainPanel },
  data() {
    return {
      activeModule: "users",
      summary: {},
      summaryLoading: false,
      deptTree: [],
      deptUserCounts: {},
      roleList: [],
      roleUserCounts: {},
      postTotal: 0,
      scopeDeptId: null,
      scopeStatus: null,
      scopeRoleId: null,
    };
  },
  mounted() {
    this.syncFromRoute();
    this.refreshAll();
  },
  watch: {
    "$route.query"() {
      this.syncFromRoute();
    },
  },
  methods: {
    syncFromRoute() {
      const q = this.$route.query || {};
      let mod = q.module;
      if (!mod && q.tab && LEGACY_TAB_MAP[q.tab]) {
        mod = q.tab;
      }
      if (mod && MODULES.has(mod)) {
        this.activeModule = mod;
      }
      this.scopeDeptId = q.deptId ? Number(q.deptId) || q.deptId : null;
      this.scopeStatus = q.status ?? null;
      this.scopeRoleId = q.roleId ? Number(q.roleId) || q.roleId : null;
    },
    buildQuery(patch = {}) {
      const query = { module: this.activeModule };
      if (this.scopeDeptId != null && this.scopeDeptId !== "") query.deptId = String(this.scopeDeptId);
      if (this.scopeStatus != null && this.scopeStatus !== "") query.status = this.scopeStatus;
      if (this.scopeRoleId != null && this.scopeRoleId !== "" && this.activeModule === "roles") {
        query.roleId = String(this.scopeRoleId);
      }
      return { ...query, ...patch };
    },
    pushRoute(patch = {}) {
      this.$router.replace({ query: this.buildQuery(patch) });
    },
    onModuleChange(name) {
      this.activeModule = name;
      if (name !== "roles") this.scopeRoleId = null;
      this.pushRoute({ module: name, roleId: name === "roles" && this.scopeRoleId ? String(this.scopeRoleId) : undefined });
    },
    onDeptChange(id) {
      this.scopeDeptId = id;
      this.pushRoute({ deptId: id != null ? String(id) : undefined });
    },
    onStatusChange(status) {
      this.scopeStatus = status;
      this.pushRoute({ status: status ?? undefined });
    },
    onRoleChange(id) {
      this.scopeRoleId = id;
      this.pushRoute({ roleId: id != null ? String(id) : undefined });
    },
    onPanelChanged() {
      this.loadRoleList();
      this.loadRoleStats();
      this.loadOrgStats();
      this.loadSummary();
    },
    onDeptExpand(expand) {
      const panel = this.$refs.mainPanel?.getPanel?.();
      if (!panel) return;
      if (expand && panel.expandAll) panel.expandAll();
      else if (!expand && panel.collapseAll) panel.collapseAll();
      else panel.toggleExpandAll?.();
    },
    async refreshAll() {
      this.summaryLoading = true;
      try {
        await Promise.all([
          this.loadSummary(),
          this.loadOrgStats(),
          this.loadDeptTree(),
          this.loadRoleList(),
          this.loadRoleStats(),
          this.loadPostTotal(),
        ]);
      } finally {
        this.summaryLoading = false;
      }
    },
    async loadOrgStats() {
      try {
        const res = await getAccessOrgStats();
        const data = res?.data || {};
        this.deptUserCounts = data.deptUserCounts || {};
        if (data.userTotal != null && this.summary.userTotal == null) {
          this.summary = { ...this.summary, userTotal: data.userTotal };
        }
        if (data.deptTotal != null && this.summary.deptTotal == null) {
          this.summary = { ...this.summary, deptTotal: data.deptTotal };
        }
      } catch {
        this.deptUserCounts = {};
      }
    },
    async loadSummary() {
      try {
        const res = await getAccessSummary();
        this.summary = res?.data || {};
        if (this.summary.postTotal != null) this.postTotal = this.summary.postTotal;
      } catch {
        this.summary = {};
      }
    },
    async loadDeptTree() {
      try {
        const res = await treeselect();
        this.deptTree = res?.data || [];
      } catch {
        this.deptTree = [];
      }
    },
    async loadRoleList() {
      try {
        const res = await listRole({ pageNum: 1, pageSize: 500 });
        this.roleList = res?.rows || [];
      } catch {
        this.roleList = [];
      }
    },
    async loadRoleStats() {
      try {
        const res = await getAccessRoleStats();
        this.roleUserCounts = res?.data?.roleUserCounts || {};
      } catch {
        this.roleUserCounts = {};
      }
    },
    async loadPostTotal() {
      if (this.summary?.postTotal != null) {
        this.postTotal = this.summary.postTotal;
        return;
      }
      try {
        const res = await listPost({ pageNum: 1, pageSize: 1 });
        this.postTotal = res?.total ?? 0;
      } catch {
        this.postTotal = 0;
      }
    },
    createUser() {
      if (this.activeModule !== "users") {
        this.onModuleChange("users");
      }
      this.$nextTick(() => {
        this.$refs.mainPanel?.getPanel?.()?.handleAdd?.();
      });
    },
  },
};
</script>
