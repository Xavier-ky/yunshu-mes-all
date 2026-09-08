<template>
  <div class="app-container qc-hub-command-center">
    <section v-show="showSearch" class="workorders-filter-panel">
      <el-form :model="queryParams" ref="queryForm" :inline="true" class="workorders-filter-form" @submit.prevent>
        <el-form-item label="方案编号" prop="templateCode">
          <el-input
            v-model="queryParams.templateCode"
            placeholder="请输入方案编号"
            clearable
            @keyup.enter="handleQuery"
          />
        </el-form-item>
        <el-form-item label="方案名称" prop="templateName">
          <el-input
            v-model="queryParams.templateName"
            placeholder="请输入方案名称"
            clearable
            @keyup.enter="handleQuery"
          />
        </el-form-item>
        <el-form-item label="业务类型" prop="qcTypeFilter">
          <el-select v-model="qcTypeFilter" placeholder="请选择业务类型" clearable>
            <el-option
              v-for="dict in dict.type.mes_qc_type"
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
          v-hasPermi="['mes:qc:qctemplate:add']"
        >新增</el-button>
        <el-button
          size="small"
          icon="el-icon-edit"
          :disabled="single"
          @click="handleUpdate"
          v-hasPermi="['mes:qc:qctemplate:edit']"
        >修改</el-button>
        <el-button
          size="small"
          icon="el-icon-delete"
          :disabled="multiple"
          @click="handleDelete"
          v-hasPermi="['mes:qc:qctemplate:remove']"
        >删除</el-button>
        <span class="toolbar-divider"></span>
        <span class="result-count">共 <b>{{ total }}</b> 项模板</span>
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
        :data="qctemplateList"
        @selection-change="handleSelectionChange"
      >
        <el-table-column type="selection" width="55" align="center" />
        <el-table-column label="方案编号" min-width="148" align="center" header-align="center" prop="templateCode" fixed="left">
          <template #default="scope">
            <el-button
              size="small"
              link
              @click="handleView(scope.row)"
              v-hasPermi="['mes:qc:qctemplate:query']"
            >{{ scope.row.templateCode }}</el-button>
          </template>
        </el-table-column>
        <el-table-column label="方案名称" min-width="160" align="center" header-align="center" prop="templateName" show-overflow-tooltip />
        <el-table-column label="业务类型" min-width="120" align="center" header-align="center" prop="qcTypesParam" show-overflow-tooltip>
          <template #default="scope">
            <dict-tag :options="dict.type.mes_qc_detail_type" :value="scope.row.qcTypesParam"/>
          </template>
        </el-table-column>
        <el-table-column label="是否启用" min-width="100" align="center" header-align="center" prop="enableFlag" show-overflow-tooltip>
          <template #default="scope">
            <dict-tag :options="dict.type.sys_yes_no" :value="scope.row.enableFlag"/>
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
                v-hasPermi="['mes:qc:qctemplate:edit']"
              >修改</el-button>
              <el-button
                size="small"
                link
                icon="el-icon-delete"
                @click="handleDelete(scope.row)"
                v-hasPermi="['mes:qc:qctemplate:remove']"
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

    <!-- 添加或修改检测模板对话框 -->
    <el-dialog :title="title" v-model="open" width="960px" append-to-body>
      <el-form ref="form" :model="form" :rules="rules" label-width="100px">
        <el-row>
          <el-col :span="8">
            <el-form-item label="方案编号" prop="templateCode">
              <el-input v-model="form.templateCode" placeholder="请输入方案编号" />
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
            <el-form-item label="方案名称" prop="templateName">
              <el-input v-model="form.templateName" placeholder="请输入方案名称" />
            </el-form-item>
          </el-col>
        </el-row>
        <el-row>
          <el-col :span="18">
            <el-form-item label="业务类型" prop="qcTypesParam">
              <el-checkbox-group v-model="form.qcTypesParam">
                <el-checkbox v-for="dict in dict.type.mes_qc_detail_type"
                      :key="dict.value"
                      :label="dict.value"
                      :value="dict.value">{{dict.label}}</el-checkbox>
              </el-checkbox-group>
            </el-form-item>
          </el-col>
          <el-col :span="6">
            <el-form-item label="是否启用" prop="enableFlag">
              <el-radio-group v-model="form.enableFlag" disabled v-if="optType=='view'">
                <el-radio
                  v-for="dict in dict.type.sys_yes_no"
                  :key="dict.value"
                  :label="dict.value"
                >{{dict.label}}</el-radio>
              </el-radio-group>
              <el-radio-group v-model="form.enableFlag" v-else>
                <el-radio
                  v-for="dict in dict.type.sys_yes_no"
                  :key="dict.value"
                  :label="dict.value"
                >{{dict.label}}</el-radio>
              </el-radio-group>
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
      <el-tabs type="border-card" v-if="form.templateId != null">
        <el-tab-pane label="检测项">
          <TemplateIndex ref="indexTab" :templateId="form.templateId" :optType="optType"></TemplateIndex>
        </el-tab-pane>
        <el-tab-pane label="物料产品">
          <TemplateProduct ref="productTab" :templateId="form.templateId" :optType="optType"></TemplateProduct>
        </el-tab-pane>
      </el-tabs>

      <template #footer><div class="dialog-footer">
        <el-button type="primary" @click="submitForm" v-if="optType !='view'">确 定</el-button>
        <el-button @click="cancel">取 消</el-button>
      </div></template>
    </el-dialog>
  </div>
