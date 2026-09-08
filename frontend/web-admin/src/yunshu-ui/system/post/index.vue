<template>
  <div :class="embedMode ? 'inbound-doc-panel access-doc-panel' : 'app-container'">
    <template v-if="embedMode">
      <section v-show="showSearch" class="inbound-filter-panel">
        <el-form :model="queryParams" ref="queryForm" size="small" :inline="true" class="inbound-filter-form" label-width="0" @submit.prevent>
          <el-form-item prop="postCode">
            <el-input v-model="queryParams.postCode" placeholder="岗位编码" clearable @keyup.enter="handleQuery" />
          </el-form-item>
          <el-form-item prop="postName">
            <el-input v-model="queryParams.postName" placeholder="岗位名称" clearable @keyup.enter="handleQuery" />
          </el-form-item>
          <el-form-item class="filter-actions">
            <el-button type="primary" icon="el-icon-search" size="default" @click="handleQuery">搜索</el-button>
            <el-button icon="el-icon-refresh" size="default" @click="resetQuery">重置</el-button>
          </el-form-item>
        </el-form>
      </section>

      <div class="inbound-toolbar">
        <div class="inbound-toolbar-left">
          <span class="inbound-page-title">岗位管理</span>
          <el-button type="primary" plain icon="el-icon-plus" size="default" @click="handleAdd" v-hasPermi="['system:post:add']">新增</el-button>
          <el-button type="success" plain icon="el-icon-edit" size="default" :disabled="single" @click="handleUpdate" v-hasPermi="['system:post:edit']">修改</el-button>
          <el-button type="danger" plain icon="el-icon-delete" size="default" :disabled="multiple" @click="handleDelete" v-hasPermi="['system:post:remove']">删除</el-button>
          <el-button type="warning" plain icon="el-icon-download" size="default" @click="handleExport" v-hasPermi="['system:post:export']">导出</el-button>
        </div>
        <div class="inbound-toolbar-meta">
          <span>共 {{ total }} 条</span>
          <right-toolbar :show-search="showSearch" @update:showSearch="showSearch = $event" @queryTable="getList"></right-toolbar>
        </div>
      </div>

      <div class="inbound-table-frame">
        <span class="frame-corner frame-corner--tl" aria-hidden="true"></span>
        <span class="frame-corner frame-corner--tr" aria-hidden="true"></span>
        <span class="frame-corner frame-corner--bl" aria-hidden="true"></span>
        <span class="frame-corner frame-corner--br" aria-hidden="true"></span>
        <el-table class="yunshu-data-table inbound-table" stripe border height="100%" v-loading="loading" :data="postList" @selection-change="handleSelectionChange">
          <el-table-column type="selection" width="42" align="center" />
          <el-table-column label="岗位编号" align="center" prop="postId" min-width="90" />
          <el-table-column label="岗位编码" align="center" prop="postCode" min-width="120">
            <template #default="scope">
              <button type="button" class="doc-link-btn" @click.stop.prevent="handleUpdate(scope.row)">{{ scope.row.postCode }}</button>
            </template>
          </el-table-column>
          <el-table-column label="岗位名称" align="center" prop="postName" min-width="120" show-overflow-tooltip />
          <el-table-column label="岗位排序" align="center" prop="postSort" min-width="100" />
          <el-table-column label="状态" align="center" prop="status" width="100">
            <template #default="scope"><dict-tag :options="dict.type.sys_normal_disable" :value="scope.row.status"/></template>
          </el-table-column>
          <el-table-column label="创建时间" align="center" prop="createTime" min-width="160">
            <template #default="scope"><span>{{ parseTime(scope.row.createTime) }}</span></template>
          </el-table-column>
          <el-table-column label="操作" align="center" min-width="140" class-name="col-actions small-padding fixed-width">
            <template #default="scope">
              <div class="yunshu-row-actions">
                <el-button size="small" link icon="el-icon-edit" @click="handleUpdate(scope.row)" v-hasPermi="['system:post:edit']">修改</el-button>
                <el-button size="small" link icon="el-icon-delete" @click="handleDelete(scope.row)" v-hasPermi="['system:post:remove']">删除</el-button>
              </div>
            </template>
          </el-table-column>
        </el-table>
      </div>

      <pagination v-show="total>0" :total="total" :page="queryParams.pageNum" @update:page="queryParams.pageNum = $event" :limit="queryParams.pageSize" @update:limit="queryParams.pageSize = $event" @pagination="getList" />
    </template>

    <template v-else>
      <el-form :model="queryParams" ref="queryForm" size="small" :inline="true" v-show="showSearch" label-width="68px">
        <el-form-item label="岗位编码" prop="postCode"><el-input v-model="queryParams.postCode" placeholder="请输入岗位编码" clearable @keyup.enter="handleQuery" /></el-form-item>
        <el-form-item label="岗位名称" prop="postName"><el-input v-model="queryParams.postName" placeholder="请输入岗位名称" clearable @keyup.enter="handleQuery" /></el-form-item>
        <el-form-item label="状态" prop="status">
          <el-select v-model="queryParams.status" placeholder="岗位状态" clearable>
            <el-option v-for="dict in dict.type.sys_normal_disable" :key="dict.value" :label="dict.label" :value="dict.value" />
          </el-select>
        </el-form-item>
        <el-form-item>
          <el-button type="primary" icon="el-icon-search" size="mini" @click="handleQuery">搜索</el-button>
          <el-button icon="el-icon-refresh" size="mini" @click="resetQuery">重置</el-button>
        </el-form-item>
      </el-form>
      <el-row :gutter="10" class="mb8">
        <el-col :span="1.5"><el-button type="primary" plain icon="el-icon-plus" size="mini" @click="handleAdd" v-hasPermi="['system:post:add']">新增</el-button></el-col>
        <el-col :span="1.5"><el-button type="success" plain icon="el-icon-edit" size="mini" :disabled="single" @click="handleUpdate" v-hasPermi="['system:post:edit']">修改</el-button></el-col>
        <el-col :span="1.5"><el-button type="danger" plain icon="el-icon-delete" size="mini" :disabled="multiple" @click="handleDelete" v-hasPermi="['system:post:remove']">删除</el-button></el-col>
        <el-col :span="1.5"><el-button type="warning" plain icon="el-icon-download" size="mini" @click="handleExport" v-hasPermi="['system:post:export']">导出</el-button></el-col>
        <right-toolbar :show-search="showSearch" @update:showSearch="showSearch = $event" @queryTable="getList"></right-toolbar>
      </el-row>
      <el-table class="yunshu-data-table" stripe border v-loading="loading" :data="postList" @selection-change="handleSelectionChange">
        <el-table-column type="selection" width="55" align="center" />
        <el-table-column label="岗位编号" align="center" prop="postId" />
        <el-table-column label="岗位编码" align="center" prop="postCode" />
        <el-table-column label="岗位名称" align="center" prop="postName" />
        <el-table-column label="岗位排序" align="center" prop="postSort" />
        <el-table-column label="状态" align="center" prop="status"><template #default="scope"><dict-tag :options="dict.type.sys_normal_disable" :value="scope.row.status"/></template></el-table-column>
        <el-table-column label="创建时间" align="center" prop="createTime" width="180"><template #default="scope"><span>{{ parseTime(scope.row.createTime) }}</span></template></el-table-column>
        <el-table-column label="操作" align="center" class-name="small-padding fixed-width">
          <template #default="scope">
            <el-button size="mini" type="text" icon="el-icon-edit" @click="handleUpdate(scope.row)" v-hasPermi="['system:post:edit']">修改</el-button>
            <el-button size="mini" type="text" icon="el-icon-delete" @click="handleDelete(scope.row)" v-hasPermi="['system:post:remove']">删除</el-button>
          </template>
        </el-table-column>
      </el-table>
      <pagination v-show="total>0" :total="total" :page="queryParams.pageNum" @update:page="queryParams.pageNum = $event" :limit="queryParams.pageSize" @update:limit="queryParams.pageSize = $event" @pagination="getList" />
    </template>

    <el-dialog :title="title" v-model="open" class="access-form-dialog" width="560px" append-to-body align-center destroy-on-close>
      <el-form ref="form" :model="form" :rules="rules" label-width="96px" class="access-form-dialog__form">
        <el-form-item label="岗位名称" prop="postName"><el-input v-model="form.postName" placeholder="请输入岗位名称" /></el-form-item>
        <el-form-item label="岗位编码" prop="postCode"><el-input v-model="form.postCode" placeholder="请输入编码名称" /></el-form-item>
        <el-form-item label="岗位顺序" prop="postSort"><el-input-number v-model="form.postSort" controls-position="right" :min="0" class="access-form-dialog__number" /></el-form-item>
        <el-form-item label="岗位状态" prop="status">
          <el-radio-group v-model="form.status">
            <el-radio v-for="dict in dict.type.sys_normal_disable" :key="dict.value" :label="dict.value">{{ dict.label }}</el-radio>
          </el-radio-group>
        </el-form-item>
        <el-form-item label="备注" prop="remark"><el-input v-model="form.remark" type="textarea" :rows="3" placeholder="请输入内容" /></el-form-item>
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
import { listPost, getPost, delPost, addPost, updatePost } from "@/yunshu-ui/api/system/post";

