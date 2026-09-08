<template>
  <AccessScopeNav
    title="组织架构"
    :depth-hint="depthHint"
    :crumb-text="crumbText"
    :insight-items="insightItems"
    :search-text="deptFilter"
    search-placeholder="搜索部门"
    :show-status-filter="showStatusFilter"
    :status="status"
    @update:search-text="deptFilter = $event"
    @update:status="$emit('update:status', $event)"
  >
    <template #actions>
      <button
        type="button"
        :disabled="!hasExpandableNodes"
        :title="expandBtnTitle"
        @click.stop="onExpandAll"
      >展开</button>
      <button
        type="button"
        :disabled="!hasExpandableNodes"
        :title="expandBtnTitle"
        @click.stop="onCollapseAll"
      >折叠</button>
      <button type="button" class="is-reset" @click.stop="onReset">重置</button>
    </template>

    <div class="access-scope-nav__tree-wrap hub-tree-wrap access-org-tree--guide">
      <el-tree
        ref="deptTree"
        :data="deptTree"
        :props="treeProps"
        node-key="id"
        highlight-current
        :expand-on-click-node="false"
        :filter-node-method="filterDeptNode"
        default-expand-all
        :current-node-key="deptId || undefined"
        @node-click="onDeptClick"
      >
        <template #default="{ data }">
          <div class="access-org-node">
            <span class="access-org-node__label">{{ data.label }}</span>
            <span class="access-org-node__count">{{ displayCount(data) }}</span>
          </div>
        </template>
      </el-tree>
    </div>
  </AccessScopeNav>
</template>

<script>
import AccessScopeNav from "./AccessScopeNav.vue";

function findNode(nodes, id) {
  if (id == null || id === "") return null;
  for (const node of nodes || []) {
    if (String(node.id) === String(id)) return node;
    const found = findNode(node.children, id);
    if (found) return found;
  }
  return null;
}

function findPath(nodes, id, trail = []) {
  for (const node of nodes || []) {
    const next = [...trail, node];
    if (String(node.id) === String(id)) return next;
    const found = findPath(node.children, id, next);
    if (found) return found;
  }
  return null;
}

function countNodes(nodes) {
  let n = 0;
  for (const node of nodes || []) {
    n += 1;
    n += countNodes(node.children);
  }
  return n;
}

function maxDepth(nodes, depth = 1) {
  if (!nodes?.length) return Math.max(0, depth - 1);
  let max = depth;
  for (const node of nodes) {
    max = Math.max(max, maxDepth(node.children, depth + 1));
  }
  return max;
}

function subtreeUserTotal(node, counts) {
  if (!node) return 0;
  let total = Number(counts[String(node.id)] || 0);
  for (const child of node.children || []) {
    total += subtreeUserTotal(child, counts);
  }
  return total;
}

