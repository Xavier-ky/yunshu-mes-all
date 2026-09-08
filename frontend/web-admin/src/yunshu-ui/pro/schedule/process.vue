<template>
    <div class="app-container">
      <el-table v-loading="loading" :data="protaskList">
        <el-table-column type="selection" width="55" align="center" />      
        <el-table-column label="工序名称" align="center" width="120px" prop="processName" :show-overflow-tooltip="true"/>   
        <el-table-column label="排产数量" align="center" prop="quantity" />
        <el-table-column label="已生产数量" align="center" width="100px" prop="quantityProduced" />
        <el-table-column label="生产进度" align="center" width="150px" prop="quantityProduced" >
            <template #default="scope">                
                <el-progress v-if="scope.row.quantity !=0" :text-inside="true" :stroke-width="26" :percentage="(scope.row.quantityProduced/scope.row.quantity*100).toFixed(2)"></el-progress>
                <el-progress v-else :stroke-width="26" :text-inside="true" :percentage="0"></el-progress>
            </template>
        </el-table-column>
      </el-table>
    </div>
  </template>
  
  <script>
  import { listTaskListByWorkorder, getProtask, delProtask, addProtask, updateProtask } from "@/yunshu-ui/api/mes/pro/protask";
  import WorkstationSelect from "@/yunshu-ui/components/workstationSelect/simpletableSingle.vue"
  import WorkorderSelect from "@/yunshu-ui/components/workorderSelect/single.vue"
  export default {
    name: "Protask",
    dicts: ['mes_task_status'],
    components: {WorkorderSelect,WorkstationSelect},
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
          workorderCode: null,
          workorderName: null,
          workstationId: null,
          workstationCode: null,
          workstationName: null,
          routeId: this.routeId,
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
          workorderCode: [
            { required: true, message: "请选择生产工单", trigger: "blur" }
          ],
          workstationName: [
            { required: true, message: "请选择工作站", trigger: "blur" }
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
    props:{
        workorderId: null,
        colorCode: null,
        routeId: null,
        processId: null,
        optType: null
    },
    watch: {
        workorderId(){
            this.queryParams.workorderId = this.workorderId; 
        }        
    },
    created() {
      this.getList();
    },
    methods: {
      /** 查询生产任务列表 */
      getList() {
        this.loading = true;
        listTaskListByWorkorder(this.queryParams).then(response => {
          this.protaskList = response.data;
          this.loading = false;
        },err =>{
            this.loading = false;
        });
      }    
    }
  };
  </script>
  