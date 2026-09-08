<template>
  <div class="dv-analytics-panel-card dv-analytics-block">
    <div class="dv-analytics-block-head qc-analytics-bottom-panel__head">
      <span class="qc-analytics-bottom-panel__title">OEE Top5</span>
      <span class="qc-analytics-bottom-panel__meta">按 OEE 降序</span>
    </div>
    <div class="dv-analytics-panel-card__body">
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
          :data="topRows"
        >
          <el-table-column label="设备编码" prop="deviceCode" min-width="100" align="center" header-align="center" show-overflow-tooltip />
          <el-table-column label="设备名称" prop="deviceName" min-width="110" align="center" header-align="center" show-overflow-tooltip />
          <el-table-column label="OEE" width="72" align="center" header-align="center">
            <template #default="scope">
              <strong>{{ pct(scope.row.oeeRate) }}</strong>
            </template>
          </el-table-column>
          <el-table-column label="开动率" width="72" align="center" header-align="center">
            <template #default="scope">{{ pct(scope.row.availabilityRate) }}</template>
          </el-table-column>
        </el-table>
      </div>
    </div>
  </div>
</template>

<script>
export default {
  name: "DvOeeCompactPanel",
  props: {
    rows: { type: Array, default: () => [] },
    loading: { type: Boolean, default: false },
  },
  computed: {
    topRows() {
      return [...(this.rows || [])]
        .sort((a, b) => (Number(b.oeeRate) || 0) - (Number(a.oeeRate) || 0))
        .slice(0, 5);
    },
  },
  methods: {
    pct(v) {
      const n = Number(v);
      if (Number.isNaN(n)) return "—";
      return `${(n * 100).toFixed(1)}%`;
    },
  },
};
</script>
