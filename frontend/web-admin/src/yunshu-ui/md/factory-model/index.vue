<template>
  <div class="app-container inventory-doc-shell factory-model-shell">
    <div class="cal-kpi-strip factory-model-kpi" aria-label="工厂建模概览">
      <div class="factory-model-kpi__item is-accent">
        <span class="factory-model-kpi__label">车间总数</span><b>{{ hubKpis.workshopTotal }}</b><small>个</small>
      </div>
      <div class="factory-model-kpi__item">
        <span class="factory-model-kpi__label">启用车间</span><b>{{ hubKpis.workshopEnabled }}</b><small>个</small>
      </div>
      <div class="factory-model-kpi__item">
        <span class="factory-model-kpi__label">产线总数</span><b>{{ hubKpis.lineTotal }}</b><small>条</small>
      </div>
      <div class="factory-model-kpi__item">
        <span class="factory-model-kpi__label">启用产线</span><b>{{ hubKpis.lineEnabled }}</b><small>条</small>
      </div>
      <div class="factory-model-kpi__item">
        <span class="factory-model-kpi__label">工位总数</span><b>{{ hubKpis.workstationTotal }}</b><small>个</small>
      </div>
    </div>
    <el-tabs v-model="hubMode" class="inventory-doc-tabs" @tab-change="onModeChange">
      <el-tab-pane label="车间" name="workshop" lazy>
        <WorkshopManage v-if="hubMode === 'workshop'" />
      </el-tab-pane>
      <el-tab-pane label="产线" name="line" lazy>
        <LineManage v-if="hubMode === 'line'" />
      </el-tab-pane>
      <el-tab-pane label="工位" name="workstation" lazy>
        <WorkstationManage v-if="hubMode === 'workstation'" />
      </el-tab-pane>
    </el-tabs>
  </div>
</template>

<script>
import WorkshopManage from "@/yunshu-ui/md/workshop/index.vue";
import LineManage from "@/yunshu-ui/md/line/index.vue";
import WorkstationManage from "@/yunshu-ui/md/workstation/index.vue";
import { listWorkshop } from "@/yunshu-ui/api/mes/md/workshop";
import { listLine } from "@/yunshu-ui/api/mes/md/line";
import { listWorkstation } from "@/yunshu-ui/api/mes/md/workstation";

const MODES = ["workshop", "line", "workstation"];

function unwrapRows(res) {
  return Array.isArray(res?.rows) ? res.rows : [];
}

function countEnabled(rows) {
  return rows.filter((r) => r.enableFlag === "Y").length;
}

export default {
  name: "FactoryModelHub",
  components: { WorkshopManage, LineManage, WorkstationManage },
  data() {
    return {
      hubMode: "workshop",
      hubKpis: {
        workshopTotal: 0,
        workshopEnabled: 0,
        lineTotal: 0,
        lineEnabled: 0,
        workstationTotal: 0,
      },
    };
  },
  created() {
    this.syncFromRoute();
    this.loadHubKpis();
  },
  watch: {
    "$route.query.mode"() {
      this.syncFromRoute();
    },
    hubMode() {
      this.loadHubKpis();
    },
  },
  methods: {
    syncFromRoute() {
      const mode = this.$route.query.mode;
      const next = MODES.includes(mode) ? mode : "workshop";
      this.hubMode = next;
      if (mode !== next) {
        this.$router.replace({ path: "/app/factory/model", query: { ...this.$route.query, mode: next } });
      }
    },
    onModeChange(mode) {
      if (this.$route.query.mode === mode) return;
      this.$router.replace({ path: "/app/factory/model", query: { ...this.$route.query, mode } });
    },
    async loadHubKpis() {
      try {
        const [workshopRes, lineRes, workstationRes] = await Promise.all([
          listWorkshop({ pageNum: 1, pageSize: 500 }),
          listLine({ pageNum: 1, pageSize: 500 }),
          listWorkstation({ pageNum: 1, pageSize: 500 }),
        ]);
        const workshops = unwrapRows(workshopRes);
        const lines = unwrapRows(lineRes);
        const workstations = unwrapRows(workstationRes);
        this.hubKpis = {
          workshopTotal: workshopRes?.total ?? workshops.length,
          workshopEnabled: countEnabled(workshops),
          lineTotal: lineRes?.total ?? lines.length,
          lineEnabled: countEnabled(lines),
          workstationTotal: workstationRes?.total ?? workstations.length,
        };
      } catch {
        // keep previous KPI values
      }
    },
  },
};
</script>
