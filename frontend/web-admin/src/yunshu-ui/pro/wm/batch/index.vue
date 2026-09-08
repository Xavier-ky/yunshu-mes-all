<template>
  <div class="app-container batch-command-center">
    <el-form :model="queryParams" ref="queryForm" size="small" :inline="true" v-show="showSearch" label-width="68px" class="batch-search-form">
      <el-form-item label="批次编号" prop="batchCode">
        <el-input
          v-model="queryParams.batchCode"
          placeholder="请输入批次编号"
          clearable
          @keyup.enter="handleQuery"
        />
      </el-form-item>
      <el-form-item label="产品物料ID" prop="itemId">
        <el-input
          v-model="queryParams.itemId"
          placeholder="请输入产品物料ID"
          clearable
          @keyup.enter="handleQuery"
        />
      </el-form-item>
      <el-form-item label="产品物料编码" prop="itemCode">
        <el-input
          v-model="queryParams.itemCode"
          placeholder="请输入产品物料编码"
          clearable
          @keyup.enter="handleQuery"
        />
      </el-form-item>
      <el-form-item label="产品物料名称" prop="itemName">
        <el-input
          v-model="queryParams.itemName"
          placeholder="请输入产品物料名称"
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
      <el-form-item label="生产日期" prop="produceDate">
        <el-date-picker clearable
          v-model="queryParams.produceDate"
          type="date"
          value-format="yyyy-MM-dd"
          placeholder="请选择生产日期">
        </el-date-picker>
      </el-form-item>
      <el-form-item label="有效期" prop="expireDate">
        <el-date-picker clearable
          v-model="queryParams.expireDate"
          type="date"
          value-format="yyyy-MM-dd"
          placeholder="请选择有效期">
        </el-date-picker>
      </el-form-item>
      <el-form-item label="入库日期" prop="recptDate">
        <el-date-picker clearable
          v-model="queryParams.recptDate"
          type="date"
          value-format="yyyy-MM-dd"
          placeholder="请选择入库日期">
        </el-date-picker>
      </el-form-item>
      <el-form-item label="供应商ID" prop="vendorId">
        <el-input
          v-model="queryParams.vendorId"
          placeholder="请输入供应商ID"
          clearable
          @keyup.enter="handleQuery"
        />
      </el-form-item>
      <el-form-item label="供应商编码" prop="vendorCode">
        <el-input
          v-model="queryParams.vendorCode"
          placeholder="请输入供应商编码"
          clearable
          @keyup.enter="handleQuery"
        />
      </el-form-item>
      <el-form-item label="供应商名称" prop="vendorName">
        <el-input
          v-model="queryParams.vendorName"
          placeholder="请输入供应商名称"
          clearable
          @keyup.enter="handleQuery"
        />
      </el-form-item>
      <el-form-item label="供应商简称" prop="vendorNick">
        <el-input
          v-model="queryParams.vendorNick"
          placeholder="请输入供应商简称"
          clearable
          @keyup.enter="handleQuery"
        />
      </el-form-item>
      <el-form-item label="客户ID" prop="clientId">
        <el-input
          v-model="queryParams.clientId"
          placeholder="请输入客户ID"
          clearable
          @keyup.enter="handleQuery"
        />
      </el-form-item>
      <el-form-item label="客户编码" prop="clientCode">
        <el-input
          v-model="queryParams.clientCode"
          placeholder="请输入客户编码"
          clearable
          @keyup.enter="handleQuery"
        />
      </el-form-item>
      <el-form-item label="客户名称" prop="clientName">
        <el-input
          v-model="queryParams.clientName"
          placeholder="请输入客户名称"
          clearable
          @keyup.enter="handleQuery"
        />
      </el-form-item>
      <el-form-item label="客户简称" prop="clientNick">
        <el-input
          v-model="queryParams.clientNick"
          placeholder="请输入客户简称"
          clearable
          @keyup.enter="handleQuery"
        />
      </el-form-item>
      <el-form-item label="销售订单编号" prop="coCode">
        <el-input
          v-model="queryParams.coCode"
          placeholder="请输入销售订单编号"
          clearable
          @keyup.enter="handleQuery"
        />
      </el-form-item>
      <el-form-item label="采购订单编号" prop="poCode">
        <el-input
          v-model="queryParams.poCode"
          placeholder="请输入采购订单编号"
          clearable
          @keyup.enter="handleQuery"
        />
      </el-form-item>
      <el-form-item label="生产工单ID" prop="workorderId">
        <el-input
          v-model="queryParams.workorderId"
          placeholder="请输入生产工单ID"
          clearable
          @keyup.enter="handleQuery"
        />
      </el-form-item>
      <el-form-item label="生产工单编码" prop="workorderCode">
        <el-input
          v-model="queryParams.workorderCode"
          placeholder="请输入生产工单编码"
          clearable
          @keyup.enter="handleQuery"
        />
      </el-form-item>
      <el-form-item label="生产任务ID" prop="taskId">
        <el-input
          v-model="queryParams.taskId"
          placeholder="请输入生产任务ID"
          clearable
          @keyup.enter="handleQuery"
        />
      </el-form-item>
      <el-form-item label="生产任务编号" prop="taskCode">
        <el-input
          v-model="queryParams.taskCode"
          placeholder="请输入生产任务编号"
          clearable
          @keyup.enter="handleQuery"
        />
      </el-form-item>
      <el-form-item label="工作站ID" prop="workstationId">
        <el-input
          v-model="queryParams.workstationId"
          placeholder="请输入工作站ID"
          clearable
          @keyup.enter="handleQuery"
        />
      </el-form-item>
      <el-form-item label="工作站编码" prop="workstationCode">
        <el-input
          v-model="queryParams.workstationCode"
          placeholder="请输入工作站编码"
          clearable
          @keyup.enter="handleQuery"
        />
      </el-form-item>
      <el-form-item label="工具ID" prop="toolId">
        <el-input
          v-model="queryParams.toolId"
          placeholder="请输入工具ID"
          clearable
          @keyup.enter="handleQuery"
        />
      </el-form-item>
      <el-form-item label="工具编号" prop="toolCode">
        <el-input
          v-model="queryParams.toolCode"
          placeholder="请输入工具编号"
          clearable
          @keyup.enter="handleQuery"
        />
      </el-form-item>
      <el-form-item label="模具ID" prop="moldId">
        <el-input
          v-model="queryParams.moldId"
          placeholder="请输入模具ID"
          clearable
          @keyup.enter="handleQuery"
        />
      </el-form-item>
      <el-form-item label="模具编号" prop="moldCode">
        <el-input
          v-model="queryParams.moldCode"
          placeholder="请输入模具编号"
          clearable
          @keyup.enter="handleQuery"
        />
      </el-form-item>
      <el-form-item label="生产批号" prop="productCode">
        <el-input
          v-model="queryParams.productCode"
          placeholder="请输入生产批号"
          clearable
          @keyup.enter="handleQuery"
        />
      </el-form-item>
      <el-form-item label="质量状态" prop="qualityStatus">
        <el-select v-model="queryParams.qualityStatus" placeholder="请选择质量状态" clearable>
          <el-option label="请选择字典生成" value="" />
        </el-select>
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
          v-hasPermi="['wm:batch:add']"
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
          v-hasPermi="['wm:batch:edit']"
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
          v-hasPermi="['wm:batch:remove']"
        >删除</el-button>
      </el-col>
      <el-col :span="1.5">
        <el-button
          type="warning"
          plain
          icon="el-icon-download"
          size="default"
          @click="handleExport"
          v-hasPermi="['wm:batch:export']"
        >导出</el-button>
      </el-col>
      <right-toolbar :show-search="showSearch" @update:showSearch="showSearch = $event" @queryTable="getList"></right-toolbar>
    </el-row>

    <el-table v-loading="loading" class="yunshu-data-table" stripe border :data="batchList" @selection-change="handleSelectionChange">
      <el-table-column type="selection" width="55" align="center" />
      <el-table-column label="批次ID" align="center" prop="batchId" />
      <el-table-column label="批次编号" align="center" prop="batchCode" />
      <el-table-column label="产品物料ID" align="center" prop="itemId" />
      <el-table-column label="产品物料编码" align="center" prop="itemCode" />
      <el-table-column label="产品物料名称" align="center" prop="itemName" />
      <el-table-column label="规格型号" align="center" prop="specification" />
      <el-table-column label="单位" align="center" prop="unitOfMeasure" />
      <el-table-column label="生产日期" align="center" prop="produceDate" width="180">
        <template #default="scope">
          <span>{{ parseTime(scope.row.produceDate, '{y}-{m}-{d}') }}</span>
        </template>
      </el-table-column>
      <el-table-column label="有效期" align="center" prop="expireDate" width="180">
        <template #default="scope">
          <span>{{ parseTime(scope.row.expireDate, '{y}-{m}-{d}') }}</span>
        </template>
      </el-table-column>
      <el-table-column label="入库日期" align="center" prop="recptDate" width="180">
        <template #default="scope">
          <span>{{ parseTime(scope.row.recptDate, '{y}-{m}-{d}') }}</span>
        </template>
      </el-table-column>
      <el-table-column label="供应商ID" align="center" prop="vendorId" />
      <el-table-column label="供应商编码" align="center" prop="vendorCode" />
      <el-table-column label="供应商名称" align="center" prop="vendorName" />
      <el-table-column label="供应商简称" align="center" prop="vendorNick" />
      <el-table-column label="客户ID" align="center" prop="clientId" />
      <el-table-column label="客户编码" align="center" prop="clientCode" />
      <el-table-column label="客户名称" align="center" prop="clientName" />
      <el-table-column label="客户简称" align="center" prop="clientNick" />
      <el-table-column label="销售订单编号" align="center" prop="coCode" />
      <el-table-column label="采购订单编号" align="center" prop="poCode" />
      <el-table-column label="生产工单ID" align="center" prop="workorderId" />
      <el-table-column label="生产工单编码" align="center" prop="workorderCode" />
      <el-table-column label="生产任务ID" align="center" prop="taskId" />
      <el-table-column label="生产任务编号" align="center" prop="taskCode" />
      <el-table-column label="工作站ID" align="center" prop="workstationId" />
      <el-table-column label="工作站编码" align="center" prop="workstationCode" />
      <el-table-column label="工具ID" align="center" prop="toolId" />
      <el-table-column label="工具编号" align="center" prop="toolCode" />
      <el-table-column label="模具ID" align="center" prop="moldId" />
      <el-table-column label="模具编号" align="center" prop="moldCode" />
      <el-table-column label="生产批号" align="center" prop="productCode" />
      <el-table-column label="质量状态" align="center" prop="qualityStatus" />
      <el-table-column label="操作" align="center" class-name="small-padding fixed-width">
        <template #default="scope">
          <el-button
            size="default"
            type="text"
            icon="el-icon-edit"
            @click="handleUpdate(scope.row)"
            v-hasPermi="['wm:batch:edit']"
          >修改</el-button>
          <el-button
            size="default"
            type="text"
            icon="el-icon-delete"
            @click="handleDelete(scope.row)"
            v-hasPermi="['wm:batch:remove']"
          >删除</el-button>
        </template>
      </el-table-column>
    </el-table>
    
    <pagination
      v-show="total>0"
      :total="total"
      :page.sync="queryParams.pageNum"
      :limit.sync="queryParams.pageSize"
      @pagination="getList"
    />

    <!-- 添加或修改批次记录对话框 -->
    <el-dialog :title="title" v-model="open" width="500px" append-to-body>
      <el-form ref="form" :model="form" :rules="rules" label-width="80px">
        <el-form-item label="批次编号" prop="batchCode">
          <el-input v-model="form.batchCode" placeholder="请输入批次编号" />
        </el-form-item>
        <el-form-item label="产品物料ID" prop="itemId">
          <el-input v-model="form.itemId" placeholder="请输入产品物料ID" />
        </el-form-item>
        <el-form-item label="产品物料编码" prop="itemCode">
          <el-input v-model="form.itemCode" placeholder="请输入产品物料编码" />
        </el-form-item>
        <el-form-item label="产品物料名称" prop="itemName">
          <el-input v-model="form.itemName" placeholder="请输入产品物料名称" />
        </el-form-item>
        <el-form-item label="规格型号" prop="specification">
          <el-input v-model="form.specification" type="textarea" placeholder="请输入内容" />
        </el-form-item>
        <el-form-item label="单位" prop="unitOfMeasure">
          <el-input v-model="form.unitOfMeasure" placeholder="请输入单位" />
        </el-form-item>
        <el-form-item label="生产日期" prop="produceDate">
          <el-date-picker clearable
            v-model="form.produceDate"
            type="date"
            value-format="yyyy-MM-dd"
            placeholder="请选择生产日期">
          </el-date-picker>
        </el-form-item>
        <el-form-item label="有效期" prop="expireDate">
          <el-date-picker clearable
            v-model="form.expireDate"
            type="date"
            value-format="yyyy-MM-dd"
            placeholder="请选择有效期">
          </el-date-picker>
        </el-form-item>
        <el-form-item label="入库日期" prop="recptDate">
          <el-date-picker clearable
            v-model="form.recptDate"
            type="date"
            value-format="yyyy-MM-dd"
            placeholder="请选择入库日期">
          </el-date-picker>
        </el-form-item>
        <el-form-item label="供应商ID" prop="vendorId">
          <el-input v-model="form.vendorId" placeholder="请输入供应商ID" />
        </el-form-item>
        <el-form-item label="供应商编码" prop="vendorCode">
          <el-input v-model="form.vendorCode" placeholder="请输入供应商编码" />
        </el-form-item>
        <el-form-item label="供应商名称" prop="vendorName">
          <el-input v-model="form.vendorName" placeholder="请输入供应商名称" />
        </el-form-item>
        <el-form-item label="供应商简称" prop="vendorNick">
          <el-input v-model="form.vendorNick" placeholder="请输入供应商简称" />
        </el-form-item>
        <el-form-item label="客户ID" prop="clientId">
          <el-input v-model="form.clientId" placeholder="请输入客户ID" />
        </el-form-item>
        <el-form-item label="客户编码" prop="clientCode">
          <el-input v-model="form.clientCode" placeholder="请输入客户编码" />
        </el-form-item>
        <el-form-item label="客户名称" prop="clientName">
          <el-input v-model="form.clientName" placeholder="请输入客户名称" />
        </el-form-item>
        <el-form-item label="客户简称" prop="clientNick">
          <el-input v-model="form.clientNick" placeholder="请输入客户简称" />
        </el-form-item>
        <el-form-item label="销售订单编号" prop="coCode">
          <el-input v-model="form.coCode" placeholder="请输入销售订单编号" />
        </el-form-item>
        <el-form-item label="采购订单编号" prop="poCode">
          <el-input v-model="form.poCode" placeholder="请输入采购订单编号" />
        </el-form-item>
        <el-form-item label="生产工单ID" prop="workorderId">
          <el-input v-model="form.workorderId" placeholder="请输入生产工单ID" />
        </el-form-item>
        <el-form-item label="生产工单编码" prop="workorderCode">
          <el-input v-model="form.workorderCode" placeholder="请输入生产工单编码" />
        </el-form-item>
        <el-form-item label="生产任务ID" prop="taskId">
          <el-input v-model="form.taskId" placeholder="请输入生产任务ID" />
        </el-form-item>
        <el-form-item label="生产任务编号" prop="taskCode">
          <el-input v-model="form.taskCode" placeholder="请输入生产任务编号" />
        </el-form-item>
        <el-form-item label="工作站ID" prop="workstationId">
          <el-input v-model="form.workstationId" placeholder="请输入工作站ID" />
        </el-form-item>
        <el-form-item label="工作站编码" prop="workstationCode">
          <el-input v-model="form.workstationCode" placeholder="请输入工作站编码" />
        </el-form-item>
        <el-form-item label="工具ID" prop="toolId">
          <el-input v-model="form.toolId" placeholder="请输入工具ID" />
        </el-form-item>
        <el-form-item label="工具编号" prop="toolCode">
          <el-input v-model="form.toolCode" placeholder="请输入工具编号" />
        </el-form-item>
        <el-form-item label="模具ID" prop="moldId">
          <el-input v-model="form.moldId" placeholder="请输入模具ID" />
        </el-form-item>
        <el-form-item label="模具编号" prop="moldCode">
          <el-input v-model="form.moldCode" placeholder="请输入模具编号" />
        </el-form-item>
        <el-form-item label="生产批号" prop="productCode">
          <el-input v-model="form.productCode" placeholder="请输入生产批号" />
        </el-form-item>
        <el-form-item label="质量状态">
          <el-radio-group v-model="form.qualityStatus">
            <el-radio label="1">请选择字典生成</el-radio>
          </el-radio-group>
        </el-form-item>
      </el-form>
      <div slot="footer" class="dialog-footer">
        <el-button type="primary" @click="submitForm">确 定</el-button>
        <el-button @click="cancel">取 消</el-button>
      </div>
    </el-dialog>
  </div>
