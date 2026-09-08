<template>
  <div class="app-container inventory-doc-shell">
    <el-tabs v-model="activeTab" class="inventory-doc-tabs" @tab-change="onTabChange">
      <el-tab-pane label="外部系统" name="systems" lazy>
        <SystemsPanel v-if="activeTab === 'systems'" />
      </el-tab-pane>
      <el-tab-pane label="接口定义" name="endpoints" lazy>
        <IntegrationMasterDetail v-if="activeTab === 'endpoints'" :initial-system-id="initialSystemId" />
      </el-tab-pane>
      <el-tab-pane label="同步日志" name="logs" lazy>
        <SyncLogsPanel v-if="activeTab === 'logs'" />
      </el-tab-pane>
    </el-tabs>
  </div>
</template>

<script>
import SystemsPanel from "@/yunshu-ui/integration/systems/index.vue";
import SyncLogsPanel from "@/yunshu-ui/integration/sync-logs/index.vue";
import IntegrationMasterDetail from "../components/IntegrationMasterDetail.vue";

const TABS = new Set(["systems", "endpoints", "logs"]);

export default {
  name: "AnalyticsIntegrationHub",
  components: { SystemsPanel, SyncLogsPanel, IntegrationMasterDetail },
  data() {
    return {
      activeTab: "systems",
      initialSystemId: null,
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
      this.initialSystemId = this.$route.query?.systemId || null;
    },
    onTabChange(name) {
      const query = { tab: name };
      if (name === "endpoints" && this.initialSystemId) {
        query.systemId = this.initialSystemId;
      }
      this.$router.replace({ query });
    },
  },
};
</script>
