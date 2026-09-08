<template>
  <div class="app-container inbound-doc-panel sys-doc-panel sys-comms-doc-panel">
    <section class="inbound-filter-panel sys-comms-filter-panel" :class="{ 'sys-comms-filter-panel--compact': !showSearch }">
      <div class="sys-comms-filter__bar">
        <div class="sys-comms-filter__bar-main">
          <el-form v-show="showSearch" ref="queryForm" :model="queryParams" size="default" :inline="true" class="inbound-filter-form sys-comms-filter__fields" label-width="0" @submit.prevent>
            <el-form-item prop="noticeTitle">
              <el-input v-model="queryParams.noticeTitle" placeholder="公告标题" clearable @keyup.enter="handleQuery" />
            </el-form-item>
            <el-form-item prop="createBy">
              <el-input v-model="queryParams.createBy" placeholder="操作人员" clearable @keyup.enter="handleQuery" />
            </el-form-item>
            <el-form-item prop="noticeType">
              <el-select v-model="queryParams.noticeType" placeholder="公告类型" clearable>
                <el-option v-for="dict in dict.type.sys_notice_type" :key="dict.value" :label="dict.label" :value="dict.value" />
              </el-select>
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
          <el-button type="primary" plain icon="el-icon-plus" size="default" @click="handleAdd" v-hasPermi="['system:notice:add']">新增</el-button>
          <el-button type="success" plain icon="el-icon-edit" size="default" :disabled="single" @click="handleUpdate" v-hasPermi="['system:notice:edit']">修改</el-button>
          <el-button type="danger" plain icon="el-icon-delete" size="default" :disabled="multiple" @click="handleDelete" v-hasPermi="['system:notice:remove']">删除</el-button>
        </div>
      </div>
    </section>

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
        :data="noticeList"
        @selection-change="handleSelectionChange"
      >
        <el-table-column type="selection" width="42" align="center" />
        <el-table-column label="序号" align="center" prop="noticeId" min-width="80" />
        <el-table-column label="公告标题" align="center" prop="noticeTitle" min-width="160" show-overflow-tooltip />
        <el-table-column label="公告类型" align="center" prop="noticeType" min-width="100">
          <template #default="scope">
            <dict-tag :options="dict.type.sys_notice_type" :value="scope.row.noticeType" />
          </template>
        </el-table-column>
        <el-table-column label="状态" align="center" prop="status" min-width="90">
          <template #default="scope">
            <dict-tag :options="dict.type.sys_notice_status" :value="scope.row.status" />
          </template>
        </el-table-column>
        <el-table-column label="创建者" align="center" prop="createBy" min-width="100" show-overflow-tooltip />
        <el-table-column label="创建时间" align="center" prop="createTime" min-width="120">
          <template #default="scope">
            <span>{{ parseTime(scope.row.createTime, '{y}-{m}-{d}') }}</span>
          </template>
        </el-table-column>
        <el-table-column label="操作" align="center" min-width="140" class-name="col-actions">
          <template #default="scope">
            <div class="yunshu-row-actions">
              <el-button size="small" link icon="el-icon-edit" @click="handleUpdate(scope.row)" v-hasPermi="['system:notice:edit']">修改</el-button>
              <el-button size="small" link icon="el-icon-delete" @click="handleDelete(scope.row)" v-hasPermi="['system:notice:remove']">删除</el-button>
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

    <el-dialog :title="title" v-model="open" class="access-form-dialog" width="780px" append-to-body align-center destroy-on-close>
      <el-form ref="form" :model="form" :rules="rules" label-width="96px" class="access-form-dialog__form">
        <el-row>
          <el-col :span="12">
            <el-form-item label="公告标题" prop="noticeTitle">
              <el-input v-model="form.noticeTitle" placeholder="请输入公告标题" />
            </el-form-item>
          </el-col>
          <el-col :span="12">
            <el-form-item label="公告类型" prop="noticeType">
              <el-select v-model="form.noticeType" placeholder="请选择公告类型">
                <el-option v-for="dict in dict.type.sys_notice_type" :key="dict.value" :label="dict.label" :value="dict.value" />
              </el-select>
            </el-form-item>
          </el-col>
          <el-col :span="24">
            <el-form-item label="状态">
              <el-radio-group v-model="form.status">
                <el-radio v-for="dict in dict.type.sys_notice_status" :key="dict.value" :label="dict.value">{{ dict.label }}</el-radio>
              </el-radio-group>
            </el-form-item>
          </el-col>
          <el-col :span="24">
            <el-form-item label="内容">
              <el-input type="textarea" :rows="6" v-model="form.noticeContent" />
            </el-form-item>
          </el-col>
        </el-row>
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
import { listNotice, getNotice, delNotice, addNotice, updateNotice } from "@/yunshu-ui/api/system/notice";

export default {
  name: "Notice",
  dicts: ['sys_notice_status', 'sys_notice_type'],
  data() {
    return {
      loading: true,
      ids: [],
      single: true,
      multiple: true,
      showSearch: true,
      total: 0,
      noticeList: [],
      title: "",
      open: false,
      queryParams: {
        pageNum: 1,
        pageSize: 10,
        noticeTitle: undefined,
        createBy: undefined,
        noticeType: undefined,
        status: undefined
      },
      form: {},
      rules: {
        noticeTitle: [
          { required: true, message: "公告标题不能为空", trigger: "blur" }
        ],
        noticeType: [
          { required: true, message: "公告类型不能为空", trigger: "change" }
        ]
      }
    };
  },
  created() {
    this.getList();
  },
  methods: {
    getList() {
      this.loading = true;
      listNotice(this.queryParams).then(response => {
        this.noticeList = response.rows;
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
        noticeId: undefined,
        noticeTitle: undefined,
        noticeType: undefined,
        noticeContent: undefined,
        status: "0"
      };
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
      this.ids = selection.map(item => item.noticeId)
      this.single = selection.length != 1
      this.multiple = !selection.length
    },
    handleAdd() {
      this.reset();
      this.open = true;
      this.title = "添加公告";
    },
    handleUpdate(row) {
      this.reset();
      const noticeId = row.noticeId || this.ids
      getNotice(noticeId).then(response => {
        this.form = response.data;
        this.open = true;
        this.title = "修改公告";
      });
    },
    submitForm: function() {
      this.$refs["form"].validate(valid => {
        if (valid) {
          if (this.form.noticeId != undefined) {
            updateNotice(this.form).then(() => {
              this.$modal.msgSuccess("修改成功");
              this.open = false;
              this.getList();
            });
          } else {
            addNotice(this.form).then(() => {
              this.$modal.msgSuccess("新增成功");
              this.open = false;
              this.getList();
            });
          }
        }
      });
    },
    handleDelete(row) {
      const noticeIds = row.noticeId || this.ids
      this.$modal.confirm('是否确认删除公告编号为"' + noticeIds + '"的数据项？').then(function() {
        return delNotice(noticeIds);
      }).then(() => {
        this.getList();
        this.$modal.msgSuccess("删除成功");
      }).catch(() => {});
    }
  }
};
</script>
