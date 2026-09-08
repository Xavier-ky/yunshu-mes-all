<template>
  <div class="andon-quick-launch">
    <el-row :gutter="16">
      <el-col :span="10">
        <div class="quick-section">
          <h3 class="quick-section__title">选择工位</h3>
          <div v-loading="loading" class="quick-station-list">
            <div
              v-for="row in eligibleTasks"
              :key="row._key"
              class="quick-station-card"
              :class="{ 'is-selected': selectedKey === row._key, 'is-disabled': !rowSelectable(row) }"
              @click="selectTask(row)"
            >
              <strong>{{ row.stationName || row.workstationName }}</strong>
              <span>{{ row.workOrderNo || row.workorderCode }}</span>
              <span>{{ row.stepName || row.processName }}</span>
              <el-tag v-if="hasActiveAndon(row)" size="small" type="danger">待处置中</el-tag>
            </div>
            <el-empty v-if="!loading && !eligibleTasks.length" description="暂无可发起派工" />
          </div>
        </div>
      </el-col>
      <el-col :span="14">
        <div class="quick-section">
          <h3 class="quick-section__title">选择原因 · 拉绳</h3>
          <div v-if="!selectedTask" class="quick-hint">请先从左侧选择工位</div>
          <template v-else>
            <div class="quick-reason-grid">
              <button
                v-for="cfg in reasonOptions"
                :key="cfg.configId"
                type="button"
                class="quick-reason-card"
                :class="[reasonTone(cfg), { 'is-selected': selectedConfigId === cfg.configId }]"
                @click="pickReason(cfg)"
              >
                <span class="quick-reason-card__label">{{ cfg.andonReason }}</span>
                <span class="quick-reason-card__level">{{ cfg.andonLevel }}</span>
              </button>
            </div>
            <el-input v-model="remark" type="textarea" :rows="3" placeholder="补充说明（可选）" class="quick-remark" />
            <el-button type="warning" size="large" class="quick-pull-btn" :disabled="!canSubmit" :loading="submitting" @click="submit">
              拉绳 · 发起安灯
            </el-button>
          </template>
        </div>
      </el-col>
    </el-row>
  </div>
</template>

<script>
import { addAndonrecord } from "@/yunshu-ui/api/mes/pro/andonrecord";

export default {
  name: "QuickLaunchPanel",
  props: {
    loading: { type: Boolean, default: false },
    tasks: { type: Array, default: () => [] },
    reasonOptions: { type: Array, default: () => [] },
    activeByStation: { type: Object, default: () => ({}) },
    authUser: { type: Object, default: () => ({}) },
    initialStationId: { type: [String, Number], default: null },
    initialDispatchId: { type: [String, Number], default: null },
  },
  emits: ["submitted"],
  data() {
    return {
      selectedKey: null,
      selectedConfigId: null,
      selectedConfig: null,
      remark: "",
      submitting: false,
    };
  },
  computed: {
    eligibleTasks() {
      return this.tasks.filter((t) => t.status !== "COMPLETED");
    },
    selectedTask() {
      return this.tasks.find((t) => t._key === this.selectedKey) || null;
    },
    canSubmit() {
      return this.selectedTask && this.selectedConfig && this.rowSelectable(this.selectedTask);
    },
  },
  watch: {
    tasks: {
      immediate: true,
      handler(list) {
        this.tryPreselect(list);
      },
    },
  },
  methods: {
    tryPreselect(list) {
      if (!list?.length || this.selectedKey) return;
      let hit = null;
      if (this.initialDispatchId) {
        hit = list.find((t) => String(t.dispatchId || t.taskId) === String(this.initialDispatchId));
      }
      if (!hit && this.initialStationId) {
        hit = list.find((t) => String(t.stationId || t.workstationId) === String(this.initialStationId));
      }
      if (hit && this.rowSelectable(hit)) this.selectTask(hit);
      else if (list.length === 1 && this.rowSelectable(list[0])) this.selectTask(list[0]);
    },
    rowSelectable(row) {
      return row.status !== "COMPLETED" && !this.hasActiveAndon(row);
    },
    hasActiveAndon(row) {
      const wsId = row.stationId || row.workstationId;
      return wsId && this.activeByStation[String(wsId)];
    },
    selectTask(row) {
      if (!this.rowSelectable(row)) return;
      this.selectedKey = row._key;
    },
    pickReason(cfg) {
      this.selectedConfigId = cfg.configId;
      this.selectedConfig = cfg;
    },
    reasonTone(cfg) {
      const t = (cfg.andonReason || "").toLowerCase();
      if (t.includes("料") || t.includes("物料")) return "tone-material";
      if (t.includes("质") || t.includes("检")) return "tone-quality";
      if (t.includes("设备") || t.includes("机")) return "tone-equipment";
      if (t.includes("工艺") || t.includes("程")) return "tone-process";
      return "tone-default";
    },
    async submit() {
      if (!this.canSubmit) return;
      this.submitting = true;
      try {
        const row = this.selectedTask;
        const cfg = this.selectedConfig;
        const u = this.authUser;
        await addAndonrecord({
          workstationId: row.stationId || row.workstationId,
          workstationCode: row.stationCode || row.workstationCode,
          workstationName: row.stationName || row.workstationName,
          workorderId: row.workOrderId || row.workorderId,
          workorderCode: row.workOrderNo || row.workorderCode,
          workorderName: row.workOrderName || row.productName,
          processId: row.stepId || row.processId,
          processCode: row.stepCode || row.processCode,
          processName: row.stepName || row.processName,
          userId: u.userId,
          userName: u.username,
          nickName: u.realName || u.displayName || u.username,
          andonReason: cfg.andonReason,
          andonLevel: cfg.andonLevel || "LEVEL3",
          handlerRoleId: cfg.handlerRoleId,
          handlerRoleName: cfg.handlerRoleName,
          handlerUserId: cfg.handlerUserId,
          handlerUserName: cfg.handlerUserName,
          handlerNickName: cfg.handlerNickName,
          status: "ACTIVE",
          remark: this.remark,
        });
        this.$modal.msgSuccess("安灯已发起");
        this.remark = "";
        this.selectedConfigId = null;
        this.selectedConfig = null;
        this.$emit("submitted");
      } finally {
        this.submitting = false;
      }
    },
  },
};
</script>
