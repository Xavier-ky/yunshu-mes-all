<template>
  <div class="app-container">
    <el-form :inline="true" size="small" class="mb8">
      <el-form-item label="状态">
        <el-select v-model="queryStatus" clearable placeholder="全部" @change="load">
          <el-option label="待执行" value="CREATED" />
          <el-option label="已完成" value="FINISHED" />
        </el-select>
      </el-form-item>
      <el-form-item><el-button icon="Refresh" @click="load">刷新</el-button></el-form-item>
    </el-form>
    <el-table v-loading="loading" class="yunshu-data-table" stripe border :data="list">
      <el-table-column label="计划编号" prop="planCode" min-width="120" show-overflow-tooltip />
      <el-table-column label="设备编码" prop="deviceCode" min-width="120" show-overflow-tooltip />
      <el-table-column label="设备名称" prop="deviceName" min-width="140" show-overflow-tooltip />
      <el-table-column label="计划日期" prop="plannedDate" width="120" align="center" />
      <el-table-column label="状态" prop="status" width="100" align="center" />
      <el-table-column label="完成时间" prop="finishTime" width="170" align="center" />
      <el-table-column label="操作" align="center" class-name="col-actions" width="120">
        <template #default="scope">
          <div class="yunshu-row-actions">
            <el-button v-if="scope.row.status !== 'FINISHED'" type="primary" link @click="finish(scope.row)">完成</el-button>
          </div>
        </template>
      </el-table-column>
    </el-table>
  </div>
</template>

<script>
import { request } from "@/api/request";

export default {
  name: "EqMaintenanceTasks",
  data() {
    return { loading: false, list: [], queryStatus: "" };
  },
  created() {
    this.load();
  },
  methods: {
    async load() {
      this.loading = true;
      try {
        const params = this.queryStatus ? { status: this.queryStatus } : {};
        this.list = (await request.get("/equipment/maintenance-tasks", { params }))?.data || [];
      } finally {
        this.loading = false;
      }
    },
    async finish(row) {
      await this.$modal.confirm(`确认完成保养任务 ${row.planCode}？`);
      await request.put(`/equipment/maintenance-tasks/${row.maintenanceTaskId}/status`, { status: "FINISHED" });
      await this.load();
    },
  },
};
</script>
