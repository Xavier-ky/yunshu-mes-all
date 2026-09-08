<template>
  <div v-if="row" class="qc-exec-context-bar dv-exec-context-bar">
    <div class="qc-exec-context-bar__task operator-task-bar" :class="`dv-exec-context-bar__task--${(row.taskType || '').toLowerCase()}`">
      <div class="operator-task-bar__lead">
        <span class="operator-task-bar__code">{{ row.machineryCode || "—" }}</span>
        <span class="operator-task-bar__name">{{ row.machineryName || "—" }}</span>
      </div>
      <div class="operator-task-bar__meta">
        <span><i>类型</i>{{ taskLabel }}</span>
        <span><i>单号</i>{{ row.planCode || "—" }}</span>
        <span><i>名称</i>{{ row.planName || "—" }}</span>
        <span v-if="row.stationName"><i>工位</i>{{ row.stationName }}</span>
        <span v-if="row.workOrderNo"><i>工单</i>{{ row.workOrderNo }}</span>
        <span v-if="row.lineName"><i>产线</i>{{ row.lineName }}</span>
      </div>
      <div class="qc-exec-context-bar__type-tag">
        <el-tag size="small" :type="tagType">{{ taskLabel }}</el-tag>
      </div>
    </div>
    <div class="operator-action-row qc-exec-context-bar__actions">
      <el-button class="operator-ui-btn" type="primary" :auto-insert-space="false" @click="$emit('inspect')">
        <span class="operator-ui-btn__label">开始处理</span>
      </el-button>
      <el-button class="operator-ui-btn" :auto-insert-space="false" @click="$emit('machinery')">
        <span class="operator-ui-btn__label">设备台账</span>
      </el-button>
      <el-button class="operator-ui-btn" :auto-insert-space="false" @click="$emit('records')">
        <span class="operator-ui-btn__label">执行记录</span>
      </el-button>
      <el-button class="operator-ui-btn" type="warning" :auto-insert-space="false" @click="$emit('andon')">
        <span class="operator-ui-btn__label">发起安灯</span>
      </el-button>
    </div>
  </div>
</template>

<script>
const LABEL = { CHECK: "点检", MAINTEN: "保养", REPAIR: "维修" };
const TAG = { CHECK: "primary", MAINTEN: "success", REPAIR: "warning" };

export default {
  name: "DvExecContextBar",
  props: { row: { type: Object, default: null } },
  emits: ["inspect", "machinery", "records", "andon"],
  computed: {
    taskLabel() {
      return LABEL[this.row?.taskType] || this.row?.taskType || "—";
    },
    tagType() {
      return TAG[this.row?.taskType] || "info";
    },
  },
};
</script>
