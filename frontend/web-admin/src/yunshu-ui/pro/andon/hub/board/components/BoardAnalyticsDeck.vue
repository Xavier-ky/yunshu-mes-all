<template>
  <div class="andon-analytics-deck">
    <el-tabs v-model="activeTab" class="sys-hub-tabs andon-analytics-deck__tabs">
      <el-tab-pane label="本班概览" name="overview">
        <AnalyticsPanel v-if="activeTab === 'overview'" :stats="stats" />
      </el-tab-pane>
      <el-tab-pane label="原因分析" name="reason">
        <div v-if="activeTab === 'reason'" class="andon-reason-top">
          <el-table class="yunshu-data-table" stripe border :data="stats.reasonTop || []" max-height="220">
            <el-table-column type="index" width="50" align="center" />
            <el-table-column label="呼叫原因" prop="reason" min-width="160" show-overflow-tooltip />
            <el-table-column label="次数" prop="cnt" width="80" align="center" />
          </el-table>
        </div>
      </el-tab-pane>
    </el-tabs>
  </div>
</template>

<script>
import AnalyticsPanel from "../../../panels/AnalyticsPanel.vue";

export default {
  name: "BoardAnalyticsDeck",
  components: { AnalyticsPanel },
  props: {
    stats: { type: Object, default: () => ({}) },
  },
  data() {
    return { activeTab: "overview" };
  },
};
</script>
