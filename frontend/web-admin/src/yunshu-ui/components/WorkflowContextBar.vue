<template>
  <section v-if="visible" class="workflow-context-bar">
    <div class="workflow-context-bar__main">
      <span class="workflow-context-bar__label">工单流程</span>
      <strong class="workflow-context-bar__code">{{ workOrderCode }}</strong>
      <el-tag size="small" type="info">{{ lifecycleLabel }}</el-tag>
      <span v-if="orderNo" class="workflow-context-bar__meta">订单 {{ orderNo }}</span>
      <span v-if="productName" class="workflow-context-bar__meta">{{ productName }}</span>
    </div>
    <div class="workflow-context-bar__actions">
      <el-button v-if="nextRoute" size="small" type="primary" link @click="goNext">
        下一步：{{ nextLabel }}
      </el-button>
      <el-button size="small" link @click="refresh">刷新</el-button>
    </div>
  </section>
</template>

<script>
import { fetchWorkflowPipeline } from "@/yunshu-ui/api/mes/workflow";

const LIFECYCLE_LABELS = {
  DRAFT: "草稿",
  RELEASED: "已下达",
  KITTING_OK: "齐套完成",
  SCHEDULED: "已排产",
  MATERIAL_ISSUED: "已领料",
  IN_PROGRESS: "生产中",
  QC_PENDING: "待质检",
  QC_PASSED: "质检通过",
  QC_FAILED: "质检不合格",
  COMPLETED: "已完工",
};

export default {
  name: "WorkflowContextBar",
  props: {
    workOrderCode: { type: String, default: "" },
  },
  emits: ["loaded"],
  data() {
    return {
      pipeline: null,
      loading: false,
    };
  },
  computed: {
    visible() {
      return Boolean((this.workOrderCode || "").trim());
    },
    lifecycleLabel() {
      const ls = this.pipeline?.workOrder?.lifecycleStatus;
      return LIFECYCLE_LABELS[ls] || ls || "—";
    },
    orderNo() {
      return this.pipeline?.workOrder?.orderNo || "";
    },
    productName() {
      return this.pipeline?.workOrder?.productName || "";
    },
    nextLabel() {
      return this.pipeline?.nextAction?.label || "";
    },
    nextRoute() {
      return this.pipeline?.nextAction?.route || "";
    },
  },
  watch: {
    workOrderCode: {
      immediate: true,
      handler(code) {
        if ((code || "").trim()) {
          this.load(code.trim());
        } else {
          this.pipeline = null;
        }
      },
    },
  },
  methods: {
    async load(code) {
      this.loading = true;
      try {
        const res = await fetchWorkflowPipeline(code);
        this.pipeline = res?.data || null;
        this.$emit("loaded", this.pipeline);
      } catch {
        this.pipeline = null;
      } finally {
        this.loading = false;
      }
    },
    refresh() {
      if (this.visible) {
        this.load(this.workOrderCode.trim());
      }
    },
    goNext() {
      if (this.nextRoute) {
        this.$router.push(this.nextRoute);
      }
    },
  },
};
</script>

<style scoped>
.workflow-context-bar {
  display: flex;
  align-items: center;
  justify-content: space-between;
  gap: 12px;
  margin-bottom: 12px;
  padding: 10px 14px;
  border: 1px solid rgba(64, 158, 255, 0.25);
  border-radius: 8px;
  background: linear-gradient(90deg, rgba(64, 158, 255, 0.08), rgba(64, 158, 255, 0.02));
}
.workflow-context-bar__main {
  display: flex;
  flex-wrap: wrap;
  align-items: center;
  gap: 8px;
  min-width: 0;
}
.workflow-context-bar__label {
  color: #606266;
  font-size: 13px;
}
.workflow-context-bar__code {
  color: #1a1a1a;
  font-size: 14px;
}
.workflow-context-bar__meta {
  color: #909399;
  font-size: 12px;
}
.workflow-context-bar__actions {
  display: flex;
  align-items: center;
  gap: 4px;
  flex-shrink: 0;
}
</style>
