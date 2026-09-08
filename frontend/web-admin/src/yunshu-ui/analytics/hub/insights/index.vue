<template>
  <div class="app-container sys-hub-page analytics-hub-page">
    <div class="sys-hub-head analytics-hub-head">
      <div class="analytics-hub-head__row">
        <div>
          <h2 class="sys-hub-head__title">数据看板</h2>
          <p class="sys-hub-head__desc">图形看板 · 报表档案</p>
        </div>
        <el-button type="primary" @click="goReportDesigner">打开报表设计 →</el-button>
      </div>
    </div>
    <el-tabs v-model="activeTab" class="sys-hub-tabs" @tab-change="onTabChange">
      <el-tab-pane label="图形看板" name="charts">
        <ChartPanel v-if="activeTab === 'charts'" embed-mode />
      </el-tab-pane>
      <el-tab-pane label="报表档案" name="archive">
        <ReportArchivePanel v-if="activeTab === 'archive'" embed-mode />
      </el-tab-pane>
    </el-tabs>
  </div>
</template>

<script>
import ChartPanel from "@/yunshu-ui/analytics/chart/index.vue";
import ReportArchivePanel from "@/yunshu-ui/analytics/report/index.vue";

const TABS = new Set(["charts", "archive"]);

export default {
  name: "AnalyticsInsightsHub",
  components: { ChartPanel, ReportArchivePanel },
  data() {
    return {
      activeTab: "charts",
    };
  },
  mounted() {
    this.syncFromRoute();
  },
  watch: {
    "$route.query"() {
      this.syncFromRoute();
    },
  },
  methods: {
    syncFromRoute() {
      const tab = this.$route.query?.tab;
      if (tab && TABS.has(tab)) {
        this.activeTab = tab;
      }
    },
    onTabChange(name) {
      this.$router.replace({ query: { tab: name } });
    },
    goReportDesigner() {
      this.$router.push("/app/analytics/report-designer");
    },
  },
};
</script>
