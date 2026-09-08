<template>
  <div class="recpt-line-root">
    <el-row :gutter="10" class="recpt-line-toolbar">
      <el-col :span="1.5">
        <el-button
          type="primary"
          plain
          icon="el-icon-plus"
          size="default"
          @click="handleAdd"
          v-hasPermi="['mes:wm:productrecpt:add']"
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
          v-if="optType != 'view'"
          v-hasPermi="['mes:wm:productrecpt:edit']"
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
          v-if="optType != 'view'"
          v-hasPermi="['mes:wm:productrecpt:remove']"
        >删除</el-button>
      </el-col>
      <right-toolbar :show-search="showSearch" @update:showSearch="showSearch = $event" @queryTable="getList"></right-toolbar>
    </el-row>

    <el-table
      v-loading="loading"
      class="yunshu-data-table recpt-line-table"
      stripe
      border
      :data="productrecptlineList"
      @selection-change="handleSelectionChange"
    >
      <el-table-column type="selection" width="48" align="center" />
      <el-table-column label="产品物料编码" min-width="118" align="center" prop="itemCode" show-overflow-tooltip />
      <el-table-column label="产品物料名称" min-width="150" align="center" prop="itemName" show-overflow-tooltip />
      <el-table-column label="规格型号" min-width="100" align="center" prop="specification" show-overflow-tooltip />
      <el-table-column label="单位" width="72" align="center" prop="unitName" />
      <el-table-column label="入库数量" width="96" align="center" prop="quantityRecived" />
      <el-table-column label="批次号" min-width="150" align="center" prop="batchCode" show-overflow-tooltip />
      <el-table-column label="操作" width="120" align="center" class-name="col-actions">
        <template #default="scope">
          <div class="yunshu-row-actions">
          <el-button
            size="small"
            link
            icon="el-icon-edit"
            @click="handleUpdate(scope.row)"
            v-if="optType != 'view'"
            v-hasPermi="['mes:wm:productrecpt:edit']"
          >修改</el-button>
          <el-button
            size="small"
            link
            icon="el-icon-delete"
            @click="handleDelete(scope.row)"
            v-if="optType != 'view'"
            v-hasPermi="['mes:wm:productrecpt:remove']"
          >删除</el-button>
          </div>
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
      @pagination="getList"
    />

    <!-- 添加或修改产品入库记录行对话框 -->
    <el-dialog :title="title" v-model="open" width="960px" append-to-body>
      <el-form ref="form" :model="form" :rules="rules" label-width="110px">
        <el-row>
          <el-col :span="8">
            <el-form-item label="产品物料编码" prop="itemCode">
              <el-input v-model="form.itemCode" placeholder="请输入产品物料编码" :readonly="!!headerItemCode">
                <template #append v-if="!headerItemCode"><el-button @click="handleSelectStock" icon="el-icon-search"></el-button></template>
              </el-input>
              <StockSelect v-if="!headerItemCode" ref="stockSelect" :itemId="itemId" warehouseCode="XBK_VIRTUAL" @onSelected="onStockSelected"></StockSelect>
            </el-form-item>
          </el-col>
          <el-col :span="8">
            <el-form-item label="产品物料名称" prop="itemName">
              <el-input v-model="form.itemName" placeholder="请输入产品物料名称" />
            </el-form-item>
          </el-col>
          <el-col :span="8">
            <el-form-item label="单位" prop="unitName">
              <el-input v-model="form.unitName" placeholder="请输入单位" />
            </el-form-item>
          </el-col>
        </el-row>
        <el-row>
          <el-col :span="24">
            <el-form-item label="规格型号" prop="specification">
              <el-input v-model="form.specification" type="textarea" placeholder="请输入内容" />
            </el-form-item>
          </el-col>
        </el-row>
        <el-row>
          <el-col :span="8">
            <el-form-item label="入库数量" prop="quantityRecived">
              <el-input-number :min="0" :max="form.quantityMax" v-model="form.quantityRecived" placeholder="请输入入库数量" />
            </el-form-item>
          </el-col>
          <el-col :span="8">
            <el-form-item label="批次号" prop="batchCode">
              <el-input v-model="form.batchCode" placeholder="请输入批次号" />
            </el-form-item>
          </el-col>
          <el-col :span="8">

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
        <el-button type="primary" @click="submitForm">确 定</el-button>
        <el-button @click="cancel">取 消</el-button>
      </div></template>
    </el-dialog>
  </div>
</template>

