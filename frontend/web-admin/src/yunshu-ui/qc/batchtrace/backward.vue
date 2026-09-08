<template>
    <div class="app-container">
        <el-button icon="el-icon-refresh" size="default" @click="resetQuery">刷新</el-button>
      <el-table v-loading="loading" :data="batchList" @selection-change="handleSelectionChange">
        <el-table-column type="selection" width="55" align="center" />      
        <el-table-column label="生产工单号" width="150px" align="center" prop="workorderCode" />
        <el-table-column label="批次编号" align="center" prop="batchCode" />      
        <el-table-column label="物资编码" align="center" prop="itemCode" />
        <el-table-column label="物资名称" align="center" prop="itemName" />
        <el-table-column label="规格型号" align="center" prop="specification" />
        <el-table-column label="单位" align="center" prop="unitName" />     
      </el-table>
      
      <pagination
        v-show="total>0"
        :total="total"
        :page="queryParams.pageNum"
        :limit="queryParams.pageSize"
        @pagination="getList"
      />
    </div>
  </template>
  
  <script>
  import { listBatch, listBackward, getBatch } from "@/yunshu-ui/api/mes/wm/batch";
  
  export default {
    name: "Batch",
    props: {
        batchId: null,
        batchCode: null,
    },
    watch:{
        batchId: function(val){
            if(val){
                this.queryParams.batchId = val;
                this.getList();
            }            
        },

        batchCode: function(val){
            if(val){
                this.queryParams.batchCode = val;
                this.getList();
            }            
        }
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
          batchCode: this.batchCode,        itemId: null,        itemCode: null,        itemName: null,        specification: null,        unitOfMeasure: null,        produceDate: null,        expireDate: null,        recptDate: null,        vendorId: null,        vendorCode: null,        vendorName: null,        vendorNick: null,        clientId: null,        clientCode: null,        clientName: null,        clientNick: null,        coCode: null,        poCode: null,        workorderId: null,        workorderCode: null,        taskId: null,        taskCode: null,        workstationId: null,        workstationCode: null,        toolId: null,        toolCode: null,        moldId: null,        moldCode: null,        productCode: null,        qualityStatus: null,      },
      };
    },
    created() {
      this.getList();
    },
    methods: {
      /** 查询批次记录列表 */
      getList() {
        this.loading = true;
        listBackward(this.queryParams).then(response => {
          this.batchList = response.rows;
          this.total = response.total;
          this.loading = false;
        });
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
      } 
    }
  };
  </script>
  