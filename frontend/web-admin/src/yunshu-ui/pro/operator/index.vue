<template>
  <div class="app-container operator-command-center">
    <WorkflowContextBar :work-order-code="selectedTask?.workOrderNo || ''" />
    <OperatorHeroBar :kpi="kpi" @refresh="refreshAll" @open-andon="openAndonFromHero" />

    <el-row :gutter="0" class="operator-hub-row">
      <el-col :span="5" class="operator-hub-aside">
        <TaskQueueAside
          :tasks="filteredTasks"
          :all-tasks="tasks"
          :selected-id="selectedTask?.dispatchId"
          :status-filter="statusFilter"
          :loading="loading"
          :user-label="userLabel"
          :employee-no="authUser.employeeNo || authUser.username"
          :today-qualified="todayQualified"
          :initial-keyword="routeKeyword"
          @update:status-filter="statusFilter = $event"
          @select="selectTask"
        />
      </el-col>
      <el-col :span="14" class="operator-hub-main-col">
        <TaskWorkbench
          :task="selectedTask"
          :today-feedback="todayFeedback"
          :feedback-loading="feedbackLoading"
          :starting="starting"
          @start="startTask"
          @report="openReport"
          @bind="openBind"
          @andon="openAndon"
        />
      </el-col>
      <el-col :span="5" class="operator-hub-sop">
        <SopPanel :task="selectedTask" />
      </el-col>
    </el-row>

    <ReportDrawer ref="reportDrawer" @submitted="onReportSubmitted" />
    <BindMaterialDrawer ref="bindDrawer" />
    <AndonDialog ref="andonDialog" @submitted="loadAndonKpi" />
    <AndonLaunch ref="andonLaunch" @submitted="loadAndonKpi" />
  </div>
</template>

<script>
import { getAuthUser } from "@/utils/auth-context";
import OperatorHeroBar from "./components/OperatorHeroBar.vue";
import TaskQueueAside from "./components/TaskQueueAside.vue";
import TaskWorkbench from "./components/TaskWorkbench.vue";
import SopPanel from "./components/SopPanel.vue";
import ReportDrawer from "./components/ReportDrawer.vue";
import BindMaterialDrawer from "./components/BindMaterialDrawer.vue";
import AndonDialog from "./components/AndonDialog.vue";
import AndonLaunch from "@/yunshu-ui/pro/andon/launch.vue";
import WorkflowContextBar from "@/yunshu-ui/components/WorkflowContextBar.vue";
import {
  fetchMyDispatchTasks,
  fetchTodayFeedback,
  fetchActiveAndonCount,
  startDispatchTask,
  filterDispatchTasksForUser,
  sortDispatchTasksRecentFirst,
  shouldViewAllDispatchTasks,
} from "@/yunshu-ui/api/mes/pro/operator";
import { useAppStore } from "@/stores/app";

