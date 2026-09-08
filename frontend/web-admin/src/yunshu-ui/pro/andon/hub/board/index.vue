<template>
  <div class="app-container andon-command-center">
    <template v-if="view !== 'handle'">
      <AndonHeroBar
        :summary="boardSummary"
        :loading="loading"
        :show-settings="isManager"
        :show-handle="canHandle"
        :show-mine="canHandle"
        @refresh="refresh"
        @launch="goLaunch"
        @handle="goHandle"
        @settings="openSettings"
      />

      <div class="andon-board-body">
        <ActiveAlertStrip
          :alerts="activeAlerts"
          :loading="loading"
          :category-filter="categoryFilter"
          @select="onAlertSelect"
        />

        <el-row :gutter="0" class="andon-board-main">
          <el-col :span="12" class="andon-board-col">
            <LineMatrixHub
              :stations="stations"
              :lines="lines"
              :loading="loading"
              :category-filter="categoryFilter"
              @select-station="onStationSelect"
              @select-alert="onStationAlert"
              @clear-filter="categoryFilter = ''"
            />
          </el-col>
          <el-col :span="12" class="andon-board-col">
            <AndonInsightsHub
              :active-alerts="activeAlerts"
              :category-filter="categoryFilter"
              :trend="trend"
              :loading-trend="loadingTrend"
              :timeline="timeline"
              :loading-timeline="loadingTimeline"
              :analytics-stats="analyticsStats"
              @filter="categoryFilter = $event"
              @timeline-select="onTimelineSelect"
            />
          </el-col>
        </el-row>

        <AndonConfigDrawer ref="configDrawer" />
      </div>
    </template>

    <HandleOverlay
      v-else
      :record-id="recordId"
      :detail-tab="detailTab"
      @back="goOverview"
      @saved="onHandleSaved"
      @select-record="onHandleSelectRecord"
    />
  </div>
</template>

<script>
import { getAuthUser } from "@/utils/auth-context";
import AndonHeroBar from "./components/AndonHeroBar.vue";
import ActiveAlertStrip from "./components/ActiveAlertStrip.vue";
import LineMatrixHub from "./components/LineMatrixHub.vue";
import AndonInsightsHub from "./components/AndonInsightsHub.vue";
import HandleOverlay from "./components/HandleOverlay.vue";
import AndonConfigDrawer from "./components/AndonConfigDrawer.vue";
import {
  getAndonBoardSummary,
  getAndonBoardActive,
  getAndonBoardStations,
  getAndonBoardLines,
  getAndonBoardTrend,
  getAndonBoardTimeline,
} from "@/yunshu-ui/api/mes/pro/andonboard";
import { request } from "@/api/request";

const HANDLE_ROLES = ["MANAGER", "PROD_SUPERVISOR", "WAREHOUSE_CLERK", "QUALITY_INSPECTOR", "EQUIPMENT_MAINTAINER", "TESTER"];

export default {
  name: "AndonBoardHub",
  components: {
    AndonHeroBar,
    ActiveAlertStrip,
    LineMatrixHub,
    AndonInsightsHub,
    HandleOverlay,
    AndonConfigDrawer,
  },
  data() {
    return {
      loading: false,
      loadingTrend: false,
      loadingTimeline: false,
      boardSummary: {},
      activeAlerts: [],
      stations: [],
      lines: [],
      trend: {},
      timeline: [],
      analyticsStats: {},
      authUser: {},
      categoryFilter: "",
      pollTimer: null,
    };
  },
  computed: {
    view() {
      return this.$route.query?.view === "handle" ? "handle" : "overview";
    },
    recordId() {
      return this.$route.query?.recordId || null;
    },
    detailTab() {
      const tab = this.$route.query?.tab;
      return tab === "tasks" ? "tasks" : "detail";
    },
    isManager() {
      const roles = this.authUser.roleCodes || [];
      return roles.includes("MANAGER") || roles.includes("TESTER");
    },
    canHandle() {
      const roles = this.authUser.roleCodes || [];
      return roles.some((r) => HANDLE_ROLES.includes(r));
    },
  },
  watch: {
    "$route.query.view"(v) {
      if (v === "handle" && !this.canHandle) {
        this.goOverview();
      }
    },
  },
  mounted() {
    this.authUser = getAuthUser() || {};
    if (this.view === "handle" && !this.canHandle) {
      this.goOverview();
      return;
    }
    this.refresh();
    this.loadTrend();
    this.loadTimeline();
    this.pollTimer = setInterval(() => {
      if (this.view === "overview") this.refreshLight();
    }, 60000);
  },
  beforeUnmount() {
    if (this.pollTimer) clearInterval(this.pollTimer);
  },
  methods: {
    async refresh() {
      this.loading = true;
      try {
        const [board, active, stations, lines, analytics] = await Promise.all([
          getAndonBoardSummary(),
          getAndonBoardActive(),
          getAndonBoardStations(),
          getAndonBoardLines(),
          request.get("/andon/analytics/summary"),
        ]);
        this.boardSummary = board?.data || {};
        this.activeAlerts = active?.data || [];
        this.stations = stations?.data || [];
        this.lines = lines?.data || [];
        this.analyticsStats = analytics?.data || {};
      } catch {
        this.boardSummary = {};
        this.activeAlerts = [];
        this.stations = [];
        this.lines = [];
      } finally {
        this.loading = false;
      }
    },
    async refreshLight() {
      try {
        const [board, active] = await Promise.all([getAndonBoardSummary(), getAndonBoardActive()]);
        this.boardSummary = board?.data || {};
        this.activeAlerts = active?.data || [];
      } catch { /* ignore poll errors */ }
    },
    async loadTrend() {
      this.loadingTrend = true;
      try {
        const res = await getAndonBoardTrend();
        this.trend = res?.data || {};
      } finally {
        this.loadingTrend = false;
      }
    },
    async loadTimeline() {
      this.loadingTimeline = true;
      try {
        const res = await getAndonBoardTimeline();
        this.timeline = res?.data || [];
      } finally {
        this.loadingTimeline = false;
      }
    },
    goLaunch() {
      this.$router.push("/app/andon/launch");
    },
    goHandle(recordId) {
      const query = { view: "handle" };
      if (recordId) query.recordId = String(recordId);
      this.$router.replace({ path: "/app/andon", query });
    },
    goOverview() {
      this.$router.replace({ path: "/app/andon" });
    },
    openSettings() {
      this.$refs.configDrawer?.open("config");
    },
    onAlertSelect(item) {
      this.goHandle(item.recordId);
    },
    onStationSelect(station) {
      this.$router.push({
        path: "/app/andon/launch",
        query: { stationId: String(station.stationId) },
      });
    },
    onStationAlert(station) {
      this.goHandle(station.activeRecordId);
    },
    onTimelineSelect(item) {
      if (item.eventType === "HANDLED") return;
      this.goHandle(item.recordId);
    },
    onHandleSaved() {
      this.refresh();
      this.loadTrend();
      this.loadTimeline();
    },
    onHandleSelectRecord(id) {
      this.$router.replace({ path: "/app/andon", query: { view: "handle", recordId: String(id) } });
    },
  },
};
</script>
