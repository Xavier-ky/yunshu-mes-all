<template>
  <div class="app-container">
    <el-row>
      <el-col :span="24" style="text-align: right;">
        <el-button icon="el-icon-refresh" size="default" @click="resetQuery">刷新</el-button>
      </el-col>
    </el-row>
    <el-table v-loading="loading" :data="rtvendorlineList" style="overflow-x: auto; width: 100%;" @selection-change="handleSelectionChange">      
      <el-table-column type="expand" style="overflow-x: auto; width: 100%;">
        <template #default="props">
          <el-table :data="props.row.details" class="child-table" style="margin-left: 60px;">            
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
                  v-hasPermi="['mes:wm:rtvendor:edit']"
                >修改</el-button>
                <el-button
                  size="default"
                  type="text"
                  icon="el-icon-delete"
                  @click="handleDelete(scope.row)"
                  v-hasPermi="['mes:wm:rtvendor:remove']"
                >删除</el-button>
              </template>
            </el-table-column>
          </el-table>
        </template>
      </el-table-column>
      <el-table-column label="物资编码" align="center" width="120px" prop="itemCode" />
      <el-table-column label="物资名称" align="center" prop="itemName" :show-overflow-tooltip="true"/>
      <el-table-column label="规格型号" align="center" prop="specification" :show-overflow-tooltip="true"/>
      <el-table-column label="单位" align="center" prop="unitName" />
      <el-table-column label="退货数量" align="center" prop="quantityRted" />
      <el-table-column label="批次号" align="center" prop="batchCode" />
      <el-table-column label="操作" align="center" width="100px" v-if="optType != 'view'" class-name="small-padding fixed-width">
        <template #default="scope">
          <el-button
            size="default"
            type="text"
            icon="el-icon-shopping-cart-full"
            @click="handleAdd(scope.row)"
            v-hasPermi="['mes:wm:rtvendor:edit']"
          >拣货</el-button>
          <printLabel :businessId="scope.row.batchId" :businessCode="scope.row.batchCode" labelText="标签打印" businessType = "BATCH"></printLabel>
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

    <!-- 添加或修改采购退货单明细对话框 -->
    <el-dialog :title="title" v-model="open" width="960px" append-to-body>
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
                <el-input v-model="form.warehouseName" readonly="readonly" >
                  <template #append><el-button @click="handleSelectStock" icon="el-icon-search"></el-button></template>
                </el-input>
              </el-form-item>
              <StockSelect ref="stockSelect" :itemId="form.itemId" :batchCode="form.batchCode" @onSelected="onStockSelected"></StockSelect>
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
  </div>
</template>

