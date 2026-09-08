<template>
  <div class="app-container" :class="{ 'hub-embed-panel': embedMode }">
    <el-form :model="queryParams" ref="queryForm" size="small" :inline="true" v-show="showSearch" label-width="68px" @submit.prevent>
      <el-form-item label="报表名称" prop="name">
        <el-input
          v-model="queryParams.name"
          placeholder="请输入报表名称"
          clearable
          @keyup.enter="handleQuery"
        />
      </el-form-item>
      <el-form-item>
        <el-button type="primary" icon="el-icon-search" size="small" @click="handleQuery">搜索</el-button>
        <el-button icon="el-icon-refresh" size="small" @click="resetQuery">重置</el-button>
      </el-form-item>
    </el-form>

    <el-row :gutter="10" class="mb8">
      <el-col :span="1.5">
        <el-button
          type="primary"
          plain
          icon="el-icon-plus"
          size="mini"
          @click="handleAdd"
        >新增</el-button>
      </el-col>
      <el-col :span="1.5">
        <el-button
          type="success"
          plain
          icon="el-icon-edit"
          size="mini"
          :disabled="single"
          @click="handleUpdate"
        >修改</el-button>
      </el-col>
      <el-col :span="1.5">
        <el-button
          type="danger"
          plain
          icon="el-icon-delete"
          size="mini"
          :disabled="multiple"
          @click="handleDelete"
        >删除</el-button>
      </el-col>
      <el-col :span="1.5">
        <el-button
          type="warning"
          plain
          icon="el-icon-download"
          size="mini"
          @click="handleExport"
        >导出</el-button>
      </el-col>
      <right-toolbar :show-search="showSearch" @queryTable="getList"></right-toolbar>
    </el-row>

    <el-table class="yunshu-data-table" stripe border v-loading="loading" :data="listData" @selection-change="handleSelectionChange">
      <el-table-column type="selection" width="55" align="center" />
      <el-table-column label="报表名称" align="center" prop="name" />
      <el-table-column label="创建时间" align="center" prop="createTime" width="180">
        <template #default="scope">
          <span>{{ parseTime(scope.row.createTime, '{y}-{m}-{d}') }}</span>
        </template>
      </el-table-column>
      <el-table-column label="更新时间" align="center" prop="updateTime" width="180">
        <template #default="scope">
          <span>{{ parseTime(scope.row.updateTime, '{y}-{m}-{d}') }}</span>
        </template>
      </el-table-column>
      <el-table-column label="操作" align="center" class-name="col-actions">
        <template #default="scope">
          <div class="yunshu-row-actions">
            <el-button
              size="small"
              link
              icon="el-icon-edit"
              @click="handleDesign(scope.row.name)"
              v-hasPermi="['mes:report:edit']"
            >设计</el-button>
            <el-button
              size="small"
              link
              icon="el-icon-view"
              @click="handlePreview(scope.row.name)"
              v-hasPermi="['mes:report:edit']"
            >预览</el-button>
            <el-button
              size="small"
              link
              icon="el-icon-delete"
              @click="handleDelete(scope.row)"
              v-hasPermi="['mes:report:remove']"
            >删除</el-button>
          </div>
        </template>
      </el-table-column>
    </el-table>

    <pagination
      v-show="total>0"
      :total="total"
      :page="queryParams.pageNum"
      @update:page="queryParams.pageNum = $event"
      :limit="queryParams.pageSize"
      @update:limit="queryParams.pageSize = $event"
      @pagination="getList"
    />

    <!-- 添加或修改报表管理对话框 -->
    <el-dialog :title="title" v-model="open" width="500px" append-to-body>
      <el-form ref="form" :model="form" :rules="rules" label-width="80px">
        <el-form-item label="报表名称" prop="name">
          <el-input v-model="form.name" placeholder="请输入报表名称" />
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
import { listReport, getReport, delReport, addReport, updateReport } from "@/yunshu-ui/api/mes/report/ureport";

