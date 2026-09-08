<template>
  <div class="app-container">
    <el-row :gutter="10" v-if="optType !='view'" class="mb8">
      <el-col :span="1.5">
        <el-button
          type="primary"
          plain
          icon="el-icon-plus"
          size="default"
          @click="handleAdd"
          v-hasPermi="['mes:wm:package:add']"

        >新增</el-button>
      </el-col>
      <el-col :span="1.5">
        <el-button
          type="danger"
          plain
          icon="el-icon-delete"
          size="default"
          :disabled="multiple"
          @click="handleDelete"
          v-hasPermi="['mes:wm:package:remove']"
        >删除</el-button>
      </el-col>
      <right-toolbar :show-search="showSearch" @update:showSearch="showSearch = $event" @queryTable="getList"></right-toolbar>
    </el-row>

    <el-table
      v-loading="loading"
      class="yunshu-data-table"
      stripe
      border
      :data="packagelineList"
      @selection-change="handleSelectionChange"
    >
      <el-table-column type="selection" width="48" align="center" header-align="center" />
      <el-table-column label="产品物料编码" min-width="120" align="center" header-align="center" prop="itemCode" show-overflow-tooltip />
      <el-table-column label="产品物料名称" min-width="140" align="center" header-align="center" prop="itemName" show-overflow-tooltip />
      <el-table-column label="规格型号" min-width="120" align="center" header-align="center" prop="specification" show-overflow-tooltip />
      <el-table-column label="单位" width="72" align="center" header-align="center" prop="unitName" />
      <el-table-column label="装箱数量" width="90" align="center" header-align="center" prop="quantityPackage" />
      <el-table-column label="生产工单编号" min-width="130" align="center" header-align="center" prop="workorderCode" show-overflow-tooltip />
      <el-table-column label="批次号" min-width="100" align="center" header-align="center" prop="batchCode" show-overflow-tooltip />
      <el-table-column label="有效期" align="center" header-align="center" prop="expireDate" width="110">
        <template #default="scope">
          <span>{{ parseTime(scope.row.expireDate, '{y}-{m}-{d}') }}</span>
        </template>
      </el-table-column>
      <el-table-column label="操作" min-width="120" align="center" header-align="center" v-if="optType !='view'" class-name="col-actions small-padding fixed-width">
        <template #default="scope">
          <div class="yunshu-row-actions">
            <el-button
              size="small"
              link
              icon="el-icon-edit"
              @click="handleUpdate(scope.row)"
              v-hasPermi="['mes:wm:package:edit']"
            >修改</el-button>
            <el-button
              size="small"
              link
              icon="el-icon-delete"
              @click="handleDelete(scope.row)"
              v-hasPermi="['mes:wm:package:remove']"
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

    <!-- 添加或修改装箱明细对话框 -->
    <el-dialog :title="title" v-model="open" width="960px" append-to-body>
      <el-form ref="form" :model="form" :rules="rules" label-width="100px">
        <el-row>
          <el-col :span="8">
            <el-form-item label="生产工单编号" prop="workorderCode">
              <el-input v-model="form.workorderCode" placeholder="请输入生产工单编号" >
                <template #append><el-button icon="el-icon-search" @click="handleWorkorderSelect"></el-button></template>
              </el-input>
            </el-form-item>
            <WorkorderSelect ref="woSelect" @onSelected="onWorkorderSelected"></WorkorderSelect>
          </el-col>
          <el-col :span="8">
            <el-form-item label="批次号" prop="batchCode">
              <el-input v-model="form.batchCode" readonly="readonly" />
            </el-form-item>
          </el-col>
          <el-col :span="8">
            <el-form-item label="装箱数量" prop="quantityPackage">
              <el-input-number :min="0" v-model="form.quantityPackage" placeholder="请输入装箱数量" />
            </el-form-item>
          </el-col>
        </el-row>
        <el-row>
          <el-col :span="8">
            <el-form-item label="产品物料编码" prop="itemCode">
              <el-input v-model="form.itemCode" readonly="readonly" />
            </el-form-item>
          </el-col>
          <el-col :span="8">
            <el-form-item label="产品物料名称" prop="itemName">
              <el-input v-model="form.itemName" readonly="readonly" />
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
              <el-input v-model="form.specification" type="textarea" readonly="readonly" />
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
      <template #footer><div class="dialog-footer">
        <el-button type="primary" @click="submitForm">确 定</el-button>
        <el-button @click="cancel">取 消</el-button>
      </div></template>
    </el-dialog>
  </div>
