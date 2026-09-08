<template>
  <div class="app-container inbound-doc-panel sys-doc-panel sys-comms-doc-panel">
    <section class="inbound-filter-panel sys-comms-filter-panel" :class="{ 'sys-comms-filter-panel--compact': !showSearch }">
      <div class="sys-comms-filter__bar">
        <div class="sys-comms-filter__bar-main">
          <el-form v-show="showSearch" ref="queryForm" :model="queryParams" size="default" :inline="true" class="inbound-filter-form sys-comms-filter__fields" label-width="0" @submit.prevent>
            <el-form-item prop="ipaddr">
              <el-input v-model="queryParams.ipaddr" placeholder="登录地址" clearable @keyup.enter="handleQuery" />
            </el-form-item>
            <el-form-item prop="userName">
              <el-input v-model="queryParams.userName" placeholder="用户名称" clearable @keyup.enter="handleQuery" />
            </el-form-item>
            <el-form-item prop="status">
              <el-select v-model="queryParams.status" placeholder="登录状态" clearable>
                <el-option v-for="dict in dict.type.sys_common_status" :key="dict.value" :label="dict.label" :value="dict.value" />
              </el-select>
            </el-form-item>
            <el-form-item class="filter-date-range">
              <el-date-picker
                v-model="dateRange"
                value-format="yyyy-MM-dd"
                type="daterange"
                range-separator="-"
                start-placeholder="开始日期"
                end-placeholder="结束日期"
              />
            </el-form-item>
            <el-form-item class="sys-comms-filter__query-actions">
              <el-button type="primary" icon="el-icon-search" size="default" @click="handleQuery">搜索</el-button>
              <el-button icon="el-icon-refresh" size="default" @click="resetQuery">重置</el-button>
            </el-form-item>
          </el-form>
          <div class="sys-comms-filter__bar-meta">
            <span class="sys-comms-filter__count">共 {{ total }} 条</span>
            <right-toolbar :show-search="showSearch" @update:showSearch="showSearch = $event" @queryTable="getList" />
          </div>
        </div>
        <div class="sys-comms-filter__bar-crud">
          <el-button type="danger" plain icon="el-icon-delete" size="default" :disabled="multiple" @click="handleDelete" v-hasPermi="['monitor:logininfor:remove']">删除</el-button>
          <el-button type="danger" plain icon="el-icon-delete" size="default" @click="handleClean" v-hasPermi="['monitor:logininfor:remove']">清空</el-button>
          <el-button type="warning" plain icon="el-icon-download" size="default" @click="handleExport" v-hasPermi="['monitor:logininfor:export']">导出</el-button>
        </div>
      </div>
    </section>

    <div class="inbound-table-frame">
      <span class="frame-corner frame-corner--tl" aria-hidden="true"></span>
      <span class="frame-corner frame-corner--tr" aria-hidden="true"></span>
      <span class="frame-corner frame-corner--bl" aria-hidden="true"></span>
      <span class="frame-corner frame-corner--br" aria-hidden="true"></span>
      <el-table
        ref="tables"
        v-loading="loading"
        class="yunshu-data-table sys-table inbound-table"
        stripe
        border
        height="100%"
        :data="list"
        :default-sort="defaultSort"
        @selection-change="handleSelectionChange"
        @sort-change="handleSortChange"
      >
        <el-table-column type="selection" width="42" align="center" />
        <el-table-column label="访问编号" align="center" prop="infoId" min-width="90" />
        <el-table-column label="用户名称" align="center" prop="userName" min-width="110" show-overflow-tooltip sortable="custom" :sort-orders="['descending', 'ascending']" />
        <el-table-column label="登录地址" align="center" prop="ipaddr" min-width="120" show-overflow-tooltip />
        <el-table-column label="登录地点" align="center" prop="loginLocation" min-width="120" show-overflow-tooltip />
        <el-table-column label="浏览器" align="center" prop="browser" min-width="100" show-overflow-tooltip />
        <el-table-column label="操作系统" align="center" prop="os" min-width="100" show-overflow-tooltip />
        <el-table-column label="登录状态" align="center" prop="status" min-width="90">
          <template #default="scope">
            <dict-tag :options="dict.type.sys_common_status" :value="scope.row.status" />
          </template>
        </el-table-column>
        <el-table-column label="操作信息" align="center" prop="msg" min-width="120" show-overflow-tooltip />
        <el-table-column label="登录日期" align="center" prop="loginTime" min-width="160" sortable="custom" :sort-orders="['descending', 'ascending']">
          <template #default="scope">
            <span>{{ parseTime(scope.row.loginTime) }}</span>
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
import { list, delLogininfor, cleanLogininfor } from "@/yunshu-ui/api/monitor/logininfor";

export default {
  name: "Logininfor",
  dicts: ['sys_common_status'],
  data() {
    return {
      loading: true,
      ids: [],
      multiple: true,
      showSearch: true,
      total: 0,
      list: [],
      dateRange: [],
      defaultSort: { prop: 'loginTime', order: 'descending' },
      queryParams: {
        pageNum: 1,
        pageSize: 10,
        ipaddr: undefined,
        userName: undefined,
        status: undefined
      }
    };
  },
  created() {
    this.getList();
  },
  methods: {
    getList() {
      this.loading = true;
      list(this.addDateRange(this.queryParams, this.dateRange)).then(response => {
        this.list = response.rows;
        this.total = response.total;
      }).finally(() => {
        this.loading = false;
      });
    },
    handleQuery() {
      this.queryParams.pageNum = 1;
      this.getList();
    },
    resetQuery() {
      this.dateRange = [];
      this.resetForm("queryForm");
      this.$refs.tables.sort(this.defaultSort.prop, this.defaultSort.order);
      this.handleQuery();
    },
    handleSelectionChange(selection) {
      this.ids = selection.map(item => item.infoId);
      this.multiple = !selection.length;
    },
    handleSortChange(column) {
      this.queryParams.orderByColumn = column.prop;
      this.queryParams.isAsc = column.order;
      this.getList();
    },
    handleDelete(row) {
      const infoIds = row.infoId || this.ids;
      this.$modal.confirm('是否确认删除访问编号为"' + infoIds + '"的数据项？').then(function() {
        return delLogininfor(infoIds);
      }).then(() => {
        this.getList();
        this.$modal.msgSuccess("删除成功");
      }).catch(() => {});
    },
    handleClean() {
      this.$modal.confirm('是否确认清空所有登录日志数据项？').then(function() {
        return cleanLogininfor();
      }).then(() => {
        this.getList();
        this.$modal.msgSuccess("清空成功");
      }).catch(() => {});
    },
    handleExport() {
      this.download('monitor/logininfor/export', {
        ...this.queryParams
      }, `logininfor_${new Date().getTime()}.xlsx`);
    }
  }
};
</script>
