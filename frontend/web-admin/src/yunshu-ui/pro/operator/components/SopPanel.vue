<template>
  <div v-loading="loading" class="operator-sop-panel">
    <div class="operator-sop-head">
      <span>SOP 作业指引</span>
      <el-tag v-if="task && steps.length" size="small" type="info">{{ steps.length }} 步</el-tag>
    </div>
    <template v-if="task">
      <img v-if="coverUrl && !coverFailed" class="operator-sop-cover" :src="coverUrl" alt="工序封面" @error="coverFailed = true" />
      <div class="operator-sop-body">
        <p v-if="processName" class="operator-sop-subtitle">
          {{ processName }} · {{ task.stepCode || "" }}
        </p>
        <div v-for="(step, idx) in steps" :key="step.contentId || idx" class="operator-sop-step" :data-step="step.orderNum || idx + 1">
          <p>{{ step.contentText || step.remark || "—" }}</p>
          <small v-if="step.device || step.material">
            工装 {{ step.device || "—" }} · 物料 {{ step.material || "—" }}
          </small>
          <img v-if="step.docUrl" :src="step.docUrl" alt="步骤图" @error="$event.target.style.display='none'" />
        </div>
        <div v-if="!loading && !steps.length" class="operator-empty-hint">
          该工序暂无作业指导，请联系工艺员
        </div>
      </div>
    </template>
    <div v-else class="operator-empty-hint">选择任务后显示 SOP</div>
  </div>
</template>

<script>
import { fetchProcessByCode, fetchProcessSop } from "@/yunshu-ui/api/mes/pro/operator";

export default {
  name: "OperatorSopPanel",
  props: {
    task: { type: Object, default: null },
  },
  data() {
    return {
      loading: false,
      steps: [],
      coverUrl: "",
      coverFailed: false,
      processName: "",
    };
  },
  watch: {
    task: {
      immediate: true,
      handler(t) {
        this.loadSop(t);
      },
    },
  },
  methods: {
    async loadSop(task) {
      this.steps = [];
      this.coverUrl = "";
      this.coverFailed = false;
      this.processName = "";
      if (!task?.stepCode) return;
      this.loading = true;
      try {
        const procRes = await fetchProcessByCode(task.stepCode);
        const rows = procRes?.rows || procRes?.data || [];
        const proc = Array.isArray(rows) ? rows[0] : null;
        if (!proc?.processId) {
          return;
        }
        this.processName = proc.processName || task.stepName || "";
        this.coverUrl = proc.attr2 || "";
        const sopRes = await fetchProcessSop(proc.processId);
        const list = sopRes?.rows || sopRes?.data || [];
        this.steps = [...list].sort((a, b) => (a.orderNum || 0) - (b.orderNum || 0));
      } catch {
        this.steps = [];
      } finally {
        this.loading = false;
      }
    },
  },
};
</script>
