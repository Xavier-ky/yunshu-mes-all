<template>
  <el-drawer v-model="visible" title="安灯管理中心" size="88%" append-to-body destroy-on-close @closed="activeTab = 'analytics'">
    <div class="andon-hub">
      <div class="andon-hub-kpi">
        <el-card v-for="k in kpiCards" :key="k.key" shadow="never" class="kpi-card" :class="k.tone">
          <span>{{ k.label }}</span>
          <strong>{{ k.value }}</strong>
        </el-card>
      </div>
      <el-tabs v-model="activeTab" @tab-change="onTabChange">
        <el-tab-pane label="异常分析" name="analytics">
          <AnalyticsPanel v-if="loaded.analytics" :stats="summary" />
        </el-tab-pane>
        <el-tab-pane label="安灯类型" name="types">
          <TypePanel v-if="loaded.types" ref="typesRef" />
        </el-tab-pane>
        <el-tab-pane label="异常原因" name="reasons">
          <ReasonPanel v-if="loaded.reasons" ref="reasonsRef" />
        </el-tab-pane>
        <el-tab-pane label="处理任务" name="tasks">
          <TaskPanel v-if="loaded.tasks" ref="tasksRef" />
        </el-tab-pane>
      </el-tabs>
    </div>
  </el-drawer>
</template>

<script>
import { request } from "@/api/request";
import AnalyticsPanel from "./panels/AnalyticsPanel.vue";
import TypePanel from "./panels/TypePanel.vue";
import ReasonPanel from "./panels/ReasonPanel.vue";
import TaskPanel from "./panels/TaskPanel.vue";

export default {
  name: "AndonHub",
  components: { AnalyticsPanel, TypePanel, ReasonPanel, TaskPanel },
  data() {
    return {
      visible: false,
      activeTab: "analytics",
      summary: {},
      loaded: { analytics: false, types: false, reasons: false, tasks: false },
    };
  },
  computed: {
    kpiCards() {
      const s = this.summary;
      return [
        { key: "total", label: "总记录", value: s.totalRecords || 0, tone: "" },
        { key: "active", label: "待处置", value: s.activeRecords || 0, tone: "warn" },
        { key: "handled", label: "已处置", value: s.handledRecords || 0, tone: "ok" },
        { key: "open", label: "开放事件", value: s.openEvents || 0, tone: "warn" },
      ];
    },
  },
  methods: {
    open(tab = "analytics") {
      this.activeTab = tab;
      this.visible = true;
      this.loaded = { analytics: false, types: false, reasons: false, tasks: false };
      this.loadSummary().then(() => {
        this.loaded.analytics = true;
        this.onTabChange(tab);
      });
    },
    async loadSummary() {
      this.summary = (await request.get("/andon/analytics/summary"))?.data || {};
    },
    onTabChange(name) {
      const tab = name || this.activeTab;
      if (this.loaded[tab]) {
        this.$nextTick(() => this.refreshTab(tab));
        return;
      }
      this.loaded[tab] = true;
      this.$nextTick(() => this.refreshTab(tab));
    },
    refreshTab(tab) {
      const refMap = { types: "typesRef", reasons: "reasonsRef", tasks: "tasksRef" };
      const r = this.$refs[refMap[tab]];
      if (r && r.load) r.load();
    },
  },
};
</script>

<style scoped>
.andon-hub { display: flex; flex-direction: column; gap: 12px; height: 100%; }
.andon-hub-kpi { display: grid; grid-template-columns: repeat(4, 1fr); gap: 10px; }
.kpi-card { text-align: center; }
.kpi-card span { display: block; font-size: 12px; color: #888; margin-bottom: 4px; }
.kpi-card strong { font-size: 22px; }
.kpi-card.warn strong { color: #d97706; }
.kpi-card.ok strong { color: #059669; }
</style>
