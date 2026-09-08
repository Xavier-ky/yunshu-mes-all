<template>
  <div class="qc-workbench-empty">
    <div class="qc-workbench-empty__inner">
      <strong class="qc-workbench-empty__title">选择左侧待检任务</strong>
      <p class="qc-workbench-empty__hint">单击预览任务 · 双击或点「开始检验」进入执行</p>
      <div class="qc-workbench-empty__legend">
        <div
          v-for="item in legendItems"
          :key="item.code"
          class="qc-workbench-empty__chip"
          :class="`qc-workbench-empty__chip--${item.code.toLowerCase()}`"
        >
          <span class="qc-workbench-empty__dot" aria-hidden="true"></span>
          <span>{{ item.label }}</span>
          <b>{{ item.count }}</b>
        </div>
      </div>
    </div>
  </div>
</template>

<script>
const META = [
  { code: "IQC", label: "IQC" },
  { code: "PQC", label: "IPQC" },
  { code: "OQC", label: "OQC" },
  { code: "RQC", label: "RQC" },
];

export default {
  name: "QcWorkbenchEmpty",
  props: {
    pendingRows: { type: Array, default: () => [] },
  },
  computed: {
    legendItems() {
      return META.map((m) => ({
        ...m,
        count: this.pendingRows.filter((r) => r.qcType === m.code).length,
      }));
    },
  },
};
</script>
