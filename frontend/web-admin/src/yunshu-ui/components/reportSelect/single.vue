<template>
    <el-dialog title="报表选择"
      v-if="showFlag"
      v-model="showFlag"
      :modal= false
      width="80%"
      center
      >
      <el-form :model="queryParams" ref="queryForm" size="small" :inline="true" v-show="showSearch" label-width="68px">
        <el-form-item label="报表名称" prop="name">
            <el-input
            v-model="queryParams.name"
            placeholder="请输入报表名称"
            clearable
            @keyup.enter="handleQuery"
            />
        </el-form-item>
        <el-form-item>
            <el-button type="primary" icon="el-icon-search" size="mini" @click="handleQuery">搜索</el-button>
            <el-button icon="el-icon-refresh" size="mini" @click="resetQuery">重置</el-button>
        </el-form-item>
      </el-form>
  
      <el-table v-loading="loading" :data="listData" @current-change="handleCurrent" @row-dblclick="handleRowDbClick">
      <el-table-column  width="55" align="center" >
        <template v-slot="scope">
          <el-radio v-model="selectedReportId" :label="scope.row.id" @change="handleRowChange(scope.row)">{{""}}</el-radio>
        </template>
      </el-table-column>
      <el-table-column label="报表名称" align="center" prop="name" />
      <el-table-column label="创建时间" align="center" prop="createTime" width="180">
        <template #default="scope">
          <span>{{ parseTime(scope.row.createTime, '{y}-{m}-{d}') }}</span>
        </template>
      </el-table-column>
      <el-table-column label="更新时间" align="center" prop="updateTime" width="180">
        <template #default="scope">
          <span>{{ parseTime(scope.row.updateTime, '{y}-{m}-{d}') }}</span>
        </template>
      </el-table-column>
      <el-table-column label="操作" align="center" class-name="small-padding fixed-width">
        <template #default="scope">
          <el-button
            size="mini"
            type="text"
            icon="el-icon-edit"
            @click="handlePreview(scope.row.name)"
            v-hasPermi="['mes:report:edit']"
          >预览</el-button>
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
      <div slot="footer" class="dialog-footer">
        <el-button type="primary" @click="confirmSelect">确 定</el-button>
        <el-button @click="showFlag=false">取 消</el-button>
      </div>
    </el-dialog>
  </template>
  
  <script>
  import { listReport, getReport, delReport, addReport, updateReport } from "@/yunshu-ui/api/mes/report/ureport";
  export default {
    name: "ReportSelect",
    dicts: ['sys_yes_no'],
    data() {
      return {
        showFlag:false,
        selectedReportId: undefined,
        selectedRow: undefined,
        // 遮罩层
        loading: true,      
        // 非单个禁用
        single: true,
        // 非多个禁用
        multiple: true,
        // 显示搜索条件
        showSearch: true,
        // 总条数
        total: 0,
        // 报表管理表格数据
        listData: [],
        // 弹出层标题
        title: "",
        // 是否显示弹出层
        open: false,
        // 查询参数
        queryParams: {
            pageNum: 1,
            pageSize: 10,
            name: null,
            content: null,
            createTime: null,
            updateTime: null
        },
        // 表单参数
        form: {},
      };
    },
    created() {
      this.getList();
    },
    methods: {
      /** 查询报表管理列表 */
      getList() {
        this.loading = true;
        listReport(this.queryParams).then(response => {
            this.listData = response.rows;
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
        debugger;
        if(row){
          this.selectedRow = row;
        }
      },
      /** 报表预览 */
      handlePreview(name) {
        window.open(`${this.website.reportUrl}/preview?_u=mysql:`+name)
      },
      //确定选中
      confirmSelect(){
          if(this.selectedReportId ==null || this.selectedReportId==0){
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
  