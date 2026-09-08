<template>
  <div class="app-container inbound-doc-panel factory-doc-panel">
    <section v-show="showSearch" class="inbound-filter-panel">
      <el-form
        :model="queryParams"
        ref="queryForm"
        size="small"
        :inline="true"
        class="inbound-filter-form"
        label-width="0"
        @submit.prevent
      >
        <el-form-item prop="lineCode">
          <el-input v-model="queryParams.lineCode" placeholder="产线编码" clearable @keyup.enter="handleQuery" />
        </el-form-item>
        <el-form-item prop="lineName">
          <el-input v-model="queryParams.lineName" placeholder="产线名称" clearable @keyup.enter="handleQuery" />
        </el-form-item>
        <el-form-item prop="workshopId">
          <el-select v-model="queryParams.workshopId" placeholder="所属车间" clearable>
            <el-option v-for="w in workshops" :key="w.workshopId" :label="w.workshopName" :value="w.workshopId" />
          </el-select>
        </el-form-item>
        <el-form-item prop="enableFlag">
          <el-select v-model="queryParams.enableFlag" placeholder="是否启用" clearable>
            <el-option v-for="d in dict.type.sys_yes_no" :key="d.value" :label="d.label" :value="d.value" />
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
        <span class="inbound-page-title">产线管理</span>
        <el-button type="primary" plain icon="el-icon-plus" size="default" @click="handleAdd" v-hasPermi="['mes:md:line:add']">新增</el-button>
        <el-button type="success" plain icon="el-icon-edit" size="default" :disabled="single" @click="handleUpdate" v-hasPermi="['mes:md:line:edit']">修改</el-button>
        <el-button type="danger" plain icon="el-icon-delete" size="default" :disabled="multiple" @click="handleDelete" v-hasPermi="['mes:md:line:remove']">删除</el-button>
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
        class="yunshu-data-table inbound-table factory-doc-table"
        stripe
        border
        height="100%"
        :data="lineList"
        @selection-change="handleSelectionChange"
      >
        <el-table-column type="selection" width="42" align="center" header-align="center" />
        <el-table-column label="产线编码" align="center" header-align="center" min-width="120" prop="lineCode">
          <template #default="scope">
            <button type="button" class="doc-link-btn" @click="handleView(scope.row)">{{ scope.row.lineCode }}</button>
          </template>
        </el-table-column>
        <el-table-column label="产线名称" align="center" header-align="center" min-width="120" prop="lineName" show-overflow-tooltip />
        <el-table-column label="所属车间" align="center" header-align="center" min-width="110" prop="workshopName" show-overflow-tooltip />
        <el-table-column label="额定产能" align="center" header-align="center" width="88" prop="ratedCapacity" />
        <el-table-column label="是否启用" align="center" header-align="center" width="88" prop="enableFlag">
          <template #default="scope">
            <dict-tag :options="dict.type.sys_yes_no" :value="scope.row.enableFlag"/>
          </template>
        </el-table-column>
        <el-table-column label="操作" min-width="160" align="center" header-align="center" class-name="col-actions small-padding fixed-width">
          <template #default="scope">
            <div class="yunshu-row-actions">
              <el-button size="small" link icon="el-icon-view" @click="handleView(scope.row)" v-hasPermi="['mes:md:line:query']">查看</el-button>
              <el-button size="small" link icon="el-icon-edit" @click="handleUpdate(scope.row)" v-hasPermi="['mes:md:line:edit']">修改</el-button>
              <el-button size="small" link icon="el-icon-delete" @click="handleDelete(scope.row)" v-hasPermi="['mes:md:line:remove']">删除</el-button>
            </div>
          </template>
        </el-table-column>
      </el-table>
    </div>

    <pagination
      v-show="total>0"
      :total="total"
      :page="queryParams.pageNum"
      :limit="queryParams.pageSize"
      @update:page="queryParams.pageNum = $event"
      @update:limit="queryParams.pageSize = $event"
      @pagination="getList"
    />

    <el-dialog :title="title" v-model="open" width="600px" append-to-body>
      <el-form ref="form" :model="form" :rules="rules" label-width="90px" :disabled="optType === 'view'">
        <el-form-item label="产线编码" prop="lineCode">
          <el-input v-model="form.lineCode" :readonly="optType === 'view' || optType === 'edit'" />
        </el-form-item>
        <el-form-item label="产线名称" prop="lineName">
          <el-input v-model="form.lineName" />
        </el-form-item>
        <el-form-item label="车间" prop="workshopId">
          <el-select v-model="form.workshopId" placeholder="请选择车间" style="width:100%">
            <el-option v-for="w in workshops" :key="w.workshopId" :label="w.workshopName" :value="w.workshopId" />
          </el-select>
        </el-form-item>
        <el-form-item label="额定产能" prop="ratedCapacity">
          <el-input-number v-model="form.ratedCapacity" :min="0" style="width:100%" />
        </el-form-item>
        <el-form-item label="是否启用" prop="enableFlag">
          <el-radio-group v-model="form.enableFlag">
            <el-radio v-for="d in dict.type.sys_yes_no" :key="d.value" :label="d.value">{{ d.label }}</el-radio>
          </el-radio-group>
        </el-form-item>
      </el-form>
      <template #footer>
        <div class="dialog-footer">
          <el-button v-if="optType !== 'view'" type="primary" @click="submitForm">确 定</el-button>
          <el-button @click="open=false">取 消</el-button>
        </div>
      </template>
    </el-dialog>
  </div>
