<template>
  <div class="app-container sys-hub-page sys-settings-hub">
    <div class="sys-hub-head">
      <h2 class="sys-hub-head__title">系统配置</h2>
    </div>
    <el-tabs v-model="activeTab" class="sys-hub-tabs" @tab-change="onTabChange">
      <el-tab-pane label="参数字典" name="dict">
        <DictMasterDetail v-if="activeTab === 'dict'" :initial-dict-id="initialDictId" />
      </el-tab-pane>
      <el-tab-pane label="系统参数" name="config">
        <ConfigPanel v-if="activeTab === 'config'" />
      </el-tab-pane>
      <el-tab-pane label="编码规则" name="autocode">
        <AutocodeMasterDetail v-if="activeTab === 'autocode'" :initial-rule-id="initialRuleId" />
      </el-tab-pane>
    </el-tabs>
  </div>
</template>

<script>
import DictMasterDetail from "../components/DictMasterDetail.vue";
import AutocodeMasterDetail from "../components/AutocodeMasterDetail.vue";
import ConfigPanel from "../../config/index.vue";

const TABS = new Set(["dict", "config", "autocode"]);

export default {
  name: "SysSettingsHub",
  components: { DictMasterDetail, AutocodeMasterDetail, ConfigPanel },
  data() {
    return {
      activeTab: "dict",
      initialDictId: null,
      initialRuleId: null,
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
      this.initialDictId = this.$route.query?.dictId || null;
      this.initialRuleId = this.$route.query?.ruleId || null;
    },
    onTabChange(name) {
      const query = { tab: name };
      if (name === "dict" && this.initialDictId) query.dictId = this.initialDictId;
      if (name === "autocode" && this.initialRuleId) query.ruleId = this.initialRuleId;
      this.$router.replace({ query });
    },
  },
};
</script>
