<template>
  <div class="app-container">

    <el-table v-loading="loading" :data="issuelineList" @selection-change="handleSelectionChange">
      <el-table-column type="expand" style="overflow-x: auto; width: 100%;">
        <template #default="props">
          <el-table :data="props.row.details" class="child-table" style="margin-left: 60px;">            
            <el-table-column label="批次号" width="120px" align="center" prop="batchCode" />
            <el-table-column label="仓库名称" width="120px" align="center" prop="warehouseName" />
            <el-table-column label="库区名称" width="120px" align="center" prop="locationName" />
            <el-table-column label="库位名称" width="150px" align="center" prop="areaName" />  
            <el-table-column label="数量" width="120px" align="center" prop="quantity" />            
            <el-table-column label="操作" align="center" width="100px" v-if="optType != 'view'" class-name="small-padding fixed-width">
              <template #default="scope">
                <el-button
                  size="default"
                  type="text"
                  icon="el-icon-edit"
                  @click="handleUpdate(scope.row)"
                  v-hasPermi="['mes:wm:issueheader:edit']"
                >修改</el-button>
                <el-button
                  size="default"
                  type="text"
                  icon="el-icon-delete"
                  @click="handleDelete(scope.row)"
                  v-hasPermi="['mes:wm:issueheader:remove']"
                >删除</el-button>
              </template>
            </el-table-column>
          </el-table>
        </template>
      </el-table-column>
      <el-table-column label="产品物料编码" width="120px" align="center" prop="itemCode" />
      <el-table-column label="产品物料名称" width="120px"  align="center" prop="itemName" :show-overflow-tooltip="true"/>
      <el-table-column label="规格型号" align="center" prop="specification" :show-overflow-tooltip="true"/>
      <el-table-column label="单位" align="center" prop="unitName" />
      <el-table-column label="领料数量" align="center" prop="quantityIssued" />
      <el-table-column label="备注" align="center" prop="remark" :show-overflow-tooltip="true"/>
      <el-table-column label="操作" align="center" v-if="optType != 'view'" width="100px" class-name="small-padding fixed-width">
        <template #default="scope">
          <el-button
            size="default"
            type="text"
            icon="el-icon-shopping-cart-full"
            @click="handleAdd(scope.row)"
            v-hasPermi="['mes:wm:issueheader:edit']"
          >拣货</el-button>
        </template>
      </el-table-column>
    </el-table>

    <pagination
      v-show="total>0"
      :total="total"
      :page="queryParams.pageNum"
      :limit="queryParams.pageSize"
      @update:page="queryParams.pageNum = $event"
      @update:limit="queryParams.pageSize = $event"
      @pagination="getListWithDetail"
    />

    <!-- 添加或修改生产领料单明细对话框 -->
    <el-dialog :title="title" v-model="open" width="960px" append-to-body @opened="onPickDialogOpened">
      <el-form ref="form" :model="form" :rules="rules" label-width="100px">
        <el-row>
          <el-col :span="8">
              <el-form-item label="物资编码" prop="itemCode">
                  <el-input v-model="form.itemCode" readonly="readonly" placeholder="请选择产品物料编码" >
                  </el-input>
              </el-form-item>
          </el-col>
          <el-col :span="8">
            <el-form-item label="物资名称" prop="itemName">
              <el-input v-model="form.itemName" readonly="readonly"/>
            </el-form-item>
          </el-col>
          <el-col :span="8">
            <el-form-item label="单位" prop="unitName">
              <el-input v-model="form.unitName" readonly="readonly" />
            </el-form-item>
          </el-col>
        </el-row>       
        <el-row>
          <el-col :span="24">
            <el-form-item label="规格型号" prop="specification">
              <el-input v-model="form.specification" readonly="readonly" />
            </el-form-item>
          </el-col>          
        </el-row>
        <el-row>
            <el-col :span="8">
              <el-form-item label="仓库" prop="warehouseName">
                <el-input v-model="form.warehouseName" readonly placeholder="点击右侧搜索选择库存，自动带出仓库/库区/库位" >
                  <template #append><el-button @click="handleSelectStock" icon="el-icon-search" title="选择库存"></el-button></template>
                </el-input>
              </el-form-item>
            </el-col>
            <el-col :span="8">
              <el-form-item label="库区" prop="locationName">
                <el-input v-model="form.locationName" readonly="readonly" />
              </el-form-item>
            </el-col>
            <el-col :span="8">
              <el-form-item label="库位" prop="areaName">
                <el-input v-model="form.areaName" readonly="readonly" />
              </el-form-item>
            </el-col>
        </el-row>
        <el-row>
          <el-col :span="8">
            <el-form-item label="批次号" prop="batchCode">
              <el-input v-model="form.batchCode" readonly="readonly" />
            </el-form-item>
          </el-col>
          <el-col :span="8">
            <el-form-item label="数量" prop="quantity">
              <el-input-number v-model="form.quantity" :max="form.quantityMax" :min="0" placeholder="请输入入库数量" />
            </el-form-item>
          </el-col>
        </el-row>
      </el-form>
      <template #footer><div class="dialog-footer">
        <el-button type="primary" @click="submitForm">确 定</el-button>
        <el-button @click="cancel">取 消</el-button>
      </div></template>
    </el-dialog>

    <StockSelect
      ref="stockSelect"
      :itemId="form.itemId"
      :itemCode="form.itemCode"
      @onSelected="onStockSelected"
    />
  </div>