</template>

<script>
import { listLine, getLine, addLine, updateLine, delLine } from "@/yunshu-ui/api/mes/md/line";
import { listAllWorkshop } from "@/yunshu-ui/api/mes/md/workshop";

export default {
  name: "ProductionLine",
  dicts: ["sys_yes_no"],
  data() {
    return {
      loading: false,
      showSearch: true,
      total: 0,
      lineList: [],
      workshops: [],
      ids: [],
      single: true,
      multiple: true,
      open: false,
      title: "",
      optType: "add",
      form: {},
      queryParams: {
        pageNum: 1,
        pageSize: 10,
        lineCode: null,
        lineName: null,
        workshopId: null,
        enableFlag: null,
      },
      rules: {
        lineCode: [{ required: true, message: "产线编码不能为空", trigger: "blur" }],
        lineName: [{ required: true, message: "产线名称不能为空", trigger: "blur" }],
        workshopId: [{ required: true, message: "请选择车间", trigger: "change" }],
      },
    };
  },
  created() {
    this.getList();
    this.loadWorkshops();
  },
  methods: {
    loadWorkshops() {
      listAllWorkshop().then((r) => {
        this.workshops = r.rows || r.data || [];
      });
    },
    getList() {
      this.loading = true;
      listLine(this.queryParams)
        .then((r) => {
          this.lineList = r.rows || [];
          this.total = r.total || 0;
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
    handleSelectionChange(selection) {
      this.ids = selection.map((item) => item.lineId);
      this.single = selection.length !== 1;
      this.multiple = !selection.length;
    },
    resetFormData() {
      this.form = { enableFlag: "Y", ratedCapacity: 0 };
    },
    handleAdd() {
      this.resetFormData();
      this.open = true;
      this.title = "添加产线";
      this.optType = "add";
    },
    handleView(row) {
      const lineId = row.lineId || this.ids[0];
      getLine(lineId).then((r) => {
        this.form = r.data || row;
        this.open = true;
        this.title = "查看产线";
        this.optType = "view";
      });
    },
    handleUpdate(row) {
      const lineId = row.lineId || this.ids[0];
      getLine(lineId).then((r) => {
        this.form = r.data || row;
        this.open = true;
        this.title = "修改产线";
        this.optType = "edit";
      });
    },
    submitForm() {
      this.$refs.form.validate((valid) => {
        if (!valid) return;
        const fn = this.form.lineId ? updateLine : addLine;
        fn(this.form).then(() => {
          this.$modal.msgSuccess("保存成功");
          this.open = false;
          this.getList();
        });
      });
    },
    handleDelete(row) {
      const lineIds = row.lineId || this.ids;
      this.$modal.confirm("是否确认删除所选产线？").then(() => delLine(lineIds)).then(() => {
        this.getList();
        this.$modal.msgSuccess("删除成功");
      }).catch(() => {});
    },
  },
};
</script>
