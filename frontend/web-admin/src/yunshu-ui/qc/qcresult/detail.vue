<template>
  <div class="app-container">
    <el-form :model="queryParams" ref="queryForm" size="small" :inline="true" v-show="showSearch" label-width="68px">
      <el-form-item label="结果记录ID" prop="resultId">
        <el-input
          v-model="queryParams.resultId"
          placeholder="请输入结果记录ID"
          clearable
          @keyup.enter="handleQuery"
        />
      </el-form-item>
      <el-form-item label="检测项ID" prop="indexId">
        <el-input
          v-model="queryParams.indexId"
          placeholder="请输入检测项ID"
          clearable
          @keyup.enter="handleQuery"
        />
      </el-form-item>
      <el-form-item label="检测项类型" prop="indexType">
        <el-select v-model="queryParams.indexType" placeholder="请选择检测项类型" clearable>
          <el-option label="请选择字典生成" value="" />
        </el-select>
      </el-form-item>
      <el-form-item label="检测工具" prop="qcTool">
        <el-input
          v-model="queryParams.qcTool"
          placeholder="请输入检测工具"
          clearable
          @keyup.enter="handleQuery"
        />
      </el-form-item>
      <el-form-item label="标准值" prop="standerVal">
        <el-input
          v-model="queryParams.standerVal"
          placeholder="请输入标准值"
          clearable
          @keyup.enter="handleQuery"
        />
      </el-form-item>
      <el-form-item label="单位" prop="unitOfMeasure">
        <el-input
          v-model="queryParams.unitOfMeasure"
          placeholder="请输入单位"
          clearable
          @keyup.enter="handleQuery"
        />
      </el-form-item>
      <el-form-item label="误差上限" prop="thresholdMax">
        <el-input
          v-model="queryParams.thresholdMax"
          placeholder="请输入误差上限"
          clearable
          @keyup.enter="handleQuery"
        />
      </el-form-item>
      <el-form-item label="误差下限" prop="thresholdMin">
        <el-input
          v-model="queryParams.thresholdMin"
          placeholder="请输入误差下限"
          clearable
          @keyup.enter="handleQuery"
        />
      </el-form-item>
      <el-form-item label="质检值类型" prop="qcResultType">
        <el-select v-model="queryParams.qcResultType" placeholder="请选择质检值类型" clearable>
          <el-option label="请选择字典生成" value="" />
        </el-select>
      </el-form-item>
      <el-form-item label="值属性" prop="qcResultSpc">
        <el-input
          v-model="queryParams.qcResultSpc"
          placeholder="请输入值属性"
          clearable
          @keyup.enter="handleQuery"
        />
      </el-form-item>
      <el-form-item label="浮点值" prop="qcValFloat">
        <el-input
          v-model="queryParams.qcValFloat"
          placeholder="请输入浮点值"
          clearable
          @keyup.enter="handleQuery"
        />
      </el-form-item>
      <el-form-item label="整数" prop="qcValInteger">
        <el-input
          v-model="queryParams.qcValInteger"
          placeholder="请输入整数"
          clearable
          @keyup.enter="handleQuery"
        />
      </el-form-item>
      <el-form-item label="字典项" prop="qcValDict">
        <el-input
          v-model="queryParams.qcValDict"
          placeholder="请输入字典项"
          clearable
          @keyup.enter="handleQuery"
        />
      </el-form-item>
      <el-form-item label="预留字段1" prop="attr1">
        <el-input
          v-model="queryParams.attr1"
          placeholder="请输入预留字段1"
          clearable
          @keyup.enter="handleQuery"
        />
      </el-form-item>
      <el-form-item label="预留字段2" prop="attr2">
        <el-input
          v-model="queryParams.attr2"
          placeholder="请输入预留字段2"
          clearable
          @keyup.enter="handleQuery"
        />
      </el-form-item>
      <el-form-item label="预留字段3" prop="attr3">
        <el-input
          v-model="queryParams.attr3"
          placeholder="请输入预留字段3"
          clearable
          @keyup.enter="handleQuery"
        />
      </el-form-item>
      <el-form-item label="预留字段4" prop="attr4">
        <el-input
          v-model="queryParams.attr4"
          placeholder="请输入预留字段4"
          clearable
          @keyup.enter="handleQuery"
        />
      </el-form-item>
      <el-form-item>
        <el-button type="primary" icon="el-icon-search" size="default" @click="handleQuery">搜索</el-button>
        <el-button icon="el-icon-refresh" size="default" @click="resetQuery">重置</el-button>
      </el-form-item>
    </el-form>

    <el-row :gutter="10" class="mb8">
      <el-col :span="1.5">
        <el-button
          type="primary"
          plain
          icon="el-icon-plus"
          size="default"
          @click="handleAdd"
          v-hasPermi="['qc:qcresultdetail:add']"
        >新增</el-button>
      </el-col>
      <el-col :span="1.5">
        <el-button
          type="success"
          plain
          icon="el-icon-edit"
          size="default"
          :disabled="single"
          @click="handleUpdate"
          v-hasPermi="['qc:qcresultdetail:edit']"
        >修改</el-button>
      </el-col>
      <el-col :span="1.5">
        <el-button
          type="danger"
          plain
          icon="el-icon-delete"
          size="default"
          :disabled="multiple"
          @click="handleDelete"
          v-hasPermi="['qc:qcresultdetail:remove']"
        >删除</el-button>
      </el-col>
      <el-col :span="1.5">
        <el-button
          type="warning"
          plain
          icon="el-icon-download"
          size="default"
          @click="handleExport"
          v-hasPermi="['qc:qcresultdetail:export']"
        >导出</el-button>
      </el-col>
      <right-toolbar :showSearch="showSearch" @queryTable="getList"></right-toolbar>
    </el-row>

    <el-table v-loading="loading" :data="qcresultdetailList" @selection-change="handleSelectionChange">
      <el-table-column type="selection" width="55" align="center" />
      <el-table-column label="流水号" align="center" prop="detailId" />
      <el-table-column label="结果记录ID" align="center" prop="resultId" />
      <el-table-column label="检测项ID" align="center" prop="indexId" />
      <el-table-column label="检测项类型" align="center" prop="indexType" />
      <el-table-column label="检测工具" align="center" prop="qcTool" />
      <el-table-column label="检测要求" align="center" prop="checkMethod" />
      <el-table-column label="标准值" align="center" prop="standerVal" />
      <el-table-column label="单位" align="center" prop="unitOfMeasure" />
      <el-table-column label="误差上限" align="center" prop="thresholdMax" />
      <el-table-column label="误差下限" align="center" prop="thresholdMin" />
      <el-table-column label="质检值类型" align="center" prop="qcResultType" />
      <el-table-column label="值属性" align="center" prop="qcResultSpc" />
      <el-table-column label="浮点值" align="center" prop="qcValFloat" />
      <el-table-column label="整数" align="center" prop="qcValInteger" />
      <el-table-column label="文字" align="center" prop="qcValText" />
      <el-table-column label="字典项" align="center" prop="qcValDict" />
      <el-table-column label="文件" align="center" prop="qcValFile" />
      <el-table-column label="备注" align="center" prop="remark" />
      <el-table-column label="预留字段1" align="center" prop="attr1" />
      <el-table-column label="预留字段2" align="center" prop="attr2" />
      <el-table-column label="预留字段3" align="center" prop="attr3" />
      <el-table-column label="预留字段4" align="center" prop="attr4" />
      <el-table-column label="操作" align="center" class-name="small-padding fixed-width">
        <template #default="scope">
          <el-button
            size="default"
            type="text"
            icon="el-icon-edit"
            @click="handleUpdate(scope.row)"
            v-hasPermi="['qc:qcresultdetail:edit']"
          >修改</el-button>
          <el-button
            size="default"
            type="text"
            icon="el-icon-delete"
            @click="handleDelete(scope.row)"
            v-hasPermi="['qc:qcresultdetail:remove']"
          >删除</el-button>
        </template>
      </el-table-column>
    </el-table>
    
    <pagination
      v-show="total>0"
      :total="total"
      :page="queryParams.pageNum"
      :limit="queryParams.pageSize"
      @pagination="getList"
    />

    <!-- 添加或修改检测结果明细记录对话框 -->
    <el-dialog :title="title" v-model="open" width="500px" append-to-body>
      <el-form ref="form" :model="form" :rules="rules" label-width="80px">
        <el-form-item label="结果记录ID" prop="resultId">
          <el-input v-model="form.resultId" placeholder="请输入结果记录ID" />
        </el-form-item>
        <el-form-item label="检测项ID" prop="indexId">
          <el-input v-model="form.indexId" placeholder="请输入检测项ID" />
        </el-form-item>
        <el-form-item label="检测项类型" prop="indexType">
          <el-select v-model="form.indexType" placeholder="请选择检测项类型">
            <el-option label="请选择字典生成" value="" />
          </el-select>
        </el-form-item>
        <el-form-item label="检测工具" prop="qcTool">
          <el-input v-model="form.qcTool" placeholder="请输入检测工具" />
        </el-form-item>
        <el-form-item label="检测要求" prop="checkMethod">
          <el-input v-model="form.checkMethod" type="textarea" placeholder="请输入内容" />
        </el-form-item>
        <el-form-item label="标准值" prop="standerVal">
          <el-input v-model="form.standerVal" placeholder="请输入标准值" />
        </el-form-item>
        <el-form-item label="单位" prop="unitOfMeasure">
          <el-input v-model="form.unitOfMeasure" placeholder="请输入单位" />
        </el-form-item>
        <el-form-item label="误差上限" prop="thresholdMax">
          <el-input v-model="form.thresholdMax" placeholder="请输入误差上限" />
        </el-form-item>
        <el-form-item label="误差下限" prop="thresholdMin">
          <el-input v-model="form.thresholdMin" placeholder="请输入误差下限" />
        </el-form-item>
        <el-form-item label="质检值类型" prop="qcResultType">
          <el-select v-model="form.qcResultType" placeholder="请选择质检值类型">
            <el-option label="请选择字典生成" value="" />
          </el-select>
        </el-form-item>
        <el-form-item label="值属性" prop="qcResultSpc">
          <el-input v-model="form.qcResultSpc" placeholder="请输入值属性" />
        </el-form-item>
        <el-form-item label="浮点值" prop="qcValFloat">
          <el-input v-model="form.qcValFloat" placeholder="请输入浮点值" />
        </el-form-item>
        <el-form-item label="整数" prop="qcValInteger">
          <el-input v-model="form.qcValInteger" placeholder="请输入整数" />
        </el-form-item>
        <el-form-item label="文字" prop="qcValText">
          <el-input v-model="form.qcValText" type="textarea" placeholder="请输入内容" />
        </el-form-item>
        <el-form-item label="字典项" prop="qcValDict">
          <el-input v-model="form.qcValDict" placeholder="请输入字典项" />
        </el-form-item>
        <el-form-item label="文件">
          <file-upload v-model="form.qcValFile"/>
        </el-form-item>
        <el-form-item label="备注" prop="remark">
          <el-input v-model="form.remark" type="textarea" placeholder="请输入内容" />
        </el-form-item>
        <el-form-item label="预留字段1" prop="attr1">
          <el-input v-model="form.attr1" placeholder="请输入预留字段1" />
        </el-form-item>
        <el-form-item label="预留字段2" prop="attr2">
          <el-input v-model="form.attr2" placeholder="请输入预留字段2" />
        </el-form-item>
        <el-form-item label="预留字段3" prop="attr3">
          <el-input v-model="form.attr3" placeholder="请输入预留字段3" />
        </el-form-item>
        <el-form-item label="预留字段4" prop="attr4">
          <el-input v-model="form.attr4" placeholder="请输入预留字段4" />
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
import { listQcresultdetail, getQcresultdetail, delQcresultdetail, addQcresultdetail, updateQcresultdetail } from "@/yunshu-ui/api/mes/qc/qcresultdetail";

