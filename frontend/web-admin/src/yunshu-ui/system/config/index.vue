<template>
  <div class="app-container inbound-doc-panel sys-doc-panel sys-settings-doc-panel">
    <section v-show="showSearch" class="inbound-filter-panel">
      <el-form ref="queryForm" :model="queryParams" size="default" :inline="true" class="inbound-filter-form" label-width="0" @submit.prevent>
        <el-form-item prop="configName">
          <el-input v-model="queryParams.configName" placeholder="参数名称" clearable @keyup.enter="handleQuery" />
        </el-form-item>
        <el-form-item prop="configKey">
          <el-input v-model="queryParams.configKey" placeholder="参数键名" clearable @keyup.enter="handleQuery" />
        </el-form-item>
        <el-form-item prop="configType">
          <el-select v-model="queryParams.configType" placeholder="系统内置" clearable>
            <el-option v-for="dict in dict.type.sys_yes_no" :key="dict.value" :label="dict.label" :value="dict.value" />
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
        <el-form-item class="filter-actions">
          <el-button type="primary" icon="el-icon-search" size="default" @click="handleQuery">搜索</el-button>
          <el-button icon="el-icon-refresh" size="default" @click="resetQuery">重置</el-button>
        </el-form-item>
      </el-form>
    </section>

    <div class="inbound-toolbar">
      <div class="inbound-toolbar-left">
        <span class="inbound-page-title">系统参数</span>
        <el-button type="primary" plain icon="el-icon-plus" size="default" @click="handleAdd" v-hasPermi="['system:config:add']">新增</el-button>
        <el-button type="success" plain icon="el-icon-edit" size="default" :disabled="single" @click="handleUpdate" v-hasPermi="['system:config:edit']">修改</el-button>
        <el-button type="danger" plain icon="el-icon-delete" size="default" :disabled="multiple" @click="handleDelete" v-hasPermi="['system:config:remove']">删除</el-button>
        <el-button type="warning" plain icon="el-icon-download" size="default" @click="handleExport" v-hasPermi="['system:config:export']">导出</el-button>
        <el-button type="danger" plain icon="el-icon-refresh" size="default" @click="handleRefreshCache" v-hasPermi="['system:config:remove']">刷新缓存</el-button>
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
        :data="configList"
        @selection-change="handleSelectionChange"
      >
        <el-table-column type="selection" width="42" align="center" />
        <el-table-column label="参数主键" align="center" prop="configId" min-width="90" />
        <el-table-column label="参数名称" align="center" prop="configName" min-width="120" show-overflow-tooltip />
        <el-table-column label="参数键名" align="center" prop="configKey" min-width="140" show-overflow-tooltip />
        <el-table-column label="参数键值" align="center" prop="configValue" min-width="120" show-overflow-tooltip />
        <el-table-column label="系统内置" align="center" prop="configType" min-width="90">
          <template #default="scope">
            <dict-tag :options="dict.type.sys_yes_no" :value="scope.row.configType" />
          </template>
        </el-table-column>
        <el-table-column label="备注" align="center" prop="remark" min-width="120" show-overflow-tooltip />
        <el-table-column label="创建时间" align="center" prop="createTime" min-width="160">
          <template #default="scope">
            <span>{{ parseTime(scope.row.createTime) }}</span>
          </template>
        </el-table-column>
        <el-table-column label="操作" align="center" min-width="140" class-name="col-actions">
          <template #default="scope">
            <div class="yunshu-row-actions">
              <el-button size="small" link icon="el-icon-edit" @click="handleUpdate(scope.row)" v-hasPermi="['system:config:edit']">修改</el-button>
              <el-button size="small" link icon="el-icon-delete" @click="handleDelete(scope.row)" v-hasPermi="['system:config:remove']">删除</el-button>
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

    <el-dialog :title="title" v-model="open" class="access-form-dialog" width="560px" append-to-body align-center destroy-on-close>
      <el-form ref="form" :model="form" :rules="rules" label-width="96px" class="access-form-dialog__form">
        <el-form-item label="参数名称" prop="configName">
          <el-input v-model="form.configName" placeholder="请输入参数名称" />
        </el-form-item>
        <el-form-item label="参数键名" prop="configKey">
          <el-input v-model="form.configKey" placeholder="请输入参数键名" />
        </el-form-item>
        <el-form-item label="参数键值" prop="configValue">
          <el-input v-model="form.configValue" placeholder="请输入参数键值" />
        </el-form-item>
        <el-form-item label="系统内置" prop="configType">
          <el-radio-group v-model="form.configType">
            <el-radio v-for="dict in dict.type.sys_yes_no" :key="dict.value" :label="dict.value">{{ dict.label }}</el-radio>
          </el-radio-group>
        </el-form-item>
        <el-form-item label="备注" prop="remark">
          <el-input v-model="form.remark" type="textarea" :rows="3" placeholder="请输入内容" />
        </el-form-item>
      </el-form>
      <template #footer>
        <div class="dialog-footer">
          <el-button type="primary" @click="submitForm">确 定</el-button>
          <el-button @click="cancel">取 消</el-button>
        </div>
      </template>
    </el-dialog>
  </div>
