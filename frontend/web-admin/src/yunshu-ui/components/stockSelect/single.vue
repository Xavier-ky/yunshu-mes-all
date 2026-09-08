<template>
  <el-dialog title="库存物资选择"
    v-if="showFlag"
    v-model="showFlag"
    :modal="false"
    width="80%"
    center
    append-to-body
    destroy-on-close
    class="wm-stock-select-dialog"
  >
    <el-row :gutter="20">
      <el-col :span="4" :xs="24">
        <div class="head-container">
          <el-input
            v-model="itemTypeName"
            placeholder="请输入分类名称"
            clearable
            size="small"
            prefix-icon="el-icon-search"
            style="margin-bottom: 20px"
          />
        </div>
        <div class="head-container">
          <el-tree
            :data="itemTypeOptions"
            :props="defaultProps"
            :expand-on-click-node="false"
            :filter-node-method="filterNode"
            ref="tree"
            default-expand-all
            @node-click="handleNodeClick"
          />
        </div>
      </el-col>
      <el-col :span="20" :xs="24">

      <el-form :model="queryParams" ref="queryForm" size="small" :inline="true" v-show="showSearch" label-width="90px">
        <el-row>
          <el-col :span="8">
            <el-form-item label="物资编码" prop="itemCode">
              <el-input
                v-model="queryParams.itemCode"
                placeholder="请输入产品物料编码"
                clearable
                @keyup.enter="handleQuery"
              />
            </el-form-item>
          </el-col>
          <el-col :span="8">
            <el-form-item label="物资名称" prop="itemName">
              <el-input
                v-model="queryParams.itemName"
                placeholder="请输入产品物料名称"
                clearable
                @keyup.enter="handleQuery"
              />
            </el-form-item>
          </el-col>
          <el-col :span="8">
            <el-form-item label="批次号" prop="batchCode">
              <el-input
                v-model="queryParams.batchCode"
                placeholder="请输入批次号"
                clearable
                @keyup.enter="handleQuery"
              />
            </el-form-item>
          </el-col>
        </el-row>
        <el-row>
          <el-col :span="8">
            <el-form-item label="仓库名称" prop="warehouseName">
              <el-input
                v-model="queryParams.warehouseName"
                placeholder="请输入仓库名称"
                clearable
                @keyup.enter="handleQuery"
              />
            </el-form-item>
          </el-col>
          <el-col :span="8">
            <el-form-item label="供应商编号" prop="vendorCode">
              <el-input
                v-model="queryParams.vendorCode"
                placeholder="请输入供应商编号"
                clearable
                @keyup.enter="handleQuery"
              />
            </el-form-item>
          </el-col>
          <el-col :span="8">
            <el-form-item label="供应商名称" prop="vendorName">
              <el-input
                v-model="queryParams.vendorName"
                placeholder="请输入供应商名称"
                clearable
                @keyup.enter="handleQuery"
              />
            </el-form-item>
          </el-col>
        </el-row>
        <el-form-item>
          <el-button type="primary" icon="el-icon-search" size="default" @click="handleQuery">搜索</el-button>
          <el-button icon="el-icon-refresh" size="default" @click="resetQuery">重置</el-button>
        </el-form-item>
      </el-form>

      <el-row :gutter="10" class="mb8">
        <right-toolbar :show-search="showSearch" @update:showSearch="showSearch = $event" @queryTable="getList"></right-toolbar>
      </el-row>

      <el-table
        v-loading="loading"
        :data="wmstockList"
        highlight-current-row
        @current-change="handleCurrent"
        @row-click="handleRowClick"
        @row-dblclick="handleRowDbClick"
      >
        <el-table-column  width="55" align="center" >
            <template #default="scope">
                <el-radio v-model="selectedId" :label="scope.row.materialStockId" @change="handleRowChange(scope.row)">{{""}}</el-radio>
            </template>
        </el-table-column>
        <el-table-column label="产品物料编码" width="120px" align="center" prop="itemCode" />
        <el-table-column label="产品物料名称" width="150px" align="center" prop="itemName" :show-overflow-tooltip="true"/>
        <el-table-column label="规格型号" align="center" prop="specification" :show-overflow-tooltip="true"/>
        <el-table-column label="单位" align="center" prop="unitName" />
        <el-table-column label="入库批次号" width="100px" align="center" prop="batchCode" :show-overflow-tooltip="true"/>
        <el-table-column label="仓库" align="center" prop="warehouseName" />
        <el-table-column label="库区" align="center" prop="locationName" />
        <el-table-column label="库位" align="center" prop="areaName" />
        <el-table-column label="在库数量" align="center" prop="quantityOnhand" />
        <el-table-column label="入库日期" align="center" prop="recptDate" width="180">
          <template #default="scope">
            <span>{{ parseTime(scope.row.recptDate, '{y}-{m}-{d}') }}</span>
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
      </el-col>
    </el-row>
    <template #footer><div class="dialog-footer">
        <el-button type="primary" @click="confirmSelect">确 定</el-button>
        <el-button @click="showFlag=false">取 消</el-button>
    </div></template>
  </el-dialog>
</template>

