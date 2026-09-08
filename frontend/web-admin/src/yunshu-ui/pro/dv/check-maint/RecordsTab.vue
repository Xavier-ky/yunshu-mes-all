<template>
  <div class="inbound-doc-panel dv-doc-panel">
    <section v-show="showSearch" class="inbound-filter-panel">
      <el-form :model="queryParams" ref="queryForm" size="small" :inline="true" class="inbound-filter-form" label-width="0" @submit.prevent>
        <el-form-item prop="machineryCode">
          <el-input v-model="queryParams.machineryCode" placeholder="设备编码" clearable @keyup.enter="handleQuery" />
        </el-form-item>
        <el-form-item prop="planCode">
          <el-input v-model="queryParams.planCode" placeholder="计划编码" clearable @keyup.enter="handleQuery" />
        </el-form-item>
        <el-form-item prop="status">
          <el-select v-model="queryParams.status" placeholder="状态" clearable>
            <el-option v-for="dict in dict.type.mes_order_status" :key="dict.value" :label="dict.label" :value="dict.value" />
          </el-select>
        </el-form-item>
        <el-form-item class="filter-actions">
          <el-button type="primary" icon="el-icon-search" size="default" @click="handleQuery">搜索</el-button>
          <el-button icon="el-icon-refresh" size="default" @click="resetQuery">重置</el-button>
        </el-form-item>
      </el-form>
    </section>

    <div class="inbound-toolbar">
      <div class="inbound-toolbar-left">
        <div class="dv-record-type-chips">
          <button type="button" :class="{ active: recordType === 'ALL' }" @click="setRecordType('ALL')">全部</button>
          <button type="button" :class="{ active: recordType === 'CHECK' }" @click="setRecordType('CHECK')">点检</button>
          <button type="button" :class="{ active: recordType === 'MAINTEN' }" @click="setRecordType('MAINTEN')">保养</button>
        </div>
        <span class="inbound-page-title">执行记录</span>
      </div>
      <div class="inbound-toolbar-meta">
        <span>共 {{ total }} 条</span>
      </div>
    </div>

    <div class="inbound-table-frame">
      <span class="frame-corner frame-corner--tl" aria-hidden="true"></span>
      <span class="frame-corner frame-corner--tr" aria-hidden="true"></span>
      <span class="frame-corner frame-corner--bl" aria-hidden="true"></span>
      <span class="frame-corner frame-corner--br" aria-hidden="true"></span>
      <el-table v-loading="loading" class="yunshu-data-table inbound-table" stripe border height="100%" :data="displayRows">
        <el-table-column label="类型" width="80" align="center" header-align="center">
          <template #default="scope">
            <el-tag size="small" :type="scope.row.recordKind === 'CHECK' ? 'primary' : 'success'">
              {{ scope.row.recordKind === "CHECK" ? "点检" : "保养" }}
            </el-tag>
          </template>
        </el-table-column>
        <el-table-column label="设备编码" prop="machineryCode" min-width="110" align="center" header-align="center" show-overflow-tooltip />
        <el-table-column label="设备名称" prop="machineryName" min-width="120" align="center" header-align="center" show-overflow-tooltip />
        <el-table-column label="计划编码" prop="planCode" min-width="120" align="center" header-align="center" show-overflow-tooltip />
        <el-table-column label="计划名称" prop="planName" min-width="140" align="center" header-align="center" show-overflow-tooltip />
        <el-table-column label="执行时间" width="160" align="center" header-align="center">
          <template #default="scope">
            {{ scope.row.taskTime || "—" }}
          </template>
        </el-table-column>
        <el-table-column label="状态" width="100" align="center" header-align="center">
          <template #default="scope">
            <dict-tag :options="dict.type.mes_order_status" :value="scope.row.status" />
          </template>
        </el-table-column>
        <el-table-column label="操作" width="100" align="center" header-align="center" class-name="col-actions">
          <template #default="scope">
            <div class="yunshu-row-actions">
              <el-button size="small" link @click="openInWorkbench(scope.row)">处理</el-button>
            </div>
          </template>
        </el-table-column>
      </el-table>
    </div>

    <pagination
      v-show="total > 0"
      :total="total"
      :page="queryParams.pageNum"
      @update:page="queryParams.pageNum = $event"
      :limit="queryParams.pageSize"
      @update:limit="queryParams.pageSize = $event"
      @pagination="getList"
    />
  </div>
