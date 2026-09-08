<template>
  <div :class="rootClass">
    <section v-show="showSearch || embedMode" class="inbound-filter-panel" :class="{ 'sys-settings-embed-filter': embedMode, 'sys-settings-embed-filter--compact': embedMode && !showSearch }">
      <div v-if="embedMode" class="sys-settings-embed-filter__stack">
        <el-form v-show="showSearch" ref="queryForm" :model="queryParams" size="default" :inline="true" class="inbound-filter-form sys-settings-embed-filter__fields" label-width="0" @submit.prevent>
          <el-form-item prop="dictLabel">
            <el-input v-model="queryParams.dictLabel" placeholder="字典标签" clearable @keyup.enter="handleQuery" />
          </el-form-item>
          <el-form-item prop="status">
            <el-select v-model="queryParams.status" placeholder="状态" clearable>
              <el-option v-for="dict in dict.type.sys_normal_disable" :key="dict.value" :label="dict.label" :value="dict.value" />
            </el-select>
          </el-form-item>
        </el-form>
        <div class="sys-settings-embed-filter__actions sys-settings-embed-filter__actions--split">
          <div class="sys-settings-embed-filter__actions-left">
            <el-button type="primary" icon="el-icon-search" size="default" @click="handleQuery">搜索</el-button>
            <el-button icon="el-icon-refresh" size="default" @click="resetQuery">重置</el-button>
            <span class="sys-settings-embed-filter__count">共 {{ total }} 条</span>
          </div>
          <div class="sys-settings-embed-filter__actions-right">
            <el-button type="primary" plain icon="el-icon-plus" size="default" @click="handleAdd" v-hasPermi="['system:dict:add']">新增</el-button>
            <el-button type="success" plain icon="el-icon-edit" size="default" :disabled="single" @click="handleUpdate" v-hasPermi="['system:dict:edit']">修改</el-button>
            <el-button type="danger" plain icon="el-icon-delete" size="default" :disabled="multiple" @click="handleDelete" v-hasPermi="['system:dict:remove']">删除</el-button>
            <el-button type="warning" plain icon="el-icon-download" size="default" @click="handleExport" v-hasPermi="['system:dict:export']">导出</el-button>
          </div>
        </div>
      </div>
      <el-form v-else ref="queryForm" :model="queryParams" size="default" :inline="true" class="inbound-filter-form" label-width="0" @submit.prevent>
        <el-form-item label="字典名称" prop="dictType">
          <el-select v-model="queryParams.dictType" placeholder="字典类型">
            <el-option v-for="item in typeOptions" :key="item.dictId" :label="item.dictName" :value="item.dictType" />
          </el-select>
        </el-form-item>
        <el-form-item prop="dictLabel">
          <el-input v-model="queryParams.dictLabel" placeholder="字典标签" clearable @keyup.enter="handleQuery" />
        </el-form-item>
        <el-form-item prop="status">
          <el-select v-model="queryParams.status" placeholder="状态" clearable>
            <el-option v-for="dict in dict.type.sys_normal_disable" :key="dict.value" :label="dict.label" :value="dict.value" />
          </el-select>
        </el-form-item>
        <el-form-item class="filter-actions">
          <el-button type="primary" icon="el-icon-search" size="default" @click="handleQuery">搜索</el-button>
          <el-button icon="el-icon-refresh" size="default" @click="resetQuery">重置</el-button>
        </el-form-item>
      </el-form>
    </section>

    <div v-if="!embedMode" class="inbound-toolbar">
      <div class="inbound-toolbar-left">
        <span class="inbound-page-title">字典数据管理</span>
        <el-button type="primary" plain icon="el-icon-plus" size="default" @click="handleAdd" v-hasPermi="['system:dict:add']">新增</el-button>
        <el-button type="success" plain icon="el-icon-edit" size="default" :disabled="single" @click="handleUpdate" v-hasPermi="['system:dict:edit']">修改</el-button>
        <el-button type="danger" plain icon="el-icon-delete" size="default" :disabled="multiple" @click="handleDelete" v-hasPermi="['system:dict:remove']">删除</el-button>
        <el-button type="warning" plain icon="el-icon-download" size="default" @click="handleExport" v-hasPermi="['system:dict:export']">导出</el-button>
        <el-button type="warning" plain icon="el-icon-close" size="default" @click="handleClose">关闭</el-button>
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
        :data="dataList"
        @selection-change="handleSelectionChange"
      >
        <el-table-column type="selection" width="42" align="center" />
        <el-table-column label="字典编码" align="center" prop="dictCode" min-width="90" />
        <el-table-column label="字典标签" align="center" prop="dictLabel" min-width="120" show-overflow-tooltip>
          <template #default="scope">
            <span v-if="scope.row.listClass == '' || scope.row.listClass == 'default'">{{ scope.row.dictLabel }}</span>
            <el-tag v-else :type="scope.row.listClass == 'primary' ? '' : scope.row.listClass">{{ scope.row.dictLabel }}</el-tag>
          </template>
        </el-table-column>
        <el-table-column label="字典键值" align="center" prop="dictValue" min-width="100" show-overflow-tooltip />
        <el-table-column label="字典排序" align="center" prop="dictSort" min-width="90" />
        <el-table-column label="状态" align="center" prop="status" min-width="90">
          <template #default="scope">
            <dict-tag :options="dict.type.sys_normal_disable" :value="scope.row.status" />
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
              <el-button size="small" link icon="el-icon-edit" @click="handleUpdate(scope.row)" v-hasPermi="['system:dict:edit']">修改</el-button>
              <el-button size="small" link icon="el-icon-delete" @click="handleDelete(scope.row)" v-hasPermi="['system:dict:remove']">删除</el-button>
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

    <el-dialog :title="title" v-model="open" class="access-form-dialog" width="520px" append-to-body align-center destroy-on-close>
      <el-form ref="form" :model="form" :rules="rules" label-width="96px" class="access-form-dialog__form">
        <el-form-item label="字典类型">
          <el-input v-model="form.dictType" :disabled="true" />
        </el-form-item>
        <el-form-item label="数据标签" prop="dictLabel">
          <el-input v-model="form.dictLabel" placeholder="请输入数据标签" />
        </el-form-item>
        <el-form-item label="数据键值" prop="dictValue">
          <el-input v-model="form.dictValue" placeholder="请输入数据键值" />
        </el-form-item>
        <el-form-item label="样式属性" prop="cssClass">
          <el-input v-model="form.cssClass" placeholder="请输入样式属性" />
        </el-form-item>
        <el-form-item label="显示排序" prop="dictSort">
          <el-input-number v-model="form.dictSort" controls-position="right" :min="0" />
        </el-form-item>
        <el-form-item label="回显样式" prop="listClass">
          <el-select v-model="form.listClass">
            <el-option v-for="item in listClassOptions" :key="item.value" :label="item.label" :value="item.value" />
          </el-select>
        </el-form-item>
        <el-form-item label="状态" prop="status">
          <el-radio-group v-model="form.status">
            <el-radio v-for="dict in dict.type.sys_normal_disable" :key="dict.value" :label="dict.value">{{ dict.label }}</el-radio>
          </el-radio-group>
        </el-form-item>
        <el-form-item label="备注" prop="remark">
          <el-input v-model="form.remark" type="textarea" placeholder="请输入内容" />
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
import { listData, getData, delData, addData, updateData } from "@/yunshu-ui/api/system/dict/data";
import { listType, getType } from "@/yunshu-ui/api/system/dict/type";