export default {
  name: "OperatorWorkYunshu",
  components: {
    OperatorHeroBar,
    TaskQueueAside,
    TaskWorkbench,
    SopPanel,
    ReportDrawer,
    BindMaterialDrawer,
    AndonDialog,
    AndonLaunch,
    WorkflowContextBar,
  },
  data() {
    return {
      loading: false,
      feedbackLoading: false,
      starting: false,
      tasks: [],
      selectedTask: null,
      statusFilter: "all",
      todayFeedback: [],
      activeAndon: 0,
      authUser: {},
    };
  },
  computed: {
    viewAllTasks() {
      return shouldViewAllDispatchTasks(this.authUser);
    },
    userLabel() {
      return this.authUser.realName || this.authUser.displayName || this.authUser.username || "操作工";
    },
    routeKeyword() {
      return String(this.$route.query.wo || this.$route.query.workOrder || "").trim();
    },
    filteredTasks() {
      let list = this.tasks;
      if (this.statusFilter !== "all") {
        if (this.statusFilter === "CREATED") {
          list = this.tasks.filter((t) => t.status === "CREATED" || t.status === "DISPATCHED");
        } else {
          list = this.tasks.filter((t) => t.status === this.statusFilter);
        }
      }
      return sortDispatchTasksRecentFirst(list);
    },
    todayQualified() {
      const today = new Date().toISOString().slice(0, 10);
      return this.todayFeedback
        .filter((r) => String(r.feedbackTime || "").startsWith(today))
        .reduce((sum, r) => sum + (Number(r.quantityQualified) || 0), 0);
    },
    kpi() {
      return {
        total: this.tasks.length,
        running: this.tasks.filter((t) => t.status === "RUNNING").length,
        pending: this.tasks.filter((t) => t.status !== "COMPLETED").length,
        todayFeedback: this.todayFeedback.length,
        activeAndon: this.activeAndon,
      };
    },
  },
  created() {
    const app = useAppStore();
    this.authUser = app.authUser || getAuthUser() || {};
    this.refreshAll();
  },
  methods: {
    pickInitialTask(tasks) {
      const key = this.routeKeyword.toLowerCase();
      if (key) {
        const matched = tasks.filter((t) =>
          [t.workOrderNo, t.dispatchNo, t.productCode, t.productName]
            .filter(Boolean)
            .some((v) => String(v).toLowerCase().includes(key))
        );
        if (matched.length) return matched[0];
      }
      return tasks.find((t) => t.status === "RUNNING") || tasks[0] || null;
    },
    refreshAll() {
      this.loadTasks();
      this.loadTodayFeedback();
      this.loadAndonKpi();
    },
    loadTasks() {
      this.loading = true;
      fetchMyDispatchTasks()
        .then((r) => {
          const rows = sortDispatchTasksRecentFirst(filterDispatchTasksForUser(r.data || [], this.authUser));
          this.tasks = rows;
          if (this.selectedTask) {
            const hit = rows.find((t) => t.dispatchId === this.selectedTask.dispatchId);
            this.selectedTask = hit || this.pickInitialTask(rows);
          } else if (rows.length) {
            this.selectedTask = this.pickInitialTask(rows);
          } else {
            this.selectedTask = null;
          }
        })
        .catch((err) => {
          this.tasks = [];
          this.selectedTask = null;
          this.$modal?.msgError?.(err?.message || "加载派工任务失败，请确认已登录 worker 账号");
        })
        .finally(() => {
          this.loading = false;
        });
    },
    loadTodayFeedback() {
      this.feedbackLoading = true;
      const userName = this.authUser.username;
      const alt = userName === "worker" ? "worker01" : userName === "worker01" ? "worker" : null;
      fetchTodayFeedback(userName, alt, this.viewAllTasks)
        .then((res) => {
          const rows = res?.rows || [];
          const today = new Date().toISOString().slice(0, 10);
          this.todayFeedback = rows.filter((r) => String(r.feedbackTime || "").startsWith(today));
        })
        .catch(() => {
          this.todayFeedback = [];
        })
        .finally(() => {
          this.feedbackLoading = false;
        });
    },
    loadAndonKpi() {
      fetchActiveAndonCount()
        .then((res) => {
          const rows = res?.rows || res?.data || [];
          const stations = new Set();
          rows.forEach((r) => {
            if (r.workstationId) stations.add(String(r.workstationId));
          });
          this.activeAndon = stations.size;
        })
        .catch(() => {
          this.activeAndon = 0;
        });
    },
    selectTask(task) {
      this.selectedTask = task;
    },
    startTask() {
      if (!this.selectedTask || this.starting) return;
      this.starting = true;
      startDispatchTask(this.selectedTask.dispatchId, this.selectedTask)
        .then(() => {
          this.$modal.msgSuccess("已开始作业");
          this.loadTasks();
        })
        .finally(() => {
          this.starting = false;
        });
    },
    openReport() {
      if (this.selectedTask) this.$refs.reportDrawer?.open(this.selectedTask);
    },
    openBind() {
      if (this.selectedTask) this.$refs.bindDrawer?.open(this.selectedTask);
    },
    openAndon() {
      if (this.selectedTask) this.$refs.andonDialog?.open(this.selectedTask);
    },
    openAndonFromHero() {
      const t = this.selectedTask;
      const query = t
        ? { dispatchId: String(t.dispatchId || t.taskId || ""), stationId: String(t.stationId || t.workstationId || "") }
        : {};
      this.$refs.andonLaunch?.open(query);
    },
    onReportSubmitted() {
      this.loadTasks();
      this.loadTodayFeedback();
    },
  },
};
</script>
