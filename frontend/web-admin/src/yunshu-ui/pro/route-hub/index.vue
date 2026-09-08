<template>
  <div class="app-container inventory-doc-shell route-hub-shell">
    <div class="cal-kpi-strip route-hub-kpi" aria-label="工艺路线概览">
      <div class="route-hub-kpi__item is-accent"><span class="route-hub-kpi__label">工序总数</span><b>{{ hubKpis.processTotal }}</b><small>个</small></div>
      <div class="route-hub-kpi__item"><span class="route-hub-kpi__label">启用工序</span><b>{{ hubKpis.processEnabled }}</b><small>个</small></div>
      <div class="route-hub-kpi__item"><span class="route-hub-kpi__label">工艺路线</span><b>{{ hubKpis.routeTotal }}</b><small>条</small></div>
      <div class="route-hub-kpi__item"><span class="route-hub-kpi__label">启用路线</span><b>{{ hubKpis.routeEnabled }}</b><small>条</small></div>
      <div class="route-hub-kpi__item"><span class="route-hub-kpi__label">平均工序</span><b>{{ hubKpis.avgSteps }}</b><small>步/条</small></div>
    </div>
    <el-tabs v-model="hubMode" class="inventory-doc-tabs" @tab-change="onModeChange">
      <el-tab-pane label="工序库" name="process" lazy>
        <ProcessManage v-if="hubMode === 'process'" />
      </el-tab-pane>
      <el-tab-pane label="工艺流程" name="proroute" lazy>
        <ProRouteManage v-if="hubMode === 'proroute'" />
      </el-tab-pane>
    </el-tabs>
  </div>
</template>

<script>
import ProcessManage from "@/yunshu-ui/pro/process/index.vue";
import ProRouteManage from "@/yunshu-ui/pro/proroute/index.vue";
import { listAllProcess } from "@/yunshu-ui/api/mes/pro/process";
import { listProroute, summarizeProroute } from "@/yunshu-ui/api/mes/pro/proroute";

const MODES = ["process", "proroute"];

export default {
  name: "ProcessRouteHub",
  components: { ProcessManage, ProRouteManage },
  data() {
    return {
      hubMode: "process",
      hubKpis: {
        processTotal: 0,
        processEnabled: 0,
        routeTotal: 0,
        routeEnabled: 0,
        avgSteps: 0,
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
      const next = MODES.includes(mode) ? mode : "process";
      this.hubMode = next;
      if (mode !== next) {
        this.$router.replace({ path: "/app/pro/route", query: { ...this.$route.query, mode: next } });
      }
    },
    onModeChange(mode) {
      if (this.$route.query.mode === mode) return;
      this.$router.replace({ path: "/app/pro/route", query: { ...this.$route.query, mode } });
    },
    async loadHubKpis() {
      try {
        const [processRes, routeRes] = await Promise.all([
          listAllProcess(),
          listProroute({ pageNum: 1, pageSize: 500 }),
        ]);
        const processes = Array.isArray(processRes?.data)
          ? processRes.data
          : Array.isArray(processRes?.rows)
            ? processRes.rows
            : [];
        const routes = Array.isArray(routeRes?.rows) ? routeRes.rows : [];
        const routeIds = routes.map((r) => r.routeId).filter(Boolean);
        let stepTotal = 0;
        if (routeIds.length) {
          const summaryRes = await summarizeProroute(routeIds);
          const grouped = summaryRes?.data || {};
          stepTotal = Object.values(grouped).reduce((sum, steps) => sum + (steps?.length || 0), 0);
        }
        this.hubKpis = {
          processTotal: processes.length,
          processEnabled: processes.filter((p) => p.enableFlag === "Y").length,
          routeTotal: routeRes?.total ?? routes.length,
          routeEnabled: routes.filter((r) => r.enableFlag === "Y").length,
          avgSteps: routes.length ? Math.round((stepTotal / routes.length) * 10) / 10 : 0,
        };
      } catch {
        // keep previous KPI values
      }
    },
  },
};
</script>
