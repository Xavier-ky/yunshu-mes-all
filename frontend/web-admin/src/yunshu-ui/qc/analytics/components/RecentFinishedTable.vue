<template>
  <div class="qc-analytics-bottom-panel qc-analytics-bottom-panel--finished">
    <div class="qc-analytics-bottom-panel__head">
      <span class="qc-analytics-bottom-panel__title">最近完成检验</span>
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
        :row-class-name="rowClassName"
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
        <el-table-column label="结论" align="center" header-align="center" width="88" prop="checkResult">
          <template #default="scope">
            <dict-tag :options="dict.type.mes_qc_result" :value="scope.row.checkResult" />
          </template>
        </el-table-column>
        <el-table-column label="检验时间" align="center" header-align="center" width="112" prop="inspectDate" show-overflow-tooltip />
      </el-table>
    </div>
  </div>
</template>

<script>
export default {
  name: "RecentFinishedTable",
  dicts: ["mes_qc_type", "mes_qc_result"],
  props: {
    rows: { type: Array, default: () => [] },
  },
  methods: {
    rowClassName({ row }) {
      return row.checkResult === "REJECT" ? "row-reject" : "";
    },
  },
};
</script>
