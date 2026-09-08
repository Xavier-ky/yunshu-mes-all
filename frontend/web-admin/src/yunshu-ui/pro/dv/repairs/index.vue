<template>
  <div class="app-container inventory-doc-shell dv-doc-shell dv-repairs-page">
    <div class="dv-doc-hub-head">
      <h2 class="dv-doc-hub-title">维修中心</h2>
      <div class="dv-doc-hub-actions">
        <el-button type="primary" plain size="default" @click="goWorkbench">去工作台</el-button>
        <el-button size="default" @click="configOpen = true">故障原因配置</el-button>
      </div>
    </div>
    <RepairPanel />
    <el-drawer v-model="configOpen" title="故障原因配置" size="720px" append-to-body destroy-on-close>
      <FaultCausesPanel embedded />
    </el-drawer>
  </div>
</template>

<script>
import RepairPanel from "../repair/index.vue";
import FaultCausesPanel from "../supplement/FaultCauses.vue";

export default {
  name: "DvRepairsHub",
  components: { RepairPanel, FaultCausesPanel },
  provide() {
    return {
      goRepairWorkbench: this.goWorkbenchWithRow,
    };
  },
  data() {
    return {
      configOpen: false,
    };
  },
  mounted() {
    if (this.$route.query?.drawer === "fault-causes") {
      this.configOpen = true;
    }
  },
  watch: {
    "$route.query.drawer"(v) {
      if (v === "fault-causes") this.configOpen = true;
    },
    configOpen(v) {
      if (!v && this.$route.query?.drawer === "fault-causes") {
        const query = { ...this.$route.query };
        delete query.drawer;
        this.$router.replace({ query });
      }
    },
  },
  methods: {
    goWorkbench() {
      this.$router.push("/app/equipment/workbench?queue=repair");
    },
    goWorkbenchWithRow(row) {
      if (!row?.repairId) {
        this.goWorkbench();
        return;
      }
      this.$router.push({
        name: "dv-wb-repair",
        query: {
          taskType: "REPAIR",
          taskId: String(row.repairId),
          repairCode: row.repairCode,
          machineryCode: row.machineryCode,
          machineryName: row.machineryName,
          status: row.status,
        },
      });
    },
  },
};
</script>
