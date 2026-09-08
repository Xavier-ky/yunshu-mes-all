<template>
  <div class="app-container">
    <el-row :gutter="10" v-if="optType!='view'" class="mb8">
      <el-col :span="1.5">
        <el-button
          type="primary"
          plain
          icon="el-icon-plus"
          size="default"
          @click="handleAdd"          
          v-hasPermi="['mes:pro:proroute:add']"
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
          v-hasPermi="['mes:pro:proroute:remove']"
        >删除</el-button>
      </el-col>
      <right-toolbar :showSearch="showSearch" @queryTable="getList"></right-toolbar>
    </el-row>

    <el-table v-loading="loading" :data="routeproductbomList" @selection-change="handleSelectionChange">
      <el-table-column type="selection" width="55" align="center" />
      <el-table-column label="产品物料编码" align="center" prop="itemCode" />
      <el-table-column label="产品物料名称" align="center" prop="itemName" />
      <el-table-column label="规格型号" align="center" prop="specification" />
      <el-table-column label="单位" align="center" prop="unitName" />
      <el-table-column label="用料比例" align="center" prop="quantity" />
      <el-table-column label="操作" align="center" v-if="optType!='view'" class-name="small-padding fixed-width">
        <template #default="scope">
          <el-button
            size="default"
            type="text"
            icon="el-icon-edit"
            @click="handleUpdate(scope.row)"
            v-hasPermi="['mes:pro:proroute:edit']"
          >修改</el-button>
          <el-button
            size="default"
            type="text"
            icon="el-icon-delete"
            @click="handleDelete(scope.row)"
            v-hasPermi="['mes:pro:proroute:remove']"
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

    <!-- 添加或修改产品制程物料BOM对话框 -->
    <el-dialog :title="title" v-model="open" width="960px" append-to-body>
      <el-form ref="form" :model="form" :rules="rules" label-width="120px">
        <el-row>
          <el-col :span="12">
            <el-form-item label="产品物料编码" prop="itemCode">
              <el-input v-model="form.itemCode" placeholder="请选择此产品当前工序的BOM物料" >
                <template #append><el-button @click="handleItemBomSelect" icon="el-icon-search"></el-button></template>
              </el-input>
            </el-form-item>
            <ItemBomSelectSingle ref="itemBom" :itemId="productId" @onSelected="onItemBomSelected"></ItemBomSelectSingle>
          </el-col>
          <el-col :span="12">
            <el-form-item label="产品物料名称" prop="itemName">
              <el-input v-model="form.itemName" placeholder="请选择物料/产品" disabled/>
            </el-form-item>
          </el-col>
        </el-row>
        <el-row>
          <el-col :span="24">
            <el-form-item label="规格型号" prop="specification">
              <el-input v-model="form.specification" type="textarea" placeholder="请选择物料/产品" disabled/>
            </el-form-item>
          </el-col>
        </el-row>
        <el-row>
          <el-col :span="12">
            <el-form-item label="单位" prop="unitName">
              <el-input v-model="form.unitName" placeholder="请选择物料/产品" disabled/>
            </el-form-item>
          </el-col>
          <el-col :span="12">
            <el-form-item label="用料比例" prop="quantity">
              <el-input v-model="form.quantity" placeholder="请输入用料比例" />
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
import { listRouteproductbom, getRouteproductbom, delRouteproductbom, addRouteproductbom, updateRouteproductbom } from "@/yunshu-ui/api/mes/pro/routeproductbom";
import ItemBomSelectSingle from "@/yunshu-ui/components/itemBomSelect/single.vue"
export default {
  name: "Routeproductbom",
  components:{ItemBomSelectSingle},
  props:{
    routeId: null,
    productId: null,
    processId: null,
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
      // 产品制程物料BOM表格数据
      routeproductbomList: [],
      // 弹出层标题
      title: "",
      // 是否显示弹出层
      open: false,
      // 查询参数
      queryParams: {
        pageNum: 1,
        pageSize: 10,
        routeId: this.routeId,
        processId: this.processId,
        productId: this.productId,
        itemId: null,
        itemCode: null,
        itemName: null,
        specification: null,
        unitOfMeasure: null,
        quantity: null,
      },
      // 表单参数
      form: {},
      // 表单校验
      rules: {
        routeId: [
          { required: true, message: "工艺路线不能为空", trigger: "blur" }
        ],
        processId: [
          { required: true, message: "工序不能为空", trigger: "blur" }
        ],
        productId: [
          { required: true, message: "产品物料不能为空", trigger: "blur" }
        ],
        itemId: [
          { required: true, message: "产品BOM物料不能为空", trigger: "blur" }
        ]      
      }
    };
  },
  created() {
    this.getList();
  },
  methods: {
    /** 查询产品制程物料BOM列表 */
    getList() {
      debugger;
      this.loading = true;
      listRouteproductbom(this.queryParams).then(response => {
        this.routeproductbomList = response.rows;
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
        recordId: null,
        routeId: this.routeId,
        processId: this.processId,
        productId: this.productId,
        itemId: null,
        itemCode: null,
        itemName: null,
        specification: null,
        unitOfMeasure: null,
        unitName: null,
        quantity: null,
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
      this.ids = selection.map(item => item.recordId)
      this.single = selection.length!==1
      this.multiple = !selection.length
    },
    /** 新增按钮操作 */
    handleAdd() {
      this.reset();
      this.open = true;
      this.title = "添加产品制程物料BOM";
    },
    /** 修改按钮操作 */
    handleUpdate(row) {
      this.reset();
      const recordId = row.recordId || this.ids
      getRouteproductbom(recordId).then(response => {
        this.form = response.data;
        this.open = true;
        this.title = "修改产品制程物料BOM";
      });
    },
    /** 提交按钮 */
    submitForm() {
      this.$refs["form"].validate(valid => {
        if (valid) {
          if (this.form.recordId != null) {
            updateRouteproductbom(this.form).then(response => {
              this.$modal.msgSuccess("修改成功");
              this.open = false;
              this.getList();
            });
          } else {
            addRouteproductbom(this.form).then(response => {
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
      const recordIds = row.recordId || this.ids;
      this.$modal.confirm('是否确认删除产品制程物料BOM编号为"' + recordIds + '"的数据项？').then(function() {
        return delRouteproductbom(recordIds);
      }).then(() => {
        this.getList();
        this.$modal.msgSuccess("删除成功");
      }).catch(() => {});
    },
    /** 导出按钮操作 */
    handleExport() {
      this.download('pro/routeproductbom/export', {
        ...this.queryParams
      }, `routeproductbom_${new Date().getTime()}.xlsx`)
    },
    handleItemBomSelect(){
      this.$refs.itemBom.showFlag=true;
      this.$refs.itemBom.getList();
    },
    onItemBomSelected(row){
      this.form.itemId = row.bomItemId;
      this.form.itemCode = row.bomItemCode;
      this.form.itemName = row.bomItemName;
      this.form.specification = row.bomItemSpec;
      this.form.unitOfMeasure = row.unitOfMeasure;
      this.form.unitName = row.unitName;
      this.form.quantity = row.quantity;
    }
  }
};
</script>
