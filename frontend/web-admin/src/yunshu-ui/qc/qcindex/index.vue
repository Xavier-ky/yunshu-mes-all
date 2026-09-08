<template>
  <div class="app-container qc-hub-command-center">
    <section v-show="showSearch" class="workorders-filter-panel">
      <el-form :model="queryParams" ref="queryForm" :inline="true" class="workorders-filter-form" @submit.prevent>
        <el-form-item label="检测项编码" prop="indexCode">
          <el-input
            v-model="queryParams.indexCode"
            placeholder="请输入检测项编码"
            clearable
            @keyup.enter="handleQuery"
          />
        </el-form-item>
        <el-form-item label="检测项名称" prop="indexName">
          <el-input
            v-model="queryParams.indexName"
            placeholder="请输入检测项名称"
            clearable
            @keyup.enter="handleQuery"
          />
        </el-form-item>
        <el-form-item label="检测项类型" prop="indexType">
          <el-select v-model="queryParams.indexType" placeholder="请选择检测项类型" clearable>
            <el-option
              v-for="dict in dict.type.mes_index_type"
              :key="dict.value"
              :label="dict.label"
              :value="dict.value"
            />
          </el-select>
        </el-form-item>
        <el-form-item class="filter-actions">
          <el-button type="primary" icon="el-icon-search" size="small" @click="handleQuery">搜索</el-button>
          <el-button icon="el-icon-refresh" size="small" @click="resetQuery">重置</el-button>
        </el-form-item>
      </el-form>
    </section>

    <div class="workorders-toolbar">
      <div class="toolbar-left">
        <el-button
          type="primary"
          size="small"
          class="primary-action"
          icon="el-icon-plus"
          @click="handleAdd"
          v-hasPermi="['mes:qc:qcindex:add']"
        >新增</el-button>
        <el-button
          size="small"
          icon="el-icon-edit"
          :disabled="single"
          @click="handleUpdate"
          v-hasPermi="['mes:qc:qcindex:edit']"
        >修改</el-button>
        <el-button
          size="small"
          icon="el-icon-delete"
          :disabled="multiple"
          @click="handleDelete"
          v-hasPermi="['mes:qc:qcindex:remove']"
        >删除</el-button>
        <el-button
          size="small"
          icon="el-icon-download"
          @click="handleExport"
          v-hasPermi="['mes:qc:qcindex:export']"
        >导出</el-button>
        <span class="toolbar-divider"></span>
        <span class="result-count">共 <b>{{ total }}</b> 项检测项</span>
      </div>
      <div class="toolbar-right">
        <right-toolbar :show-search="showSearch" @update:showSearch="showSearch = $event" @queryTable="getList" />
      </div>
    </div>

    <div class="workorders-table-frame">
      <span class="frame-corner frame-corner--tl" aria-hidden="true"></span>
      <span class="frame-corner frame-corner--tr" aria-hidden="true"></span>
      <span class="frame-corner frame-corner--bl" aria-hidden="true"></span>
      <span class="frame-corner frame-corner--br" aria-hidden="true"></span>
      <el-table
        v-loading="loading"
        class="yunshu-data-table workorders-table"
        stripe
        border
        :data="qcindexList"
        @selection-change="handleSelectionChange"
      >
        <el-table-column type="selection" width="55" align="center" />
        <el-table-column label="检测项编码" min-width="140" align="center" header-align="center" prop="indexCode" show-overflow-tooltip />
        <el-table-column label="检测项名称" min-width="160" align="center" header-align="center" prop="indexName" show-overflow-tooltip />
        <el-table-column label="检测项类型" min-width="120" align="center" header-align="center" prop="indexType" show-overflow-tooltip>
          <template #default="scope">
            <dict-tag :options="dict.type.mes_index_type" :value="scope.row.indexType"/>
          </template>
        </el-table-column>
        <el-table-column label="检测工具" min-width="120" align="center" header-align="center" prop="qcTool" show-overflow-tooltip />
        <el-table-column label="检测值类型" min-width="120" align="center" header-align="center" prop="qcResultType" show-overflow-tooltip>
          <template #default="scope">
            <dict-tag :options="dict.type.mes_qc_result_type" :value="scope.row.qcResultType"/>
          </template>
        </el-table-column>
        <el-table-column label="操作" min-width="160" align="center" header-align="center" fixed="right" class-name="col-actions small-padding fixed-width">
          <template #default="scope">
            <div class="yunshu-row-actions">
              <el-button
                size="small"
                link
                icon="el-icon-edit"
                @click="handleUpdate(scope.row)"
                v-hasPermi="['mes:qc:qcindex:edit']"
              >修改</el-button>
              <el-button
                size="small"
                link
                icon="el-icon-delete"
                @click="handleDelete(scope.row)"
                v-hasPermi="['mes:qc:qcindex:remove']"
              >删除</el-button>
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
      @pagination="getList"
    />

    <!-- 添加或修改检测项对话框 -->
    <el-dialog :title="title" v-model="open" width="960px" append-to-body>
      <el-form ref="form" :model="form" :rules="rules" label-width="120px">
        <el-row>
          <el-col :span="8">
            <el-form-item label="检测项编码" prop="indexCode">
              <el-input v-model="form.indexCode" placeholder="请输入检测项编码" />
            </el-form-item>
          </el-col>
          <el-col :span="4">
            <el-form-item  label-width="80">
              <el-switch v-model="autoGenFlag"
                  active-color="#13ce66"
                  active-text="自动生成"
                  @change="handleAutoGenChange(autoGenFlag)" v-if="optType != 'view'">
              </el-switch>
            </el-form-item>
          </el-col>
          <el-col :span="12">
            <el-form-item label="检测项名称" prop="indexName">
              <el-input v-model="form.indexName" placeholder="请输入检测项名称" />
            </el-form-item>
          </el-col>
        </el-row>
        <el-row>
          <el-col :span="12">
            <el-form-item label="检测项类型" prop="indexType">
              <el-select v-model="form.indexType" placeholder="请选择检测项类型">
                <el-option
                  v-for="dict in dict.type.mes_index_type"
                  :key="dict.value"
                  :label="dict.label"
                  :value="dict.value"
                ></el-option>
              </el-select>
            </el-form-item>
          </el-col>
          <el-col :span="12">
            <el-form-item label="检测工具" prop="qcTool">
              <el-input v-model="form.qcTool" placeholder="请输入检测工具" />
            </el-form-item>
          </el-col>
        </el-row>
        <el-row>
          <el-col :span="12">
            <el-form-item label="检测值类型" prop="qcResultType">
              <el-select v-model="form.qcResultType" placeholder="请选择检测值类型">
                <el-option
                  v-for="dict in dict.type.mes_qc_result_type"
                  :key="dict.value"
                  :label="dict.label"
                  :value="dict.value"
                ></el-option>
              </el-select>
            </el-form-item>
          </el-col>
          <el-col :span="12">
            <el-form-item label="类型" v-if="form.qcResultType == 'FILE' " prop="qcResultSpc">
              <el-radio-group v-model="form.qcResultSpc" placeholder="请选择文件属性">
                <el-radio label="IMG">图片/照片</el-radio>
                <el-radio label="FILE">文件</el-radio>
              </el-radio-group>
            </el-form-item>
            <el-form-item label="字典项" v-else-if="form.qcResultType == 'DICT' " prop="qcResultSpc">
              <el-input v-model="form.qcResultSpc" readonly placeholder="请选择字典" >
                <el-button slot="append" @click="handleSelectDict" icon="el-icon-search"></el-button>
              </el-input>
            </el-form-item>
            <DictSelect ref="dictSelect" @onSelected="onDictSelected"></DictSelect>
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
      <template #footer><div class="dialog-footer">
        <el-button type="primary" @click="cancel" v-if="optType =='view'">返回</el-button>
        <el-button type="primary" @click="submitForm" v-else>确 定</el-button>
        <el-button @click="cancel">取 消</el-button>
      </div></template>
    </el-dialog>
  </div>
