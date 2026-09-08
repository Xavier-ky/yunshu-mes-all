<template>
    <div class="app-container">
      <el-form :model="queryParams" ref="queryForm" size="small" :inline="true" v-show="showSearch" label-width="100px">
        <el-form-item label="单据编码" prop="sourceDocCode">
          <el-input
            v-model="queryParams.sourceDocCode"
            placeholder="请输入单据编码"
            clearable
            @keyup.enter="handleQuery"
          />
        </el-form-item>
        <el-form-item label="物资名称" prop="itemName">
          <el-input
            v-model="queryParams.itemName"
            placeholder="请输入物资名称"
            clearable
            @keyup.enter="handleQuery"
          />
        </el-form-item>
        <el-form-item label="检测类型" prop="qcType">
          <el-select v-model="queryParams.qcType" placeholder="请选择检测类型" clearable>
            <el-option
              v-for="dict in dict.type.mes_index_type"
              :key="dict.value"
              :label="dict.label"
              :value="dict.value"
            />
          </el-select>
        </el-form-item>     
        <el-form-item>
          <el-button type="primary" icon="el-icon-search" size="default" @click="handleQuery">搜索</el-button>
          <el-button icon="el-icon-refresh" size="default" @click="resetQuery">重置</el-button>
        </el-form-item>
      </el-form>
  
      <el-table v-loading="loading" :data="pendingList" @selection-change="handleSelectionChange">
        <el-table-column type="selection" width="55" align="center" />
        <el-table-column label="来源单据编号" align="center" prop="sourceDocCode" />        
        <el-table-column label="检测类型" align="center" prop="qcType">
          <template #default="scope">
            <dict-tag :options="dict.type.mes_qc_type" :value="scope.row.qcType"/>
          </template>
        </el-table-column>
        <el-table-column label="详细检测类型" align="center" prop="qcType">
          <template #default="scope">
            <dict-tag :options="dict.type.mes_qc_detail_type" :value="scope.row.qcDetailType"/>
          </template>
        </el-table-column>
        <el-table-column label="物资编码" align="center" prop="itemCode" />
        <el-table-column label="物资名称" align="center" prop="itemName" />
        <el-table-column label="待检数量" align="center" prop="quantityCheck" />
        <el-table-column label="单位" align="center" prop="unitName" />

        <el-table-column label="操作" align="center" class-name="small-padding fixed-width">
          <template #default="scope">
            <el-button
              size="default"
              type="text"
              icon="el-icon-edit"
              @click="handleAddIQC(scope.row)"
              v-if="scope.row.qcType == 'IQC'"
              v-hasPermi="['mes:qc:iqc:edit']"
            >来料检验</el-button>
            <el-button
              size="default"
              type="text"
              icon="el-icon-edit"
              v-if="scope.row.qcType == 'PQC'"
              @click="handleAddPQC(scope.row)"
              v-hasPermi="['mes:qc:ipqc:edit']"
            >过程检验</el-button>
            <el-button
              size="default"
              type="text"
              icon="el-icon-edit"
              v-if="scope.row.qcType == 'RQC'"
              @click="handleAddRQC(scope.row)"
              v-hasPermi="['mes:qc:rqc:edit']"
            >退料检验</el-button>
            <el-button
              size="default"
              type="text"
              icon="el-icon-edit"
              v-if="scope.row.qcType == 'OQC'"
              @click="handleAddOQC(scope.row)"
              v-hasPermi="['mes:qc:oqc:edit']"
            >出货检验</el-button>
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

    </div>
  </template>
  
  <script>
  import { listPending } from "@/yunshu-ui/api/mes/qc/pending";
  export default {
    name: "Qcpending",
    dicts: ['mes_qc_type','mes_qc_detail_type'],
    data() {
      return {
        //自动生成编码
        autoGenFlag:false,
        optType: undefined,
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
        // 检测项表格数据
        pendingList: [],
        // 弹出层标题
        title: "",
        // 是否显示弹出层
        open: false,
        // 查询参数
        queryParams: {
          pageNum: 1,
          pageSize: 10,
          sourceDocCode: null,
          itemName: null,
          qcType: null          
        },
        // 表单参数
        form: {}
      };
    },
    created() {
      this.getList();
    },
    methods: {
      /** 查询待检测任务列表 */
      getList() {
        this.loading = true;
        listPending(this.queryParams).then(response => {
          this.pendingList = response.rows;
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
        this.ids = selection.map(item => item.indexId)
        this.single = selection.length!==1
        this.multiple = !selection.length
      },
      
      // 生成来料检验单（整行入 query，params: row）
      handleAddIQC(row){
        this.$router.push({ name: 'qc-pending-iqc', query: this.toQuery(row) });
      },
      /** 生成过程检验单 */
      handleAddPQC(row) {
        this.$router.push({ name: 'qc-pending-pqc', query: this.toQuery(row) });
      },
      /** 生成退料检验单 */
      handleAddRQC(row) {
        this.$router.push({ name: 'qc-pending-rqc', query: this.toQuery(row) });
      },
      /** 生成出货检验单 */
      handleAddOQC(row) {
        this.$router.push({ name: 'qc-pending-oqc', query: this.toQuery(row) });
      },
      toQuery(row) {
        const q = {};
        Object.keys(row || {}).forEach((k) => {
          const v = row[k];
          if (v !== null && v !== undefined && v !== '') q[k] = String(v);
        });
        return q;
      },
    }
  };
  </script>
  