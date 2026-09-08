<template>
  <div :class="rootClass">
    <section v-show="showSearch || embedMode" class="inbound-filter-panel" :class="{ 'sys-settings-embed-filter': embedMode, 'sys-settings-embed-filter--compact': embedMode && !showSearch }">
      <div v-if="embedMode" class="sys-settings-embed-filter__stack">
        <el-form v-show="showSearch" ref="queryForm" :model="queryParams" size="default" :inline="true" class="inbound-filter-form sys-settings-embed-filter__fields" label-width="0" @submit.prevent>
          <el-form-item prop="partCode">
            <el-input v-model="queryParams.partCode" placeholder="组成编码" clearable @keyup.enter="handleQuery" />
          </el-form-item>
          <el-form-item prop="partName">
            <el-input v-model="queryParams.partName" placeholder="组成名称" clearable @keyup.enter="handleQuery" />
          </el-form-item>
        </el-form>
        <div class="sys-settings-embed-filter__actions sys-settings-embed-filter__actions--split">
          <div class="sys-settings-embed-filter__actions-left">
            <el-button type="primary" icon="el-icon-search" size="default" @click="handleQuery">搜索</el-button>
            <el-button icon="el-icon-refresh" size="default" @click="resetQuery">重置</el-button>
            <span class="sys-settings-embed-filter__count">共 {{ total }} 条</span>
          </div>
          <div class="sys-settings-embed-filter__actions-right">
            <el-button type="primary" plain icon="el-icon-plus" size="default" @click="handleAdd" v-hasPermi="['system:autocode:part:add']">新增</el-button>
            <el-button type="success" plain icon="el-icon-edit" size="default" :disabled="single" @click="handleUpdate" v-hasPermi="['system:autocode:part:edit']">修改</el-button>
            <el-button type="danger" plain icon="el-icon-delete" size="default" :disabled="multiple" @click="handleDelete" v-hasPermi="['system:autocode:part:remove']">删除</el-button>
          </div>
        </div>
      </div>
      <el-form v-else ref="queryForm" :model="queryParams" size="default" :inline="true" class="inbound-filter-form" label-width="0" @submit.prevent>
        <el-form-item prop="partCode">
          <el-input v-model="queryParams.partCode" placeholder="组成编码" clearable @keyup.enter="handleQuery" />
        </el-form-item>
        <el-form-item prop="partName">
          <el-input v-model="queryParams.partName" placeholder="组成名称" clearable @keyup.enter="handleQuery" />
        </el-form-item>
        <el-form-item class="filter-actions">
          <el-button type="primary" icon="el-icon-search" size="default" @click="handleQuery">搜索</el-button>
          <el-button icon="el-icon-refresh" size="default" @click="resetQuery">重置</el-button>
        </el-form-item>
      </el-form>
    </section>

    <div v-if="!embedMode" class="inbound-toolbar">
      <div class="inbound-toolbar-left">
        <span class="inbound-page-title">规则组成管理</span>
        <el-button type="primary" plain icon="el-icon-plus" size="default" @click="handleAdd" v-hasPermi="['system:autocode:part:add']">新增</el-button>
        <el-button type="success" plain icon="el-icon-edit" size="default" :disabled="single" @click="handleUpdate" v-hasPermi="['system:autocode:part:edit']">修改</el-button>
        <el-button type="danger" plain icon="el-icon-delete" size="default" :disabled="multiple" @click="handleDelete" v-hasPermi="['system:autocode:part:remove']">删除</el-button>
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
        :data="partList"
        @selection-change="handleSelectionChange"
      >
        <el-table-column type="selection" width="42" align="center" />
        <el-table-column label="组成编码" align="center" prop="partCode" min-width="110" show-overflow-tooltip />
        <el-table-column label="组成名称" align="center" prop="partName" min-width="110" show-overflow-tooltip />
        <el-table-column label="分段序号" align="center" prop="partIndex" min-width="90" />
        <el-table-column label="分段类型" align="center" prop="partType" min-width="100">
          <template #default="scope">
            <dict-tag :options="dict.type.sys_autocode_parttype" :value="scope.row.partType" />
          </template>
        </el-table-column>
        <el-table-column label="分段长度" align="center" prop="partLength" min-width="90" />
        <el-table-column label="备注" align="center" prop="remark" min-width="120" show-overflow-tooltip />
        <el-table-column label="创建时间" align="center" prop="createTime" min-width="160">
          <template #default="scope">
            <span>{{ parseTime(scope.row.createTime) }}</span>
          </template>
        </el-table-column>
        <el-table-column label="操作" align="center" min-width="140" class-name="col-actions">
          <template #default="scope">
            <div class="yunshu-row-actions">
              <el-button size="small" link icon="el-icon-edit" @click="handleUpdate(scope.row)" v-hasPermi="['system:autocode:part:edit']">修改</el-button>
              <el-button size="small" link icon="el-icon-delete" @click="handleDelete(scope.row)" v-hasPermi="['system:autocode:part:remove']">删除</el-button>
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

    <el-dialog :title="title" v-model="open" class="access-form-dialog" width="900px" append-to-body align-center destroy-on-close>
      <el-form ref="form" :model="form" :rules="rules" label-width="100px" class="access-form-dialog__form">
        <el-row>
          <el-col :span="12">
            <el-form-item label="分段编码" prop="partCode">
              <el-input v-model="form.partCode" placeholder="请输入分段编码" />
            </el-form-item>
          </el-col>
          <el-col :span="12">
            <el-form-item label="分段名称" prop="partName">
              <el-input v-model="form.partName" placeholder="请输入分段名称" />
            </el-form-item>
          </el-col>
        </el-row>
        <el-row>
          <el-col :span="12">
            <el-form-item label="分段序号" prop="partIndex">
              <el-input-number v-model="form.partIndex" placeholder="请输入分段序号" />
            </el-form-item>
          </el-col>
          <el-col :span="12">
            <el-form-item label="分段长度" prop="partLength">
              <el-input-number v-model="form.partLength" placeholder="请输入分段长度" />
            </el-form-item>
          </el-col>
        </el-row>
        <el-row>
          <el-col :span="12">
            <el-form-item label="分段类型" prop="partType">
              <el-select v-model="form.partType">
                <el-option v-for="item in dict.type.sys_autocode_parttype" :key="item.value" :label="item.label" :value="item.value" />
              </el-select>
            </el-form-item>
          </el-col>
          <el-col :span="12">
            <el-form-item label="日期时间格式" prop="dateFormat" v-if="form.partType == 'NOWDATE'">
              <el-input v-model="form.dateFormat" placeholder="请输入日期时间格式" />
            </el-form-item>
            <el-form-item label="输入字符" prop="inputCharacter" v-if="form.partType == 'INPUTCHAR'">
              <el-input v-model="form.inputCharacter" placeholder="请填写输入字符" />
            </el-form-item>
            <el-form-item label="固定字符" prop="fixCharacter" v-if="form.partType == 'FIXCHAR'">
              <el-input v-model="form.fixCharacter" placeholder="请填写固定字符" />
            </el-form-item>
            <el-form-item label="起始流水号" prop="seriaStartNo" v-if="form.partType == 'SERIALNO'">
              <el-input-number v-model="form.seriaStartNo" placeholder="请填写起始流水号" />
            </el-form-item>
          </el-col>
        </el-row>
        <el-row>
          <el-col :span="12">
            <el-form-item label="流水号步长" prop="seriaStep" v-if="form.partType == 'SERIALNO'">
              <el-input-number v-model="form.seriaStep" placeholder="请填写流水号步长" />
            </el-form-item>
          </el-col>
          <el-col :span="12">
            <el-form-item label="是否循环" prop="cycleFlag" v-if="form.partType == 'SERIALNO'">
              <el-radio-group v-model="form.cycleFlag">
                <el-radio label="Y">是</el-radio>
                <el-radio label="N">否</el-radio>
              </el-radio-group>
            </el-form-item>
          </el-col>
        </el-row>
        <el-row>
          <el-col :span="12">
            <el-form-item label="循环方式" prop="cycleMethod" v-if="form.partType == 'SERIALNO' && form.cycleFlag == 'Y'">
              <el-select v-model="form.cycleMethod">
                <el-option v-for="item in dict.type.sys_autocode_cyclemethod" :key="item.value" :label="item.label" :value="item.value" />
              </el-select>
            </el-form-item>
          </el-col>
        </el-row>
        <el-row>
          <el-col :span="24">
            <el-form-item label="备注" prop="remark">
              <el-input v-model="form.remark" type="textarea" placeholder="请输入内容" />
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
import { listPart, getPart, delPart, addPart, updatePart } from "@/yunshu-ui/api/system/autocode/part";