<script>
import { listRtvendordetail, getRtvendordetail, delRtvendordetail, addRtvendordetail, updateRtvendordetail } from "@/yunshu-ui/api/mes/wm/rtvendordetail";
import { listWithDetail } from "@/yunshu-ui/api/mes/wm/rtvendorline";
import printLabel from "@/yunshu-ui/components/printerLabel/index.vue"
import StockSelect from "@/yunshu-ui/components/stockSelect/single.vue"
export default {
  name: "Rtvendordetail",
  components:{
    printLabel,
    StockSelect
  },
  props: {
    rtId: null,
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
      // 采购退货单行数据(带明细)
      rtvendorlineList: [],
      // 弹出层标题
      title: "",
      // 是否显示弹出层
      open: false,
      // 查询参数
      queryParams: {
        pageNum: 1,
        pageSize: 10,
        lineId: null,        rtId: this.rtId,        materialStockId: null,        itemId: null,        itemCode: null,        itemName: null,        specification: null,        unitOfMeasure: null,        unitName: null,        quantity: null,        batchId: null,        batchCode: null},
      // 表单参数
      form: {},
      // 表单校验
      rules: {
        warehouseName: [
          { required: true, message: "请选择物资所在储位", trigger: "blur" }
        ],        quantity: [
          { required: true, message: "退货数量不能为空", trigger: "blur" }
        ],      }
    };
  },
  created() {
    this.getListWithDetail();
  },
  methods: {
    /** 查询采购退货单明细列表 */
    getListWithDetail() {
      this.loading = true;
      listWithDetail(this.queryParams).then(response => {
        this.rtvendorlineList = response.rows;
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
        detailId: null,        lineId: null,        rtId: this.rtId,        materialStockId: null,        itemId: null,        itemCode: null,        itemName: null,        specification: null,        unitOfMeasure: null,        unitName: null,        quantity: null,        batchId: null,        batchCode: null,        warehouseId: null,        warehouseCode: null,        warehouseName: null,        locationId: null,        locationCode: null,        locationName: null,        areaId: null,        areaCode: null,        areaName: null,        remark: null,        attr1: null,        attr2: null,        attr3: null,        attr4: null,        createBy: null,        createTime: null,        updateBy: null,        updateTime: null      };
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
      this.form.rtId = row.rtId
      this.form.lineId = row.lineId
      this.form.itemId = row.itemId
      this.form.itemCode = row.itemCode
      this.form.itemName = row.itemName
      this.form.specification = row.specification
      this.form.unitOfMeasure = row.unitOfMeasure
      this.form.unitName = row.unitName
      this.form.batchId = row.batchId
      this.form.batchCode = row.batchCode
      this.open = true;
      this.title = "添加采购退货单明细";
    },
    /** 修改按钮操作 */
    handleUpdate(row) {
      this.reset();
      const detailId = row.detailId || this.ids
      getRtvendordetail(detailId).then(response => {
        this.form = response.data;
        this.open = true;
        this.title = "修改采购退货单明细";
      });
    },
    /** 提交按钮 */
    submitForm() {
      this.$refs["form"].validate(valid => {
        if (valid) {
          if (this.form.detailId != null) {
            updateRtvendordetail(this.form).then(response => {
              this.$modal.msgSuccess("修改成功");
              this.open = false;
              this.getListWithDetail();
            });
          } else {
            addRtvendordetail(this.form).then(response => {
              this.$modal.msgSuccess("新增成功");
              this.open = false;
              this.getListWithDetail();
            });
          }
        }
      });
    },
    /** 删除按钮操作 */
    handleDelete(row) {
      const detailIds = row.detailId || this.ids;
      this.$modal.confirm('是否确认删除采购退货单明细编号为"' + detailIds + '"的数据项？').then(function() {
        return delRtvendordetail(detailIds);
      }).then(() => {
        this.getListWithDetail();
        this.$modal.msgSuccess("删除成功");
      }).catch(() => {});
    },
    handleSelectStock(){
      this.$refs.stockSelect.showFlag = true;
      this.$refs.stockSelect.getList();
    },
        //物料选择弹出框
    onStockSelected(obj){
        if(obj != undefined && obj != null){
          this.form.materialStockId = obj.materialStockId;
          this.form.itemId = obj.itemId;
          this.form.itemCode = obj.itemCode;
          this.form.itemName = obj.itemName;
          this.form.specification = obj.specification;
          this.form.unitOfMeasure = obj.unitOfMeasure;
          this.form.unitName = obj.unitName;
          this.form.batchId = obj.batchId;
          this.form.batchCode = obj.batchCode;
          this.form.warehouseId = obj.warehouseId;
          this.form.warehouseCode = obj.warehouseCode;
          this.form.warehouseName = obj.warehouseName;
          this.form.locationId = obj.locationId;
          this.form.locationCode = obj.locationCode;
          this.form.locationName = obj.locationName;
          this.form.areaId = obj.areaId;
          this.form.areaCode = obj.areaCode;
          this.form.areaName = obj.areaName;
          this.form.quantity = obj.quantityOnhand;
          this.form.quantityMax = obj.quantityOnhand;          
        }
    },

    /** 导出按钮操作 */
    handleExport() {
      this.download('wm/rtvendordetail/export', {
        ...this.queryParams
      }, `rtvendordetail_${new Date().getTime()}.xlsx`)
    }
  }
};
</script>
<style scoped>
::v-deep .child-table .el-table__header th {
  background-color: #D9DEF2; /* 浅蓝色 */
}
</style>