<script>
import { listProductrecptline, getProductrecptline, delProductrecptline, addProductrecptline, updateProductrecptline } from "@/yunshu-ui/api/mes/wm/productrecptline";
import StockSelect from "@/yunshu-ui/components/stockSelect/single.vue"
import {getTreeList} from "@/yunshu-ui/api/mes/wm/warehouse"
export default {
  name: "Productrecptline",
  components:{
    StockSelect
  },
  props: {
    recptId: null,
    optType: null,
    itemId: null,
    itemCode: null,
    itemName: null,
    unitName: null,
    unitOfMeasure: null,
    plannedQty: null,
    workorderCode: null,
    workorderName: null,
  },
  computed: {
    headerItemCode() {
      return this.itemCode || null;
    },
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
      // 产品入库记录行表格数据
      productrecptlineList: [],
      // 弹出层标题
      title: "",
      // 是否显示弹出层
      open: false,
      // 查询参数
      queryParams: {
        pageNum: 1,
        pageSize: 10,
        recptId: this.recptId,
        materialStockId: null,
        itemId: null,
        itemCode: null,
        itemName: null,
        specification: null,
        unitOfMeasure: null,
        unitName: null,
        quantityRecived: null,
        batchId: null,
        batchCode: null     
      },
      // 表单参数
      form: {},
      // 表单校验
      rules: {
        quantityRecived: [
          { required: true, message: "入库数量不能为空", trigger: "blur" }
        ],
        itemCode: [
          { required: true, message: "入库物资不能为空", trigger: "blur" }
        ],
        remark: [
          { max: 250, message: "字段过长", trigger: "blur" }
        ]
      }
    };
  },
  created() {
    this.getList();
  },
  methods: {
    /** 查询产品入库记录行列表 */
    getList() {
      this.loading = true;
      listProductrecptline(this.queryParams).then(response => {
        this.productrecptlineList = response.rows;
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
        lineId: null,
        recptId: this.recptId,
        materialStockId: null,
        itemId: null,
        itemCode: null,
        itemName: null,
        specification: null,
        unitOfMeasure: null,
        unitName: null,
        quantityRecived: null,
        batchId: null,
        batchCode: null,      
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
      this.ids = selection.map(item => item.lineId)
      this.single = selection.length!==1
      this.multiple = !selection.length
    },
    /** 新增按钮操作 */
    handleAdd() {
      this.reset();
      if (this.headerItemCode) {
        this.form.itemId = this.itemId;
        this.form.itemCode = this.itemCode;
        this.form.itemName = this.itemName;
        this.form.unitOfMeasure = this.unitOfMeasure || "PCS";
        this.form.unitName = this.unitName || "件";
        this.form.quantityRecived = this.plannedQty ? Number(this.plannedQty) : 1;
        this.form.quantityMax = this.plannedQty ? Number(this.plannedQty) * 2 : 999999;
        this.form.workorderCode = this.workorderCode;
        this.form.workorderName = this.workorderName;
        if (this.workorderCode) {
          this.form.batchCode = `PB-${String(this.workorderCode).replace("WO", "")}`;
        }
      }
      this.open = true;
      this.title = "添加产品入库记录行";
    },
    /** 修改按钮操作 */
    handleUpdate(row) {
      this.reset();
      const lineId = row.lineId || this.ids
      getProductrecptline(lineId).then(response => {
        this.form = response.data;
        this.open = true;
        this.title = "修改产品入库记录行";
      });
    },
    /** 提交按钮 */
    submitForm() {
      this.$refs["form"].validate(valid => {
        if (valid) {
          if (this.form.lineId != null) {
            updateProductrecptline(this.form).then(response => {
              this.$modal.msgSuccess("修改成功");
              this.open = false;
              this.getList();
            });
          } else {
            addProductrecptline(this.form).then(response => {
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
      const lineIds = row.lineId || this.ids;
      this.$modal.confirm('是否确认删除产品入库记录行编号为"' + lineIds + '"的数据项？').then(function() {
        return delProductrecptline(lineIds);
      }).then(() => {
        this.getList();
        this.$modal.msgSuccess("删除成功");
      }).catch(() => {});
    },
    /** 导出按钮操作 */
    handleExport() {
      this.download('wm/productrecptline/export', {
        ...this.queryParams
      }, `productrecptline_${new Date().getTime()}.xlsx`)
    },
    handleSelectStock(){
      if (this.headerItemCode) {
        return;
      }
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
          this.form.batchCode = obj.batchCode;
          this.form.quantityRecived = obj.quantityOnhand;
          this.form.quantityMax = obj.quantityOnhand;
          this.form.unitName = obj.unitName
          this.batchId = obj.batchId;
          this.batchCode = obj.batchCode;
        }
    }
  }
};
</script>

<style scoped>
.recpt-line-root {
  padding: 0;
}
.recpt-line-toolbar {
  margin-bottom: 8px;
}
.recpt-line-table :deep(.el-table__cell .cell) {
  white-space: nowrap;
  overflow: hidden;
  text-overflow: ellipsis;
}
</style>