</template>

<script>
import { listQcindex, getQcindex, delQcindex, addQcindex, updateQcindex } from "@/yunshu-ui/api/mes/qc/qcindex";
import DictSelect from "@/yunshu-ui/components/DictSelect/index.vue";
import {genCode} from "@/yunshu-ui/api/system/autocode/rule"
export default {
  name: "Qcindex",
  dicts: ['mes_index_type','mes_qc_result_type'],
  components: {DictSelect},
  data() {
    return {
      //自动生成编码
      autoGenFlag:false,
      optType: undefined,
      // 遮罩层
      loading: true,
      // 选中数组
      ids: [],
      // 非单个禁用
      single: true,
      // 非多个禁用
      multiple: true,
      // 显示搜索条件
      showSearch: true,
      // 总条数
      total: 0,
      // 检测项表格数据
      qcindexList: [],
      // 弹出层标题
      title: "",
      // 是否显示弹出层
      open: false,
      // 查询参数
      queryParams: {
        pageNum: 1,
        pageSize: 10,
        indexCode: null,
        indexName: null,
        indexType: null,
        qcTool: null,
      },
      // 表单参数
      form: {},
      // 表单校验
      rules: {
        indexCode: [
          { required: true, message: "检测项编码不能为空", trigger: "blur" },
          { max: 64, message: "字段过长", trigger: "blur" }
        ],
        indexName: [
          { required: true, message: "检测项名称不能为空", trigger: "blur" },
          { max: 100, message: "字段过长", trigger: "blur" }
        ],
        qcTool: [
          { max: 100, message: "字段过长", trigger: "blur" }
        ],
        indexType: [
          { required: true, message: "检测项类型不能为空", trigger: "change" }
        ],
        qcResultType: [
          { required: true, message: "请选择检测项值类型", trigger: "change" }
        ],
        qcResultSpc: [
          { required: true, message: "请选择检测项值类型", trigger: "blur",
            validator: (rule, value, callback) => {
              if (this.form.qcResultType === 'FILE' && !value) {
                callback(new Error('文件类型不能为空'));
              } else if (this.form.qcResultType === 'DICT' && !value) {
                callback(new Error('字典项不能为空'));
              } else {
                callback();
              }
            }
          }
        ],
        remark: [
          { max: 250, message: '长度必须小于250个字符', trigger: 'blur' }
        ]
      }
    };
  },
  created() {
    this.getList();
  },
  methods: {
    /** 查询检测项列表 */
    getList() {
      this.loading = true;
      listQcindex(this.queryParams).then(response => {
        this.qcindexList = response.rows;
        this.total = response.total;
        this.loading = false;
      });
    },
    // 取消按钮
    cancel() {
      this.open = false;
      this.reset();
    },
    // 表单重置
    reset() {
      this.form = {
        indexId: null,
        indexCode: null,
        indexName: null,
        indexType: null,
        qcTool: null,
        qcResultType: null,
        qcResultSpc: null,
        remark: null,
        attr1: null,
        attr2: null,
        attr3: null,
        attr4: null,
        createBy: null,
        createTime: null,
        updateBy: null,
        updateTime: null
      };
      this.autoGenFlag = false;
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
      this.ids = selection.map(item => item.indexId)
      this.single = selection.length!==1
      this.multiple = !selection.length
    },
    /** 新增按钮操作 */
    handleAdd() {
      this.reset();
      this.open = true;
      this.title = "添加检测项";
      this.optType = "add";
    },
    // 查询明细按钮操作
    handleView(row){
      this.reset();
      const indexId = row.indexId || this.ids;
      getQcindex(indexId).then(response => {
        this.form = response.data;
        this.open = true;
        this.title = "查看检测项信息";
        this.optType = "view";
      });
    },
    /** 修改按钮操作 */
    handleUpdate(row) {
      this.reset();
      const indexId = row.indexId || this.ids
      getQcindex(indexId).then(response => {
        this.form = response.data;
        this.open = true;
        this.title = "修改检测项";
        this.optType = "edit";
      });
    },
    /** 提交按钮 */
    submitForm() {
      this.$refs["form"].validate(valid => {
        if (valid) {
          if (this.form.indexId != null) {
            updateQcindex(this.form).then(response => {
              this.$modal.msgSuccess("修改成功");
              this.open = false;
              this.getList();
            });
          } else {
            addQcindex(this.form).then(response => {
              this.$modal.msgSuccess("新增成功");
              this.open = false;
              this.getList();
            });
          }
        }
      });
    },
    /** 删除按钮操作 */
    handleDelete(row) {
      const indexIds = row.indexId || this.ids;
      this.$modal.confirm('是否确认删除检测项编号为"' + indexIds + '"的数据项？').then(function() {
        return delQcindex(indexIds);
      }).then(() => {
        this.getList();
        this.$modal.msgSuccess("删除成功");
      }).catch(() => {});
    },
    /** 导出按钮操作 */
    handleExport() {
      this.$modal.msgWarning('Excel 导出暂未对接，请使用列表数据或后续版本');
    },
    //字典选择弹出框
    handleSelectDict(){
      this.$refs.dictSelect.showFlag = true;
    },
    //字典选择返回
    onDictSelected(row){
      debugger;
      this.form.qcResultSpc=row.dictType;
    },
    //自动生成编码
    handleAutoGenChange(autoGenFlag){
      if(autoGenFlag){
        genCode('QC_INDEX_CODE').then(response =>{
          this.form.indexCode = response;
        });
      }else{
        this.form.indexCode = null;
      }
    }
  }
};
</script>