</template>

<script>
import { listCheckrecord } from "@/yunshu-ui/api/mes/dv/checkrecord";
import { listMaintenrecord } from "@/yunshu-ui/api/mes/dv/maintenrecord";

const ALL_FETCH_SIZE = 100;

export default {
  name: "DvRecordsTab",
  dicts: ["mes_order_status"],
  props: {
    initialType: { type: String, default: "ALL" },
  },
  data() {
    return {
      loading: false,
      showSearch: true,
      recordType: "ALL",
      rows: [],
      allRows: [],
      total: 0,
      queryParams: {
        pageNum: 1,
        pageSize: 10,
        machineryCode: null,
        planCode: null,
        status: null,
      },
    };
  },
  computed: {
    displayRows() {
      if (this.recordType === "ALL") {
        const start = (this.queryParams.pageNum - 1) * this.queryParams.pageSize;
        return this.allRows.slice(start, start + this.queryParams.pageSize);
      }
      return this.rows;
    },
  },
  mounted() {
    this.recordType = this.initialType || "ALL";
    this.getList();
  },
  watch: {
    initialType(v) {
      if (v && v !== this.recordType) {
        this.recordType = v;
        this.queryParams.pageNum = 1;
        this.getList();
      }
    },
  },
  methods: {
    setRecordType(type) {
      if (this.recordType === type) return;
      this.recordType = type;
      this.queryParams.pageNum = 1;
      this.getList();
    },
    handleQuery() {
      this.queryParams.pageNum = 1;
      this.getList();
    },
    resetQuery() {
      this.resetForm("queryForm");
      this.handleQuery();
    },
    buildFilterParams(extra = {}) {
      const params = { ...extra };
      if (this.queryParams.machineryCode) params.machineryCode = this.queryParams.machineryCode;
      if (this.queryParams.planCode) params.planCode = this.queryParams.planCode;
      if (this.queryParams.status) params.status = this.queryParams.status;
      return params;
    },
    mapCheckRows(list) {
      return (list || []).map((r) => ({
        ...r,
        recordKind: "CHECK",
        taskId: r.recordId,
        taskTime: r.checkTime,
      }));
    },
    mapMaintenRows(list) {
      return (list || []).map((r) => ({
        ...r,
        recordKind: "MAINTEN",
        taskId: r.recordId,
        taskTime: r.maintenTime,
      }));
    },
    async getList() {
      this.loading = true;
      try {
        if (this.recordType === "CHECK") {
          const res = await listCheckrecord(
            this.buildFilterParams({
              pageNum: this.queryParams.pageNum,
              pageSize: this.queryParams.pageSize,
            })
          );
          this.rows = this.mapCheckRows(res?.rows);
          this.allRows = [];
          this.total = res?.total || 0;
        } else if (this.recordType === "MAINTEN") {
          const res = await listMaintenrecord(
            this.buildFilterParams({
              pageNum: this.queryParams.pageNum,
              pageSize: this.queryParams.pageSize,
            })
          );
          this.rows = this.mapMaintenRows(res?.rows);
          this.allRows = [];
          this.total = res?.total || 0;
        } else {
          const filter = this.buildFilterParams({ pageNum: 1, pageSize: ALL_FETCH_SIZE });
          const [checkRes, maintenRes] = await Promise.all([
            listCheckrecord(filter),
            listMaintenrecord(filter),
          ]);
          this.allRows = [...this.mapCheckRows(checkRes?.rows), ...this.mapMaintenRows(maintenRes?.rows)].sort((a, b) =>
            String(b.taskTime || "").localeCompare(String(a.taskTime || ""))
          );
          this.rows = [];
          this.total = this.allRows.length;
        }
      } catch {
        this.rows = [];
        this.allRows = [];
        this.total = 0;
      } finally {
        this.loading = false;
      }
    },
    openInWorkbench(row) {
      const routeName = row.recordKind === "CHECK" ? "dv-wb-check" : "dv-wb-mainten";
      this.$router.push({
        name: routeName,
        query: {
          taskType: row.recordKind,
          taskId: String(row.taskId),
          machineryCode: row.machineryCode,
          machineryName: row.machineryName,
          planCode: row.planCode,
          planName: row.planName,
          status: row.status,
        },
      });
    },
  },
};
</script>
