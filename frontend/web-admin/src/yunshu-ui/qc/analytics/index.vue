<template>
  <div class="app-container qc-command-center">
    <QcHeroBar :summary="summary" :loading="loading" @refresh="refresh" @open-andon="$refs.andonLaunch?.open()" />

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
          @update:type-filter="typeFilter = $event"
          @select="onPendingSelect"
          @inspect="onInspect"
        />
      </el-col>
      <el-col :span="19" class="qc-hub-main-col operator-hub-main-col">
        <QcAnalyticsMain
          :trend="trend"
          :type-distribution="typeDistribution"
          :defect-top="defectTop"
          :type-volume="typeVolume"
          :type-stats="typeStats"
          :defect-levels="defectLevels"
          :activity-feed="activityFeed"
          :recent-finished="recentFinished"
          :pending-disposition="pendingDisposition"
          :active-type-filter="typeFilter"
          @workbench="goWorkbench"
          @records="openRecords"
          @config="openConfig"
          @batch-trace="goBatchTrace"
          @inspect="onInspectOrOpen"
          @filter-type="onFilterType"
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
import QcHeroBar from "./components/QcHeroBar.vue";
import QcPendingAside from "./components/QcPendingAside.vue";
import QcAnalyticsMain from "./components/QcAnalyticsMain.vue";
import QcRecordsHub from "@/yunshu-ui/qc/hub/records.vue";
import QcConfigHub from "@/yunshu-ui/qc/hub/config.vue";
import AndonLaunch from "@/yunshu-ui/pro/andon/launch.vue";
import { listPending, sortQcPendingRecentFirst } from "@/yunshu-ui/api/mes/qc/pending";
import {
  getQcSummary,
  getQcTrend,
  getQcTypeDistribution,
  getQcDefectTop,
  getQcTypeVolume,
  getQcTypeStats,
  getQcDefectByLevel,
  getQcActivityFeed,
  getQcRecentFinished,
  getQcPendingDisposition,
} from "@/yunshu-ui/api/mes/qc/analytics";

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
  name: "QcAnalyticsCenter",
  components: { QcHeroBar, QcPendingAside, QcAnalyticsMain, QcRecordsHub, QcConfigHub, AndonLaunch },
  data() {
    return {
      loading: false,
      pendingLoading: false,
      summary: {},
      trend: [],
      typeDistribution: [],
      defectTop: [],
      typeVolume: [],
      typeStats: {},
      defectLevels: [],
      activityFeed: [],
      recentFinished: [],
      pendingDisposition: [],
      pendingRows: [],
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
    filteredPendingRows() {
      const list =
        this.typeFilter === "all"
          ? this.pendingRows
          : this.pendingRows.filter((r) => r.qcType === this.typeFilter);
      return sortQcPendingRecentFirst(list);
    },
  },
  mounted() {
    this.authUser = getAuthUser() || {};
    this.refresh();
    this.handleQueryDrawer();
  },
  watch: {
    "$route.query"() {
      this.handleQueryDrawer();
    },
  },
  methods: {
    pendingRowKey(row) {
      return `${row.qcType}-${row.sourceDocId}-${row.sourceLineId}-${row.itemId}`;
    },
    async refresh() {
      this.loading = true;
      this.pendingLoading = true;
      try {
        const [s, t, d, f, tv, ts, dl, af, rf, pd, pending] = await Promise.all([
          getQcSummary(),
          getQcTrend(7),
          getQcTypeDistribution(),
          getQcDefectTop(10),
          getQcTypeVolume(),
          getQcTypeStats(),
          getQcDefectByLevel(7),
          getQcActivityFeed(12),
          getQcRecentFinished(20),
          getQcPendingDisposition(20),
          listPending({ pageNum: 1, pageSize: 100 }),
        ]);
        this.summary = s?.data || {};
        this.trend = t?.data || [];
        this.typeDistribution = d?.data || [];
        this.defectTop = f?.data || [];
        this.typeVolume = tv?.data || [];
        this.typeStats = ts?.data || {};
        this.defectLevels = dl?.data || [];
        this.activityFeed = af?.data || [];
        this.recentFinished = rf?.data || [];
        this.pendingDisposition = pd?.data || [];
        this.pendingRows = pending?.rows || [];
      } catch {
        this.summary = {};
        this.trend = [];
        this.typeDistribution = [];
        this.defectTop = [];
        this.typeVolume = [];
        this.typeStats = {};
        this.defectLevels = [];
        this.activityFeed = [];
        this.recentFinished = [];
        this.pendingDisposition = [];
        this.pendingRows = [];
      } finally {
        this.loading = false;
        this.pendingLoading = false;
      }
    },
    handleQueryDrawer() {
      const q = this.$route.query || {};
      if (q.drawer === "records" && q.tab) {
        this.$nextTick(() => this.$refs.recordsRef?.open(String(q.tab)));
      } else if (q.drawer === "config" && q.tab) {
        this.$nextTick(() => this.$refs.configRef?.open(String(q.tab)));
      }
    },
    onPendingSelect(row) {
      this.selectedPendingKey = this.pendingRowKey(row);
    },
    onInspect(row) {
      const routeName = EXEC_ROUTE[row?.qcType];
      if (!routeName) return;
      const query = {};
      Object.keys(row || {}).forEach((k) => {
        const v = row[k];
        if (v !== null && v !== undefined && v !== "") query[k] = String(v);
      });
      this.$router.push({ name: routeName, query });
    },
    onFilterType(code) {
      this.typeFilter = code;
    },
    onInspectOrOpen(row) {
      if (row?.sourceDocCode || row?.sourceDocId) {
        this.onInspect(row);
        return;
      }
      if (row?.docCode && EXEC_ROUTE[row?.qcType]) {
        this.$router.push({ name: EXEC_ROUTE[row.qcType], query: { docCode: row.docCode } });
        return;
      }
      const tab = RECORDS_TAB[row?.qcType] || "iqc";
      this.$refs.recordsRef?.open(tab);
    },
    goWorkbench() {
      this.$router.push("/app/quality/workbench");
    },
    goBatchTrace() {
      this.$router.push("/app/analytics/batch-trace");
    },
    openRecords() {
      this.$refs.recordsRef?.open("iqc");
    },
    openConfig() {
      this.$refs.configRef?.open("template");
    },
  },
};
</script>