</template>

<script>
import { listPackageline, getPackageline, delPackageline, addPackageline, updatePackageline } from "@/yunshu-ui/api/mes/wm/packageline";
import WorkorderSelect from "@/yunshu-ui/components/workorderSelect/single.vue"
export default {
  name: "Packageline",
  components: {
    WorkorderSelect
  },
  props: {
    packageId: null,
    optType: null
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
      // 装箱明细表格数据
      packagelineList: [],
      // 弹出层标题
      title: "",
      // 是否显示弹出层
      open: false,
      // 查询参数
      queryParams: {
        pageNum: 1,
        pageSize: 10,
        packageId: this.packageId,
        materialStockId: null,
        itemId: null,
        itemCode: null,
        itemName: null,
        specification: null,
        unitOfMeasure: null,
        quantityPackage: null,
        workorderId: null,
        workorderCode: null,
        batchCode: null,
        warehouseId: null,
        warehouseCode: null,
        warehouseName: null,
        locationId: null,
        locationCode: null,
        locationName: null,
        areaId: null,
        areaCode: null,
        areaName: null,
        expireDate: null,
      },
      // 表单参数
      form: {},
      // 表单校验
      rules: {
        packageId: [
          { required: true, message: "装箱单ID不能为空", trigger: "blur" }
        ],
        itemId: [
          { required: true, message: "产品物料ID不能为空", trigger: "blur" }
        ],
        quantityPackage: [
          { required: true, message: "装箱数量不能为空", trigger: "blur" }
        ],
      }
    };
  },
  created() {
    this.getList();
  },
  methods: {
    /** 查询装箱明细列表 */
    getList() {
      this.loading = true;
      listPackageline(this.queryParams).then(response => {
        this.packagelineList = response.rows;
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
        packageId: this.packageId,
        materialStockId: null,
        itemId: null,
        itemCode: null,
        itemName: null,
        specification: null,
        unitOfMeasure: null,
        quantityPackage: null,
        workorderId: null,
        workorderCode: null,
        batchCode: null,
        warehouseId: null,
        warehouseCode: null,
        warehouseName: null,
        locationId: null,
        locationCode: null,
        locationName: null,
        areaId: null,
        areaCode: null,
        areaName: null,
        expireDate: null,
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
    //选择生产工单
    handleWorkorderSelect(){
      this.$refs.woSelect.showFlag = true;
    },
    onWorkorderSelected(row){
      if(row != undefined && row != null){
        this.form.workorderId = row.workorderId;
        this.form.workorderCode = row.workorderCode;
        this.form.workorderName = row.workorderName;
        this.form.itemId = row.productId;
        this.form.itemCode = row.productCode;
        this.form.itemName = row.productName;
        this.form.specification = row.specification;
        this.form.unitOfMeasure = row.unitOfMeasure;
        this.form.unitName = obj.unitName
      }
    },
    /** 新增按钮操作 */
    handleAdd() {
      this.reset();
      this.open = true;
      this.title = "添加装箱明细";
    },
    /** 修改按钮操作 */
    handleUpdate(row) {
      this.reset();
      const lineId = row.lineId || this.ids
      getPackageline(lineId).then(response => {
        this.form = response.data;
        this.open = true;
        this.title = "修改装箱明细";
      });
    },
    /** 提交按钮 */
    submitForm() {
      this.$refs["form"].validate(valid => {
        if (valid) {
          if (this.form.lineId != null) {
            updatePackageline(this.form).then(response => {
              this.$modal.msgSuccess("修改成功");
              this.open = false;
              this.getList();
            });
          } else {
            addPackageline(this.form).then(response => {
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
      this.$modal.confirm('是否确认删除装箱明细编号为"' + lineIds + '"的数据项？').then(function() {
        return delPackageline(lineIds);
      }).then(() => {
        this.getList();
        this.$modal.msgSuccess("删除成功");
      }).catch(() => {});
    },
    /** 导出按钮操作 */
    handleExport() {
      this.download('wm/packageline/export', {
        ...this.queryParams
      }, `packageline_${new Date().getTime()}.xlsx`)
    }
  }
};
</script>
