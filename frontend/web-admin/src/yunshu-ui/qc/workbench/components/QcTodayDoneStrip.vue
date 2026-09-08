<template>
  <div class="qc-workbench-today qc-analytics-bottom-panel qc-analytics-bottom-panel--finished">
    <div class="qc-analytics-bottom-panel__head">
      <span class="qc-analytics-bottom-panel__title">今日已检</span>
      <span class="qc-analytics-bottom-panel__meta">{{ metaLabel }}</span>
    </div>
    <div class="inbound-table-frame qc-analytics-table-frame qc-workbench-today__frame">
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
        :data="displayRows"
        :row-class-name="rowClassName"
        @row-click="onRowClick"
      >
        <el-table-column label="检验单号" align="center" header-align="center" min-width="128" prop="docCode" show-overflow-tooltip>
          <template #default="scope">
            <span class="doc-link-btn">{{ scope.row.docCode || "—" }}</span>
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
const RECORDS_TAB = {
  IQC: "iqc",
  PQC: "ipqc",
  OQC: "oqc",
  RQC: "rqc",
};

export default {
  name: "QcTodayDoneStrip",
  dicts: ["mes_qc_type", "mes_qc_result"],
  props: {
    rows: { type: Array, default: () => [] },
    loading: { type: Boolean, default: false },
  },
  emits: ["open-records"],
  computed: {
    todayStr() {
      return new Date().toISOString().slice(0, 10);
    },
    todayRows() {
      return (this.rows || []).filter((r) => String(r.inspectDate || "").startsWith(this.todayStr));
    },
    displayRows() {
      const today = this.todayRows;
      if (today.length) return today.slice(0, 6);
      return (this.rows || []).slice(0, 6);
    },
    metaLabel() {
      const today = this.todayRows.length;
      if (today > 0) return `共 ${today} 条（今日）`;
      return `共 ${this.displayRows.length} 条（最近完成）`;
    },
  },
  methods: {
    rowClassName({ row }) {
      return row.checkResult === "REJECT" ? "row-reject" : "";
    },
    onRowClick(row) {
      const tab = RECORDS_TAB[row?.qcType] || "iqc";
      this.$emit("open-records", tab);
    },
  },
};
</script>
