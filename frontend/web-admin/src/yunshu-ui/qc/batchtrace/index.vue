<template>
  <div class="app-container inbound-doc-panel">
    <section v-show="showSearch" class="inbound-filter-panel">
      <el-form :model="queryParams" ref="queryForm" size="default" :inline="true" class="inbound-filter-form" label-width="0" @submit.prevent>
        <el-form-item prop="batchCode">
          <el-input v-model="queryParams.batchCode" placeholder="批次号" clearable @keyup.enter="handleQuery" />
        </el-form-item>
        <el-form-item prop="itemCode">
          <el-input v-model="queryParams.itemCode" placeholder="产品物料编码" clearable @keyup.enter="handleQuery" />
        </el-form-item>
        <el-form-item prop="itemName">
          <el-input v-model="queryParams.itemName" placeholder="产品物料名称" clearable @keyup.enter="handleQuery" />
        </el-form-item>
        <el-form-item prop="unitOfMeasure">
          <el-input v-model="queryParams.unitOfMeasure" placeholder="单位" clearable @keyup.enter="handleQuery" />
        </el-form-item>
        <el-form-item prop="vendorCode">
          <el-input v-model="queryParams.vendorCode" placeholder="供应商编码" clearable @keyup.enter="handleQuery" />
        </el-form-item>
        <el-form-item prop="vendorName">
          <el-input v-model="queryParams.vendorName" placeholder="供应商名称" clearable @keyup.enter="handleQuery" />
        </el-form-item>
        <el-form-item prop="clientCode">
          <el-input v-model="queryParams.clientCode" placeholder="客户编码" clearable @keyup.enter="handleQuery" />
        </el-form-item>
        <el-form-item prop="clientName">
          <el-input v-model="queryParams.clientName" placeholder="客户名称" clearable @keyup.enter="handleQuery" />
        </el-form-item>
        <el-form-item prop="coCode">
          <el-input v-model="queryParams.coCode" placeholder="销售订单编号" clearable @keyup.enter="handleQuery" />
        </el-form-item>
        <el-form-item prop="poCode">
          <el-input v-model="queryParams.poCode" placeholder="采购订单编号" clearable @keyup.enter="handleQuery" />
        </el-form-item>
        <el-form-item class="filter-actions">
          <el-button type="primary" icon="el-icon-search" size="default" @click="handleQuery">搜索</el-button>
          <el-button icon="el-icon-refresh" size="default" @click="resetQuery">重置</el-button>
        </el-form-item>
      </el-form>
    </section>

    <div class="inbound-toolbar">
      <div class="inbound-toolbar-left">
        <span class="inbound-page-title">批次追溯</span>
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
      <el-table
        v-loading="loading"
        class="yunshu-data-table inbound-table"
        stripe
        border
        height="100%"
        :data="batchList"
        @selection-change="handleSelectionChange"
      >
        <el-table-column type="selection" width="42" align="center" header-align="center" />
        <el-table-column label="批次编号" align="center" header-align="center" min-width="130" prop="batchCode" show-overflow-tooltip />
        <el-table-column label="产品物料编码" align="center" header-align="center" min-width="130" prop="itemCode" show-overflow-tooltip />
        <el-table-column label="产品物料名称" align="center" header-align="center" min-width="140" prop="itemName" show-overflow-tooltip />
        <el-table-column label="规格型号" align="center" header-align="center" min-width="120" prop="specification" show-overflow-tooltip />
        <el-table-column label="单位" align="center" header-align="center" width="80" prop="unitName" />
        <el-table-column label="供应商编码" align="center" header-align="center" min-width="120" prop="vendorCode" show-overflow-tooltip />
        <el-table-column label="供应商名称" align="center" header-align="center" min-width="130" prop="vendorName" show-overflow-tooltip />
        <el-table-column label="客户编码" align="center" header-align="center" min-width="120" prop="clientCode" show-overflow-tooltip />
        <el-table-column label="客户名称" align="center" header-align="center" min-width="130" prop="clientName" show-overflow-tooltip />
        <el-table-column label="销售订单编号" align="center" header-align="center" min-width="130" prop="coCode" show-overflow-tooltip />
        <el-table-column label="采购订单编号" align="center" header-align="center" min-width="130" prop="poCode" show-overflow-tooltip />
        <el-table-column label="操作" min-width="120" align="center" header-align="center" class-name="col-actions small-padding fixed-width">
          <template #default="scope">
            <div class="yunshu-row-actions">
              <el-button size="small" link icon="el-icon-aim" @click="handleTrace(scope.row)" v-hasPermi="['mes:wm:batch:query']">批次追溯</el-button>
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

    <!-- 添加或修改批次记录对话框 -->
    <el-dialog :title="title" v-model="open" width="960px" append-to-body>
      <el-form ref="form" :model="form" :rules="rules" label-width="100px">
        <el-row>
          <el-col :span="8">
            <el-form-item label="批次编号" prop="batchCode">
              <el-input v-model="form.batchCode" readonly="readonly" />
            </el-form-item>
          </el-col>
          <el-col :span="8">
            <el-form-item label="物资编码" prop="itemCode">
              <el-input v-model="form.itemCode" readonly="readonly" />
            </el-form-item>
          </el-col>
          <el-col :span="8">
            <el-form-item label="物资名称" prop="itemName">
              <el-input v-model="form.itemName" readonly="readonly" />
            </el-form-item>
          </el-col>
        </el-row>
        <el-row>
          <el-col :span="24">
            <el-form-item label="规格型号" prop="specification">
              <el-input v-model="form.specification" type="textarea" readonly="readonly" />
            </el-form-item>
          </el-col>
        </el-row>
        <el-row>
          <el-col :span="8">
            <el-form-item label="采购订单编号" prop="poCode">
              <el-input v-model="form.poCode" readonly="readonly" />
            </el-form-item>
          </el-col>
          <el-col :span="8">
            <el-form-item label="供应商编码" prop="vendorCode">
              <el-input v-model="form.vendorCode" readonly="readonly" />
            </el-form-item>
          </el-col>
          <el-col :span="8">
            <el-form-item label="供应商名称" prop="vendorName">
              <el-input v-model="form.vendorName" readonly="readonly" />
            </el-form-item>
          </el-col>
        </el-row>
        <el-row>
          <el-col :span="8">
            <el-form-item label="销售订单编号" prop="coCode">
              <el-input v-model="form.coCode" readonly="readonly" />
            </el-form-item>
          </el-col>
          <el-col :span="8">
            <el-form-item label="客户编码" prop="clientCode">
              <el-input v-model="form.clientCode" readonly="readonly" />
            </el-form-item>
          </el-col>
          <el-col :span="8">
            <el-form-item label="客户名称" prop="clientName">
              <el-input v-model="form.clientName" readonly="readonly" />
            </el-form-item>
          </el-col>
        </el-row>
        <el-row>
          <el-col :span="8">
            <el-form-item label="生产批号" prop="productCode">
              <el-input v-model="form.productCode" readonly="readonly" />
            </el-form-item>
          </el-col>
          <el-col :span="8">
            <el-form-item label="生产工单" prop="workorderCode">
              <el-input v-model="form.workorderCode" readonly="readonly" />
            </el-form-item>
          </el-col>
          <el-col :span="8">
            <el-form-item label="工作站编码" prop="workstationCode">
              <el-input v-model="form.workstationCode" readonly="readonly" />
            </el-form-item>
          </el-col>
        </el-row>
      </el-form>
      <el-tabs type="border-card">
        <el-tab-pane label="向前追溯">
          <forward :batchId="form.batchId" :batchCode="form.batchCode"></forward>
        </el-tab-pane>
        <el-tab-pane label="向后追溯">
          <backward :batchId="form.batchId" :batchCode="form.batchCode"></backward>
        </el-tab-pane>
      </el-tabs>
      <template #footer><div class="dialog-footer">
        <el-button @click="cancel">关 闭</el-button>
      </div></template>
    </el-dialog>
  </div>
</template>

<script>
import { listBatch, getBatch, delBatch, addBatch, updateBatch } from "@/yunshu-ui/api/mes/wm/batch";
import forward from "./forward.vue";
import backward from "./backward.vue";
export default {
  name: "Batch",
  components: {forward,backward},
  props: {
    embedMode: { type: Boolean, default: false },
  },
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
    const batch = this.$route?.query?.batch;
    if (batch) {
      this.queryParams.batchCode = String(batch);
    }
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

    /** 追溯按钮操作 */
    handleTrace(row) {
      this.reset();
      const batchId = row.batchId || this.ids
      getBatch(batchId).then(response => {
        this.form = response.data;
        this.open = true;
        this.title = "批次追溯";
      });
    }    
  }
};
</script>