export default {
  name: "ureport",
  props: {
    embedMode: { type: Boolean, default: false },
  },
  emits: ["design", "changed"],
  data() {
    return {
      // 遮罩层
      loading: true,
      // 选中数组
      ids: [],
      selectedRows: [],
      // 非单个禁用
      single: true,
      // 非多个禁用
      multiple: true,
      // 显示搜索条件
      showSearch: true,
      // 总条数
      total: 0,
      // 报表管理表格数据
      listData: [],
      // 弹出层标题
      title: "",
      // 是否显示弹出层
      open: false,
      // 查询参数
      queryParams: {
        pageNum: 1,
        pageSize: 20,
        name: null,
        content: null,
        createTime: null,
        updateTime: null
      },
      // 表单参数
      form: {},
      // 表单校验
      rules: {
        name: [
          { required: true, message: "报表名称不能为空", trigger: "blur" }
        ],
      },
      website: { reportUrl: "/ureport" }
    };
  },
  created() {
    this.getList();
  },
  methods: {
    /** 查询报表管理列表 */
    getList() {
      this.loading = true;
      listReport(this.queryParams).then(response => {
        this.listData = response.rows;
        this.total = response.total;
      }).finally(() => { this.loading = false; });
    },
    // 取消按钮
    cancel() {
      this.open = false;
      this.reset();
    },
    // 表单重置
    reset() {
      this.form = {
        id: null,
        name: null,
        content: null,
        createTime: null,
        updateTime: null
      };
      this.resetForm("form");
    },
    /** 搜索按钮操作 */
    handleQuery() {
      this.queryParams.pageNum = 1;
      this.getList();
    },
    /** 重置按钮操作 */
    resetQuery() {
      this.resetForm("queryForm");
      this.handleQuery();
    },
    // 多选框选中数据
    handleSelectionChange(selection) {
      this.ids = selection.map(item => item.id)
      this.selectedRows = selection
      this.single = selection.length!==1
      this.multiple = !selection.length
    },
    /** 新增按钮操作 */
    handleAdd() {
      this.reset();
      this.open = true;
      this.title = "添加报表管理";
    },
    /** 报表预览 */
    handlePreview(name) {
      window.open(`${this.website.reportUrl}/preview?_u=mysql:`+name)
    },
    /** 报表设计 */
    handleDesign(name) {
      if (this.embedMode) {
        this.$emit("design", name);
        return;
      }
      this.$router.push({
        path: "/app/analytics/report-designer",
        query: { file: name }
      });
    },
    /** 修改按钮操作 */
    handleUpdate(row) {
      this.reset();
      const id = row.id || this.ids
      getReport(id).then(response => {
        this.form = response.data;
        this.open = true;
        this.title = "修改报表管理";
      });
    },
    /** 提交按钮 */
    submitForm() {
      this.$refs["form"].validate(valid => {
        if (valid) {
          if (this.form.id != null) {
            updateReport(this.form).then(response => {
              this.$modal.msgSuccess("修改成功");
              this.open = false;
              this.getList();
              this.$emit("changed");
            });
          } else {
            addReport(this.form).then(response => {
              this.$modal.msgSuccess("新增成功");
              this.open = false;
              this.getList();
              this.$emit("changed");
            });
          }
        }
      });
    },
    /** 删除按钮操作 */
    handleDelete(row) {
      const ids = row.id || this.ids;
      this.$modal.confirm('是否确认删除报表管理编号为"' + ids + '"的数据项？').then(function() {
        return delReport(ids);
      }).then(() => {
        this.getList();
        this.$modal.msgSuccess("删除成功");
        this.$emit("changed");
      }).catch(() => {});
    },
    /** 导出：选中单条 → UReport Excel 取数导出；否则导出档案元数据列表 */
    handleExport() {
      if (this.selectedRows.length === 1 && this.selectedRows[0]?.name) {
        const name = this.selectedRows[0].name;
        window.open(
          `${this.website.reportUrl}/excel?_u=mysql:${encodeURIComponent(name)}`,
          "_blank"
        );
        return;
      }
      this.download(
        "ureportM/export",
        { ...this.queryParams },
        `报表档案_${new Date().getTime()}.xlsx`
      );
    }
  }
};
</script>