export default {
  name: "Part",
  dicts: ['sys_autocode_cyclemethod', 'sys_autocode_parttype'],
  props: {
    embedRuleId: { type: [String, Number], default: null },
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
      partList: [],
      title: "",
      open: false,
      recentRuleId: undefined,
      queryParams: {
        pageNum: 1,
        pageSize: 10,
        ruleId: undefined,
        partCode: undefined,
        partName: undefined
      },
      form: {},
      rules: {
        partCode: [
          { required: true, message: "组成编码不能为空", trigger: "blur" }
        ],
        partName: [
          { required: true, message: "组成名称不能为空", trigger: "blur" }
        ],
        partIndex: [
          { required: true, message: "组成序号不能为空", trigger: "blur" }
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
    embedRuleId() {
      this.bootstrap();
    },
  },
  methods: {
    bootstrap() {
      const ruleId = this.embedRuleId || (this.$route.params && this.$route.params.ruleId);
      this.recentRuleId = ruleId;
      this.queryParams.ruleId = ruleId;
      if (ruleId) {
        this.getList();
      } else if (this.embedMode) {
        this.loading = false;
        this.partList = [];
        this.total = 0;
      }
    },
    getList() {
      this.loading = true;
      listPart(this.queryParams).then(response => {
        this.partList = response.rows;
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
        partCode: undefined,
        partName: undefined,
        partIndex: 0,
        partLength: 0,
        remark: undefined
      };
      this.resetForm("form");
    },
    handleQuery() {
      this.queryParams.pageNum = 1;
      this.getList();
    },
    handleClose() {
      const obj = { path: "/system/autocodeRule" };
      this.$tab.closeOpenPage(obj);
    },
    resetQuery() {
      this.resetForm("queryForm");
      this.queryParams.ruleId = this.recentRuleId;
      this.handleQuery();
    },
    handleAdd() {
      this.reset();
      this.open = true;
      this.title = "添加规则组成";
      this.form.ruleId = this.recentRuleId;
    },
    handleSelectionChange(selection) {
      this.ids = selection.map(item => item.partId);
      this.single = selection.length != 1;
      this.multiple = !selection.length;
    },
    handleUpdate(row) {
      this.reset();
      const partId = row.partId || this.ids;
      getPart(partId).then(response => {
        this.form = response.data;
        this.open = true;
        this.title = "修改规则组成";
      });
    },
    submitForm: function() {
      this.$refs["form"].validate(valid => {
        if (valid) {
          if (this.form.partId != undefined) {
            updatePart(this.form).then(() => {
              this.$modal.msgSuccess("修改成功");
              this.open = false;
              this.getList();
            });
          } else {
            addPart(this.form).then(() => {
              this.$modal.msgSuccess("新增成功");
              this.open = false;
              this.getList();
            });
          }
        }
      });
    },
    handleDelete(row) {
      const partIds = row.partId || this.ids;
      this.$modal.confirm('是否确认删除规则组成ID为"' + partIds + '"的数据项？').then(function() {
        return delPart(partIds);
      }).then(() => {
        this.getList();
        this.$modal.msgSuccess("删除成功");
      }).catch(() => {});
    }
  }
};
</script>
