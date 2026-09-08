<template>
  <div class="app-container qc-command-center qc-workbench-page">
    <QcHeroBar
      :summary="summary"
      :loading="loading"
      show-analytics-link
      @refresh="refresh"
      @open-andon="$refs.andonLaunch?.open()"
      @go-analytics="goAnalytics"
    />

    <el-row :gutter="0" class="qc-hub-row operator-hub-row">
      <el-col :span="5" class="qc-hub-aside operator-hub-aside">
        <QcPendingAside
          :all-rows="pendingRows"
          :filtered-rows="filteredPendingRows"
          :selected-key="selectedPendingKey"
          :type-filter="typeFilter"
          :loading="pendingLoading"
          :user-label="userLabel"
          :employee-no="employeeNo"
          :initial-keyword="routeKeyword"
          @update:type-filter="typeFilter = $event"
          @select="onPendingSelect"
          @inspect="onInspect"
        />
      </el-col>
      <el-col :span="19" class="qc-hub-main-col operator-hub-main-col">
        <QcExecWorkbench
          :selected-row="selectedRow"
          :pending-rows="pendingRows"
          :recent-finished="recentFinished"
          :recent-loading="loading"
          :is-exec-route="isExecRoute"
          @inspect="onInspect"
          @records="openRecords"
          @config="openConfig"
          @andon="$refs.andonLaunch?.open()"
          @open-records="openRecordsTab"
        />
      </el-col>
    </el-row>

    <QcRecordsHub ref="recordsRef" />
    <QcConfigHub ref="configRef" />
    <AndonLaunch ref="andonLaunch" />
  </div>
</template>

<script>
import { getAuthUser } from "@/utils/auth-context";
import QcHeroBar from "@/yunshu-ui/qc/analytics/components/QcHeroBar.vue";
import QcPendingAside from "@/yunshu-ui/qc/analytics/components/QcPendingAside.vue";
import QcExecWorkbench from "./components/QcExecWorkbench.vue";
import QcRecordsHub from "@/yunshu-ui/qc/hub/records.vue";
import QcConfigHub from "@/yunshu-ui/qc/hub/config.vue";
import AndonLaunch from "@/yunshu-ui/pro/andon/launch.vue";
import { listPending, sortQcPendingRecentFirst } from "@/yunshu-ui/api/mes/qc/pending";
import { getQcSummary, getQcRecentFinished } from "@/yunshu-ui/api/mes/qc/analytics";

const EXEC_ROUTE = {
  IQC: "qc-wb-iqc",
  PQC: "qc-wb-pqc",
  OQC: "qc-wb-oqc",
  RQC: "qc-wb-rqc",
};

const RECORDS_TAB = {
  IQC: "iqc",
  PQC: "ipqc",
  OQC: "oqc",
  RQC: "rqc",
};

export default {
  name: "QcWorkbench",
  components: {
    QcHeroBar,
    QcPendingAside,
    QcExecWorkbench,
    QcRecordsHub,
    QcConfigHub,
    AndonLaunch,
  },
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
      return this.authUser.realName || this.authUser.displayName || this.authUser.username || "质检员";
    },
    employeeNo() {
      return this.authUser.username || "";
    },
    routeKeyword() {
      return String(this.$route.query.wo || this.$route.query.workOrder || "").trim();
    },
    filteredPendingRows() {
      const list =
        this.typeFilter === "all"
          ? this.pendingRows
          : this.pendingRows.filter((r) => r.qcType === this.typeFilter);
      return sortQcPendingRecentFirst(list);
    },
    isExecRoute() {
      return String(this.$route.name || "").startsWith("qc-wb-");
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
    this.refresh();
    this.syncFromRoute();
  },
  watch: {
    "$route.fullPath"() {
      this.syncFromRoute();
    },
    typeFilter() {
      this.$nextTick(() => this.ensureDefaultTask(true));
    },
  },
  methods: {
    pendingRowKey(row) {
      return `${row.qcType}-${row.sourceDocId}-${row.sourceLineId}-${row.itemId}`;
    },
    rowFromRouteQuery() {
      const q = this.$route.query || {};
      if (!q.sourceDocId && !q.itemId) return null;
      return (
        this.pendingRows.find((r) => {
          const sameType = !q.qcType || r.qcType === q.qcType;
          const sameDoc = String(r.sourceDocId) === String(q.sourceDocId);
          const sameLine = String(r.sourceLineId) === String(q.sourceLineId);
          const sameItem = String(r.itemId) === String(q.itemId);
          return sameType && sameDoc && sameLine && sameItem;
        }) || null
      );
    },
    syncFromRoute() {
      if (!this.isExecRoute) return;
      const row = this.rowFromRouteQuery();
      if (row) {
        this.selectedPendingKey = this.pendingRowKey(row);
      }
    },
    /** 进入页面或切换类型筛选时：默认选中第一条并打开检验表单 */
    ensureDefaultTask(force = false) {
      if (!force && this.isExecRoute && this.rowFromRouteQuery()) return;
      const key = this.routeKeyword.toLowerCase();
      let rows = this.filteredPendingRows;
      if (key) {
        rows = rows.filter((r) =>
          [r.workOrderCode, r.sourceDocCode, r.itemCode, r.itemName, r.sourceDocName]
            .filter(Boolean)
            .some((v) => String(v).toLowerCase().includes(key))
        );
      }
      if (!rows.length) return;
      const first = rows[0];
      if (
        !force &&
        this.isExecRoute &&
        this.selectedPendingKey === this.pendingRowKey(first)
      ) {
        return;
      }
      this.onInspect(first);
    },
    async refresh() {
      this.loading = true;
      this.pendingLoading = true;
      try {
        const [s, pending, rf] = await Promise.all([
          getQcSummary(),
          listPending({ pageNum: 1, pageSize: 100 }),
          getQcRecentFinished(8),
        ]);
        this.summary = s?.data || {};
        this.pendingRows = pending?.rows || [];
        this.recentFinished = rf?.data || [];
        this.syncFromRoute();
        await this.$nextTick();
        this.ensureDefaultTask();
      } catch {
        this.summary = {};
        this.pendingRows = [];
        this.recentFinished = [];
      } finally {
        this.loading = false;
        this.pendingLoading = false;
      }
    },
    onPendingSelect(row) {
      this.selectedPendingKey = this.pendingRowKey(row);
    },
    onInspect(row) {
      if (!row) return;
      this.selectedPendingKey = this.pendingRowKey(row);
      const routeName = EXEC_ROUTE[row.qcType];
      if (!routeName) return;
      const query = {};
      Object.keys(row).forEach((k) => {
        const v = row[k];
        if (v !== null && v !== undefined && v !== "") query[k] = String(v);
      });
      this.$router.push({ name: routeName, query });
    },
    openRecords() {
      const tab = RECORDS_TAB[this.selectedRow?.qcType] || "iqc";
      this.$refs.recordsRef?.open(tab);
    },
    openRecordsTab(tab) {
      this.$refs.recordsRef?.open(tab || "iqc");
    },
    openConfig() {
      this.$refs.configRef?.open("template");
    },
    goAnalytics() {
      this.$router.push("/app/quality/analytics");
    },
  },
};
</script>
