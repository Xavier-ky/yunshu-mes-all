<template>
  <el-dialog title="设备选择"
    v-if="showFlag"
    v-model="showFlag"
    :modal="false"
    width="80%"
    center
  >
    <el-row :gutter="20">
      <!--分类数据-->
      <el-col :span="4" :xs="24">
        <div class="head-container">
          <el-input
            v-model="machineryTypeName"
            placeholder="请输入分类名称"
            clearable
            size="small"
            prefix-icon="el-icon-search"
            style="margin-bottom: 20px"
          />
        </div>
        <div class="head-container">
          <el-tree
            :data="machineryTypeOptions"
            :props="defaultProps"
            :expand-on-click-node="false"
            :filter-node-method="filterNode"
            ref="tree"
            default-expand-all
            @node-click="handleNodeClick"
          />
        </div>
      </el-col>
      <!--设备数据-->
      <el-col :span="20" :xs="24">
        <el-form :model="queryParams" ref="queryForm" size="small" :inline="true" v-show="showSearch" label-width="68px">
          <el-form-item label="设备编码" prop="machineryCode">
            <el-input
              v-model="queryParams.machineryCode"
              placeholder="请输入设备编码"
              clearable
              style="width: 240px"
              @keyup.enter="handleQuery"
            />
          </el-form-item>
          <el-form-item label="设备名称" prop="machineryName">
            <el-input
              v-model="queryParams.machineryName"
              placeholder="请输入设备名称"
              clearable
              style="width: 240px"
              @keyup.enter="handleQuery"
            />
          </el-form-item>
          <el-form-item label="所属车间" prop="workshopName">
            <el-input
              v-model="queryParams.workshopName"
              placeholder="请输入所属车间"
              clearable
              style="width: 240px"
              @keyup.enter="handleQuery"
            />
          </el-form-item>
          <el-form-item>
            <el-button type="primary" icon="el-icon-search" size="default" @click="handleQuery">搜索</el-button>
            <el-button icon="el-icon-refresh" size="default" @click="resetQuery">重置</el-button>
          </el-form-item>
        </el-form>
        <el-table v-loading="loading" :data="machineryList" @current-change="handleCurrent" @row-dblclick="handleRowDbClick">
          <el-table-column width="50" align="center" >
            <template v-slot="scope">
                <el-radio v-model="selectedMachineryId" :label="scope.row.machineryId" @change="handleRowChange(scope.row)">{{""}}</el-radio>
            </template>
          </el-table-column>
          <el-table-column label="设备编码" width = "120" align="center" key="machineryCode" prop="machineryCode">
          </el-table-column>
          <el-table-column label="设备名称" min-width="120" align="left" key="machineryName" prop="machineryName" :show-overflow-tooltip="true" />
          <el-table-column label="品牌" align="left" key="machineryBrand" prop="machineryBrand" :show-overflow-tooltip="true" />
          <el-table-column label="规格型号" align="left" key="machinerySpec" prop="machinerySpec" :show-overflow-tooltip="true" />
          <el-table-column label="所属车间" align="center" key="workshopName" prop="workshopName"  :show-overflow-tooltip="true" />
          <el-table-column label="设备状态" align="center" key="status" prop="status" >
            <template #default="scope">
              <dict-tag :options="dict.type.mes_machinery_status" :value="scope.row.status"/>
            </template>
          </el-table-column>
          <el-table-column label="创建时间" align="center" prop="createTime" width="160">
            <template #default="scope">
              <span>{{ parseTime(scope.row.createTime) }}</span>
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
import { listMachinery, getMachinery, delMachinery, addMachinery, updateMachinery } from "@/yunshu-ui/api/mes/dv/machinery";
import { listMachinerytype } from "@/yunshu-ui/api/mes/dv/machinerytype";
import { listAllWorkshop } from "@/yunshu-ui/api/mes/md/workshop";

import Treeselect from "@zanmato/vue3-treeselect";
import "@zanmato/vue3-treeselect/dist/vue3-treeselect.min.css";

export default {
  name: "MachinerySelectSingle",
  dicts: ['sys_yes_no','mes_machinery_status'],
  components: { Treeselect },
  data() {
    return {
      showFlag: false,
      // 遮罩层
      loading: true,
      // 选中数组
      selectedMachineryId: undefined,
      selectedRows: [],
      // 非单个禁用
      single: true,
      // 非多个禁用
      multiple: true,
      // 显示搜索条件
      showSearch: true,
      // 总条数
      total: 0,
      // 物料产品表格数据
      machineryList: [],
      // 弹出层标题
      title: "",
      // 设备类型树选项
      machineryTypeOptions: [],
      //车间选项
      workshopOptions:[],
      // 是否显示弹出层
      open: false,
      // 设备类型名称
      machineryTypeName: undefined,
      //自动生成物料编码标识
      autoGenFlag: false,
      // 表单参数
      form: {},
      defaultProps: {
        children: "children",
        label: "machineryTypeName"
      },
      // 查询参数
      queryParams: {
        pageNum: 1,
        pageSize: 10,
        machineryCode: null,
        machineryName: null,
        machineryBrand: null,
        machinerySpec: null,
        machineryTypeId: null,
        machineryTypeCode: null,
        machineryTypeName: null,
        workshopId: null,
        workshopCode: null,
        workshopName: null,
        status: null
      }
    };
  },
  watch: {
    // 根据设备分类名称筛选分类树
    machineryTypeName(val) {
      this.$refs.tree.filter(val);
    }
  },
  created() {
    this.getList();
    this.getTreeselect();
  },
  methods: {
    handleOpen(id) {
      this.showFlag = true
      this.selectedMachineryId = id
    },
    /** 查询物料编码列表 */
    getList() {
      this.loading = true;
      listMachinery(this.queryParams).then(response => {
          this.machineryList = response.rows;
          this.total = response.total;
          this.loading = false;
        }
      );
    },
    getWorkshops(){
      listAllWorkshop().then( response => {
        this.workshopOptions =response.data;
      });
    },
    /** 转换设备类型数据结构 */
    normalizer(node) {
      if (node.children && !node.children.length) {
        delete node.children;
      }
      return {
        id: node.machineryTypeId,
        label: node.machineryTypeName,
        children: node.children
      };
    },
	/** 查询设备类型下拉树结构 */
    getTreeselect() {
      listMachinerytype().then(response => {
        const rows = response.data || [];
        this.machineryTypeOptions = this.handleTree(rows, "machineryTypeId", "parentTypeId");
      }).catch(() => {
        this.machineryTypeOptions = [];
      });
    },
    // 筛选节点
    filterNode(value, data) {
      if (!value) return true;
      return data.machineryTypeName.indexOf(value) !== -1;
    },
    // 节点单击事件
    handleNodeClick(data) {
      this.queryParams.machineryTypeId = data.machineryTypeId;
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
    handleCurrent(row){
        if(row){
            this.selectedRows = row;
        }
    },
    //双击选中
    handleRowDbClick(row){
      if(row){
        this.selectedRows = row;
        this.$emit('onSelected',this.selectedRows);
        this.showFlag = false;
      }
    },
    // 单选选中数据
    handleRowChange(row) {
      if(row){
        this.selectedRows = row;
      }
    },
    //确定选中
    confirmSelect(){
        if(this.selectedMachineryId == null || this.selectedMachineryId == 0){
            this.$notify({
                title:'提示',
                type:'warning',
                message: '请至少选择一条数据!'
            });
            return;
        }
        this.$emit('onSelected',this.selectedRows);
        this.showFlag = false;
    }
  }
};
</script>