export default {
  name: "Data",
  dicts: ['sys_normal_disable'],
  props: {
    embedDictId: { type: [String, Number], default: null },
    embedMode: { type: Boolean, default: false },
  },
  computed: {
    rootClass() {
      return this.embedMode
        ? 'app-container sys-doc-panel sys-doc-panel--embed sys-settings-embed'
        : 'app-container inbound-doc-panel sys-doc-panel';
    },
  },
  data() {
    return {
      loading: true,
      ids: [],
      single: true,
      multiple: true,
      showSearch: true,
      total: 0,
      dataList: [],
      defaultDictType: "",
      title: "",
      open: false,
      listClassOptions: [
        { value: "default", label: "默认" },
        { value: "primary", label: "主要" },
        { value: "success", label: "成功" },
        { value: "info", label: "信息" },
        { value: "warning", label: "警告" },
        { value: "danger", label: "危险" }
      ],
      typeOptions: [],
      queryParams: {
        pageNum: 1,
        pageSize: 10,
        dictName: undefined,
        dictType: undefined,
        status: undefined
      },
      form: {},
      rules: {
        dictLabel: [
          { required: true, message: "数据标签不能为空", trigger: "blur" }
        ],
        dictValue: [
          { required: true, message: "数据键值不能为空", trigger: "blur" }
        ],
        dictSort: [
          { required: true, message: "数据顺序不能为空", trigger: "blur" }
        ],
        remark: [
          { max: 250, message: '长度必须小于250个字符', trigger: 'blur' }
        ]
      }
    };
  },
  created() {
    this.bootstrap();
  },
  watch: {
    embedDictId() {
      this.bootstrap();
    },
  },
  methods: {
    bootstrap() {
      const dictId = this.embedDictId || (this.$route.params && this.$route.params.dictId);
      if (dictId) {
        this.getType(dictId);
      } else if (this.embedMode) {
        this.loading = false;
        this.dataList = [];
        this.total = 0;
      }
      if (!this.embedMode) {
        this.getTypeList();
      }
    },
    getType(dictId) {
      getType(dictId).then(response => {
        this.queryParams.dictType = response.data.dictType;
        this.defaultDictType = response.data.dictType;
        this.getList();
      });
    },
    getTypeList() {
      listType().then(response => {
        this.typeOptions = response.rows;
      });
    },
    getList() {
      this.loading = true;
      listData(this.queryParams).then(response => {
        this.dataList = response.rows;
        this.total = response.total;
        this.loading = false;
      });
    },
    cancel() {
      this.open = false;
      this.reset();
    },
    reset() {
      this.form = {
        dictCode: undefined,
        dictLabel: undefined,
        dictValue: undefined,
        cssClass: undefined,
        listClass: 'default',
        dictSort: 0,
        status: "0",
        remark: undefined
      };
      this.resetForm("form");
    },
    handleQuery() {
      this.queryParams.pageNum = 1;
      this.getList();
    },
    handleClose() {
      const obj = { path: "/system/dict" };
      this.$tab.closeOpenPage(obj);
    },
    resetQuery() {
      this.resetForm("queryForm");
      this.queryParams.dictType = this.defaultDictType;
      this.handleQuery();
    },
    handleAdd() {
      this.reset();
      this.open = true;
      this.title = "添加字典数据";
      this.form.dictType = this.queryParams.dictType;
    },
    handleSelectionChange(selection) {
      this.ids = selection.map(item => item.dictCode);
      this.single = selection.length != 1;
      this.multiple = !selection.length;
    },
    handleUpdate(row) {
      this.reset();
      const dictCode = row.dictCode || this.ids;
      getData(dictCode).then(response => {
        this.form = response.data;
        this.open = true;
        this.title = "修改字典数据";
      });
    },
    submitForm: function() {
      this.$refs["form"].validate(valid => {
        if (valid) {
          if (this.form.dictCode != undefined) {
            updateData(this.form).then(() => {
              this.$modal.msgSuccess("修改成功");
              this.open = false;
              this.getList();
            });
          } else {
            addData(this.form).then(() => {
              this.$modal.msgSuccess("新增成功");
              this.open = false;
              this.getList();
            });
          }
        }
      });
    },
    handleDelete(row) {
      const dictCodes = row.dictCode || this.ids;
      this.$modal.confirm('是否确认删除字典编码为"' + dictCodes + '"的数据项？').then(function() {
        return delData(dictCodes);
      }).then(() => {
        this.getList();
        this.$modal.msgSuccess("删除成功");
      }).catch(() => {});
    },
    handleExport() {
      this.download('system/dict/data/export', {
        ...this.queryParams
      }, `data_${new Date().getTime()}.xlsx`);
    }
  }
};
</script>