</template>

<script>
import { listIssuedetail, getIssuedetail, delIssuedetail, addIssuedetail, updateIssuedetail } from "@/yunshu-ui/api/mes/wm/issuedetail";
import { listWithDetail } from "@/yunshu-ui/api/mes/wm/issueline";
import StockSelect from "@/yunshu-ui/components/stockSelect/single.vue"
export default {
  name: "Issuedetail",
  components:{ StockSelect},
  props:{
    issueId: null,
    optType: null,
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
      // 生产领料单行表格数据（带明细）
      issuelineList: [],
      // 弹出层标题
      title: "",
      // 是否显示弹出层
      open: false,
      autoOpenStock: false,
      // 查询参数
      queryParams: {
        pageNum: 1,
        pageSize: 10,
        issueId: this.issueId,        lineId: null,        itemId: null,        itemCode: null,        itemName: null,        specification: null,        unitOfMeasure: null,        unitName: null },
      // 表单参数
      form: {},
      // 表单校验
      rules: {
        itemCode: [
          { required: true, message: "物资不能为空", trigger: "blur" }
        ],
        warehouseName: [
          { required: true, message: "请选择仓库库存", trigger: "change" }
        ],
        quantity: [
          { required: true, message: "数量不能为空", trigger: "blur" }
        ],
      }
    };
  },
  created() {
    this.getListWithDetail();
  },
  methods: {
    /** 查询生产领料单明细列表 */
    getListWithDetail() {
      this.loading = true;
      listWithDetail(this.queryParams).then(response => {
        this.issuelineList = response.rows;
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
        detailId: null,        issueId: this.issueId,        lineId: null,        materialStockId: null,
        itemId: null,        itemCode: null,        itemName: null,        specification: null,        unitOfMeasure: null,        unitName: null,        quantity: null,        quantityMax: null,        batchId: null,        batchCode: null,        warehouseId: null,        warehouseCode: null,        warehouseName: null,        locationId: null,        locationCode: null,        locationName: null,        areaId: null,        areaCode: null,        areaName: null,        remark: null,        attr1: null,        attr2: null,        attr3: null,        attr4: null,        createBy: null,        createTime: null,        updateBy: null,        updateTime: null      };
      this.resetForm("form");
    },
    /** 搜索按钮操作 */
    handleQuery() {
      this.queryParams.pageNum = 1;
      this.getListWithDetail();
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
    handleAdd(row) {
      this.reset();
      this.form.issueId = row.issueId
      this.form.lineId = row.lineId
      this.form.itemId = row.itemId
      this.form.itemCode = row.itemCode
      this.form.itemName = row.itemName
      this.form.specification = row.specification
      this.form.unitOfMeasure = row.unitOfMeasure
      this.form.unitName = row.unitName
      this.form.quantityIssued = row.quantityIssued
      this.form.batchId = row.batchId
      this.form.batchCode = row.batchCode
      this.autoOpenStock = true
      this.open = true;
      this.title = "添加生产领料单明细";
    },
    onPickDialogOpened() {
      if (!this.autoOpenStock) {
        return;
      }
      this.autoOpenStock = false;
      this.handleSelectStock();
    },
    /** 修改按钮操作 */
    handleUpdate(row) {
      this.reset();
      const detailId = row.detailId || this.ids
      getIssuedetail(detailId).then(response => {
        this.form = response.data;
        this.open = true;
        this.title = "修改生产领料单明细";
      });
    },
    /** 提交按钮 */
    submitForm() {
      this.$refs["form"].validate(valid => {
        if (!valid) {
          return;
        }
        if (!this.form.materialStockId) {
          this.$modal.msgError("请先点击仓库右侧搜索，选择库存批次");
          return;
        }
        if (this.form.detailId != null) {
            updateIssuedetail(this.form).then(response => {
              this.$modal.msgSuccess("修改成功");
              this.open = false;
              this.getListWithDetail();
            });
          } else {
            addIssuedetail(this.form).then(response => {
              this.$modal.msgSuccess("新增成功");
              this.open = false;
              this.getListWithDetail();
            });
          }
      });
    },
    /** 删除按钮操作 */
    handleDelete(row) {
      const detailIds = row.detailId || this.ids;
      this.$modal.confirm('是否确认删除生产领料单明细编号为"' + detailIds + '"的数据项？').then(function() {
        return delIssuedetail(detailIds);
      }).then(() => {
        this.getListWithDetail();
        this.$modal.msgSuccess("删除成功");
      }).catch(() => {});
    },
    /** 导出按钮操作 */
    handleExport() {
      this.download('wm/issuedetail/export', {
        ...this.queryParams
      }, `issuedetail_${new Date().getTime()}.xlsx`)
    },
    handleSelectStock(){
      this.$refs.stockSelect?.handleOpen(null, this.form.itemId, this.form.itemCode);
    },
    onStockSelected(obj){
        if(!obj){
          return;
        }
        const qtyOnhand = Number(obj.quantityOnhand) || 0;
        const qtyIssued = Number(this.form.quantityIssued) || qtyOnhand;
        const pickQty = qtyOnhand > 0 ? Math.min(qtyOnhand, qtyIssued) : qtyIssued;
        Object.assign(this.form, {
          materialStockId: obj.materialStockId,
          itemId: obj.itemId,
          itemCode: obj.itemCode,
          itemName: obj.itemName,
          specification: obj.specification,
          unitOfMeasure: obj.unitOfMeasure,
          unitName: obj.unitName,
          batchId: obj.batchId,
          batchCode: obj.batchCode,
          warehouseId: obj.warehouseId,
          warehouseCode: obj.warehouseCode,
          warehouseName: obj.warehouseName,
          locationId: obj.locationId,
          locationCode: obj.locationCode,
          locationName: obj.locationName,
          areaId: obj.areaId,
          areaCode: obj.areaCode,
          areaName: obj.areaName,
          quantity: pickQty,
          quantityMax: qtyOnhand,
        });
        this.$nextTick(() => {
          this.$refs.form?.validateField("warehouseName");
        });
    },

  }
};
</script>
<style scoped>
::v-deep .child-table .el-table__header th {
  background-color: #D9DEF2; /* 浅蓝色 */
}
</style>