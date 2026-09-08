<template>
  <div class="operator-workbench">
    <template v-if="task">
      <div class="operator-workbench-head">
        <div class="operator-task-bar">
          <div class="operator-task-bar__lead">
            <span class="operator-task-bar__code">{{ task.dispatchNo }}</span>
            <span class="operator-task-bar__name">{{ task.productName || "—" }}</span>
            <span v-if="task.productCode" class="operator-task-bar__code2">{{ task.productCode }}</span>
          </div>
          <div class="operator-task-bar__meta">
            <span><i>工单</i>{{ task.workOrderNo || "—" }}</span>
            <span><i>工序</i>{{ task.stepName || "—" }}</span>
            <span><i>工位</i>{{ task.stationName || "—" }}</span>
            <span><i>产线</i>{{ task.lineName || "—" }}</span>
            <span><i>时段</i>{{ timeRange }}</span>
          </div>
          <div class="operator-task-bar__progress">
            <i>进度</i>
            <el-progress
              :percentage="progressPct"
              :stroke-width="6"
              :show-text="false"
              style="width: 72px"
            />
            <b>{{ task.completedQty || 0 }}/{{ task.plannedQty || 0 }}</b>
          </div>
        </div>
        <div class="operator-action-row">
          <el-button
            class="operator-ui-btn"
            type="primary"
            :auto-insert-space="false"
            :disabled="task.status === 'RUNNING' || task.status === 'COMPLETED'"
            :loading="starting"
            @click="$emit('start')"
          >
            <span class="operator-ui-btn__label">开始作业</span>
          </el-button>
          <el-button class="operator-ui-btn" type="success" :auto-insert-space="false" :disabled="task.status === 'COMPLETED'" @click="$emit('report')">
            <span class="operator-ui-btn__label">生产报工</span>
          </el-button>
          <el-button class="operator-ui-btn" type="info" :auto-insert-space="false" :disabled="task.status === 'COMPLETED'" @click="$emit('bind')">
            <span class="operator-ui-btn__label">物料绑定</span>
          </el-button>
          <el-button class="operator-ui-btn" type="warning" :auto-insert-space="false" :disabled="task.status === 'COMPLETED'" @click="$emit('andon')">
            <span class="operator-ui-btn__label">安灯呼叫</span>
          </el-button>
        </div>
      </div>
    </template>
    <div v-else class="operator-empty-hint operator-workbench-empty">请从左侧选择一条派工任务</div>

    <TaskContextTabs
      :task="task"
      :today-feedback="todayFeedback"
      :feedback-loading="feedbackLoading"
    />
  </div>
</template>

<script>
import TaskContextTabs from "./TaskContextTabs.vue";

export default {
  name: "OperatorTaskWorkbench",
  components: { TaskContextTabs },
  props: {
    task: { type: Object, default: null },
    todayFeedback: { type: Array, default: () => [] },
    feedbackLoading: { type: Boolean, default: false },
    starting: { type: Boolean, default: false },
  },
  emits: ["start", "report", "bind", "andon"],
  computed: {
    progressPct() {
      if (!this.task) return 0;
      const planned = Number(this.task.plannedQty) || 0;
      const done = Number(this.task.completedQty) || 0;
      if (planned <= 0) return 0;
      return Math.min(100, Math.round((done / planned) * 100));
    },
    timeRange() {
      if (!this.task?.plannedStartTime) return "—";
      const start = this.formatTime(this.task.plannedStartTime);
      const end = this.task.plannedEndTime ? this.formatTime(this.task.plannedEndTime) : "";
      return end ? `${start}~${end}` : start;
    },
  },
  methods: {
    formatTime(v) {
      if (!v) return "—";
      const s = String(v);
      const t = s.length > 16 ? s.slice(0, 16) : s;
      return t.replace("T", " ").slice(5, 16);
    },
  },
};
</script>
