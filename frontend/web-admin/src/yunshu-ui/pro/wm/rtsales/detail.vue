<template>
  <div class="app-container">
    
    <el-table v-loading="loading" :data="rtsaleslineList" @selection-change="handleSelectionChange">
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
                  v-hasPermi="['mes:wm:rtsales:edit']"
                >修改</el-button>
                <el-button
                  size="default"
                  type="text"
                  icon="el-icon-delete"
                  @click="handleDelete(scope.row)"
                  v-hasPermi="['mes:wm:rtsales:remove']"
                >删除</el-button>
              </template>
            </el-table-column>
          </el-table>
        </template>
      </el-table-column>
      <el-table-column label="产品编码" width="120" align="center" prop="itemCode" />
      <el-table-column label="产品名称" width="200" align="center" prop="itemName" :show-overflow-tooltip="true"/>
      <el-table-column label="规格型号" width="150" align="center" prop="specification" :show-overflow-tooltip="true"/>
      <el-table-column label="单位" align="center" prop="unitName" />
      <el-table-column label="退货数量" align="center" prop="quantityRted" />
      <el-table-column label="批次号" align="center" prop="batchCode" />
      <el-table-column label="质量状态" width="100px" align="center" prop="qualityStatus">
        <template #default="scope">
          <dict-tag :options="dict.type.mes_quality_status" :value="scope.row.qualityStatus"/>
        </template>
      </el-table-column>
      <el-table-column label="备注" align="center" prop="remark" :show-overflow-tooltip="true"/>
      <el-table-column label="操作" width="150px" align="center" v-if="optType != 'view'" class-name="small-padding fixed-width">
        <template #default="scope">
          <el-button
            size="default"
            type="text"
            icon="el-icon-shopping-cart-full"
            @click="handleAdd(scope.row)"
            v-hasPermi="['mes:wm:rtsales:edit']"
          >上架</el-button>
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
  
    <!-- 添加或修改物料入库单明细对话框 -->
    <el-dialog :title="title" v-model="open" width="960px" append-to-body>
      <el-form ref="form" :model="form" :rules="rules" label-width="100px">
        <el-row>
          <el-col :span="8">
            <el-form-item label="物资编码" prop="itemCode">
              <el-input v-model="form.itemCode" readonly="readonly" />
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
          <el-col :span="16">
            <el-form-item label="规格型号" prop="specification">
              <el-input v-model="form.specification" readonly="readonly" />
            </el-form-item>
          </el-col>          
          <el-col :span="8">
            <el-form-item label="批次号" prop="batchCode">
              <el-input v-model="form.batchCode" readonly="readonly" />
            </el-form-item>
          </el-col>
        </el-row>
        <el-row>
          <el-col :span="8">
            <el-form-item label="入库仓库" prop="warehouseInfo">
              <el-cascader v-model="warehouseInfo"
                :options="warehouseOptions"
                :props="warehouseProps"
                @change="handleWarehouseChanged"
              >
              </el-cascader>
            </el-form-item>
          </el-col>
          <el-col :span="8">
            <el-form-item label="数量" prop="quantity">
              <el-input-number v-model="form.quantity" :min="0" placeholder="请输入入库数量" />
            </el-form-item>
          </el-col>
        </el-row>
      </el-form>
      <template #footer><div class="dialog-footer">
        <el-button type="primary" @click="submitForm">确 定</el-button>
        <el-button @click="cancel">关 闭</el-button>
      </div></template>
    </el-dialog>
  </div>
</template>

