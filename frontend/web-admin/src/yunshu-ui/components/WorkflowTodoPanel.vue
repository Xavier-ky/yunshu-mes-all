<template>
  <section v-if="items.length" class="workflow-todo-panel">
    <div class="workflow-todo-panel__row">
      <span class="workflow-todo-panel__title">
        <i class="workflow-todo-panel__mark" aria-hidden="true"></i>
        流程待办
      </span>
      <div class="workflow-todo-panel__track" role="list">
        <button
          v-for="item in items"
          :key="item.workOrderId"
          type="button"
          class="workflow-todo-panel__chip"
          role="listitem"
          :title="chipTitle(item)"
          @click="$emit('select', item.workOrderCode)"
        >
          <strong class="workflow-todo-panel__code">{{ item.workOrderCode }}</strong>
          <el-tag size="small" :type="lifecycleTagType(item.lifecycleStatus)" effect="plain">
            {{ lifecycleLabel(item.lifecycleStatus) }}
          </el-tag>
          <span v-if="item.productName" class="workflow-todo-panel__product">{{ item.productName }}</span>
          <span v-if="formatCreatedAt(item)" class="workflow-todo-panel__time">{{ formatCreatedAt(item) }}</span>
        </button>
      </div>
      <el-button class="workflow-todo-panel__refresh" size="small" link @click="load">刷新</el-button>
    </div>
  </section>
</template>

<script>
import { fetchWorkflowTodos } from "@/yunshu-ui/api/mes/workflow";
import { parseTime } from "@/yunshu-ui/utils/yunshu-utils";

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
  name: "WorkflowTodoPanel",
  emits: ["select"],
  data() {
    return {
      items: [],
    };
  },
  mounted() {
    this.load();
  },
  methods: {
    async load() {
      try {
        const res = await fetchWorkflowTodos();
        const rows = Array.isArray(res?.data) ? res.data : [];
        this.items = rows
          .slice()
          .sort((a, b) => this.createdTs(b) - this.createdTs(a))
          .slice(0, 20);
      } catch {
        this.items = [];
      }
    },
    createdTs(item) {
      const raw = item?.createdAt;
      if (!raw) return 0;
      const ts = new Date(raw).getTime();
      return Number.isNaN(ts) ? 0 : ts;
    },
    lifecycleLabel(status) {
      return LIFECYCLE_LABELS[status] || status || "—";
    },
    lifecycleTagType(status) {
      if (status === "KITTING_OK" || status === "SCHEDULED" || status === "COMPLETED") return "success";
      if (status === "RELEASED" || !status) return "info";
      if (status === "QC_FAILED") return "danger";
      return "warning";
    },
    formatCreatedAt(item) {
      const raw = item?.createdAt;
      if (!raw) return "";
      return parseTime(raw, "{m}-{d} {h}:{i}") || "";
    },
    chipTitle(item) {
      const parts = [
        item.workOrderCode,
        this.lifecycleLabel(item.lifecycleStatus),
        item.productName,
        this.formatCreatedAt(item) ? `发起 ${this.formatCreatedAt(item)}` : "",
      ].filter(Boolean);
      return parts.join(" · ");
    },
  },
};
</script>

<style scoped>
.workflow-todo-panel {
  position: relative;
  display: flex;
  align-items: center;
  min-height: 50px;
  margin-bottom: 10px;
  padding: 0 12px 0 14px;
  border: 1px solid rgba(23, 105, 224, 0.14);
  border-radius: 10px;
  background:
    linear-gradient(118deg, rgba(255, 255, 255, 0.82) 0%, rgba(241, 247, 255, 0.96) 42%, rgba(232, 242, 252, 0.94) 100%);
  box-shadow:
    0 1px 2px rgba(23, 67, 120, 0.04),
    0 6px 18px rgba(23, 105, 224, 0.07),
    inset 0 1px 0 rgba(255, 255, 255, 0.9);
  overflow: hidden;
  box-sizing: border-box;
}

.workflow-todo-panel::before {
  content: "";
  position: absolute;
  left: 0;
  top: 0;
  bottom: 0;
  width: 3px;
  background: linear-gradient(180deg, #5b9cf5 0%, #1769e0 100%);
  opacity: 0.9;
}

.workflow-todo-panel__row {
  display: flex;
  align-items: center;
  gap: 10px;
  width: 100%;
  height: 32px;
}

.workflow-todo-panel__title {
  flex-shrink: 0;
  display: inline-flex;
  align-items: center;
  gap: 7px;
  height: 32px;
  margin: 0;
  line-height: 32px;
  font-size: 13px;
  font-weight: 600;
  color: #1e3a5f;
  letter-spacing: 0.02em;
  white-space: nowrap;
}

.workflow-todo-panel__mark {
  width: 7px;
  height: 7px;
  border-radius: 50%;
  background: radial-gradient(circle at 35% 35%, #7eb6ff, #1769e0);
  box-shadow: 0 0 0 3px rgba(23, 105, 224, 0.12);
}

.workflow-todo-panel__track {
  flex: 1;
  min-width: 0;
  display: flex;
  flex-wrap: nowrap;
  align-items: center;
  gap: 8px;
  height: 32px;
  overflow-x: auto;
  overflow-y: hidden;
  -ms-overflow-style: none;
  scrollbar-width: none;
}

/* 隐藏横向滚动条，避免占用底部空间导致内容偏上；仍可 Shift+滚轮 / 触控板横滑 */
.workflow-todo-panel__track::-webkit-scrollbar {
  display: none;
}

.workflow-todo-panel__chip {
  flex-shrink: 0;
  display: inline-flex;
  align-items: center;
  gap: 6px;
  max-width: 320px;
  height: 32px;
  padding: 0 11px;
  margin: 0;
  border: 1px solid rgba(23, 105, 224, 0.12);
  border-radius: 999px;
  background: rgba(255, 255, 255, 0.88);
  cursor: pointer;
  color: #1a1a1a;
  white-space: nowrap;
  box-shadow: 0 1px 2px rgba(23, 67, 120, 0.05);
  transition: border-color 0.15s ease, box-shadow 0.15s ease, background 0.15s ease;
}

.workflow-todo-panel__chip :deep(.el-tag) {
  height: 20px;
  padding: 0 6px;
  line-height: 18px;
  border-radius: 4px;
  vertical-align: middle;
}

.workflow-todo-panel__chip:hover {
  border-color: rgba(23, 105, 224, 0.34);
  background: #fff;
  box-shadow: 0 2px 10px rgba(23, 105, 224, 0.12);
}

.workflow-todo-panel__code {
  font-size: 12px;
  font-weight: 600;
  color: #173355;
}

.workflow-todo-panel__product {
  font-size: 12px;
  color: #4a607a;
  overflow: hidden;
  text-overflow: ellipsis;
  max-width: 120px;
}

.workflow-todo-panel__time {
  font-size: 11px;
  color: #6b84a0;
}

.workflow-todo-panel__refresh {
  flex-shrink: 0;
  display: inline-flex !important;
  align-items: center !important;
  justify-content: center !important;
  height: 32px !important;
  margin: 0 !important;
  padding: 0 4px !important;
  line-height: 32px !important;
  color: #1769e0 !important;
  font-weight: 500;
  vertical-align: middle;
}

.workflow-todo-panel__refresh :deep(span) {
  line-height: 32px;
}

.workflow-todo-panel__refresh:hover {
  color: #1256b8 !important;
}
</style>
