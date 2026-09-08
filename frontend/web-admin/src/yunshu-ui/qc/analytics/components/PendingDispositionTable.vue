<template>
  <div class="qc-analytics-bottom-panel qc-analytics-bottom-panel--disposition">
    <div class="qc-analytics-bottom-panel__head">
      <span class="qc-analytics-bottom-panel__title">待处置检验单</span>
      <span class="qc-analytics-bottom-panel__meta">共 {{ rows.length }} 条</span>
    </div>
    <div class="inbound-table-frame qc-analytics-table-frame qc-analytics-bottom-scroll">
      <span class="frame-corner frame-corner--tl" aria-hidden="true" />
      <span class="frame-corner frame-corner--tr" aria-hidden="true" />
      <span class="frame-corner frame-corner--bl" aria-hidden="true" />
      <span class="frame-corner frame-corner--br" aria-hidden="true" />
      <el-table
        v-loading="false"
        class="yunshu-data-table inbound-table qc-analytics-table"
        stripe
        border
        height="100%"
        :data="rows"
      >
        <el-table-column label="检验单号" align="center" header-align="center" min-width="128" prop="docCode" show-overflow-tooltip>
          <template #default="scope">
            <span class="doc-link-btn doc-link-btn--static">{{ scope.row.docCode || "—" }}</span>
          </template>
        </el-table-column>
        <el-table-column label="类型" align="center" header-align="center" width="76" prop="qcType">
          <template #default="scope">
            <dict-tag :options="dict.type.mes_qc_type" :value="scope.row.qcType" />
          </template>
        </el-table-column>
        <el-table-column label="物资" align="center" header-align="center" min-width="108" prop="itemName" show-overflow-tooltip />
        <el-table-column label="状态" align="center" header-align="center" width="96" prop="status">
          <template #default="scope">
            <dict-tag :options="dict.type.mes_order_status" :value="scope.row.status" />
          </template>
        </el-table-column>
        <el-table-column label="操作" align="center" header-align="center" width="88" class-name="col-actions">
          <template #default="scope">
            <div class="yunshu-row-actions">
              <el-button type="primary" link @click="$emit('inspect', scope.row)">去检验</el-button>
            </div>
          </template>
        </el-table-column>
      </el-table>
    </div>
  </div>
</template>

<script>
export default {
  name: "PendingDispositionTable",
  dicts: ["mes_qc_type", "mes_order_status"],
  props: {
    rows: { type: Array, default: () => [] },
  },
  emits: ["inspect"],
};
</script>
