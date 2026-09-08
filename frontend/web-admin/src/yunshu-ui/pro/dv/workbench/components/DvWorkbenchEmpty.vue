<template>
  <div class="qc-workbench-empty dv-workbench-empty">
    <div class="qc-workbench-empty__inner">
      <strong class="qc-workbench-empty__title">设备工作台</strong>
      <p class="qc-workbench-empty__hint">从左侧选择待点检、待保养或待维修任务开始处理</p>
      <div class="qc-workbench-empty__legend">
        <div v-for="item in legend" :key="item.key" class="qc-workbench-empty__chip" :class="`qc-workbench-empty__chip--${item.key}`">
          <span class="qc-workbench-empty__dot" />
          <span>{{ item.label }}</span>
          <b>{{ item.count }}</b>
        </div>
      </div>
    </div>
  </div>
</template>

<script>
export default {
  name: "DvWorkbenchEmpty",
  props: { pendingRows: { type: Array, default: () => [] } },
  computed: {
    legend() {
      const rows = this.pendingRows || [];
      return [
        { key: "check", label: "点检", count: rows.filter((r) => r.taskType === "CHECK").length },
        { key: "mainten", label: "保养", count: rows.filter((r) => r.taskType === "MAINTEN").length },
        { key: "repair", label: "维修", count: rows.filter((r) => r.taskType === "REPAIR").length },
      ];
    },
  },
};
</script>
