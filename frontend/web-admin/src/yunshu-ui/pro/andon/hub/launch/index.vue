<template>
  <div class="app-container andon-launch-page">
    <LaunchHeroBar
      :loading="loading"
      :user-label="userLabel"
      :eligible-count="eligibleCount"
      :active-station-count="activeStationCount"
      :today-count="todayRecords.length"
      :show-batch="showBatch"
      @refresh="loadAll"
      @board="goBoard"
      @batch="showBatchDrawer = true"
      @handle="goHandle"
    />

    <div class="andon-launch-body">
      <LaunchCategoryBar
        :today-records="todayRecords"
        :active-key="categoryFilter"
        @filter="categoryFilter = $event"
      />

      <el-row :gutter="0" class="andon-launch-main">
        <el-col :span="12" class="andon-launch-col">
          <LaunchLineMatrix
            :stations="stations"
            :lines="lines"
            :tasks="tasks"
            :active-by-station="activeByStation"
            :loading="loading"
            :selected-key="selectedKey"
            @select="onSelectTask"
          />
        </el-col>
        <el-col :span="12" class="andon-launch-col">
          <LaunchReasonPanel
            :selected-task="selectedTask"
            :reason-options="reasonOptions"
            :category-filter="categoryFilter"
            :auth-user="authUser"
            @submitted="onSubmitted"
            @clear-category="categoryFilter = ''"
          />
        </el-col>
      </el-row>

      <TodayLaunchStrip :records="todayRecords" :loading="loading" />
    </div>

    <BatchLaunchDrawer
      v-model="showBatchDrawer"
      :loading="loading"
      :tasks="tasks"
      :reason-options="reasonOptions"
      :active-by-station="activeByStation"
      :auth-user="authUser"
      :draft="draft"
      @reload="loadAll"
      @submitted="onSubmitted"
    />
  </div>
</template>

<script>
import { getAuthUser } from "@/utils/auth-context";
import { fetchDispatchTasks } from "@/api/planning";
import { listAndonrecord } from "@/yunshu-ui/api/mes/pro/andonrecord";
import { listAndonconfig } from "@/yunshu-ui/api/mes/pro/andonconfig";
import { getAndonBoardStations, getAndonBoardLines } from "@/yunshu-ui/api/mes/pro/andonboard";
import LaunchHeroBar from "./components/LaunchHeroBar.vue";
import LaunchCategoryBar from "./components/LaunchCategoryBar.vue";
import LaunchLineMatrix from "./components/LaunchLineMatrix.vue";
import LaunchReasonPanel from "./components/LaunchReasonPanel.vue";
import TodayLaunchStrip from "./components/TodayLaunchStrip.vue";
import BatchLaunchDrawer from "./components/BatchLaunchDrawer.vue";

function rowKey(row) {
  return String(row.dispatchId || row.taskId || row.dispatchNo || Math.random());
}

function emptyDraft() {
  return { configId: null, andonReason: "", andonLevel: "", remark: "", submitted: false };
}

