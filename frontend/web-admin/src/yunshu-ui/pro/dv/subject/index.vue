<template>
  <div class="inbound-doc-panel dv-doc-panel">
    <section v-show="showSearch" class="inbound-filter-panel">
      <el-form :model="queryParams" ref="queryForm" size="small" :inline="true" class="inbound-filter-form" label-width="0" @submit.prevent>
        <el-form-item prop="subjectCode">
          <el-input v-model="queryParams.subjectCode" placeholder="项目编码" clearable @keyup.enter="handleQuery" />
        </el-form-item>
        <el-form-item prop="subjectName">
          <el-input v-model="queryParams.subjectName" placeholder="项目名称" clearable @keyup.enter="handleQuery" />
        </el-form-item>
        <el-form-item prop="subjectType">
          <el-select v-model="queryParams.subjectType" placeholder="项目类型" clearable>
            <el-option v-for="dict in dict.type.mes_dvsubject_type" :key="dict.value" :label="dict.label" :value="dict.value" />
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
        <span class="inbound-page-title">项目库</span>
        <el-button type="primary" plain icon="el-icon-plus" size="default" @click="handleAdd" v-hasPermi="['mes:dv:dvsubject:add']">新增</el-button>
        <el-button type="success" plain icon="el-icon-edit" size="default" :disabled="single" @click="handleUpdate" v-hasPermi="['mes:dv:dvsubject:edit']">修改</el-button>
        <el-button type="danger" plain icon="el-icon-delete" size="default" :disabled="multiple" @click="handleDelete" v-hasPermi="['mes:dv:dvsubject:remove']">删除</el-button>
        <el-button type="warning" plain icon="el-icon-download" size="default" @click="handleExport" v-hasPermi="['mes:dv:dvsubject:export']">导出</el-button>
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
      <el-table v-loading="loading" class="yunshu-data-table inbound-table" stripe border height="100%" :data="dvsubjectList" @selection-change="handleSelectionChange">
        <el-table-column type="selection" width="42" align="center" header-align="center" />
        <el-table-column label="项目编码" align="center" header-align="center" min-width="120" prop="subjectCode">
          <template #default="scope">
            <button type="button" class="doc-link-btn" @click.stop.prevent="handleUpdate(scope.row)">{{ scope.row.subjectCode }}</button>
          </template>
        </el-table-column>
        <el-table-column label="项目类型" align="center" header-align="center" width="100" prop="subjectType">
          <template #default="scope">
            <dict-tag :options="dict.type.mes_dvsubject_type" :value="scope.row.subjectType"/>
          </template>
        </el-table-column>
        <el-table-column label="项目名称" align="center" header-align="center" min-width="120" prop="subjectName" show-overflow-tooltip />
        <el-table-column label="项目内容" align="center" header-align="center" prop="subjectContent" min-width="140" show-overflow-tooltip />
        <el-table-column label="标准" align="center" header-align="center" prop="subjectStandard" min-width="120" show-overflow-tooltip />
        <el-table-column label="是否启用" align="center" header-align="center" width="96" prop="enableFlag">
          <template #default="scope">
            <dict-tag :options="dict.type.sys_yes_no" :value="scope.row.enableFlag"/>
          </template>
        </el-table-column>
        <el-table-column label="备注" align="center" header-align="center" prop="remark" min-width="100" show-overflow-tooltip />
        <el-table-column label="操作" align="center" header-align="center" min-width="130" class-name="col-actions small-padding fixed-width">
          <template #default="scope">
            <div class="yunshu-row-actions">
              <el-button size="small" link icon="el-icon-edit" @click="handleUpdate(scope.row)" v-hasPermi="['mes:dv:dvsubject:edit']">修改</el-button>
              <el-button size="small" link icon="el-icon-delete" @click="handleDelete(scope.row)" v-hasPermi="['mes:dv:dvsubject:remove']">删除</el-button>
            </div>
          </template>
        </el-table-column>
      </el-table>
    </div>

    <pagination
      v-show="total>0"
      :total="total"
      :page="queryParams.pageNum" @update:page="queryParams.pageNum = $event"
      :limit="queryParams.pageSize" @update:limit="queryParams.pageSize = $event"
      @pagination="getList"
    />

    <!-- 添加或修改设备点检保养项目对话框 -->
    <el-dialog :title="title" v-model="open" width="960px" append-to-body>
      <el-form ref="form" :model="form" :rules="rules" label-width="100px">
        <el-row>
          <el-col :span="8">
            <el-form-item label="项目编码" prop="subjectCode">
              <el-input v-model="form.subjectCode" placeholder="请输入项目编码" />
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
            <el-form-item label="项目名称" prop="subjectName">
              <el-input v-model="form.subjectName" placeholder="请输入项目名称" />
            </el-form-item>
          </el-col>
        </el-row>
        <el-row>
          <el-col :span="12">
            <el-form-item label="项目类型" prop="subjectType">
              <el-radio-group v-model="form.subjectType">
                <el-radio
                  v-for="dict in dict.type.mes_dvsubject_type"
                  :key="dict.value"
                  :label="dict.value"
                >{{dict.label}}</el-radio>
              </el-radio-group>
            </el-form-item>
          </el-col>
          <el-col :span="12">
            <el-form-item label="是否启用">
              <el-radio-group v-model="form.enableFlag">
                <el-radio
                  v-for="dict in dict.type.sys_yes_no"
                  :key="dict.value"
                  :label="dict.value"
                >{{dict.label}}</el-radio>
              </el-radio-group>
            </el-form-item>
          </el-col>
        </el-row>
        <el-form-item label="项目内容" prop="subjectContent">
          <el-input type="textarea" v-model="form.subjectContent" placeholder="请输入项目内容" />
        </el-form-item>
        <el-form-item label="标准" prop="subjectStandard">
          <el-input type="textarea" v-model="form.subjectStandard" placeholder="请输入标准" autosize/>
        </el-form-item>
      </el-form>
      <template #footer><div class="dialog-footer">
        <el-button type="primary" @click="submitForm">确 定</el-button>
        <el-button @click="cancel">取 消</el-button>
      </div></template>
    </el-dialog>
  </div>
