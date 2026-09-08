<template>
  <div class="app-container inbound-doc-panel md-doc-panel">
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
        <el-form-item prop="measureCode">
          <el-input
            v-model="queryParams.measureCode"
            placeholder="单位编码"
            clearable
            @keyup.enter="handleQuery"
          />
        </el-form-item>
        <el-form-item prop="measureName">
          <el-input
            v-model="queryParams.measureName"
            placeholder="单位名称"
            clearable
            @keyup.enter="handleQuery"
          />
        </el-form-item>
        <el-form-item class="filter-actions">
          <el-button type="primary" icon="el-icon-search" size="default" @click="handleQuery">搜索</el-button>
          <el-button icon="el-icon-refresh" size="default" @click="resetQuery">重置</el-button>
        </el-form-item>
      </el-form>
    </section>

    <div class="inbound-toolbar">
      <div class="inbound-toolbar-left">
        <span class="inbound-page-title">计量单位</span>
        <el-button type="primary" plain icon="el-icon-plus" size="default" @click="handleAdd" v-hasPermi="['mes:md:unitmeasure:add']">新增</el-button>
        <el-button type="success" plain icon="el-icon-edit" size="default" :disabled="single" @click="handleUpdate" v-hasPermi="['mes:md:unitmeasure:edit']">修改</el-button>
        <el-button type="danger" plain icon="el-icon-delete" size="default" :disabled="multiple" @click="handleDelete" v-hasPermi="['mes:md:unitmeasure:remove']">删除</el-button>
        <el-button type="warning" plain icon="el-icon-download" size="default" @click="handleExport" v-hasPermi="['mes:md:unitmeasure:export']">导出</el-button>
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
        class="yunshu-data-table inbound-table md-doc-table"
        stripe
        border
        height="100%"
        :data="unitmeasureList"
        @selection-change="handleSelectionChange"
      >
      <el-table-column type="selection" width="42" align="center" header-align="center" />
      <el-table-column label="单位编码" align="center" header-align="center" min-width="120" prop="measureCode" show-overflow-tooltip />
      <el-table-column label="单位名称" align="center" header-align="center" min-width="120" prop="measureName" show-overflow-tooltip />
      <el-table-column label="是否是主单位" align="center" header-align="center" width="120" prop="primaryFlag" >
        <template #default="scope">
          <dict-tag :options="dict.type.sys_yes_no" :value="scope.row.primaryFlag"/>
        </template>
      </el-table-column>
      <el-table-column label="与主单位换算比例" align="center" header-align="center" min-width="140" prop="changeRate" />
      <el-table-column label="是否启用" align="center" header-align="center" width="100" prop="enableFlag" >
        <template #default="scope">
          <dict-tag :options="dict.type.sys_yes_no" :value="scope.row.enableFlag"/>
        </template>
      </el-table-column>
      <el-table-column label="备注" align="center" header-align="center" min-width="120" prop="remark" show-overflow-tooltip />
      <el-table-column label="操作" align="center" header-align="center" min-width="140" class-name="col-actions small-padding fixed-width">
        <template #default="scope">
          <div class="yunshu-row-actions">
          <el-button
            size="small"
            link
            icon="el-icon-edit"
            @click="handleUpdate(scope.row)"
            v-hasPermi="['mes:md:unitmeasure:edit']"
          >修改</el-button>
          <el-button
            size="small"
            link
            icon="el-icon-delete"
            @click="handleDelete(scope.row)"
            v-hasPermi="['mes:md:unitmeasure:remove']"
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
      @update:page="queryParams.pageNum = $event"
      @update:limit="queryParams.pageSize = $event"
      @pagination="getList"
    />

    <!-- 添加或修改单位对话框 -->
    <el-dialog :title="title" v-model="open" width="800px" append-to-body>
      <el-form ref="form" :model="form" :rules="rules" label-width="150px">
        <el-form-item label="单位编码" prop="measureCode">
          <el-input v-model="form.measureCode" placeholder="请输入单位编码" />
        </el-form-item>
        <el-form-item label="单位名称" prop="measureName">
          <el-input v-model="form.measureName" placeholder="请输入单位名称" />
        </el-form-item>
        <el-form-item label="是否是主单位" prop="primaryFlag">
           <el-radio-group v-model="form.primaryFlag">
                <el-radio
                  v-for="dict in dict.type.sys_yes_no"
                  :key="dict.value"
                  :label="dict.value"
                >{{dict.label}}</el-radio>
            </el-radio-group>
        </el-form-item>
        <el-form-item label="主单位" v-if="form.primaryFlag =='N'">
          <el-select v-model="form.primaryId" placeholder="请选择主单位">
              <el-option
                  v-for="item in measureOptions"
                  :key="item.measureId"
                  :label="item.measureName"
                  :value="item.measureId"
                  :disabled="item.enableFlag == 'N'"
                ></el-option>
              </el-select>
        </el-form-item>
        <el-form-item label="与主单位换算比例" prop="changeRate" v-if="form.primaryFlag =='N'">
          <el-input-number :step="1" :percision="2" v-model="form.changeRate" placeholder="请输入与主单位换算比例" />
        </el-form-item>
        <el-form-item label="是否启用" prop="enableFlag">
            <el-radio-group v-model="form.enableFlag">
                <el-radio
                  v-for="dict in dict.type.sys_yes_no"
                  :key="dict.value"
                  :label="dict.value"
                >{{dict.label}}</el-radio>
            </el-radio-group>
        </el-form-item>
        <el-form-item label="备注" prop="remark">
          <el-input v-model="form.remark" type="textarea" placeholder="请输入内容" />
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
import { listUnitmeasure,listPrimaryUnitmeasure, getUnitmeasure, delUnitmeasure, addUnitmeasure, updateUnitmeasure } from "@/yunshu-ui/api/mes/md/unitmeasure";

