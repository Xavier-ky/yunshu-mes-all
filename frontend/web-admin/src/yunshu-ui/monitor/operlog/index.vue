<template>
  <div class="app-container inbound-doc-panel sys-doc-panel sys-comms-doc-panel">
    <section class="inbound-filter-panel sys-comms-filter-panel" :class="{ 'sys-comms-filter-panel--compact': !showSearch }">
      <div class="sys-comms-filter__bar">
        <div class="sys-comms-filter__bar-main">
          <el-form v-show="showSearch" ref="queryForm" :model="queryParams" size="default" :inline="true" class="inbound-filter-form sys-comms-filter__fields" label-width="0" @submit.prevent>
            <el-form-item prop="title">
              <el-input v-model="queryParams.title" placeholder="系统模块" clearable @keyup.enter="handleQuery" />
            </el-form-item>
            <el-form-item prop="operName">
              <el-input v-model="queryParams.operName" placeholder="操作人员" clearable @keyup.enter="handleQuery" />
            </el-form-item>
            <el-form-item prop="businessType">
              <el-select v-model="queryParams.businessType" placeholder="操作类型" clearable>
                <el-option v-for="dict in dict.type.sys_oper_type" :key="dict.value" :label="dict.label" :value="dict.value" />
              </el-select>
            </el-form-item>
            <el-form-item prop="status">
              <el-select v-model="queryParams.status" placeholder="操作状态" clearable>
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
          <el-button type="danger" plain icon="el-icon-delete" size="default" :disabled="multiple" @click="handleDelete" v-hasPermi="['monitor:operlog:remove']">删除</el-button>
          <el-button type="danger" plain icon="el-icon-delete" size="default" @click="handleClean" v-hasPermi="['monitor:operlog:remove']">清空</el-button>
          <el-button type="warning" plain icon="el-icon-download" size="default" @click="handleExport" v-hasPermi="['monitor:operlog:export']">导出</el-button>
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
        <el-table-column label="日志编号" align="center" prop="operId" min-width="90" />
        <el-table-column label="系统模块" align="center" prop="title" min-width="120" show-overflow-tooltip />
        <el-table-column label="操作类型" align="center" prop="businessType" min-width="100">
          <template #default="scope">
            <dict-tag :options="dict.type.sys_oper_type" :value="scope.row.businessType" />
          </template>
        </el-table-column>
        <el-table-column label="请求方式" align="center" prop="requestMethod" min-width="90" />
        <el-table-column label="操作人员" align="center" prop="operName" min-width="100" show-overflow-tooltip sortable="custom" :sort-orders="['descending', 'ascending']" />
        <el-table-column label="操作地址" align="center" prop="operIp" min-width="120" show-overflow-tooltip />
        <el-table-column label="操作地点" align="center" prop="operLocation" min-width="120" show-overflow-tooltip />
        <el-table-column label="操作状态" align="center" prop="status" min-width="90">
          <template #default="scope">
            <dict-tag :options="dict.type.sys_common_status" :value="scope.row.status" />
          </template>
        </el-table-column>
        <el-table-column label="操作日期" align="center" prop="operTime" min-width="160" sortable="custom" :sort-orders="['descending', 'ascending']">
          <template #default="scope">
            <span>{{ parseTime(scope.row.operTime) }}</span>
          </template>
        </el-table-column>
        <el-table-column label="操作" align="center" min-width="90" class-name="col-actions">
          <template #default="scope">
            <div class="yunshu-row-actions">
              <el-button size="small" link icon="el-icon-view" @click="handleView(scope.row)" v-hasPermi="['monitor:operlog:query']">详细</el-button>
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

    <el-dialog title="操作日志详细" v-model="open" class="access-form-dialog" width="700px" append-to-body align-center destroy-on-close>
      <el-form ref="form" :model="form" label-width="100px" size="small" class="access-form-dialog__form">
        <el-row>
          <el-col :span="12">
            <el-form-item label="操作模块：">{{ form.title }} / {{ typeFormat(form) }}</el-form-item>
            <el-form-item label="登录信息：">{{ form.operName }} / {{ form.operIp }} / {{ form.operLocation }}</el-form-item>
          </el-col>
          <el-col :span="12">
            <el-form-item label="请求地址：">{{ form.operUrl }}</el-form-item>
            <el-form-item label="请求方式：">{{ form.requestMethod }}</el-form-item>
          </el-col>
          <el-col :span="24">
            <el-form-item label="操作方法：">{{ form.method }}</el-form-item>
          </el-col>
          <el-col :span="24">
            <el-form-item label="请求参数：">{{ form.operParam }}</el-form-item>
          </el-col>
          <el-col :span="24">
            <el-form-item label="返回参数：">{{ form.jsonResult }}</el-form-item>
          </el-col>
          <el-col :span="12">
            <el-form-item label="操作状态：">
              <div v-if="form.status === 0">正常</div>
              <div v-else-if="form.status === 1">失败</div>
            </el-form-item>
          </el-col>
          <el-col :span="12">
            <el-form-item label="操作时间：">{{ parseTime(form.operTime) }}</el-form-item>
          </el-col>
          <el-col :span="24">
            <el-form-item label="异常信息：" v-if="form.status === 1">{{ form.errorMsg }}</el-form-item>
          </el-col>
        </el-row>
      </el-form>
      <template #footer>
        <div class="dialog-footer">
          <el-button @click="open = false">关 闭</el-button>
        </div>
      </template>
    </el-dialog>
  </div>
</template>

<script>
import { list, delOperlog, cleanOperlog } from "@/yunshu-ui/api/monitor/operlog";

export default {
  name: "Operlog",
  dicts: ['sys_oper_type', 'sys_common_status'],
  data() {
    return {
      loading: true,
      ids: [],
      multiple: true,
      showSearch: true,
      total: 0,
      list: [],
      open: false,
      dateRange: [],
      defaultSort: { prop: 'operTime', order: 'descending' },
      form: {},
      queryParams: {
        pageNum: 1,
        pageSize: 10,
        title: undefined,
        operName: undefined,
        businessType: undefined,
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
    typeFormat(row) {
      return this.selectDictLabel(this.dict.type.sys_oper_type, row.businessType);
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
      this.ids = selection.map(item => item.operId);
      this.multiple = !selection.length;
    },
    handleSortChange(column) {
      this.queryParams.orderByColumn = column.prop;
      this.queryParams.isAsc = column.order;
      this.getList();
    },
    handleView(row) {
      this.open = true;
      this.form = row;
    },
    handleDelete(row) {
      const operIds = row.operId || this.ids;
      this.$modal.confirm('是否确认删除日志编号为"' + operIds + '"的数据项？').then(function() {
        return delOperlog(operIds);
      }).then(() => {
        this.getList();
        this.$modal.msgSuccess("删除成功");
      }).catch(() => {});
    },
    handleClean() {
      this.$modal.confirm('是否确认清空所有操作日志数据项？').then(function() {
        return cleanOperlog();
      }).then(() => {
        this.getList();
        this.$modal.msgSuccess("清空成功");
      }).catch(() => {});
    },
    handleExport() {
      this.download('monitor/operlog/export', {
        ...this.queryParams
      }, `operlog_${new Date().getTime()}.xlsx`);
    }
  }
};
</script>