export default {
  name: "Qcresultdetail",
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
      // 检测结果明细记录表格数据
      qcresultdetailList: [],
      // 弹出层标题
      title: "",
      // 是否显示弹出层
      open: false,
      // 查询参数
      queryParams: {
        pageNum: 1,
        pageSize: 10,
        resultId: null,        indexId: null,        indexType: null,        qcTool: null,        checkMethod: null,        standerVal: null,        unitOfMeasure: null,        thresholdMax: null,        thresholdMin: null,        qcResultType: null,        qcResultSpc: null,        qcValFloat: null,        qcValInteger: null,        qcValText: null,        qcValDict: null,        qcValFile: null,        attr1: null,        attr2: null,        attr3: null,        attr4: null,      },
      // 表单参数
      form: {},
      // 表单校验
      rules: {
        resultId: [
          { required: true, message: "结果记录ID不能为空", trigger: "blur" }
        ],        indexId: [
          { required: true, message: "检测项ID不能为空", trigger: "blur" }
        ],        qcResultType: [
          { required: true, message: "质检值类型不能为空", trigger: "change" }
        ],      }
    };
  },
  created() {
    this.getList();
  },
  methods: {
    /** 查询检测结果明细记录列表 */
    getList() {
      this.loading = true;
      listQcresultdetail(this.queryParams).then(response => {
        this.qcresultdetailList = response.rows;
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
        detailId: null,        resultId: null,        indexId: null,        indexType: null,        qcTool: null,        checkMethod: null,        standerVal: null,        unitOfMeasure: null,        thresholdMax: null,        thresholdMin: null,        qcResultType: null,        qcResultSpc: null,        qcValFloat: null,        qcValInteger: null,        qcValText: null,        qcValDict: null,        qcValFile: null,        remark: null,        attr1: null,        attr2: null,        attr3: null,        attr4: null,        createBy: null,        createTime: null,        updateBy: null,        updateTime: null      };
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
      this.ids = selection.map(item => item.detailId)
      this.single = selection.length!==1
      this.multiple = !selection.length
    },
    /** 新增按钮操作 */
    handleAdd() {
      this.reset();
      this.open = true;
      this.title = "添加检测结果明细记录";
    },
    /** 修改按钮操作 */
    handleUpdate(row) {
      this.reset();
      const detailId = row.detailId || this.ids
      getQcresultdetail(detailId).then(response => {
        this.form = response.data;
        this.open = true;
        this.title = "修改检测结果明细记录";
      });
    },
    /** 提交按钮 */
    submitForm() {
      this.$refs["form"].validate(valid => {
        if (valid) {
          if (this.form.detailId != null) {
            updateQcresultdetail(this.form).then(response => {
              this.$modal.msgSuccess("修改成功");
              this.open = false;
              this.getList();
            });
          } else {
            addQcresultdetail(this.form).then(response => {
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
      const detailIds = row.detailId || this.ids;
      this.$modal.confirm('是否确认删除检测结果明细记录编号为"' + detailIds + '"的数据项？').then(function() {
        return delQcresultdetail(detailIds);
      }).then(() => {
        this.getList();
        this.$modal.msgSuccess("删除成功");
      }).catch(() => {});
    },
    /** 导出按钮操作 */
    handleExport() {
      this.$modal.msgWarning('Excel 导出暂未对接，请使用列表数据或后续版本');
    }
  }
};
</script>