</template>

<script>
import { listConfig, getConfig, delConfig, addConfig, updateConfig, refreshCache } from "@/yunshu-ui/api/system/config";

export default {
  name: "Config",
  dicts: ['sys_yes_no'],
  data() {
    return {
      loading: true,
      ids: [],
      single: true,
      multiple: true,
      showSearch: true,
      total: 0,
      configList: [],
      title: "",
      open: false,
      dateRange: [],
      queryParams: {
        pageNum: 1,
        pageSize: 10,
        configName: undefined,
        configKey: undefined,
        configType: undefined
      },
      form: {},
      rules: {
        configName: [{ required: true, message: "参数名称不能为空", trigger: "blur" }],
        configKey: [{ required: true, message: "参数键名不能为空", trigger: "blur" }],
        configValue: [{ required: true, message: "参数键值不能为空", trigger: "blur" }],
        remark: [{ max: 250, message: '长度必须小于250个字符', trigger: 'blur' }]
      }
    };
  },
  created() {
    this.getList();
  },
  methods: {
    getList() {
      this.loading = true;
      listConfig(this.addDateRange(this.queryParams, this.dateRange)).then(response => {
        this.configList = response.rows;
        this.total = response.total;
      }).finally(() => {
        this.loading = false;
      });
    },
    cancel() {
      this.open = false;
      this.reset();
    },
    reset() {
      this.form = {
        configId: undefined,
        configName: undefined,
        configKey: undefined,
        configValue: undefined,
        configType: "Y",
        remark: undefined
      };
      this.resetForm("form");
    },
    handleQuery() {
      this.queryParams.pageNum = 1;
      this.getList();
    },
    resetQuery() {
      this.dateRange = [];
      this.resetForm("queryForm");
      this.handleQuery();
    },
    handleAdd() {
      this.reset();
      this.open = true;
      this.title = "添加参数";
    },
    handleSelectionChange(selection) {
      this.ids = selection.map(item => item.configId);
      this.single = selection.length != 1;
      this.multiple = !selection.length;
    },
    handleUpdate(row) {
      this.reset();
      const configId = row.configId || this.ids;
      getConfig(configId).then(response => {
        this.form = response.data;
        this.open = true;
        this.title = "修改参数";
      });
    },
    submitForm() {
      this.$refs["form"].validate(valid => {
        if (!valid) return;
        const req = this.form.configId != undefined ? updateConfig(this.form) : addConfig(this.form);
        req.then(() => {
          this.$modal.msgSuccess(this.form.configId != undefined ? "修改成功" : "新增成功");
          this.open = false;
          this.getList();
        });
      });
    },
    handleDelete(row) {
      const configIds = row.configId || this.ids;
      this.$modal.confirm('是否确认删除参数编号为"' + configIds + '"的数据项？').then(function() {
        return delConfig(configIds);
      }).then(() => {
        this.getList();
        this.$modal.msgSuccess("删除成功");
      }).catch(() => {});
    },
    handleExport() {
      this.download('system/config/export', { ...this.queryParams }, `config_${new Date().getTime()}.xlsx`);
    },
    handleRefreshCache() {
      refreshCache().then(() => {
        this.$modal.msgSuccess("刷新成功");
      });
    }
  }
};
</script>
