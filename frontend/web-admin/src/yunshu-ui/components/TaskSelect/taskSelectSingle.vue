<template>
  <el-dialog title="生产任务选择"
    v-if="showFlag"
    v-model="showFlag"
    :modal="false"
    width="80%"
    center
    >
    <el-form :model="queryParams" ref="queryForm" size="small" :inline="true" v-show="showSearch" label-width="100px">

          <el-form-item label="所属工序" prop="processName">
            <el-select v-model="queryParams.processId" placeholder="请选择工序">
                <el-option
                  v-for="item in processOptions"
                  :key="item.processId"
                  :label="item.processName"
                  :value="item.processId"
                ></el-option>
              </el-select>
          </el-form-item>
          <el-form-item label="生产工单" prop="workorderCode">
            <el-input
              v-model="queryParams.workorderCode"
              placeholder="请输入生产工单号"
              clearable
              @keyup.enter="handleQuery"
            />
          </el-form-item>
          <el-form-item label="所属工作站" prop="workstationCode">
            <el-input
              v-model="queryParams.workstationCode"
              placeholder="请输入工作站编号"
              clearable
              @keyup.enter="handleQuery"
            />
          </el-form-item>
          <el-form-item label="任务编号" prop="taskCode">
            <el-input
              v-model="queryParams.taskCode"
              placeholder="请输入生产任务编号"
              clearable
              @keyup.enter="handleQuery"
            />
          </el-form-item>
          <el-form-item>
            <el-button type="primary" icon="el-icon-search" size="default" @click="handleQuery">搜索</el-button>
            <el-button icon="el-icon-refresh" size="default" @click="resetQuery">重置</el-button>
          </el-form-item>

    </el-form>

      <el-table v-loading="loading" :data="protaskList" @current-change="handleCurrent" @row-dblclick="handleRowDbClick">
        <el-table-column  width="55" align="center" >
          <template #default="scope">
            <el-radio v-model="selectedTaskId" :label="scope.row.taskId" @change="handleRowChange(scope.row)">{{""}}</el-radio>
          </template>
        </el-table-column>
        <el-table-column label="任务编号" align="center" width="150px" prop="taskCode" :show-overflow-tooltip="true"/>
        <el-table-column label="任务名称" align="center"  prop="taskName" :show-overflow-tooltip="true"/>
        <el-table-column label="工作站编号" align="center" width="150px" prop="workstationCode" :show-overflow-tooltip="true"/>
        <el-table-column label="工作站名称" align="center" width="150px" prop="workstationName" :show-overflow-tooltip="true"/>
        <el-table-column label="工序" align="center" prop="processName" :show-overflow-tooltip="true"/>
        <el-table-column label="物资编码" align="center" prop="itemCode" :show-overflow-tooltip="true"/>
        <el-table-column label="物资名称" align="center" prop="itemName" :show-overflow-tooltip="true"/>
        <el-table-column label="规格" align="center" prop="specification" :show-overflow-tooltip="true"/>
        <el-table-column label="排产数量" align="center" prop="quantity" />
        <el-table-column label="已生产数量" align="center" width="100px" prop="quantityProduced" />
        <el-table-column label="开始生产时间" align="center" prop="startTime" width="180">
          <template #default="scope">
            <span>{{ parseTime(scope.row.startTime,'{y}-{m}-{d} {h}') }}</span>
          </template>
        </el-table-column>
        <el-table-column label="生产时长" align="center" prop="duration" />
        <el-table-column label="预计完成时间" align="center" prop="endTime" width="180">
          <template #default="scope">
            <span>{{ parseTime(scope.row.endTime,'{y}-{m}-{d} {h}') }}</span>
          </template>
        </el-table-column>
        <el-table-column label="是否质检" align="center" prop="isCheck" >
          <template #default="scope">
          <dict-tag :options="dict.type.sys_yes_no" :value="scope.row.isCheck"/>
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
      <template #footer><div class="dialog-footer">
      <el-button type="primary" @click="confirmSelect">确 定</el-button>
      <el-button @click="showFlag=false">取 消</el-button>
    </div></template>
    </el-dialog>
  </template>

  <script>
  import { listProtask} from "@/yunshu-ui/api/mes/pro/protask";
  import {listAllProcess} from "@/yunshu-ui/api/mes/pro/process";
  import WorkstationSelect from "@/yunshu-ui/components/workstationSelect/simpletableSingle.vue"
  export default {
    name: "ProtaskSelect",
    components: {WorkstationSelect},
    dicts: ['sys_yes_no'],
    props: {
      workorderId: null,
      workorderCode: null,
      workstationId: null,
      workstationCode: null,      
      processId: null,
      processCode: null,
      workstationId: null,
      workstationCode: null
    },
    watch: {
      workorderId(v){
        this.queryParams.workorderId = v;
      },
      workstationId(v){
        this.queryParams.workstationId = v;
      }
    },
    data() {
      return {
        showFlag: false,
        selectedTaskId: undefined,
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
        // 生产任务表格数据
        protaskList: [],
        // 弹出层标题
        title: "",
        // 是否显示弹出层
        open: false,
        // 查询参数
        queryParams: {
          pageNum: 1,
          pageSize: 10,
          taskCode: null,
          taskName: null,
          workorderId: this.workorderId,
          workorderCode: this.workorderCode,
          workorderName: null,
          workstationId: this.workstationId,
          workstationCode: this.workstationCode,
          workstationName: null,
          routeId: null,
          processId: this.processId,
          processCode: null,
          processName: null,
          itemId: null,
          itemCode: null,
          itemName: null,
          specification: null,
          unitOfMeasure: null,
          quantity: null,
          quantityProduced: null,
          quantityChanged: null,
          status: null,
          clientId: null,
          clientCode: null,
          clientName: null,
          clientNick: null,
          startTime: null,
          duration: null,
          endTime: null,
          colorCode: null,
          requestDate: null,
        },
        // 表单参数
        form: {},
        // 表单校验
        rules: {
          workstationId: [
            { required: true, message: "工作站不能为空", trigger: "blur" }
          ],
          quantity: [
            { required: true, message: "排产数量不能为空", trigger: "blur" }
          ],
          startTime: [
            { required: true, message: "请选择开始生产日期",trigger: "blur"}
          ],
          duration: [
            { required: true, message: "清输入估算的生产用时",trigger: "blur"}
          ]
        }
      };
    },
    created() {
      this.getList();
      this.getProcess();
    },
    methods: {
      handleOpen(id) {
        this.showFlag = true
        this.selectedTaskId = id
        this.getList();
        this.getProcess();
      },
      /** 查询生产任务列表 */
      getList() {
        this.loading = true;
        listProtask(this.queryParams).then(response => {
          this.protaskList = response.rows.filter(item => item.status !== 'FINISHED' && item.status !== 'CANCELED' );
          this.total = response.total;
          this.loading = false;
        });
      },
      //查询工序信息
      getProcess(){
        listAllProcess().then( response =>{
          this.processOptions = response.data;
        });
      },

      /** 搜索按钮操作 */
      handleQuery() {
        this.queryParams.pageNum = 1;
        this.getList();
      },
      /** 重置按钮操作 */
      resetQuery() {
        this.queryParams.processName = ''
        this.queryParams.workstationCode = ''
        this.queryParams.taskCode = ''
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
          if(this.selectedTaskId ==null || this.selectedTaskId==0){
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
