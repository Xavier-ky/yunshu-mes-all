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
      <button type="button" class="is-reset" @click.stop="onReset">重置</button>
    </template>

    <div class="access-scope-nav__tree-wrap hub-tree-wrap access-org-picker">
      <div class="access-org-dist">
        <div class="access-org-dist__chart">
          <div ref="donutRef" class="access-org-dist__donut"></div>
          <div class="access-org-dist__center">
            <b>{{ allOrgTotal }}</b>
            <span>用户总数</span>
          </div>
        </div>

        <div class="access-org-dist__list">
          <button
            type="button"
            class="access-org-dist__row is-all"
            :class="{ 'is-active': deptId == null || deptId === '' }"
            @click="selectAll"
          >
            <span class="access-org-dist__dot access-org-dist__dot--all"></span>
            <span class="access-org-dist__name">全部组织</span>
            <span class="access-org-dist__bar"><i style="width: 100%"></i></span>
            <span class="access-org-dist__val">{{ allOrgTotal }}<em>100%</em></span>
          </button>

          <button
            v-for="dept in distribution"
            :key="dept.id"
            type="button"
            class="access-org-dist__row"
            :class="{ 'is-active': isCardActive(dept) }"
            @click="onCardClick(dept)"
          >
            <span class="access-org-dist__dot" :style="{ background: dept.color }"></span>
            <span class="access-org-dist__name" :title="dept.label">{{ dept.label }}</span>
            <span class="access-org-dist__bar"><i :style="{ width: dept.barWidth + '%', background: dept.color }"></i></span>
            <span class="access-org-dist__val">{{ dept.count }}<em>{{ dept.pct }}%</em></span>
          </button>

          <div v-if="!distribution.length && deptFilter" class="access-org-picker__empty">未找到匹配的部门</div>
        </div>
      </div>
    </div>
  </AccessScopeNav>
</template>

<script>
import * as echarts from "echarts";
import AccessScopeNav from "./AccessScopeNav.vue";
import {
  findNode,
  findPath,
  countNodes,
  maxDepth,
  subtreeUserTotal,
  getTopLevelDepts,
  getCorpRoot,
  isCorpRoot,
} from "../access-org-utils.js";

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
  name: "AccessOrgPicker",
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
  emits: ["update:deptId", "update:status", "reset"],
  data() {
    return {
      deptFilter: "",
    };
  },
  computed: {
    topLevelDepts() {
      return getTopLevelDepts(this.deptTree);
    },
    filteredTopDepts() {
      const q = (this.deptFilter || "").trim();
      if (!q) return this.topLevelDepts;
      return this.topLevelDepts.filter((dept) => {
        if ((dept.label || "").includes(q)) return true;
        return (dept.children || []).some((child) => (child.label || "").includes(q));
      });
    },
    corpRoot() {
      return getCorpRoot(this.deptTree);
    },
    allOrgTotal() {
      if (this.corpRoot) {
        return subtreeUserTotal(this.corpRoot, this.deptUserCounts) || this.userTotal || 0;
      }
      return this.userTotal || 0;
    },
    focusNode() {
      if (!this.deptId) return null;
      const node = this.selectedNode;
      if (node && isCorpRoot(this.deptTree, node)) return null;
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
      return this.breadcrumbPath
        .filter((n) => !isCorpRoot(this.deptTree, n))
        .map((n) => n.label)
        .join(" › ") || "全部组织";
    },
    depthHint() {
      const depth = maxDepth(this.deptTree);
      const total = this.deptTotal || countNodes(this.deptTree);
      if (!total) return "";
      const hasChildren = this.topLevelDepts.some((d) => d.children?.length);
      if (!hasChildren) return `一级 · ${total} 部门`;
      return `L${depth || 1} · ${total} 部门`;
    },
    distribution() {
      const total = this.allOrgTotal || 0;
      const list = this.filteredTopDepts.map((dept, idx) => {
        const count = this.cardCount(dept);
        return {
          ...dept,
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
      if (this.focusNode) {
        const childCount = (this.focusNode.children || []).length;
        const depth = this.breadcrumbPath.filter((n) => !isCorpRoot(this.deptTree, n)).length;
        return [
          { key: "direct", label: "直属用户", value: this.directCount(this.deptId) },
          { key: "children", label: "子部门", value: childCount },
          { key: "level", label: "层级", value: `L${depth || 1}` },
          {
            key: "cover",
            label: "覆盖",
            value: subtreeUserTotal(this.focusNode, this.deptUserCounts),
          },
        ];
      }
      return [
        { key: "depts", label: "部门总数", value: this.deptTotal || countNodes(this.deptTree) },
        { key: "depth", label: "最大深度", value: `L${maxDepth(this.deptTree) || 1}` },
        { key: "users", label: "用户总数", value: this.userTotal ?? "—" },
        { key: "roots", label: "一级部门", value: this.topLevelDepts.length },
      ];
    },
  },
  watch: {
    deptId(val) {
      if (val != null && isCorpRoot(this.deptTree, { id: val })) {
        this.$emit("update:deptId", null);
      }
    },
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
        name: d.label,
        value: d.count,
        itemStyle: { color: d.color },
      }));
      const hasData = this.allOrgTotal > 0 && data.some((d) => d.value > 0);
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
    directCount(id) {
      return this.deptUserCounts[String(id)] ?? 0;
    },
    cardCount(dept) {
      return subtreeUserTotal(dept, this.deptUserCounts);
    },
    isCardActive(dept) {
      if (this.deptId == null || this.deptId === "") return false;
      if (String(this.deptId) === String(dept.id)) return true;
      return (dept.children || []).some((c) => String(c.id) === String(this.deptId));
    },
    selectAll() {
      this.$emit("update:deptId", null);
    },
    onCardClick(dept) {
      this.$emit("update:deptId", dept.id);
    },
    onReset() {
      this.deptFilter = "";
      this.$emit("reset");
      this.$emit("update:deptId", null);
    },
  },
};
</script>
