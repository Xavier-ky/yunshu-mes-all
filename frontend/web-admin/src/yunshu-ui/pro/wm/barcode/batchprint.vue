<template>
    <el-dialog title="批量打印"
      v-if="showFlag"
      v-model="showFlag"
      :modal="false"
      width="80%"
      center
      >
      <el-form :model="queryParams" ref="queryForm" size="small" :inline="true" v-show="showSearch" label-width="68px">
        <el-form-item label="条码类型" prop="barcodeType">
            <el-select v-model="queryParams.barcodeType" placeholder="请选择条码类型">
            <el-option
                v-for="dict in dict.type.mes_barcode_type"
                :key="dict.value"
                :label="dict.label"
                :value="dict.value"
            ></el-option>
            </el-select>
        </el-form-item>
        <el-form-item>
            <el-button type="primary" icon="el-icon-search" size="default" @click="handleQuery">搜索</el-button>
            <el-button icon="el-icon-refresh" size="default" @click="resetQuery">重置</el-button>
        </el-form-item>
        <el-form-item label="打印模板" prop="bussinessCode">
            <el-input
            v-model="queryParams.bussinessCode"
            placeholder="请选择打印模板"
            clearable
            @keyup.enter="handleQuery"
            />
        </el-form-item>
        <el-form-item>
            <el-button type="success" icon="el-icon-printer" size="default" @click="handlePrint">打印</el-button>
        </el-form-item>

        </el-form>

        <el-table v-loading="loading" :data="barcodeList" @selection-change="handleSelectionChange">
          <el-table-column type="selection" width="55" align="center" />     
          <el-table-column label="条码" align="center">
            <template #default="scope">
                <el-image @click="handleView(scope.row)" class="barcodeClass" fit="scale-down" :src="scope.row.barcodeUrl">
                  <template #error><div class="image-slot">
                    <i class="el-icon-picture-outline"></i>
                  </div></template>
                </el-image>
            </template>
          </el-table-column> 
          <el-table-column label="条码格式" align="center" prop="barcodeFormart">
            <template #default="scope">
              <dict-tag :options="dict.type.mes_barcode_formart" :value="scope.row.barcodeFormart"/>
            </template>
          </el-table-column>          
          <el-table-column label="条码类型" align="center" prop="barcodeType">
            <template #default="scope">
              <dict-tag :options="dict.type.mes_barcode_type" :value="scope.row.barcodeType"/>
            </template>
          </el-table-column>
          <el-table-column label="条码内容" align="center" prop="barcodeContent" />
          <el-table-column label="业务编码" align="center" prop="bussinessCode" />
          <el-table-column label="业务名称" align="center" prop="bussinessName" />
          <el-table-column label="是否生效" align="center" prop="enableFlag" >
            <template #default="scope">
              <dict-tag :options="dict.type.sys_yes_no" :value="scope.row.enableFlag"/>
            </template>
          </el-table-column>        
          <el-table-column label="生成时间" align="center" prop="createTime" >
            <template #default="scope">
              <span>{{ parseTime(scope.row.createTime, '{y}-{m}-{d} {h}:{i}:{s}') }}</span>
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
        <el-button @click="showFlag=false">取 消</el-button>
      </div></template>
    </el-dialog>
  </template>
  
  <script>
  import { listBarcode, updateBarcode } from "@/yunshu-ui/api/mes/wm/barcode";
  export default {
    name: "BatchPrint",
    dicts: ['sys_yes_no', 'mes_barcode_type','mes_barcode_formart'],
    data() {
      return {
        showFlag: false,
        // 遮罩层
        loading: false,
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
        // 条码清单表格数据
        barcodeList: [],
        // 弹出层标题
        title: "",
        // 是否显示弹出层
        open: false,
        // 查询参数
        queryParams: {
          pageNum: 1,
          pageSize: 10,
          barcodeFormart: null,
          barcodeType: null,
          barcodeContent: null,
          bussinessId: null,
          bussinessCode: null,
          bussinessName: null,
          barcodeUrl: null,
          enableFlag: null
        }
      };
    },
    created() {

    },
    methods: {
      /** 查询条码清单列表 */
      getList() {
        this.loading = true;
        listBarcode(this.queryParams).then(response => {
          this.barcodeList = response.rows;
          this.total = response.total;
          this.loading = false;
        });
      },
      // 取消按钮
      cancel() {
        this.open = false;
        this.reset();
      },

      /** 搜索按钮操作 */
      handleQuery() {
        this.queryParams.pageNum = 1;
        this.getList();
      },
      /** 重置按钮操作 */
      resetQuery() {
        this.resetForm("queryForm");
      },
      // 多选框选中数据
      handleSelectionChange(selection) {
        this.ids = selection.map(item => item.configId)
        this.single = selection.length!==1
        this.multiple = !selection.length
      },
      //批量打印
      handlePrint(){

      }
    }
  };
  </script>
  