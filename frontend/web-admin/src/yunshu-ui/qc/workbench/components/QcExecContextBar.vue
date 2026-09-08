<template>
  <div v-if="row" class="qc-exec-context-bar">
    <div
      class="qc-exec-context-bar__task operator-task-bar"
      :class="`qc-exec-context-bar__task--${(row.qcType || '').toLowerCase()}`"
    >
      <div class="operator-task-bar__lead">
        <span class="operator-task-bar__code">{{ row.sourceDocCode || "—" }}</span>
        <span class="operator-task-bar__name">{{ row.itemName || "—" }}</span>
        <span v-if="row.itemCode" class="operator-task-bar__code2">{{ row.itemCode }}</span>
      </div>
      <div class="operator-task-bar__meta">
        <span v-for="item in metaItems" :key="item.label"><i>{{ item.label }}</i>{{ item.value }}</span>
        <span><i>待检</i>{{ row.quantityCheck ?? "—" }} {{ row.unitName || "" }}</span>
      </div>
      <div class="qc-exec-context-bar__type-tag">
        <dict-tag :options="dict.type.mes_qc_type" :value="row.qcType" />
      </div>
    </div>
    <div class="operator-action-row qc-exec-context-bar__actions">
      <el-button
        class="operator-ui-btn"
        type="primary"
        :auto-insert-space="false"
        @click="handleStartInspect"
      >
        <span class="operator-ui-btn__label">开始检验</span>
      </el-button>
      <el-button class="operator-ui-btn" :auto-insert-space="false" @click="$emit('records')">
        <span class="operator-ui-btn__label">检验记录</span>
      </el-button>
      <el-button class="operator-ui-btn" :auto-insert-space="false" @click="$emit('config')">
        <span class="operator-ui-btn__label">标准配置</span>
      </el-button>
      <el-button class="operator-ui-btn" type="warning" :auto-insert-space="false" @click="$emit('andon')">
        <span class="operator-ui-btn__label">发起安灯</span>
      </el-button>
    </div>
  </div>
</template>

<script>
import { isBladeInstallQcTask } from "../blade-install-qc-photos";

export default {
  name: "QcExecContextBar",
  dicts: ["mes_qc_type"],
  props: {
    row: { type: Object, default: null },
  },
  emits: ["inspect", "open-blade-photos", "records", "config", "andon"],
  computed: {
    isBladeInstall() {
      return isBladeInstallQcTask(this.row);
    },
    metaItems() {
      const r = this.row || {};
      const t = r.qcType;
      if (t === "IQC") {
        return [
          { label: "供应商", value: r.clientName || r.vendorName || "—" },
          { label: "批次", value: r.batchCode || "—" },
        ];
      }
      if (t === "PQC") {
        return [
          { label: "工单", value: r.workOrderCode || "—" },
          { label: "工序", value: r.taskCode || r.workstationName || "—" },
        ];
      }
      if (t === "OQC") {
        return [
          { label: "客户", value: r.clientName || "—" },
          { label: "批次", value: r.batchCode || "—" },
        ];
      }
      if (t === "RQC") {
        return [{ label: "来源单", value: r.sourceDocCode || "—" }];
      }
      return [];
    },
  },
  methods: {
    handleStartInspect() {
      if (this.isBladeInstall) {
        this.$emit("open-blade-photos");
      }
      this.$emit("inspect");
    },
  },
};
</script>
