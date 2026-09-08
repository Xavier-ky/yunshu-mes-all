<template>
  <div class="andon-launch-reason">
    <div class="andon-launch-reason__head">
      <strong class="andon-launch-reason__title">选择原因 · 拉绳</strong>
      <span v-if="categoryFilter" class="andon-launch-reason__filter">
        分类：{{ categoryLabel }}
        <el-button link type="primary" size="small" @click="$emit('clear-category')">清除</el-button>
      </span>
    </div>

    <div v-if="!selectedTask" class="andon-launch-reason__empty">
      <p>请从左侧矩阵选择有派工且未处置中的工位</p>
      <p class="andon-launch-reason__empty-sub">也可从看板点击工位跳转至本页</p>
    </div>

    <div v-else class="andon-launch-reason__fill">
      <div class="andon-launch-reason__scroll">
        <div class="andon-launch-context">
          <div class="andon-launch-context__item">
            <span class="andon-launch-context__label">产线</span>
            <strong>{{ selectedTask.lineName || "—" }}</strong>
          </div>
          <div class="andon-launch-context__item">
            <span class="andon-launch-context__label">工位</span>
            <strong>{{ selectedTask.stationName || selectedTask.workstationName || "—" }}</strong>
          </div>
          <div class="andon-launch-context__item">
            <span class="andon-launch-context__label">工单</span>
            <strong>{{ selectedTask.workOrderNo || selectedTask.workorderCode || "—" }}</strong>
          </div>
          <div class="andon-launch-context__item">
            <span class="andon-launch-context__label">工序</span>
            <strong>{{ selectedTask.stepName || selectedTask.processName || "—" }}</strong>
          </div>
        </div>

        <div class="andon-launch-reason__section-head">选择安灯原因</div>

        <div class="andon-launch-reason__grid">
          <button
            v-for="cfg in filteredReasons"
            :key="cfg.configId"
            type="button"
            class="quick-reason-card"
            :class="[reasonTone(cfg), { 'is-selected': selectedConfigId === cfg.configId }]"
            @click="pickReason(cfg)"
          >
            <span class="quick-reason-card__label">{{ cfg.andonReason }}</span>
            <span class="quick-reason-card__level">{{ cfg.andonLevel || "LEVEL3" }}</span>
          </button>
          <div v-if="!filteredReasons.length" class="andon-launch-reason__no-reason">
            当前分类暂无配置原因，请切换分类或联系管理员
          </div>
        </div>

        <div v-if="selectedConfig" class="andon-launch-handler">
          <span><em>处置角色</em>{{ selectedConfig.handlerRoleName || "—" }}</span>
          <span><em>处置人</em>{{ selectedConfig.handlerNickName || selectedConfig.handlerUserName || "—" }}</span>
          <span><em>级别</em>{{ selectedConfig.andonLevel || "LEVEL3" }}</span>
        </div>
      </div>

      <div class="andon-launch-reason__footer">
        <label class="andon-launch-reason__remark-label">补充说明（可选）</label>
        <el-input
          v-model="remark"
          type="textarea"
          :rows="6"
          placeholder="简要描述现场情况…"
          class="andon-launch-remark"
        />
        <el-button
          type="warning"
          size="large"
          class="andon-launch-pull-btn"
          :disabled="!canSubmit"
          :loading="submitting"
          :auto-insert-space="false"
          @click="submit"
        >
          拉绳 · 发起安灯
        </el-button>
      </div>
    </div>
  </div>
</template>

<script>
import { addAndonrecord } from "@/yunshu-ui/api/mes/pro/andonrecord";
import { categorizeReason, CATEGORIES, reasonTone } from "../../../utils/category";

export default {
  name: "LaunchReasonPanel",
  props: {
    selectedTask: { type: Object, default: null },
    reasonOptions: { type: Array, default: () => [] },
    categoryFilter: { type: String, default: "" },
    authUser: { type: Object, default: () => ({}) },
  },
  emits: ["submitted", "clear-category"],
  data() {
    return {
      selectedConfigId: null,
      selectedConfig: null,
      remark: "",
      submitting: false,
    };
  },
  computed: {
    categoryLabel() {
      return CATEGORIES.find((c) => c.key === this.categoryFilter)?.label || this.categoryFilter;
    },
    filteredReasons() {
      let list = this.reasonOptions || [];
      if (this.categoryFilter) {
        list = list.filter((cfg) => categorizeReason(cfg.andonReason) === this.categoryFilter);
      }
      return list;
    },
    canSubmit() {
      return this.selectedTask && this.selectedConfig;
    },
  },
  watch: {
    selectedTask() {
      this.selectedConfigId = null;
      this.selectedConfig = null;
      this.remark = "";
    },
    categoryFilter() {
      this.selectedConfigId = null;
      this.selectedConfig = null;
    },
  },
  methods: {
    reasonTone(cfg) {
      return reasonTone(cfg.andonReason);
    },
    pickReason(cfg) {
      this.selectedConfigId = cfg.configId;
      this.selectedConfig = cfg;
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
