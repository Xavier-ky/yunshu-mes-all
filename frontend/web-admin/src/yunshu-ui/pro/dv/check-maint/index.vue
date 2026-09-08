<template>
  <div class="app-container inventory-doc-shell dv-doc-shell">
    <div class="dv-doc-hub-head">
      <h2 class="dv-doc-hub-title">点检保养</h2>
      <el-button type="primary" plain size="default" @click="goWorkbench">去工作台</el-button>
    </div>
    <el-tabs v-model="activeTab" class="inventory-doc-tabs dv-doc-tabs" lazy @tab-change="onTabChange">
      <el-tab-pane label="计划管理" name="plans" lazy>
        <CheckPlanPanel v-if="activeTab === 'plans'" />
      </el-tab-pane>
      <el-tab-pane label="项目库" name="subjects" lazy>
        <SubjectPanel v-if="activeTab === 'subjects'" />
      </el-tab-pane>
      <el-tab-pane label="执行记录" name="records" lazy>
        <RecordsTab v-if="activeTab === 'records'" :initial-type="recordType" />
      </el-tab-pane>
    </el-tabs>
  </div>
</template>

<script>
import CheckPlanPanel from "../checkplan/index.vue";
import SubjectPanel from "../subject/index.vue";
import RecordsTab from "./RecordsTab.vue";

export default {
  name: "DvCheckMaintHub",
  components: { CheckPlanPanel, SubjectPanel, RecordsTab },
  data() {
    return {
      activeTab: "plans",
      recordType: "ALL",
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
      if (tab === "plans" || tab === "subjects" || tab === "records") {
        this.activeTab = tab;
      }
      const type = this.$route.query?.type;
      if (type === "CHECK" || type === "MAINTEN") {
        this.recordType = type;
      } else if (this.activeTab === "records") {
        this.recordType = "ALL";
      }
    },
    onTabChange(name) {
      const query = { ...this.$route.query, tab: name };
      if (name !== "records") delete query.type;
      this.$router.replace({ query });
    },
    goWorkbench() {
      this.$router.push("/app/equipment/workbench");
    },
  },
};
</script>
