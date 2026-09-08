<template>
  <div class="app-container dv-analytics-command-center">
    <div class="dv-analytics-head">
      <h2 class="dv-analytics-head__title">设备分析</h2>
      <div class="dv-analytics-head__actions">
        <el-button type="primary" plain size="default" :loading="loading" @click="loadAll">刷新</el-button>
        <el-button size="default" @click="goWorkbench">进入工作台</el-button>
      </div>
    </div>
    <DvAnalyticsMain
      :loading="loading"
      :pending-loading="pendingLoading"
      :oee-loading="oeeLoading"
      :overview="overview"
      :summary="summary"
      :status-rows="statusRows"
      :trend-rows="trendRows"
      :oee-rows="oeeRows"
      :pending-rows="pendingRows"
      :recent-finished="recentFinished"
      @workbench="goWorkbench"
      @machinery="goMachinery"
      @check-maint="goCheckMaint"
      @repairs="goRepairs"
      @queue="goQueue"
      @inspect="onInspect"
      @records="goRecords"
      @activity-open="goRecords"
    />
  </div>
</template>

<script>
import DvAnalyticsMain from "./components/DvAnalyticsMain.vue";
import {
  getDvAnalyticsOverview,
  getDvSummary,
  listDvPending,
  getDvRecentFinished,
} from "@/yunshu-ui/api/mes/dv/workbench";
import { request } from "@/api/request";

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

export default {
  name: "DvAnalyticsHub",
  components: { DvAnalyticsMain },
  data() {
    return {
      loading: false,
      pendingLoading: false,
      oeeLoading: false,
      overview: {},
      summary: {},
      statusRows: [],
      trendRows: [],
      oeeRows: [],
      pendingRows: [],
      recentFinished: [],
    };
  },
  mounted() {
    this.loadAll();
  },
  methods: {
    async loadAll() {
      this.loading = true;
      this.pendingLoading = true;
      this.oeeLoading = true;
      try {
        const [overviewRes, summaryRes, pendingRes, recentRes] = await Promise.all([
          getDvAnalyticsOverview(),
          getDvSummary(),
          listDvPending({ limit: 12 }),
          getDvRecentFinished(8),
        ]);
        const data = overviewRes?.data || {};
        this.overview = data.summary || {};
        this.statusRows = data.statusDistribution || [];
        this.trendRows = data.maintenanceTrend || [];
        this.summary = summaryRes?.data || {};
        this.pendingRows = (pendingRes?.rows || []).slice(0, 8);
        this.recentFinished = recentRes?.data || [];
      } catch {
        this.overview = {};
        this.summary = {};
        this.statusRows = [];
        this.trendRows = [];
        this.pendingRows = [];
        this.recentFinished = [];
      } finally {
        this.loading = false;
        this.pendingLoading = false;
      }
      try {
        const oeeRes = await request.get("/equipment/oee");
        this.oeeRows = oeeRes?.data || [];
      } catch {
        this.oeeRows = [];
      } finally {
        this.oeeLoading = false;
      }
    },
    buildTaskQuery(row) {
      const query = {};
      TASK_QUERY_KEYS.forEach((k) => {
        const v = row?.[k];
        if (v !== null && v !== undefined && v !== "") query[k] = String(v);
      });
      return query;
    },
    onInspect(row) {
      if (!row) return;
      const routeName = EXEC_ROUTE[row.taskType];
      if (!routeName) return;
      this.$router.push({ name: routeName, query: this.buildTaskQuery(row) });
    },
    goWorkbench() {
      this.$router.push("/app/equipment/workbench");
    },
    goMachinery() {
      this.$router.push("/app/equipment/machinery");
    },
    goCheckMaint() {
      this.$router.push("/app/equipment/check-maint");
    },
    goRepairs() {
      this.$router.push("/app/equipment/repairs");
    },
    goQueue(queue) {
      this.$router.push(`/app/equipment/workbench?queue=${queue}`);
    },
    goRecords() {
      this.$router.push({ path: "/app/equipment/check-maint", query: { tab: "records" } });
    },
  },
};
</script>
