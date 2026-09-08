<template>
  <div class="app-container sys-hub-page sys-audit-hub">
    <div class="sys-hub-head">
      <h2 class="sys-hub-head__title">安全审计</h2>
    </div>
    <el-tabs v-model="activeTab" class="sys-hub-tabs" @tab-change="onTabChange">
      <el-tab-pane label="操作日志" name="operlog">
        <OperlogPanel v-if="activeTab === 'operlog'" />
      </el-tab-pane>
      <el-tab-pane label="登录日志" name="logininfor">
        <LogininforPanel v-if="activeTab === 'logininfor'" />
      </el-tab-pane>
    </el-tabs>
  </div>
</template>

<script>
import OperlogPanel from "../../../monitor/operlog/index.vue";
import LogininforPanel from "../../../monitor/logininfor/index.vue";

const TABS = new Set(["operlog", "logininfor"]);

export default {
  name: "SysAuditHub",
  components: { OperlogPanel, LogininforPanel },
  data() {
    return { activeTab: "operlog" };
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
      if (this.$route.query?.dictId || this.$route.query?.ruleId) {
        /* no-op on audit hub */
      }
    },
    onTabChange(name) {
      this.$router.replace({ query: { tab: name } });
    },
  },
};
</script>