export default {
  name: "Post",
  dicts: ['sys_normal_disable'],
  props: {
    embedMode: { type: Boolean, default: false },
    scopeStatus: { type: String, default: null },
  },
  data() {
    return {
      loading: true,
      ids: [],
      single: true,
      multiple: true,
      showSearch: true,
      total: 0,
      postList: [],
      title: "",
      open: false,
      queryParams: { pageNum: 1, pageSize: 10, postCode: undefined, postName: undefined, status: undefined },
      form: {},
      rules: {
        postName: [{ required: true, message: "岗位名称不能为空", trigger: "blur" }],
        postCode: [{ required: true, message: "岗位编码不能为空", trigger: "blur" }],
        postSort: [{ required: true, message: "岗位顺序不能为空", trigger: "blur" }],
        remark: [{ max: 250, message: '长度必须小于250个字符', trigger: 'blur' }]
      }
    };
  },
  watch: {
    scopeStatus(val) {
      if (!this.embedMode) return;
      this.queryParams.status = val ?? undefined;
      this.handleQuery();
    },
  },
  created() {
    if (this.embedMode) this.queryParams.status = this.scopeStatus ?? undefined;
    this.getList();
  },
  methods: {
    getList() {
      this.loading = true;
      listPost(this.queryParams).then(response => {
        this.postList = response.rows;
        this.total = response.total;
      }).finally(() => { this.loading = false; });
    },
    cancel() { this.open = false; this.reset(); },
    reset() {
      this.form = { postId: undefined, postCode: undefined, postName: undefined, postSort: 0, status: "0", remark: undefined };
      this.resetForm("form");
    },
    handleQuery() { this.queryParams.pageNum = 1; this.getList(); },
    resetQuery() {
      this.resetForm("queryForm");
      if (this.embedMode) this.queryParams.status = this.scopeStatus ?? undefined;
      this.handleQuery();
    },
    handleSelectionChange(selection) {
      this.ids = selection.map(item => item.postId);
      this.single = selection.length != 1;
      this.multiple = !selection.length;
    },
    handleAdd() { this.reset(); this.open = true; this.title = "添加岗位"; },
    handleUpdate(row) {
      this.reset();
      const postId = row.postId || this.ids;
      getPost(postId).then(response => { this.form = response.data; this.open = true; this.title = "修改岗位"; });
    },
    submitForm() {
      this.$refs["form"].validate(valid => {
        if (!valid) return;
        const req = this.form.postId != undefined ? updatePost(this.form) : addPost(this.form);
        req.then(() => { this.$modal.msgSuccess(this.form.postId != undefined ? "修改成功" : "新增成功"); this.open = false; this.getList(); });
      });
    },
    handleDelete(row) {
      const postIds = row.postId || this.ids;
      this.$modal.confirm('是否确认删除岗位编号为"' + postIds + '"的数据项？').then(function() { return delPost(postIds); }).then(() => {
        this.getList(); this.$modal.msgSuccess("删除成功");
      }).catch(() => {});
    },
    handleExport() { this.download('system/post/export', { ...this.queryParams }, `post_${new Date().getTime()}.xlsx`); }
  }
};
</script>
