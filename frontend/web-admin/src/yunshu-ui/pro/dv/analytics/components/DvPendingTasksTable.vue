<template>
  <div class="qc-analytics-bottom-panel qc-analytics-bottom-panel--disposition dv-analytics-block">
    <div class="qc-analytics-bottom-panel__head dv-analytics-block-head">
      <span class="qc-analytics-bottom-panel__title">待处理工单</span>
      <span class="qc-analytics-bottom-panel__meta">共 {{ rows.length }} 条</span>
    </div>
    <div class="inbound-table-frame qc-analytics-table-frame">
      <span class="frame-corner frame-corner--tl" aria-hidden="true" />
      <span class="frame-corner frame-corner--tr" aria-hidden="true" />
      <span class="frame-corner frame-corner--bl" aria-hidden="true" />
      <span class="frame-corner frame-corner--br" aria-hidden="true" />
      <el-table
        v-loading="loading"
        class="yunshu-data-table inbound-table qc-analytics-table"
        stripe
        border
        height="100%"
        :data="rows"
      >
        <el-table-column label="类型" width="76" align="center" header-align="center">
          <template #default="scope">
            <el-tag size="small" :type="tagType(scope.row.taskType)">{{ taskLabel(scope.row.taskType) }}</el-tag>
          </template>
        </el-table-column>
        <el-table-column label="设备编码" prop="machineryCode" min-width="100" align="center" header-align="center" show-overflow-tooltip />
        <el-table-column label="设备名称" prop="machineryName" min-width="110" align="center" header-align="center" show-overflow-tooltip />
        <el-table-column label="计划/单号" min-width="110" align="center" header-align="center" show-overflow-tooltip>
          <template #default="scope">
            {{ scope.row.planCode || scope.row.planName || "—" }}
          </template>
        </el-table-column>
        <el-table-column label="操作" width="88" align="center" header-align="center" class-name="col-actions">
          <template #default="scope">
            <div class="yunshu-row-actions">
              <el-button type="primary" link @click="$emit('inspect', scope.row)">处理</el-button>
            </div>
          </template>
        </el-table-column>
      </el-table>
    </div>
  </div>
</template>

<script>
const LABEL = { CHECK: "点检", MAINTEN: "保养", REPAIR: "维修" };
const TAG = { CHECK: "primary", MAINTEN: "success", REPAIR: "warning" };

export default {
  name: "DvPendingTasksTable",
  props: {
    rows: { type: Array, default: () => [] },
    loading: { type: Boolean, default: false },
  },
  emits: ["inspect"],
  methods: {
    taskLabel(t) {
      return LABEL[t] || t || "—";
    },
    tagType(t) {
      return TAG[t] || "info";
    },
  },
};
</script>