</template>

<script>
import { listQctemplate, getQctemplate, delQctemplate, addQctemplate, updateQctemplate } from "@/yunshu-ui/api/mes/qc/qctemplate";
import {genCode} from "@/yunshu-ui/api/system/autocode/rule"
import TemplateIndex from "./templateindex.vue"
import TemplateProduct from "./templateproduct.vue";
export default {
  name: "Qctemplate",
  dicts: ['sys_yes_no','mes_qc_detail_type'],
  components: {TemplateIndex,TemplateProduct},
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
      qcTypeFilter: null,
      // 总条数
      total: 0,
      // 检测模板表格数据
      qctemplateList: [],
      // 弹出层标题
      title: "",
      // 是否显示弹出层
      open: false,
      // 查询参数
      queryParams: {
        pageNum: 1,
        pageSize: 10,
        templateCode: null,
        templateName: null,
        itemCode:null,
        itemName:null,
        qcTypesParam: [],
        enableFlag: null,
      },
      // 表单参数
      form: {
      },
      // 表单校验
      rules: {
        templateCode: [
          { required: true, message: "方案编号不能为空", trigger: "blur" },
          { max: 64, message: "字段过长", trigger: "blur" }
        ],
        templateName: [
          { required: true, message: "方案名称不能为空", trigger: "blur" },
          { max: 100, message: "字段过长", trigger: "blur" }
        ],
        qcTypes: [
          { required: true, message: "业务类型不能为空", trigger: "blur" }
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
    /** 查询检测模板列表 */
    getList() {
      this.loading = true;
      listQctemplate(this.queryParams).then(response => {
        this.qctemplateList = response.rows;
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
        templateId: null,
        templateCode: null,
        templateName: null,
        qcTypesParam: [],
        enableFlag: 'Y',
        remark: null,
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
      this.queryParams.qcTypesParam = this.qcTypeFilter ? [this.qcTypeFilter] : [];
      this.getList();
    },
    /** 重置按钮操作 */
    resetQuery() {
      this.qcTypeFilter = null;
      this.resetForm("queryForm");
      this.handleQuery();
    },
    // 多选框选中数据
    handleSelectionChange(selection) {
      this.ids = selection.map(item => item.templateId)
      this.single = selection.length!==1
      this.multiple = !selection.length
    },
    /** 新增按钮操作 */
    handleAdd() {
      this.reset();
      this.open = true;
      this.title = "添加质检方案";
      this.optType = "add";
    },
    // 查询明细按钮操作
    handleView(row){
      this.reset();
      const templateId = row.templateId || this.ids;
      getQctemplate(templateId).then(response => {
        this.form = response.data;
        this.open = true;
        this.title = "查看质检方案信息";
        this.optType = "view";
      });
    },
    /** 修改按钮操作 */
    handleUpdate(row) {
      this.reset();
      const templateId = row.templateId || this.ids
      getQctemplate(templateId).then(response => {
        debugger;
        this.form = response.data;
        this.open = true;
        this.title = "修改质检方案";
        this.optType = "edit";
      });
    },
    /** 提交按钮 */
    submitForm() {
      this.$refs["form"].validate(valid => {
        if (valid) {
          if (this.form.templateId != null) {
            updateQctemplate(this.form).then(response => {
              this.$modal.msgSuccess("修改成功");
              this.open = false;
              this.getList();
            });
          } else {
            addQctemplate(this.form).then(response => {
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
      const templateIds = row.templateId || this.ids;
      this.$modal.confirm('是否确认删除质检方案编号为"' + templateIds + '"的数据项？').then(function() {
        return delQctemplate(templateIds);
      }).then(() => {
        this.getList();
        this.$modal.msgSuccess("删除成功");
      }).catch(() => {});
    },
    //自动生成编码
    handleAutoGenChange(autoGenFlag){
      if(autoGenFlag){
        genCode('QC_TEMPLATE_CODE').then(response =>{
          this.form.templateCode = response;
        });
      }else{
        this.form.templateCode = null;
      }
    }
  }
};
</script>
