<template>
  <div class="app-container dv-command-center qc-command-center dv-workbench-page">
    <DvHeroBar
      :summary="summary"
      :loading="loading"
      show-analytics-link
      @refresh="refresh"
      @open-andon="openAndon"
      @go-analytics="goAnalytics"
    />
    <el-row :gutter="0" class="qc-hub-row operator-hub-row">
      <el-col :span="5" class="qc-hub-aside operator-hub-aside">
        <DvPendingAside
          :all-rows="pendingRows"
          :filtered-rows="filteredPendingRows"
          :selected-key="selectedPendingKey"
          :type-filter="typeFilter"
          :loading="pendingLoading"
          :user-label="userLabel"
          :employee-no="employeeNo"
          @update:type-filter="onTypeFilter"
          @select="onPendingSelect"
          @inspect="onInspect"
        />
      </el-col>
      <el-col :span="19" class="qc-hub-main-col operator-hub-main-col">
        <DvExecWorkbench
          :selected-row="selectedRow"
          :pending-rows="pendingRows"
          :recent-finished="recentFinished"
          :recent-loading="loading"
          :is-exec-route="isExecRoute"
          @inspect="onInspect"
          @machinery="goMachinery"
          @records="goRecords"
          @andon="openAndon"
          @open-records="goRecords"
        />
      </el-col>
    </el-row>
  </div>
</template>

<script>
import { getAuthUser } from "@/utils/auth-context";
import DvHeroBar from "./components/DvHeroBar.vue";
import DvPendingAside from "./components/DvPendingAside.vue";
import DvExecWorkbench from "./components/DvExecWorkbench.vue";
import { getDvSummary, listDvPending, getDvRecentFinished } from "@/yunshu-ui/api/mes/dv/workbench";

const EXEC_ROUTE = {
  CHECK: "dv-wb-check",
  MAINTEN: "dv-wb-mainten",
  REPAIR: "dv-wb-repair",
};

const TASK_QUERY_KEYS = [
  "taskType",
  "taskId",
  "machineryId",
  "machineryCode",
  "machineryName",
  "planId",
  "planCode",
  "planName",
  "status",
];

const QUEUE_FILTER = {
  check: "CHECK",
  mainten: "MAINTEN",
  repair: "REPAIR",
};

const TASK_TYPE_RANK = { REPAIR: 0, MAINTEN: 1, CHECK: 2 };

function sortPendingRows(rows) {
  return [...rows].sort((a, b) => {
    const ra = TASK_TYPE_RANK[a.taskType] ?? 9;
    const rb = TASK_TYPE_RANK[b.taskType] ?? 9;
    if (ra !== rb) return ra - rb;
    return String(b.sortTime || "").localeCompare(String(a.sortTime || ""));
  });
}

