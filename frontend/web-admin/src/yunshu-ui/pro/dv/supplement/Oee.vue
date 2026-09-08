<template>
  <div :class="embedded ? 'dv-oee-embedded' : 'app-container'">
    <el-form v-if="!embedded" :inline="true" size="small" class="mb8">
      <el-form-item label="统计日期">
        <el-date-picker v-model="statDate" type="date" value-format="YYYY-MM-DD" clearable @change="load" />
      </el-form-item>
      <el-form-item><el-button icon="Refresh" @click="load">刷新</el-button></el-form-item>
    </el-form>
    <el-table v-loading="loading" class="yunshu-data-table" stripe border :data="list" :height="embedded ? '100%' : undefined">
      <el-table-column label="设备编码" prop="deviceCode" min-width="120" show-overflow-tooltip />
      <el-table-column label="设备名称" prop="deviceName" min-width="140" show-overflow-tooltip />
      <el-table-column v-if="!embedded" label="日期" prop="statDate" width="120" align="center" />
      <el-table-column v-if="!embedded" label="计划(min)" prop="plannedTimeMin" width="100" align="center" />
      <el-table-column v-if="!embedded" label="运行(min)" prop="runTimeMin" width="100" align="center" />
      <el-table-column v-if="!embedded" label="停机(min)" prop="stopTimeMin" width="100" align="center" />
      <el-table-column v-if="!embedded" label="产量" prop="outputQty" width="90" align="center" />
      <el-table-column v-if="!embedded" label="良品" prop="goodQty" width="90" align="center" />
      <el-table-column label="开动率" width="100" align="center">
        <template #default="scope">{{ pct(scope.row.availabilityRate) }}</template>
      </el-table-column>
      <el-table-column v-if="!embedded" label="性能率" width="100" align="center">
        <template #default="scope">{{ pct(scope.row.performanceRate) }}</template>
      </el-table-column>
      <el-table-column v-if="!embedded" label="合格率" width="100" align="center">
        <template #default="scope">{{ pct(scope.row.qualityRate) }}</template>
      </el-table-column>
      <el-table-column label="OEE" width="100" align="center">
        <template #default="scope"><strong>{{ pct(scope.row.oeeRate) }}</strong></template>
      </el-table-column>
    </el-table>
  </div>
</template>

<script>
import { request } from "@/api/request";

export default {
  name: "EqOee",
  props: {
    embedded: { type: Boolean, default: false },
  },
  data() {
    return { loading: false, list: [], statDate: "" };
  },
  created() {
    this.load();
  },
  methods: {
    pct(v) {
      const n = Number(v);
      if (Number.isNaN(n)) return "—";
      return `${(n * 100).toFixed(1)}%`;
    },
    async load() {
      this.loading = true;
      try {
        const params = this.statDate ? { statDate: this.statDate } : {};
        this.list = (await request.get("/equipment/oee", { params }))?.data || [];
      } finally {
        this.loading = false;
      }
    },
  },
};
</script>