export default {
  name: "AccessOrgTree",
  components: { AccessScopeNav },
  props: {
    deptTree: { type: Array, default: () => [] },
    deptId: { type: [Number, String], default: null },
    module: { type: String, default: "users" },
    deptUserCounts: { type: Object, default: () => ({}) },
    userTotal: { type: Number, default: 0 },
    deptTotal: { type: Number, default: 0 },
    showStatusFilter: { type: Boolean, default: false },
    status: { type: String, default: null },
  },
  emits: ["update:deptId", "update:status", "expand-all", "collapse-all", "reset"],
  data() {
    return {
      deptFilter: "",
      treeProps: { children: "children", label: "label" },
    };
  },
  computed: {
    hasExpandableNodes() {
      const walk = (nodes) => {
        for (const node of nodes || []) {
          if (node.children?.length) return true;
          if (walk(node.children)) return true;
        }
        return false;
      };
      return walk(this.deptTree);
    },
    expandBtnTitle() {
      return this.hasExpandableNodes
        ? "展开或折叠全部层级"
        : "当前均为一级部门，无下级可展开";
    },
    rootNodeKey() {
      const root = (this.deptTree || [])[0];
      return root?.id ?? null;
    },
    focusNode() {
      if (!this.deptId) return null;
      const node = this.selectedNode;
      if (node && this.isRootNode(node)) return null;
      return node;
    },
    selectedNode() {
      return findNode(this.deptTree, this.deptId);
    },
    breadcrumbPath() {
      if (!this.deptId) return [];
      return findPath(this.deptTree, this.deptId) || [];
    },
    crumbText() {
      if (!this.breadcrumbPath.length) return "全部组织";
      return this.breadcrumbPath.map((n) => n.label).join(" › ");
    },
    depthHint() {
      const depth = maxDepth(this.deptTree);
      const total = this.deptTotal || countNodes(this.deptTree);
      if (!total) return "";
      if (!this.hasExpandableNodes) return `一级 · ${total} 部门`;
      return `L${depth || 1} · ${total} 部门`;
    },
    insightItems() {
      if (this.focusNode) {
        const childCount = (this.focusNode.children || []).length;
        const depth = this.breadcrumbPath.length;
        return [
          { key: "direct", label: "直属用户", value: this.directCount(this.deptId) },
          { key: "children", label: "子部门", value: childCount },
          { key: "level", label: "层级", value: `L${depth}` },
          { key: "cover", label: "覆盖", value: subtreeUserTotal(this.focusNode, this.deptUserCounts) },
        ];
      }
      return [
        { key: "depts", label: "部门总数", value: this.deptTotal || countNodes(this.deptTree) },
        { key: "depth", label: "最大深度", value: `L${maxDepth(this.deptTree) || 1}` },
        { key: "users", label: "用户总数", value: this.userTotal ?? "—" },
        { key: "roots", label: "根节点", value: (this.deptTree || []).length },
      ];
    },
  },
  watch: {
    deptFilter(val) {
      this.$refs.deptTree?.filter(val);
    },
    deptId(val) {
      if (val != null && this.isRootNode({ id: val })) {
        this.$emit("update:deptId", null);
        return;
      }
      this.$nextTick(() => this.syncTreeCurrentKey(val));
    },
    deptTree: {
      handler() {
        this.$nextTick(() => this.applyDefaultExpansion());
      },
      deep: true,
    },
  },
  mounted() {
    this.$nextTick(() => {
      this.applyDefaultExpansion();
      this.syncTreeCurrentKey(this.deptId);
    });
  },
  methods: {
    isRootNode(data) {
      return (this.deptTree || []).some((n) => String(n.id) === String(data.id));
    },
    syncTreeCurrentKey(deptId) {
      const tree = this.$refs.deptTree;
      if (!tree) return;
      if (deptId != null && deptId !== "") {
        tree.setCurrentKey(deptId);
      } else if (this.rootNodeKey != null) {
        tree.setCurrentKey(this.rootNodeKey);
      } else {
        tree.setCurrentKey(null);
      }
    },
    displayCount(data) {
      if (this.isRootNode(data)) {
        const node = findNode(this.deptTree, data.id);
        return subtreeUserTotal(node, this.deptUserCounts) || this.userTotal || 0;
      }
      return this.deptUserCounts[String(data.id)] ?? 0;
    },
    directCount(id) {
      return this.deptUserCounts[String(id)] ?? 0;
    },
    filterDeptNode(value, data) {
      if (!value) return true;
      return (data.label || "").indexOf(value) !== -1;
    },
    onDeptClick(data) {
      if (this.isRootNode(data)) {
        this.$emit("update:deptId", null);
        this.$nextTick(() => this.syncTreeCurrentKey(null));
        return;
      }
      const next = this.deptId === data.id ? null : data.id;
      this.$emit("update:deptId", next);
      this.$nextTick(() => this.syncTreeCurrentKey(next));
    },
    onReset() {
      this.deptFilter = "";
      this.$refs.deptTree?.filter("");
      this.$emit("reset");
      this.$nextTick(() => this.syncTreeCurrentKey(null));
    },
    setAllExpanded(expanded) {
      const tree = this.$refs.deptTree;
      const nodesMap = tree?.store?.nodesMap;
      if (!nodesMap) return;
      Object.keys(nodesMap).forEach((key) => {
        const node = nodesMap[key];
        if (node.childNodes?.length) {
          node.expanded = expanded;
        }
      });
    },
    applyDefaultExpansion() {
      this.setAllExpanded(true);
    },
    onExpandAll() {
      if (!this.hasExpandableNodes) return;
      this.setAllExpanded(true);
      this.$emit("expand-all");
    },
    onCollapseAll() {
      if (!this.hasExpandableNodes) return;
      this.setAllExpanded(false);
      this.$emit("collapse-all");
    },
  },
};
</script>
