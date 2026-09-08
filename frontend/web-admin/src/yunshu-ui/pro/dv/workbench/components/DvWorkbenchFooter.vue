<template>
  <div class="dv-workbench-footer-wrap">
    <div
      class="dv-workbench-today-collapse"
      :class="{ 'is-expanded': todayExpanded }"
    >
      <DvRecentDoneStrip
        embedded
        :rows="recentRows"
        :loading="recentLoading"
        @open-records="$emit('open-records')"
      />
    </div>
    <div class="dv-workbench-footer">
      <span class="dv-workbench-footer__count">共 {{ lineTotal }} 条</span>
      <div class="dv-workbench-footer__actions">
        <el-button class="operator-ui-btn" type="primary" :auto-insert-space="false" @click="$emit('back')">
          <span class="operator-ui-btn__label">{{ backLabel }}</span>
        </el-button>
        <el-button class="operator-ui-btn" :auto-insert-space="false" @click="$emit('close')">
          <span class="operator-ui-btn__label">关闭</span>
        </el-button>
        <el-button
          class="operator-ui-btn dv-workbench-footer__today-btn"
          :auto-insert-space="false"
          @click="$emit('toggle-today')"
        >
          <span class="operator-ui-btn__label">
            今日已完成 ({{ todayCount }})
            <span class="dv-workbench-footer__chevron" :class="{ 'is-up': todayExpanded }">▼</span>
          </span>
        </el-button>
      </div>
    </div>
  </div>
</template>

<script>
import DvRecentDoneStrip from "./DvRecentDoneStrip.vue";

const BACK_LABEL = {
  CHECK: "返回点检记录",
  MAINTEN: "返回保养记录",
  REPAIR: "返回维修中心",
};

export default {
  name: "DvWorkbenchFooter",
  components: { DvRecentDoneStrip },
  props: {
    taskType: { type: String, default: "" },
    lineTotal: { type: Number, default: 0 },
    recentRows: { type: Array, default: () => [] },
    recentLoading: { type: Boolean, default: false },
    todayExpanded: { type: Boolean, default: false },
  },
  emits: ["back", "close", "toggle-today", "open-records"],
  computed: {
    backLabel() {
      return BACK_LABEL[this.taskType] || "返回记录";
    },
    todayCount() {
      const todayStr = new Date().toISOString().slice(0, 10);
      const today = (this.recentRows || []).filter((r) =>
        String(r.finishTime || "").startsWith(todayStr)
      );
      return today.length || (this.recentRows || []).length;
    },
  },
};
</script>
