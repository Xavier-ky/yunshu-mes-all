<template>
  <div class="app-container inventory-doc-shell trace-hub-shell analytics-hub-page">
    <div class="sys-hub-head trace-hub-head">
      <div class="trace-hub-head__row">
        <div class="trace-hub-head__brand">
          <span class="trace-hub-head__icon" aria-hidden="true">
            <svg viewBox="0 0 24 24" fill="none" xmlns="http://www.w3.org/2000/svg">
              <path d="M8 12a4 4 0 1 0 0-8 4 4 0 0 0 0 8Z" stroke="currentColor" stroke-width="1.6" />
              <path d="M16 20a4 4 0 1 0 0-8 4 4 0 0 0 0 8Z" stroke="currentColor" stroke-width="1.6" />
              <path d="M10.5 10.5 13.5 13.5" stroke="currentColor" stroke-width="1.6" stroke-linecap="round" />
            </svg>
          </span>
          <h2 class="sys-hub-head__title">追溯中心</h2>
        </div>
        <div class="trace-hub-head__right">
          <p class="sys-hub-head__desc">从客户订单到成品入库，一键串联全链路数据</p>
          <div class="trace-hub-head__tags" aria-hidden="true">
            <span class="trace-hub-head__tag">工单追溯</span>
            <span class="trace-hub-head__tag">产品追溯</span>
            <span class="trace-hub-head__tag trace-hub-head__tag--accent">批次追溯</span>
          </div>
        </div>
      </div>
    </div>
    <el-tabs v-model="activeTab" class="inventory-doc-tabs" @tab-change="onTabChange">
      <el-tab-pane label="工单追溯" name="workorder">
        <WorkOrderTracePanel />
      </el-tab-pane>
      <el-tab-pane label="产品追溯" name="product">
        <ProcardPanel />
      </el-tab-pane>
      <el-tab-pane label="批次追溯" name="batch">
        <BatchTracePanel />
      </el-tab-pane>
    </el-tabs>
  </div>
</template>

<script>
import WorkOrderTracePanel from "./WorkOrderTracePanel.vue";
import ProcardPanel from "@/yunshu-ui/analytics/procard/index.vue";
import BatchTracePanel from "@/yunshu-ui/qc/batchtrace/index.vue";

const TABS = new Set(["workorder", "product", "batch"]);

export default {
  name: "AnalyticsTraceHub",
  components: { WorkOrderTracePanel, ProcardPanel, BatchTracePanel },
  data() {
    return {
      activeTab: "workorder",
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
      } else if (this.$route.query?.wo) {
        this.activeTab = "workorder";
      }
    },
    onTabChange(name) {
      const query = { tab: name };
      if (this.$route.query?.wo) {
        query.wo = this.$route.query.wo;
      }
      this.$router.replace({ query });
    },
  },
};
</script>
