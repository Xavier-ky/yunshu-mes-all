<template>
  <div class="app-container inbound-doc-panel">
    <section v-show="showSearch" class="inbound-filter-panel">
      <el-form :model="queryParams" ref="queryForm" size="default" :inline="true" class="inbound-filter-form" label-width="0" @submit.prevent>
        <el-form-item prop="syncType">
          <el-input v-model="queryParams.syncType" placeholder="同步类型" clearable @keyup.enter="handleQuery" />
        </el-form-item>
        <el-form-item prop="bizNo">
          <el-input v-model="queryParams.bizNo" placeholder="业务单号" clearable @keyup.enter="handleQuery" />
        </el-form-item>
        <el-form-item class="filter-actions">
          <el-button type="primary" icon="el-icon-search" size="default" @click="handleQuery">搜索</el-button>
          <el-button icon="el-icon-refresh" size="default" @click="resetQuery">重置</el-button>
        </el-form-item>
      </el-form>
    </section>

    <div class="inbound-toolbar">
      <div class="inbound-toolbar-left">
        <span class="inbound-page-title">同步日志</span>
      </div>
      <div class="inbound-toolbar-meta">
        <span>共 {{ total }} 条</span>
        <right-toolbar :show-search="showSearch" @update:showSearch="showSearch = $event" @queryTable="getList" />
      </div>
    </div>

    <div class="inbound-table-frame">
      <span class="frame-corner frame-corner--tl" aria-hidden="true"></span>
      <span class="frame-corner frame-corner--tr" aria-hidden="true"></span>
      <span class="frame-corner frame-corner--bl" aria-hidden="true"></span>
      <span class="frame-corner frame-corner--br" aria-hidden="true"></span>
      <el-table class="yunshu-data-table inbound-table" stripe border height="100%" v-loading="loading" :data="list">
        <el-table-column label="同步类型" prop="syncType" align="center" header-align="center" min-width="120" show-overflow-tooltip />
        <el-table-column label="业务单号" prop="syncKey" align="center" header-align="center" min-width="160" show-overflow-tooltip />
        <el-table-column label="结果" prop="syncStatus" align="center" header-align="center" width="100" />
        <el-table-column label="同步时间" prop="syncTime" align="center" header-align="center" min-width="160" show-overflow-tooltip />
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
import { listSyncLogs } from "@/yunshu-ui/api/integration/syncLogs";

export default {
  name: "IntegrationSyncLogs",
  props: {
    embedMode: { type: Boolean, default: false },
  },
  data() {
    return {
      loading: true,
      showSearch: true,
      total: 0,
      list: [],
      queryParams: { pageNum: 1, pageSize: 10, syncType: undefined, bizNo: undefined },
    };
  },
  created() {
    this.getList();
  },
  methods: {
    getList() {
      this.loading = true;
      listSyncLogs(this.queryParams)
        .then((response) => {
          this.list = response.rows || [];
          this.total = response.total || 0;
        })
        .finally(() => {
          this.loading = false;
        });
    },
    handleQuery() {
      this.queryParams.pageNum = 1;
      this.getList();
    },
    resetQuery() {
      this.resetForm("queryForm");
      this.handleQuery();
    },
  },
};
</script>
