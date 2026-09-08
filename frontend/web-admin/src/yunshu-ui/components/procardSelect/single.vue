<template>
  <el-dialog title="客户选择"
    v-if="showFlag"
    v-model="showFlag"
    :modal= false
    width="80%"
    center
    >
      <el-form :model="queryParams" ref="queryForm" size="small" :inline="true" v-show="showSearch" label-width="100px">
        <el-form-item label="流转卡编号" prop="cardCode">
          <el-input
            v-model="queryParams.cardCode"
            placeholder="请输入流转卡编号"
            clearable
            @keyup.enter="handleQuery"
          />
        </el-form-item>
        <el-form-item label="生产工单编号" prop="workorderCode">
          <el-input
            v-model="queryParams.workorderCode"
            placeholder="请输入生产工单编号"
            clearable
            @keyup.enter="handleQuery"
          />
        </el-form-item>
        <el-form-item label="批次号" prop="batchCode">
          <el-input
            v-model="queryParams.batchCode"
            placeholder="请输入批次号"
            clearable
            @keyup.enter="handleQuery"
          />
        </el-form-item>
        <el-form-item label="产品物料编码" prop="itemCode">
          <el-input
            v-model="queryParams.itemCode"
            placeholder="请输入产品物料编码"
            clearable
            @keyup.enter="handleQuery"
          />
        </el-form-item>
        <el-form-item label="产品物料名称" prop="itemName">
          <el-input
            v-model="queryParams.itemName"
            placeholder="请输入产品物料名称"
            clearable
            @keyup.enter="handleQuery"
          />
        </el-form-item>
        <el-form-item>
          <el-button type="primary" icon="el-icon-search" size="mini" @click="handleQuery">搜索</el-button>
          <el-button icon="el-icon-refresh" size="mini" @click="resetQuery">重置</el-button>
        </el-form-item>
      </el-form>

      <el-table v-loading="loading" :data="procardList" @current-change="handleCurrent" @row-dblclick="handleRowDbClick">
        <el-table-column  width="50" align="center" >
            <template v-slot="scope">
                <el-radio v-model="selectedId" :label="scope.row.cardId" @change="handleRowChange(scope.row)">{{""}}</el-radio>
            </template>
        </el-table-column>
        <el-table-column label="流转卡编号" align="center" prop="cardCode" >
        </el-table-column>
        <el-table-column label="生产工单编号" align="center" prop="workorderCode" />
        <el-table-column label="批次号" align="center" prop="batchCode" />
        <el-table-column label="产品物料编码" align="center" prop="itemCode" />
        <el-table-column label="产品物料名称" align="center" prop="itemName" />
        <el-table-column label="规格型号" align="center" prop="specification" />
        <el-table-column label="单位" align="center" prop="unitName" />
        <el-table-column label="流转数量" align="center" prop="quantityTransfered" />
      </el-table>
      <pagination
        v-show="total>0"
        :total="total"
        :page="queryParams.pageNum"
        :limit="queryParams.pageSize"
        @pagination="getList"
      />
      <div slot="footer" class="dialog-footer">
        <el-button type="primary" @click="confirmSelect">确 定</el-button>
        <el-button @click="showFlag=false">关 闭</el-button>
    </div>
  </el-dialog>

  </template>
  
  <script>
  import { listProcard } from "@/yunshu-ui/api/mes/pro/procard";
  export default {
    name: "Procard",
    data() {
      return {
        showFlag:false,
        selectedId: undefined,
        selectedRow: undefined,
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
        // 工序流转卡表格数据
        procardList: [],
        // 查询参数
        queryParams: {
          pageNum: 1,
          pageSize: 10,
          cardCode: null,        workorderId: null,        workorderCode: null,        workorderName: null,        batchCode: null,        itemId: null,        itemCode: null,        itemName: null,        specification: null,        unitOfMeasure: null,        barcodeUrl: null,        quantityTransfered: null,        status: null,        attr1: null,        attr2: null,        attr3: null,        attr4: null,      }
      };
    },
    created() {
      this.getList();
    },
    methods: {
        handleOpen(id) {
            this.showFlag = true
            this.selectedId = id
        },
        /** 查询工序流转卡列表 */
        getList() {
            this.loading = true;
            listProcard(this.queryParams).then(response => {
            this.procardList = response.rows;
            this.total = response.total;
            this.loading = false;
            });
        },
        /** 搜索按钮操作 */
        handleQuery() {
            this.queryParams.pageNum = 1;
            this.getList();
        },
        handleCurrent(row){
            if(row){
                this.selectedRow = row;
            }
        },
        //行双击选中
        handleRowDbClick(row){
        if(row){
            this.selectedRow = row;
            this.$emit('onSelected',this.selectedRow);
            this.showFlag = false;
        }
        },
        // 单选选中数据
        handleRowChange(row) {
        debugger;
        if(row){
            this.selectedRow = row;
        }
        },
        //确定选中
        confirmSelect(){
            if(this.selectedId ==null || this.selectedId==0){
                this.$notify({
                    title:'提示',
                    type:'warning',
                    message: '请至少选择一条数据!'
                });
                return;
            }
            this.$emit('onSelected',this.selectedRow);
            this.showFlag = false;
        }
    }
  };
  </script>
  