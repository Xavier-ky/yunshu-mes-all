<template>
  <div class="app-container inventory-doc-shell cal-command-center">
    <el-tabs v-model="hubMode" class="inventory-doc-tabs" @tab-change="onModeChange">
      <el-tab-pane label="日历总览" name="overview" lazy>
        <CalScheduleOverview
          v-if="hubMode === 'overview'"
          :calendar-types="dict.type.mes_calendar_type"
          @navigate-tab="switchMode"
        />
      </el-tab-pane>
      <el-tab-pane label="节假日" name="holiday" lazy>
        <HolidayManage v-if="hubMode === 'holiday'" @navigate-tab="switchMode" />
      </el-tab-pane>
      <el-tab-pane label="班组" name="team" lazy>
        <TeamManage v-if="hubMode === 'team'" @navigate-tab="switchMode" />
      </el-tab-pane>
      <el-tab-pane label="排班计划" name="plan" lazy>
        <PlanManage v-if="hubMode === 'plan'" @navigate-tab="switchMode" />
      </el-tab-pane>
    </el-tabs>
  </div>
</template>

<script>
import CalScheduleOverview from "./CalScheduleOverview.vue";
import HolidayManage from "@/yunshu-ui/cal/holiday/index.vue";
import TeamManage from "@/yunshu-ui/cal/team/index.vue";
import PlanManage from "@/yunshu-ui/cal/plan/index.vue";
import { normalizeHubMode } from "./calScheduleModel";

export default {
  name: "CalCommandCenter",
  dicts: ["mes_calendar_type"],
  components: {
    CalScheduleOverview,
    HolidayManage,
    TeamManage,
    PlanManage,
  },
  data() {
    return {
      hubMode: "overview",
    };
  },
  created() {
    this.syncFromRoute();
  },
  watch: {
    "$route.query.mode"() {
      this.syncFromRoute();
    },
  },
  methods: {
    syncFromRoute() {
      this.hubMode = normalizeHubMode(this.$route.query.mode);
    },
    switchMode(mode) {
      this.hubMode = mode;
      this.onModeChange(mode);
    },
    onModeChange(mode) {
      this.hubMode = mode;
      if (this.$route.query.mode === mode) return;
      this.$router.replace({ path: "/app/factory/calendar", query: { ...this.$route.query, mode } });
    },
  },
};
</script>