<script>
import { getRtsalesdetail, delRtsalesdetail, addRtsalesdetail, updateRtsalesdetail } from "@/yunshu-ui/api/mes/wm/rtsalesdetail";
import { listWithDetail } from "@/yunshu-ui/api/mes/wm/rtsalesline";
import printLabel from "@/yunshu-ui/components/printerLabel/index.vue"
import {getTreeList} from "@/yunshu-ui/api/mes/wm/warehouse"
export default {
  name: "Rtsalesdetail",
  components: {printLabel},
  dicts: ['mes_quality_status'],
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
      // 产品销售退货行表格数据
      rtsaleslineList: [],
      warehouseInfo:[],
      warehouseOptions:[],
      warehouseProps:{
        multiple: false,
        value: 'pId',
        label: 'pName',
      },
      // 弹出层标题
      title: "",
      // 是否显示弹出层
      open: false,
      // 查询参数
      queryParams: {
        pageNum: 1,
        pageSize: 10,
        lineId: null,        rtId: this.rtId,        itemId: null,        itemCode: null,        itemName: null,        specification: null,        unitOfMeasure: null,        unitName: null,        quantity: null,        batchId: null,        batchCode: null,        warehouseId: null,        warehouseCode: null,        warehouseName: null,        locationId: null,        locationCode: null,        locationName: null,        areaId: null,        areaCode: null,        areaName: null,      },
      // 表单参数
      form: {},
      // 表单校验
      rules: {
        lineId: [
          { required: true, message: "行ID不能为空", trigger: "blur" }
        ],        itemId: [
          { required: true, message: "产品物料ID不能为空", trigger: "blur" }
        ],        quantity: [
          { required: true, message: "退货数量不能为空", trigger: "blur" }
        ],        warehouseInfo: [
          { validator: this.validateWarehouseInfo, trigger: "blur" }
        ],       }
    };
  },
  created() {
    this.getListWithDetail();
    this.getWarehouseList();
  },
  methods: {
    /** 查询销售退货记录明细列表 */
    getListWithDetail() {
      this.loading = true;
      listWithDetail(this.queryParams).then(response => {
        this.rtsaleslineList = response.rows;
        this.total = response.total;
        this.loading = false;
      });
    },
    getWarehouseList(){
      getTreeList().then( response =>{
        if(response.data){
          this.warehouseOptions = response.data.filter((el) =>{
              return el.warehouseCode.indexOf('VIR') == -1;
          });;
        }
        this.warehouseOptions.map(w =>{          
          w.children.map(l =>{
                  let lstr =JSON.stringify(l.children).replace(/locationId/g,'lId').replace(/areaId/g, 'pId').replace(/areaName/g,'pName');
                  l.children = JSON.parse(lstr);
          });

          let wstr = JSON.stringify(w.children).replace(/warehouseId/g,'wId').replace(/locationId/g, 'pId').replace(/locationName/g,'pName');
          w.children =  JSON.parse(wstr);

        });
        let ostr=JSON.stringify(this.warehouseOptions).replace(/warehouseId/g,'pId').replace(/warehouseName/g, 'pName');
        this.warehouseOptions = JSON.parse(ostr);        
      });
    },
    //选择仓库、库区、库位
    handleWarehouseChanged(obj){    
      if(obj !=null){
        this.form.warehouseId = obj[0];
        this.form.locationId = obj[1];
        this.form.areaId = obj[2];
      }
    },
    validateWarehouseInfo(rule, value, callback) {      
      if ( this.warehouseInfo.length === 0) {
        callback(new Error("请选择仓库、库区、库位"));
      } else {
        callback();
      }
    },
    // 取消按钮
    cancel() {
      this.open = false;
      this.reset();
    },
    // 表单重置
    reset() {
      this.form = {
        detailId: null,        lineId: null,        rtId: this.rtId,        itemId: null,        itemCode: null,        itemName: null,        specification: null,        unitOfMeasure: null,        unitName: null,        quantity: null,        batchId: null,        batchCode: null,        warehouseId: null,        warehouseCode: null,        warehouseName: null,        locationId: null,        locationCode: null,        locationName: null,        areaId: null,        areaCode: null,        areaName: null,        remark: null,        attr1: null,        attr2: null,        attr3: null,        attr4: null,        createBy: null,        createTime: null,        updateBy: null,        updateTime: null      };
        this.warehouseInfo = [];
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
      this.form.materialStockId = row.materialStockId;
      this.form.itemId = row.itemId
      this.form.itemCode = row.itemCode
      this.form.itemName = row.itemName
      this.form.specification = row.specification
      this.form.unitOfMeasure = row.unitOfMeasure
      this.form.unitName = row.unitName
      this.form.batchId = row.batchId
      this.form.batchCode = row.batchCode
      this.open = true;
      this.title = "添加销售退货记录明细";
    },
    /** 修改按钮操作 */
    handleUpdate(row) {
      this.reset();
      const detailId = row.detailId || this.ids
      getRtsalesdetail(detailId).then(response => {      
        this.form = response.data;
        this.warehouseInfo[0] = response.data.warehouseId;
        this.warehouseInfo[1] = response.data.locationId;
        this.warehouseInfo[2] = response.data.areaId;
        this.open = true;
        this.title = "修改销售退货记录明细";
      });
    },
    /** 提交按钮 */
    submitForm() {
      this.$refs["form"].validate(valid => {
        if (valid) {
          if (this.form.detailId != null) {
            updateRtsalesdetail(this.form).then(response => {
              this.$modal.msgSuccess("修改成功");
              this.open = false;
              this.getListWithDetail();
            });
          } else {
            addRtsalesdetail(this.form).then(response => {
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
      this.$modal.confirm('是否确认删除销售退货记录明细编号为"' + detailIds + '"的数据项？').then(function() {
        return delRtsalesdetail(detailIds);
      }).then(() => {
        this.getListWithDetail();
        this.$modal.msgSuccess("删除成功");
      }).catch(() => {});
    },
    /** 导出按钮操作 */
    handleExport() {
      this.download('wm/rtsalesdetail/export', {
        ...this.queryParams
      }, `rtsalesdetail_${new Date().getTime()}.xlsx`)
    }
  }
};
</script>
