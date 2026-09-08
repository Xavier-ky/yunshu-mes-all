<template>
  <div class="app-container inbound-doc-panel">
    <section v-show="showSearch" class="inbound-filter-panel">
      <el-form :model="queryParams" ref="queryForm" size="default" :inline="true" class="inbound-filter-form" label-width="0" @submit.prevent>
        <el-form-item prop="systemCode">
          <el-input v-model="queryParams.systemCode" placeholder="系统编码" clearable @keyup.enter="handleQuery" />
        </el-form-item>
        <el-form-item prop="systemName">
          <el-input v-model="queryParams.systemName" placeholder="系统名称" clearable @keyup.enter="handleQuery" />
        </el-form-item>
        <el-form-item class="filter-actions">
          <el-button type="primary" icon="el-icon-search" size="default" @click="handleQuery">搜索</el-button>
          <el-button icon="el-icon-refresh" size="default" @click="resetQuery">重置</el-button>
        </el-form-item>
      </el-form>
    </section>

    <div class="inbound-toolbar">
      <div class="inbound-toolbar-left">
        <span class="inbound-page-title">外部系统</span>
        <el-button type="primary" plain icon="el-icon-plus" size="default" @click="handleAdd">新增</el-button>
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
      <el-table class="yunshu-data-table inbound-table" stripe border height="100%" v-loading="loading" :data="list" @selection-change="handleSelectionChange">
        <el-table-column type="selection" width="42" align="center" header-align="center" />
        <el-table-column label="系统编码" prop="systemCode" align="center" header-align="center" min-width="120" show-overflow-tooltip />
        <el-table-column label="系统名称" prop="systemName" align="center" header-align="center" min-width="140" show-overflow-tooltip />
        <el-table-column label="类型" prop="systemType" align="center" header-align="center" width="100" />
        <el-table-column label="状态" prop="status" align="center" header-align="center" width="100" />
        <el-table-column label="操作" align="center" header-align="center" class-name="col-actions small-padding fixed-width" width="160">
          <template #default="scope">
            <div class="yunshu-row-actions">
              <el-button size="small" link icon="el-icon-edit" @click="handleUpdate(scope.row)">修改</el-button>
              <el-button size="small" link icon="el-icon-delete" @click="handleDelete(scope.row)">删除</el-button>
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

    <el-dialog :title="title" v-model="open" width="520px" append-to-body>
      <el-form ref="form" :model="form" :rules="rules" label-width="90px">
        <el-form-item label="系统编码" prop="systemCode">
          <el-input v-model="form.systemCode" placeholder="如 SAP_ERP" />
        </el-form-item>
        <el-form-item label="系统名称" prop="systemName">
          <el-input v-model="form.systemName" placeholder="系统名称" />
        </el-form-item>
        <el-form-item label="系统类型" prop="systemType">
          <el-input v-model="form.systemType" placeholder="ERP / WMS / IOT" />
        </el-form-item>
        <el-form-item label="状态" prop="status">
          <el-select v-model="form.status" placeholder="状态">
            <el-option label="启用" value="ENABLED" />
            <el-option label="停用" value="DISABLED" />
          </el-select>
        </el-form-item>
      </el-form>
      <template #footer>
        <el-button type="primary" @click="submitForm">确 定</el-button>
        <el-button @click="cancel">取 消</el-button>
      </template>
    </el-dialog>
  </div>
</template>

<script>
import { listSystems, getSystem, addSystem, updateSystem, delSystem } from "@/yunshu-ui/api/integration/systems";

export default {
  name: "IntegrationSystems",
  props: {
    embedMode: { type: Boolean, default: false },
  },
  data() {
    return {
      loading: true,
      ids: [],
      single: true,
      multiple: true,
      showSearch: true,
      total: 0,
      list: [],
      title: "",
      open: false,
      queryParams: { pageNum: 1, pageSize: 10, systemCode: undefined, systemName: undefined },
      form: {},
      rules: {
        systemCode: [{ required: true, message: "系统编码不能为空", trigger: "blur" }],
        systemName: [{ required: true, message: "系统名称不能为空", trigger: "blur" }],
        systemType: [{ required: true, message: "系统类型不能为空", trigger: "blur" }],
      },
    };
  },
  created() {
    this.getList();
  },
  methods: {
    getList() {
      this.loading = true;
      listSystems(this.queryParams)
        .then((response) => {
          this.list = response.rows || [];
          this.total = response.total || 0;
        })
        .finally(() => {
          this.loading = false;
        });
    },
    cancel() {
      this.open = false;
      this.reset();
    },
    reset() {
      this.form = { systemId: undefined, systemCode: "", systemName: "", systemType: "ERP", status: "ENABLED" };
      this.resetForm("form");
    },
    handleQuery() {
      this.queryParams.pageNum = 1;
      this.getList();
    },
    resetQuery() {
      this.resetForm("queryForm");
      this.handleQuery();
    },
    handleSelectionChange(selection) {
      this.ids = selection.map((item) => item.systemId);
      this.single = selection.length !== 1;
      this.multiple = !selection.length;
    },
    handleAdd() {
      this.reset();
      this.open = true;
      this.title = "新增外部系统";
    },
    handleUpdate(row) {
      this.reset();
      const id = row.systemId || this.ids[0];
      getSystem(id).then((response) => {
        this.form = response.data || {};
        this.open = true;
        this.title = "修改外部系统";
      });
    },
    submitForm() {
      this.$refs.form.validate((valid) => {
        if (!valid) return;
        const payload = {
          systemCode: this.form.systemCode,
          systemName: this.form.systemName,
          systemType: this.form.systemType,
          status: this.form.status || "ENABLED",
        };
        const req = this.form.systemId
          ? updateSystem(this.form.systemId, payload)
          : addSystem(payload);
        req.then(() => {
          this.$modal.msgSuccess(this.form.systemId ? "修改成功" : "新增成功");
          this.open = false;
          this.getList();
        });
      });
    },
    handleDelete(row) {
      const id = row.systemId || this.ids[0];
      this.$modal.confirm('是否确认删除外部系统编号为"' + id + '"的数据？').then(() => delSystem(id)).then(() => {
        this.getList();
        this.$modal.msgSuccess("删除成功");
      }).catch(() => {});
    },
  },
};
</script>