<script>
import Pagination from "@/yunshu-ui/components/Pagination/index.vue";
import RightToolbar from "@/yunshu-ui/components/RightToolbar/index.vue";
import { listWmstock, getWmstock, delWmstock, addWmstock, updateWmstock } from "@/yunshu-ui/api/mes/wm/wmstock";
import { treeselect } from "@/yunshu-ui/api/mes/md/itemtype";
export default {
  components: { Pagination, RightToolbar },
  name: "WmstockMultiSelect",
  data() {
    return {
      showFlag:false,
      // 遮罩层
      loading: true,
      // 选中数组
      selectedId: null,
      selectedRow: null,
      // 非单个禁用
      single: true,
      // 非多个禁用
      multiple: true,
      // 显示搜索条件
      showSearch: true,
      itemTypeName: undefined,
      // 总条数
      total: 0,
      //物料产品分类树
      itemTypeOptions: undefined,
      // 库存记录表格数据
      wmstockList: [],
      // 弹出层标题
      title: "",
      // 是否显示弹出层
      open: false,
      defaultProps: {
        children: "children",
        label: "label"
      },
      // 查询参数
      queryParams: {
        pageNum: 1,
        pageSize: 10,
        itemTypeId: null,
        itemTypeName: null,
        itemId: this.itemId,
        itemCode: this.itemCode,
        itemName: null,
        specification: null,
        unitOfMeasure: null,
        unitName: null,
        batchId: this.batchId,
        batchCode: this.batchCode,
        warehouseId: this.warehouseId,
        warehouseCode: this.warehouseCode,
        warehouseName: null,
        locationId: this.locationId,
        locationCode: this.locationCode,
        locationName: null,
        areaId: this.areaId,
        areaCode: this.areaCode,
        areaName: null,
        vendorId: this.vendorId,
        vendorCode: null,
        vendorName: null,
        vendorNick: null,
        clientId: null,
        clientCode: null,
        clientName: null,
        workorderId: this.workorderId,
        quantityOnhand: null,
        expireDate: null,
      },
      // 表单参数
      form: {},
    };
  },
  props:{
    vendorId: null,
    batchId: null,
    batchCode: null,
    warehouseId: null,
    warehouseCode: null,
    locationId: null,
    locationCode: null,
    areaId: null,
    areaCode: null,
    workorderId: null,
    itemId: null,
    itemCode: null
  },
  watch:{
    itemId(newVal){
      this.queryParams.itemId = newVal;
    },
    itemCode(newVal){
      this.queryParams.itemCode = newVal;
    },
    batchId(newVal){
      this.queryParams.batchId = newVal;
    },
    batchCode(newVal){
      this.queryParams.batchCode = newVal;
    },
    warehouseId(newVal){
      this.queryParams.warehouseId = newVal;
    },
    warehouseCode(newVal){
      this.queryParams.warehouseCode = newVal;
    },
    locationId(newVal){
      this.queryParams.locationId = newVal;
    },
    locationCode(newVal){
      this.queryParams.locationCode = newVal;
    },
    areaId(newVal){
      this.queryParams.areaId = newVal;
    },
    areaCode(newVal){
      this.queryParams.areaCode = newVal;
    },
    workorderId(newVal){
      this.queryParams.workorderId = newVal;
    },
    vendorId(newVal){
      this.queryParams.vendorId = newVal;
    },
  },
  created() {
    this.getList();
    this.getTreeselect();
  },
  methods: {
    handleOpen(id, itemId, itemCode) {
      const resolvedItemId = itemId != null && itemId !== "" ? itemId : this.itemId
      const resolvedItemCode = itemCode != null && itemCode !== "" ? itemCode : this.itemCode
      this.showFlag = true
      this.selectedId = id || null
      this.selectedRow = null
      if (resolvedItemId != null && resolvedItemId !== "") {
        this.queryParams.itemId = resolvedItemId
      }
      if (resolvedItemCode != null && resolvedItemCode !== "") {
        this.queryParams.itemCode = resolvedItemCode
      }
      this.$nextTick(() => this.getList())
    },
    /** 查询库存记录列表 */
    getList() {
      this.loading = true;
      listWmstock(this.queryParams).then(response => {
        let rows = response.rows || [];
        const includeVirtual =
          this.warehouseCode != null &&
          this.warehouseCode !== undefined &&
          String(this.warehouseCode).indexOf("VIR") > -1;
        if (!includeVirtual) {
          rows = rows.filter((el) => !el.warehouseCode || String(el.warehouseCode).indexOf("VIR") === -1);
        }
        const filterItemId = this.queryParams.itemId;
        if (filterItemId != null && filterItemId !== "") {
          rows = rows.filter((el) => String(el.itemId) === String(filterItemId));
        }
        this.wmstockList = rows;
        this.total = rows.length;
        this.loading = false;
        if (this.showFlag && rows.length === 1) {
          this.applySelection(rows[0]);
        }
      });
    },
    /** 查询分类下拉树结构 */
    getTreeselect() {
      treeselect().then(response => {
        this.itemTypeOptions = response.data;
      });
    },
    // 筛选节点
    filterNode(value, data) {
      if (!value) return true;
      return data.label.indexOf(value) !== -1;
    },
    // 节点单击事件
    handleNodeClick(data) {
      this.queryParams.itemTypeId = data.id;
      this.handleQuery();
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
    handleRowClick(row) {
      this.handleCurrent(row);
    },
    handleCurrent(row){
        if(row){
            this.selectedRow = row;
            this.selectedId = row.materialStockId;
        }
    },
    applySelection(row) {
      if (!row) return;
      this.selectedRow = row;
      this.selectedId = row.materialStockId;
      this.$emit("onSelected", row);
      this.showFlag = false;
    },
    //行双击选中
    handleRowDbClick(row){
      if(row){
        this.applySelection(row);
      }
    },
    // 单选选中数据
    handleRowChange(row) {
      if(row){
        this.selectedRow = row;
      }
    },
    //确定选中
    confirmSelect(){
        if(!this.selectedRow || this.selectedId == null || this.selectedId === 0){
            this.$notify({
                title:'提示',
                type:'warning',
                message: '请至少选择一条库存记录（可单击行或点选左侧圆点）'
            });
            return;
        }
        this.applySelection(this.selectedRow);
    }
  }
};
</script>