</template>

<script>
import { listBatch, getBatch, delBatch, addBatch, updateBatch } from "@/yunshu-ui/api/mes/wm/batch";

export default {
  name: "Batch",
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
      // 批次记录表格数据
      batchList: [],
      // 弹出层标题
      title: "",
      // 是否显示弹出层
      open: false,
      // 查询参数
      queryParams: {
        pageNum: 1,
        pageSize: 10,
        batchCode: null,        itemId: null,        itemCode: null,        itemName: null,        specification: null,        unitOfMeasure: null,        produceDate: null,        expireDate: null,        recptDate: null,        vendorId: null,        vendorCode: null,        vendorName: null,        vendorNick: null,        clientId: null,        clientCode: null,        clientName: null,        clientNick: null,        coCode: null,        poCode: null,        workorderId: null,        workorderCode: null,        taskId: null,        taskCode: null,        workstationId: null,        workstationCode: null,        toolId: null,        toolCode: null,        moldId: null,        moldCode: null,        productCode: null,        qualityStatus: null,      },
      // 表单参数
      form: {},
      // 表单校验
      rules: {
        batchCode: [
          { required: true, message: "批次编号不能为空", trigger: "blur" }
        ],        itemId: [
          { required: true, message: "产品物料ID不能为空", trigger: "blur" }
        ],      }
    };
  },
  created() {
    this.getList();
  },
  methods: {
    /** 查询批次记录列表 */
    getList() {
      this.loading = true;
      listBatch(this.queryParams).then(response => {
        this.batchList = response.rows;
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
        batchId: null,        batchCode: null,        itemId: null,        itemCode: null,        itemName: null,        specification: null,        unitOfMeasure: null,        produceDate: null,        expireDate: null,        recptDate: null,        vendorId: null,        vendorCode: null,        vendorName: null,        vendorNick: null,        clientId: null,        clientCode: null,        clientName: null,        clientNick: null,        coCode: null,        poCode: null,        workorderId: null,        workorderCode: null,        taskId: null,        taskCode: null,        workstationId: null,        workstationCode: null,        toolId: null,        toolCode: null,        moldId: null,        moldCode: null,        productCode: null,        qualityStatus: "0",        remark: null,        attr1: null,        attr2: null,        attr3: null,        attr4: null,        createBy: null,        createTime: null,        updateBy: null,        updateTime: null      };
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
      this.ids = selection.map(item => item.batchId)
      this.single = selection.length!==1
      this.multiple = !selection.length
    },
    /** 新增按钮操作 */
    handleAdd() {
      this.reset();
      this.open = true;
      this.title = "添加批次记录";
    },
    /** 修改按钮操作 */
    handleUpdate(row) {
      this.reset();
      const batchId = row.batchId || this.ids
      getBatch(batchId).then(response => {
        this.form = response.data;
        this.open = true;
        this.title = "修改批次记录";
      });
    },
    /** 提交按钮 */
    submitForm() {
      this.$refs["form"].validate(valid => {
        if (valid) {
          if (this.form.batchId != null) {
            updateBatch(this.form).then(response => {
              this.$modal.msgSuccess("修改成功");
              this.open = false;
              this.getList();
            });
          } else {
            addBatch(this.form).then(response => {
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
      const batchIds = row.batchId || this.ids;
      this.$modal.confirm('是否确认删除批次记录编号为"' + batchIds + '"的数据项？').then(function() {
        return delBatch(batchIds);
      }).then(() => {
        this.getList();
        this.$modal.msgSuccess("删除成功");
      }).catch(() => {});
    },
    /** 导出按钮操作 */
    handleExport() {
      this.download('wm/batch/export', {
        ...this.queryParams
      }, `batch_${new Date().getTime()}.xlsx`)
    }
  }
};
</script>