export default {
  name: "Unitmeasure",
  dicts: ['sys_yes_no'],
  data() {
    return {
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
      // 单位表格数据
      unitmeasureList: [],
      //主单位列表
      measureOptions: [],
      // 弹出层标题
      title: "",
      // 是否显示弹出层
      open: false,
      // 查询参数
      queryParams: {
        pageNum: 1,
        pageSize: 10,
        measureCode: null,
        measureName: null,
        primaryFlag: null,
        primaryId: null,
        changeRate: null,
        enableFlag: null
      },
      // 表单参数
      form: {},
      // 表单校验
      rules: {
        measureCode: [
          { required: true, message: "单位编码不能为空", trigger: "blur" },
          { max: 64, message: "字段过长", trigger: "blur" }
        ],
        measureName: [
          { required: true, message: "单位名称不能为空", trigger: "blur" },
          { max: 100, message: "字段过长", trigger: "blur" }
        ],
        primaryFlag: [
          { required: true, message: "是否是主单位不能为空", trigger: "blur" }
        ],
        enableFlag: [
          { required: true, message: "是否启用不能为空", trigger: "blur" }
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
    /** 查询单位列表 */
    getList() {
      this.loading = true;
      listUnitmeasure(this.queryParams).then(response => {
        this.unitmeasureList = response.rows;
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
        measureId: null,
        measureCode: null,
        measureName: null,
        primaryFlag: null,
        primaryId: null,
        changeRate: null,
        enableFlag: 'Y',
        remark: null
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
      this.ids = selection.map(item => item.measureId)
      this.single = selection.length!==1
      this.multiple = !selection.length
    },
    /** 新增按钮操作 */
    handleAdd() {
      this.reset();
      listPrimaryUnitmeasure().then(response =>{
        this.measureOptions = response.data;
      })
      this.open = true;
      this.title = "添加单位";
    },
    /** 修改按钮操作 */
    handleUpdate(row) {
      this.reset();
      listPrimaryUnitmeasure().then(response =>{
        this.measureOptions = response.data;
      })
      const measureId = row.measureId || this.ids
      getUnitmeasure(measureId).then(response => {
        this.form = response.data;
        this.open = true;
        this.title = "修改单位";
      });
    },
    /** 提交按钮 */
    submitForm() {
      this.$refs["form"].validate(valid => {
        if (valid) {
          if (this.form.measureId != null) {
            updateUnitmeasure(this.form).then(response => {
              this.$modal.msgSuccess("修改成功");
              this.open = false;
              this.getList();
            });
          } else {
            addUnitmeasure(this.form).then(response => {
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
      const measureIds = row.measureId || this.ids;
      this.$modal.confirm('是否确认删除单位编号为"' + measureIds + '"的数据项？').then(function() {
        return delUnitmeasure(measureIds);
      }).then(() => {
        this.getList();
        this.$modal.msgSuccess("删除成功");
      }).catch(() => {});
    },
    /** 导出按钮操作 */
    handleExport() {
      this.download('mes/md/unitmeasure/export', {
        ...this.queryParams
      }, `unitmeasure_${new Date().getTime()}.xlsx`)
    }
  }
};
</script>