</template>

<script>
import { listDvsubject, getDvsubject, delDvsubject, addDvsubject, updateDvsubject } from "@/yunshu-ui/api/mes/dv/dvsubject";
import {genCode} from "@/yunshu-ui/api/system/autocode/rule"
export default {
  name: "Dvsubject",
  dicts: ['sys_yes_no', 'mes_dvsubject_type'],
  data() {
    return {
      autoGenFlag: false,
      // 遮罩层
      loading: true,
      // 选中数组s
      ids: [],
      // 非单个禁用
      single: true,
      // 非多个禁用
      multiple: true,
      // 显示搜索条件
      showSearch: true,
      // 总条数
      total: 0,
      // 设备点检保养项目表格数据
      dvsubjectList: [],
      // 弹出层标题
      title: "",
      // 是否显示弹出层
      open: false,
      // 查询参数
      queryParams: {
        pageNum: 1,
        pageSize: 10,
        subjectCode: null,
        subjectName: null,
        subjectType: null,
        subjectContent: null,
        subjectStandard: null,
        enableFlag: null,
      },
      // 表单参数
      form: {},
      // 表单校验
      rules: {
        subjectCode: [
          { required: true, message: "项目编码不能为空", trigger: "blur" },
          { max: 64, message: "字段过长", trigger: "blur" }
        ],
        subjectType: [
          { required: true, message: "请选择项目类型", trigger: "blur" }
        ],
        subjectName: [
          { max: 100, message: "字段过长", trigger: "blur" }
        ],
        subjectContent: [
          { required: true, message: "项目内容不能为空", trigger: "blur" },
          { max: 250, message: "字段过长", trigger: "blur" }
        ],
        enableFlag: [
          { required: true, message: "是否启用不能为空", trigger: "blur" }
        ],
      }
    };
  },
  created() {
    this.getList();
  },
  methods: {
    /** 查询设备点检保养项目列表 */
    getList() {
      this.loading = true;
      listDvsubject(this.queryParams).then(response => {
        this.dvsubjectList = response.rows;
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
        subjectId: null,
        subjectCode: null,
        subjectName: null,
        subjectType: null,
        subjectContent: null,
        subjectStandard: null,
        enableFlag: "Y",
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
      this.ids = selection.map(item => item.subjectId)
      this.single = selection.length!==1
      this.multiple = !selection.length
    },
    /** 新增按钮操作 */
    handleAdd() {
      this.reset();
      this.open = true;
      this.title = "添加设备点检保养项目";
    },
    /** 修改按钮操作 */
    handleUpdate(row) {
      this.reset();
      const subjectId = row.subjectId || this.ids
      getDvsubject(subjectId).then(response => {
        this.form = response.data;
        this.open = true;
        this.title = "修改设备点检保养项目";
      });
    },
    /** 提交按钮 */
    submitForm() {
      this.$refs["form"].validate(valid => {
        if (valid) {
          if (this.form.subjectId != null) {
            updateDvsubject(this.form).then(response => {
              this.$modal.msgSuccess("修改成功");
              this.open = false;
              this.getList();
            });
          } else {
            addDvsubject(this.form).then(response => {
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
      const subjectIds = row.subjectId || this.ids;
      this.$modal.confirm('是否确认删除设备点检保养项目编号为"' + subjectIds + '"的数据项？').then(function() {
        return delDvsubject(subjectIds);
      }).then(() => {
        this.getList();
        this.$modal.msgSuccess("删除成功");
      }).catch(() => {});
    },
    /** 导出按钮操作 */
    handleExport() {
      this.download('mes/dv/dvsubject/export', {
        ...this.queryParams
      }, `dvsubject_${new Date().getTime()}.xlsx`)
    },
        //自动生成编码
    handleAutoGenChange(autoGenFlag){
      if(autoGenFlag){
        genCode('SUBJECT_CODE').then(response =>{
          this.form.subjectCode = response;
        });
      }else{
        this.form.subjectCode = null;
      }
    }
  }
};
</script>
