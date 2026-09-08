<template>
  <div :class="rootClass">
    <section v-show="showSearch" class="inbound-filter-panel integration-endpoints-filter">
      <div class="integration-endpoints-filter__row">
        <el-form ref="queryForm" :model="queryParams" size="default" :inline="true" class="inbound-filter-form" label-width="0" @submit.prevent>
          <el-form-item prop="apiPath">
            <el-input v-model="queryParams.apiPath" placeholder="接口路径" clearable @keyup.enter="handleQuery" />
          </el-form-item>
          <el-form-item class="filter-actions">
            <el-button type="primary" icon="el-icon-search" size="default" @click="handleQuery">搜索</el-button>
            <el-button icon="el-icon-refresh" size="default" @click="resetQuery">重置</el-button>
          </el-form-item>
        </el-form>
        <span v-if="embedMode" class="integration-endpoints-filter__count">共 {{ total }} 条</span>
      </div>
    </section>

    <div v-if="!embedMode" class="inbound-toolbar">
      <div class="inbound-toolbar-left">
        <span class="inbound-page-title">接口定义管理</span>
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
      <el-table
        v-loading="loading"
        class="yunshu-data-table sys-table inbound-table"
        stripe
        border
        height="100%"
        :data="list"
      >
        <el-table-column label="接口编码" prop="endpointCode" align="center" header-align="center" min-width="120" show-overflow-tooltip />
        <el-table-column label="接口名称" prop="endpointName" align="center" header-align="center" min-width="140" show-overflow-tooltip />
        <el-table-column label="接口路径" prop="endpointPath" align="center" header-align="center" min-width="180" show-overflow-tooltip>
          <template #default="scope">
            <span class="integration-endpoint-path">{{ scope.row.endpointPath }}</span>
          </template>
        </el-table-column>
        <el-table-column label="方法" prop="httpMethod" align="center" header-align="center" width="90">
          <template #default="scope">
            <el-tag size="small" :type="methodTagType(scope.row.httpMethod)">{{ scope.row.httpMethod || "-" }}</el-tag>
          </template>
        </el-table-column>
        <el-table-column label="方向" prop="direction" align="center" header-align="center" width="100" show-overflow-tooltip />
        <el-table-column label="状态" prop="status" align="center" header-align="center" width="100">
          <template #default="scope">
            <el-tag size="small" :type="statusTagType(scope.row.status)">{{ statusLabel(scope.row.status) }}</el-tag>
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
import { listEndpoints } from "@/yunshu-ui/api/integration/endpoints";

export default {
  name: "IntegrationEndpoints",
  props: {
    embedMode: { type: Boolean, default: false },
    systemId: { type: [String, Number], default: null },
  },
  computed: {
    rootClass() {
      return this.embedMode
        ? "app-container sys-doc-panel sys-doc-panel--embed integration-endpoints-embed"
        : "app-container inbound-doc-panel";
    },
  },
  data() {
    return {
      loading: true,
      showSearch: true,
      total: 0,
      list: [],
      queryParams: { pageNum: 1, pageSize: 10, apiPath: undefined, externalSystemId: undefined },
    };
  },
  watch: {
    systemId: {
      immediate: true,
      handler(id) {
        this.queryParams.externalSystemId = id || undefined;
        this.queryParams.pageNum = 1;
        this.getList();
      },
    },
  },
  methods: {
    getList() {
      this.loading = true;
      listEndpoints(this.queryParams)
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
      this.queryParams.apiPath = undefined;
      this.handleQuery();
    },
    methodTagType(method) {
      const m = String(method || "").toUpperCase();
      if (m === "GET") return "success";
      if (m === "POST") return "";
      if (m === "PUT") return "warning";
      if (m === "DELETE") return "danger";
      return "info";
    },
    statusTagType(status) {
      const s = String(status || "").toUpperCase();
      if (s === "ENABLED" || s === "ACTIVE" || s === "1") return "success";
      if (s === "DISABLED" || s === "INACTIVE" || s === "0") return "info";
      return "";
    },
    statusLabel(status) {
      const s = String(status || "").toUpperCase();
      if (s === "ENABLED" || s === "ACTIVE" || s === "1") return "启用";
      if (s === "DISABLED" || s === "INACTIVE" || s === "0") return "停用";
      return status || "-";
    },
  },
};
</script>
