<template>
  <div class="andon-panel">
    <div class="andon-panel-toolbar">
      <el-button icon="Refresh" @click="load">刷新</el-button>
    </div>
    <el-table v-loading="loading" class="yunshu-data-table" stripe border :data="tasks">
      <el-table-column label="安灯编号" prop="andon_no" min-width="120" show-overflow-tooltip />
      <el-table-column label="异常描述" prop="exception_desc" min-width="180" show-overflow-tooltip />
      <el-table-column label="处理人" prop="handler_name" min-width="100" show-overflow-tooltip />
      <el-table-column label="状态" width="110" align="center">
        <template #default="s"><el-tag size="small" :type="s.row.status === 'ASSIGNED' ? 'warning' : 'success'">{{ s.row.status }}</el-tag></template>
      </el-table-column>
      <el-table-column label="分派时间" width="160" align="center">
        <template #default="s">{{ fmt(s.row.assign_time) }}</template>
      </el-table-column>
      <el-table-column label="操作" width="90" align="center" class-name="col-actions">
        <template #default="s">
          <div class="yunshu-row-actions">
            <el-button v-if="s.row.status === 'ASSIGNED'" type="primary" link @click="closeTask(s.row)">完成</el-button>
          </div>
        </template>
      </el-table-column>
    </el-table>
    <el-empty v-if="!loading && !tasks.length" description="暂无处理任务" />
  </div>
</template>

<script>
import { request } from "@/api/request";

export default {
  name: "AndonTaskPanel",
  data() {
    return { loading: false, tasks: [] };
  },
  methods: {
    fmt(v) { return v ? String(v).substring(0, 16) : "—"; },
    async load() {
      this.loading = true;
      try { this.tasks = (await request.get("/andon/tasks"))?.data || []; }
      finally { this.loading = false; }
    },
    async closeTask(t) {
      const andonId = t.andon_id || t.andonId;
      await request.post(`/andon/events/${andonId}/result`, { handleMeasure: "已处理", resultDesc: "任务完成", status: "CLOSED" });
      this.$modal.msgSuccess("任务已完成");
      await this.load();
    },
  },
};
</script>

<style scoped>
.andon-panel { display: flex; flex-direction: column; gap: 12px; min-height: 55vh; }
.andon-panel-toolbar { display: flex; gap: 8px; }
</style>
