<template>
      <el-dialog title="设备选择"
    v-if="showFlag"
    v-model="showFlag"
    :modal= false
    width="80%"
    center
  >
      <el-form :model="queryParams" ref="queryForm" size="small" :inline="true" v-show="showSearch" label-width="68px">
        <el-form-item label="计划编号" prop="planCode">
          <el-input
            v-model="queryParams.planCode"
            placeholder="请输入计划编号"
            clearable
            @keyup.enter="handleQuery"
          />
        </el-form-item>
        <el-form-item label="计划名称" prop="planName">
          <el-input
            v-model="queryParams.planName"
            placeholder="请输入计划名称"
            clearable
            @keyup.enter="handleQuery"
          />
        </el-form-item>
        <el-form-item>
          <el-button type="primary" icon="el-icon-search" size="mini" @click="handleQuery">搜索</el-button>
          <el-button icon="el-icon-refresh" size="mini" @click="resetQuery">重置</el-button>
        </el-form-item>
      </el-form>

      <el-table v-loading="loading" class="yunshu-data-table" stripe border :data="checkplanList"  @current-change="handleCurrent" @row-dblclick="handleRowDbClick">
        <el-table-column  width="55" align="center" >
            <template #default="scope">
                <el-radio v-model="selectedPlanId" :label="scope.row.planId" @change="handleRowChange(scope.row)">{{""}}</el-radio>
            </template>
        </el-table-column>
        <el-table-column label="计划编码" align="center" prop="planCode" >
          <template #default="scope">
            <el-button
              type="text"
              @click="handleView(scope.row)"
              v-hasPermi="['mes:dv:checkplan:query']"
            >{{scope.row.planCode}}</el-button>
          </template>
        </el-table-column>
        <el-table-column label="计划名称" align="center" width="200px" prop="planName" />
        <el-table-column label="计划类型" align="center" width="120px" prop="planType">
          <template #default="scope">
            <dict-tag :options="dict.type.dv_plan_type" :value="scope.row.planType"/>
          </template>
        </el-table-column>
        <el-table-column label="开始日期" align="center" prop="startDate" width="180">
          <template #default="scope">
            <span>{{ parseTime(scope.row.startDate, '{y}-{m}-{d}') }}</span>
          </template>
        </el-table-column>
        <el-table-column label="结束日期" align="center" prop="endDate" width="180">
          <template #default="scope">
            <span>{{ parseTime(scope.row.endDate, '{y}-{m}-{d}') }}</span>
          </template>
        </el-table-column>
        <el-table-column label="频率" align="center" prop="cycleType" width="120px">
          <template #default="scope">
            {{scope.row.cycleCount}}<dict-tag :options="dict.type.mes_cycle_type" :value="scope.row.cycleType"/>
          </template>
        </el-table-column>
        <el-table-column label="状态" align="center" prop="status">
          <template #default="scope">
            <dict-tag :options="dict.type.mes_order_status" :value="scope.row.status"/>
          </template>
        </el-table-column>
        <el-table-column label="操作" align="center" width="130px" class-name="col-actions small-padding fixed-width">
          <template #default="scope">
          <div class="yunshu-row-actions">
          <el-button
              size="mini"
              type="text"
              icon="el-icon-edit"
              @click="handleUpdate(scope.row)"
              v-if="scope.row.status =='PREPARE'"
              v-hasPermi="['mes:dv:checkplan:edit']"
            >修改</el-button>
            <el-button
              size="mini"
              type="text"
              icon="el-icon-delete"
              @click="handleDelete(scope.row)"
              v-if="scope.row.status =='PREPARE'"
              v-hasPermi="['mes:dv:checkplan:remove']"
            >删除</el-button>
            <el-button
              size="mini"
              type="text"
              icon="el-icon-delete"
              @click="handleUpdate(scope.row)"
              v-if="scope.row.status =='FINISHED'"
              v-hasPermi="['mes:dv:checkplan:edit']"
            >停用</el-button>
          </div>
          </template>
        </el-table-column>
      </el-table>
  
      <pagination
        v-show="total>0"
        :total="total"
        :page="queryParams.pageNum" @update:page="queryParams.pageNum = $event"
        :limit="queryParams.pageSize" @update:limit="queryParams.pageSize = $event"
        @pagination="getList"
      />

      <template #footer><div class="dialog-footer">
      <el-button type="primary" @click="confirmSelect">确 定</el-button>
      <el-button @click="showFlag=false">取 消</el-button>
    </div></template>
</el-dialog>
  </template>
  
  <script>
  import { listCheckplan } from "@/yunshu-ui/api/mes/dv/checkplan";

  export default {
    name: "CheckplanSelect",
    dicts: ['mes_cycle_type','mes_order_status','dv_plan_type'],
    props: {
        planType: {
            type: String,
            default: 'CHECK'
        }
    },
    data() {
      return {
        showFlag: false,
        // 遮罩层
        loading: true,
        // 选中数组
        selectedPlanId: undefined,
        selectedRow: undefined,
        // 非单个禁用
        single: true,
        // 非多个禁用
        multiple: true,
        // 显示搜索条件
        showSearch: true,
        // 总条数
        total: 0,
        // 设备点检计划头表格数据
        checkplanList: [],
        // 弹出层标题
        title: "",
        // 是否显示弹出层
        open: false,
        // 查询参数
        queryParams: {
          pageNum: 1,
          pageSize: 10,
          planCode: null,
          planName: null,
          planType: this.planType,
          startDate: null,
          endDate: null,
          cycleType: null,
          cycleCount: null,
          status: 'FINISHED'
        }
      };
    },
    created() {
      this.getList();
    },
    methods: {
        /** 查询设备点检计划头列表 */
        getList() {
            this.loading = true;
            listCheckplan(this.queryParams).then(response => {
            this.checkplanList = response.rows;
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
            if(this.selectedPlanId ==null || this.selectedPlanId==0){
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
  