export default {
  name: "AndonLaunchHub",
  components: {
    LaunchHeroBar,
    LaunchCategoryBar,
    LaunchLineMatrix,
    LaunchReasonPanel,
    TodayLaunchStrip,
    BatchLaunchDrawer,
  },
  data() {
    return {
      loading: false,
      tasks: [],
      stations: [],
      lines: [],
      reasonOptions: [],
      activeByStation: {},
      todayRecords: [],
      draft: {},
      authUser: {},
      selectedKey: null,
      categoryFilter: "",
      showBatchDrawer: false,
      initialStationId: null,
      initialDispatchId: null,
    };
  },
  computed: {
    userLabel() {
      return this.authUser.realName || this.authUser.displayName || this.authUser.username || "—";
    },
    eligibleCount() {
      return this.tasks.filter((t) => this.taskSelectable(t)).length;
    },
    activeStationCount() {
      return Object.keys(this.activeByStation).length;
    },
    selectedTask() {
      return this.tasks.find((t) => t._key === this.selectedKey) || null;
    },
    showBatch() {
      const roles = this.authUser.roleCodes || [];
      return roles.some((r) => ["MANAGER", "PROD_SUPERVISOR", "TESTER"].includes(r));
    },
  },
  mounted() {
    this.syncFromRoute();
    this.authUser = getAuthUser() || {};
    this.loadAll();
  },
  watch: {
    "$route.query"() {
      this.syncFromRoute();
      this.tryPreselect();
    },
    tasks() {
      this.tryPreselect();
    },
  },
  methods: {
    syncFromRoute() {
      this.initialStationId = this.$route.query?.stationId || null;
      this.initialDispatchId = this.$route.query?.dispatchId || null;
      const cat = this.$route.query?.category;
      if (cat && ["material", "quality", "equipment", "process"].includes(String(cat))) {
        this.categoryFilter = String(cat);
      }
    },
    taskSelectable(row) {
      if (row.status === "COMPLETED") return false;
      const wsId = row.stationId || row.workstationId;
      return !(wsId && this.activeByStation[String(wsId)]);
    },
    tryPreselect() {
      if (!this.tasks.length || this.selectedKey) return;
      let hit = null;
      if (this.initialDispatchId) {
        hit = this.tasks.find((t) => String(t.dispatchId || t.taskId) === String(this.initialDispatchId));
      }
      if (!hit && this.initialStationId) {
        hit = this.tasks.find((t) => String(t.stationId || t.workstationId) === String(this.initialStationId));
      }
      if (hit && this.taskSelectable(hit)) {
        this.onSelectTask(hit);
        return;
      }
      const first = this.tasks.find((t) => this.taskSelectable(t));
      if (first) this.onSelectTask(first);
    },
    onSelectTask(task) {
      if (!this.taskSelectable(task)) return;
      this.selectedKey = task._key;
    },
    goBoard() {
      this.$router.push("/app/andon");
    },
    goHandle() {
      this.$router.push("/app/andon/handle");
    },
    async loadAll() {
      this.loading = true;
      try {
        const nick = this.authUser.realName || this.authUser.username;
        const [tasksRes, reasonsRes, activeRes, todayRes, stationsRes, linesRes] = await Promise.all([
          fetchDispatchTasks(),
          listAndonconfig({ pageNum: 1, pageSize: 200 }),
          listAndonrecord({ status: "ACTIVE", pageNum: 1, pageSize: 200 }),
          listAndonrecord({ pageNum: 1, pageSize: 200, nickName: nick }),
          getAndonBoardStations(),
          getAndonBoardLines(),
        ]);
        let tasks = tasksRes?.data || [];
        const roleCodes = this.authUser.roleCodes || [];
        if (roleCodes.includes("LINE_OPERATOR") && this.authUser.userId) {
          tasks = tasks.filter((t) => String(t.operatorId) === String(this.authUser.userId));
        }
        this.tasks = tasks.map((t) => ({ ...t, _key: rowKey(t) }));
        this.reasonOptions = reasonsRes?.data || reasonsRes?.rows || [];
        this.stations = stationsRes?.data || [];
        this.lines = linesRes?.data || [];
        const activeRows = activeRes?.rows || [];
        this.activeByStation = {};
        activeRows.forEach((r) => {
          const id = r.workstationId || r.workstation_id;
          if (id) this.activeByStation[String(id)] = true;
        });
        const today = new Date().toISOString().slice(0, 10);
        this.todayRecords = (todayRes?.rows || []).filter((r) => String(r.createTime || "").startsWith(today));
        const d = {};
        this.tasks.forEach((t) => { d[t._key] = { ...emptyDraft() }; });
        this.draft = d;
        this.tryPreselect();
      } finally {
        this.loading = false;
      }
    },
    onSubmitted() {
      this.showBatchDrawer = false;
      this.loadAll();
    },
  },
};
</script>