export default {
  name: "DvWorkbench",
  components: { DvHeroBar, DvPendingAside, DvExecWorkbench },
  data() {
    return {
      loading: false,
      pendingLoading: false,
      summary: {},
      pendingRows: [],
      recentFinished: [],
      typeFilter: "all",
      selectedPendingKey: "",
      authUser: {},
    };
  },
  computed: {
    userLabel() {
      return this.authUser.realName || this.authUser.displayName || this.authUser.username || "设备员";
    },
    employeeNo() {
      return this.authUser.username || "";
    },
    filteredPendingRows() {
      const rows =
        this.typeFilter === "all"
          ? this.pendingRows
          : this.pendingRows.filter((r) => r.taskType === this.typeFilter);
      return sortPendingRows(rows);
    },
    isExecRoute() {
      return String(this.$route.name || "").startsWith("dv-wb-");
    },
    selectedRow() {
      if (this.selectedPendingKey) {
        const hit = this.pendingRows.find((r) => this.pendingRowKey(r) === this.selectedPendingKey);
        if (hit) return hit;
      }
      return this.rowFromRouteQuery();
    },
  },
  mounted() {
    this.authUser = getAuthUser() || {};
    this.applyQueueQuery();
    this.refresh();
    this.syncFromRoute();
  },
  watch: {
    "$route.fullPath"() {
      this.applyQueueQuery();
      this.syncFromRoute();
    },
    typeFilter() {
      this.$nextTick(() => this.ensureDefaultTask(true));
    },
  },
  methods: {
    pendingRowKey(row) {
      return `${row.taskType}-${row.taskId}`;
    },
    applyQueueQuery() {
      const q = this.$route.query?.queue;
      if (q && QUEUE_FILTER[q]) {
        this.typeFilter = QUEUE_FILTER[q];
      }
    },
    rowFromRouteQuery() {
      const q = this.$route.query || {};
      if (!q.taskId || !q.taskType) return null;
      return (
        this.pendingRows.find(
          (r) => String(r.taskId) === String(q.taskId) && r.taskType === q.taskType
        ) || {
          taskType: q.taskType,
          taskId: q.taskId,
          machineryCode: q.machineryCode,
          machineryName: q.machineryName,
          planCode: q.planCode,
          planName: q.planName,
          status: q.status,
        }
      );
    },
    syncFromRoute() {
      if (!this.isExecRoute) return;
      const row = this.rowFromRouteQuery();
      if (row) this.selectedPendingKey = this.pendingRowKey(row);
    },
    onTypeFilter(v) {
      this.typeFilter = v;
    },
    ensureDefaultTask(force = false) {
      if (!force && this.isExecRoute && this.rowFromRouteQuery()) return;
      const rows = this.filteredPendingRows;
      if (!rows.length) return;
      const first = rows[0];
      if (!force && this.isExecRoute && this.selectedPendingKey === this.pendingRowKey(first)) return;
      this.navigateToTask(first);
    },
    async refresh() {
      this.loading = true;
      this.pendingLoading = true;
      try {
        const [s, pending, rf] = await Promise.all([
          getDvSummary(),
          listDvPending({ limit: 100 }),
          getDvRecentFinished(8),
        ]);
        this.summary = s?.data || {};
        this.pendingRows = sortPendingRows(pending?.rows || []);
        this.recentFinished = rf?.data || [];
        this.syncFromRoute();
        await this.$nextTick();
        this.ensureDefaultTask();
      } catch (e) {
        this.summary = {};
        this.pendingRows = [];
        this.recentFinished = [];
        this.$modal?.msgError?.(e?.message || "加载设备工作台数据失败，请确认后端已启动");
      } finally {
        this.loading = false;
        this.pendingLoading = false;
      }
    },
    onPendingSelect(row) {
      if (!row) return;
      this.selectedPendingKey = this.pendingRowKey(row);
      this.navigateToTask(row);
    },
    buildTaskQuery(row) {
      const query = {};
      TASK_QUERY_KEYS.forEach((k) => {
        const v = row?.[k];
        if (v !== null && v !== undefined && v !== "") query[k] = String(v);
      });
      return query;
    },
    navigateToTask(row) {
      if (!row) return;
      const routeName = EXEC_ROUTE[row.taskType];
      if (!routeName) return;
      this.selectedPendingKey = this.pendingRowKey(row);
      const query = this.buildTaskQuery(row);
      const current = this.$route;
      const sameTask =
        current.name === routeName &&
        String(current.query.taskId || "") === String(row.taskId || "") &&
        String(current.query.taskType || "") === String(row.taskType || "");
      if (sameTask) return;
      const nav = { name: routeName, query };
      const action = current.name === routeName ? this.$router.replace(nav) : this.$router.push(nav);
      action.catch(() => {});
    },
    onInspect(row) {
      this.navigateToTask(row);
    },
    goAnalytics() {
      this.$router.push("/app/equipment/analytics");
    },
    goMachinery() {
      const code = this.selectedRow?.machineryCode;
      this.$router.push(code ? `/app/equipment/machinery?machineryCode=${code}` : "/app/equipment/machinery");
    },
    goRecords() {
      const t = this.selectedRow?.taskType;
      const type = t === "MAINTEN" ? "MAINTEN" : t === "CHECK" ? "CHECK" : undefined;
      this.$router.push({ path: "/app/equipment/check-maint", query: { tab: "records", ...(type ? { type } : {}) } });
    },
    openAndon() {
      const row = this.selectedRow;
      const query = { category: "equipment" };
      if (row?.workstationId || row?.stationId) {
        query.stationId = String(row.workstationId || row.stationId);
      }
      if (row?.dispatchId) query.dispatchId = String(row.dispatchId);
      if (row?.machineryId) query.machineryId = String(row.machineryId);
      if (row?.machineryCode) query.machineryCode = row.machineryCode;
      this.$router.push({ path: "/app/andon/launch", query });
    },
  },
};
</script>
