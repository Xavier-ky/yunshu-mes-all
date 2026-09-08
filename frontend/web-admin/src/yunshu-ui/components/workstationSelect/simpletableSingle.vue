<template>
  <el-dialog
    title="工作站选择"
    v-model="showFlag"
    append-to-body
    :modal="true"
    width="min(920px, 92vw)"
    class="yunshu-picker-dialog workstation-picker-dialog"
    :z-index="3100"
    destroy-on-close
    @opened="getList"
  >
    <section class="picker-filter-panel">
      <el-form :model="queryParams" ref="queryForm" size="small" :inline="true" class="picker-filter-form" label-width="0" @submit.prevent>
        <el-form-item prop="workstationCode">
          <el-input v-model="queryParams.workstationCode" placeholder="工作站编号" clearable @keyup.enter="handleQuery" />
        </el-form-item>
        <el-form-item prop="processId">
          <el-select v-model="queryParams.processId" placeholder="所属工序" clearable style="width: 160px">
            <el-option v-for="item in processOptions" :key="item.processId" :label="item.processName" :value="item.processId" />
          </el-select>
        </el-form-item>
        <el-form-item prop="workshopId">
          <el-select v-model="queryParams.workshopId" placeholder="所在车间" clearable style="width: 160px">
            <el-option v-for="item in workshopOptions" :key="item.workshopId" :label="item.workshopName" :value="item.workshopId" />
          </el-select>
        </el-form-item>
        <el-form-item class="picker-filter-actions">
          <el-button type="primary" icon="el-icon-search" size="default" @click="handleQuery">搜索</el-button>
          <el-button icon="el-icon-refresh" size="default" @click="resetQuery">重置</el-button>
        </el-form-item>
      </el-form>
    </section>
    <div class="picker-table-frame">
      <el-table
        v-loading="loading"
        class="yunshu-data-table picker-table"
        stripe
        border
        height="360"
        highlight-current-row
        :data="workstationList"
        @current-change="handleCurrent"
        @row-dblclick="handleRowDbClick"
      >
        <el-table-column width="52" align="center" fixed="left">
          <template #default="scope">
            <el-radio v-model="selectedWorkstationId" :label="scope.row.workstationId" @change="handleRowChange(scope.row)">{{ "" }}</el-radio>
          </template>
        </el-table-column>
        <el-table-column label="工作站编号" min-width="120" align="center" prop="workstationCode" show-overflow-tooltip />
        <el-table-column label="工作站名称" min-width="120" align="center" prop="workstationName" show-overflow-tooltip />
        <el-table-column label="所在车间" min-width="110" align="center" prop="workshopName" show-overflow-tooltip />
        <el-table-column label="所属工序" min-width="110" align="center" prop="processName" show-overflow-tooltip />
        <el-table-column label="地点" min-width="120" align="center" prop="workstationAddress" show-overflow-tooltip />
      </el-table>
    </div>
    <pagination
      v-show="total > 0"
      :total="total"
      :page="queryParams.pageNum"
      :limit="queryParams.pageSize"
      @update:page="queryParams.pageNum = $event"
      @update:limit="queryParams.pageSize = $event"
      @pagination="getList"
    />
    <template #footer>
      <div class="dialog-footer">
        <el-button type="primary" @click="confirmSelect">确 定</el-button>
        <el-button @click="showFlag = false">取 消</el-button>
      </div>
    </template>
  </el-dialog>
</template>

<script>
import { listWorkstation, getWorkstation, delWorkstation, addWorkstation, updateWorkstation } from "@/yunshu-ui/api/mes/md/workstation";
import {listAllProcess} from "@/yunshu-ui/api/mes/pro/process";
import { listAllWorkshop } from "@/yunshu-ui/api/mes/md/workshop";
export default {
  name: "WorkstationSelect",
  dicts: ['sys_yes_no'],
  data() {
    return {
      showFlag:false,
      // 遮罩层
      loading: true,
      // 选中数组
      selectedWorkstationId: undefined,
      selectedRows: [],
      // 非单个禁用
      single: true,
      // 非多个禁用
      multiple: true,
      // 显示搜索条件
      showSearch: true,
      // 总条数
      total: 0,
      // 工作站表格数据
      workstationList: [],
      //车间选项
      workshopOptions:[],
      //工序选项
      processOptions:[],
      // 弹出层标题
      title: "",
      // 是否显示弹出层
      open: false,
      // 查询参数
      queryParams: {
        pageNum: 1,
        pageSize: 10,
        workstationCode: null,
        workstationName: null,
        workstationAddress: null,
        workshopId: null,
        workshopCode: null,
        workshopName: null,
        processId: this.processId,
        processCode: null,
        processName: null,
        enableFlag: null,
      },
      // 表单参数
      form: {},
    };
  },
  props:{
      processId: undefined //外部传入的工序过滤条件
  },
  created() {
    this.getList();
    this.getWorkshops();
    this.getProcess();
  },
  methods: {
    handleOpen(id) {
      this.showFlag = true;
      this.selectedWorkstationId = id || undefined;
      this.selectedRows = null;
    },
    /** 查询工作站列表 */
    getList() {
      this.loading = true;
      listWorkstation(this.queryParams).then(response => {
        this.workstationList = response.rows;
        this.total = response.total;
        this.loading = false;
      });
    },
    //查询车间信息
    getWorkshops(){
      listAllWorkshop().then( response => {
        this.workshopOptions = response.data;
      });
    },
    //查询工序信息
    getProcess(){
      listAllProcess().then( response =>{
        this.processOptions = response.data;
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
        workstationId: null,
        workstationCode: null,
        workstationName: null,
        workstationAddress: null,
        workshopId: null,
        workshopCode: null,
        workshopName: null,
        processId: null,
        processCode: null,
        processName: null,
        enableFlag: 'Y',
        remark: null,
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
    handleCurrent(row) {
      if (row) {
        this.selectedRows = row;
        this.selectedWorkstationId = row.workstationId;
      }
    },
    // 单选选中数据
    handleRowChange(row) {
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
    //确定选中
    confirmSelect() {
      if (!this.selectedWorkstationId) {
        this.$modal.msgWarning("请先选择一条工作站");
        return;
      }
      if (!this.selectedRows) {
        this.selectedRows = this.workstationList.find((r) => r.workstationId === this.selectedWorkstationId) || null;
      }
      if (!this.selectedRows) {
        this.$modal.msgWarning("请先选择一条工作站");
        return;
      }
      this.$emit("onSelected", this.selectedRows);
      this.showFlag = false;
    },
  }
};
</script>
