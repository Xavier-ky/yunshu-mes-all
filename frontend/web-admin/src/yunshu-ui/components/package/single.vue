<template>
  <el-dialog title="子箱选择"
    v-if="showFlag"
    v-model="showFlag"
    :modal="false"
    width="80%"
    center
  >
      <el-form :model="queryParams" ref="queryForm" size="small" :inline="true" v-show="showSearch" label-width="100px">
  
        <el-form-item label="装箱单编号" prop="packageCode">
          <el-input
            v-model="queryParams.packageCode"
            placeholder="请输入装箱单编号"
            clearable
            @keyup.enter="handleQuery"
          />
        </el-form-item>
        <el-form-item label="条码内容" prop="barcodeContent">
          <el-input
            v-model="queryParams.barcodeContent"
            placeholder="请输入条码内容"
            clearable
            @keyup.enter="handleQuery"
          />
        </el-form-item>
        <el-form-item label="销售订单编号" prop="soCode">
          <el-input
            v-model="queryParams.soCode"
            placeholder="请输入销售订单编号"
            clearable
            @keyup.enter="handleQuery"
          />
        </el-form-item>
        <el-form-item label="客户名称" prop="clientName">
          <el-input
            v-model="queryParams.clientName"
            placeholder="请输入客户名称"
            clearable
            @keyup.enter="handleQuery"
          />
        </el-form-item>
        <el-form-item label="检查员名称" prop="inspectorName">
          <el-input
            v-model="queryParams.inspectorName"
            placeholder="请输入检查员名称"
            clearable
            @keyup.enter="handleQuery"
          />
        </el-form-item>
        <el-form-item>
          <el-button type="primary" icon="el-icon-search" size="default" @click="handleQuery">搜索</el-button>
          <el-button icon="el-icon-refresh" size="default" @click="resetQuery">重置</el-button>
        </el-form-item>
      </el-form>
  
      <el-table v-loading="loading" :data="packageList" @current-change="handleCurrent" @row-dblclick="handleRowDbClick">
        <el-table-column  width="55" align="center" >
        <template v-slot="scope">
          <el-radio v-model="selectedPackageId" :label="scope.row.packageId" @change="handleRowChange(scope.row)">{{""}}</el-radio>
        </template>
      </el-table-column>
        <el-table-column label="装箱单编号" width="150px" align="center" prop="packageCode" />
        <el-table-column label="装箱日期" align="center" prop="packageDate" width="120">
          <template #default="scope">
            <span>{{ parseTime(scope.row.packageDate, '{y}-{m}-{d}') }}</span>
          </template>
        </el-table-column>
        <el-table-column label="销售订单编号" width="100px" align="center" prop="soCode" />
        <el-table-column label="发票编号" align="center" prop="invoiceCode" />
        <el-table-column label="客户编码" align="center" prop="clientCode" />
        <el-table-column label="客户名称" width="120px" align="center" prop="clientName" :show-overflow-tooltip="true"/>
        <el-table-column label="箱长度" align="center" prop="packageLength" />
        <el-table-column label="箱宽度" align="center" prop="packageWidth" />
        <el-table-column label="箱高度" align="center" prop="packageHeight" />
        <el-table-column label="尺寸单位" align="center" prop="sizeUnit" />
        <el-table-column label="净重" align="center" prop="netWeight" />
        <el-table-column label="毛重" align="center" prop="crossWeight" />
        <el-table-column label="重量单位" align="center" prop="weightUnit" />
        <el-table-column label="检查员" align="center" prop="inspectorName" />
        <el-table-column label="是否启用" align="center" prop="enableFlag">
          <template #default="scope">
            <dict-tag :options="dict.type.sys_yes_no" :value="scope.row.enableFlag"/>
          </template>
        </el-table-column>
        <el-table-column label="操作" align="center" class-name="small-padding fixed-width">
          <template #default="scope">
            <el-button
              size="default"
              type="text"
              icon="el-icon-edit"
              @click="handleUpdate(scope.row)"
              v-hasPermi="['mes:wm:package:edit']"
            >修改</el-button>
            <el-button
              size="default"
              type="text"
              icon="el-icon-delete"
              @click="handleDelete(scope.row)"
              v-hasPermi="['mes:wm:package:remove']"
            >删除</el-button>
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
      <template #footer><div class="dialog-footer">
        <el-button type="primary" @click="confirmSelect">确 定</el-button>
        <el-button @click="showFlag=false">取 消</el-button>
      </div></template>
    </el-dialog>
  </template>
  
  <script>
  import { listPackage} from "@/yunshu-ui/api/mes/wm/package";

  export default {
    name: "PackageSelectSingle",
    dicts: ['sys_yes_no'],
    props: {
        parentId: null,
        clientId: null,
        clientCode: null
    },
    data() {
      return {
        showFlag:false,
        selectedPackageId: undefined,
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
        // 装箱单表格数据
        packageList: [],
        // 弹出层标题
        title: "",
        // 是否显示弹出层
        open: false,
        // 查询参数
        queryParams: {
          pageNum: 1,
          pageSize: 10,
          parentId: this.parentId,
          ancestors: '0',//没有父包装的才能添加
          packageCode: null,
          barcodeId: null,
          barcodeContent: null,
          barcodeUrl: null,
          packageDate: null,
          soCode: null,
          invoiceCode: null,
          clientId: this.clientId,
          clientCode: this.clientCode,
          clientName: null,
          clientNick: null,
          packageLength: null,
          packageWidth: null,
          packageHeight: null,
          sizeUnit: null,
          netWeight: null,
          crossWeight: null,
          weightUnit: null,
          inspector: null,
          inspectorName: null,
          enableFlag: null,
          status: 'FINISHED'
        },


      };
    },
    created() {
      this.getList();
    },
    methods: {
        /** 查询装箱单列表 */
        getList() {
            this.loading = true;
            listPackage(this.queryParams).then(response => {
            this.packageList = response.rows;
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
        if(row){
            this.selectedRow = row;
        }
        },
        //确定选中
        confirmSelect(){
            if(this.selectedPackageId ==null || this.selectedPackageId==0){
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
  