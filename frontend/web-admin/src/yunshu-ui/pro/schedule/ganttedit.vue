<template>
    <div class="app-container yunshu-gantt-edit">
    <el-row :gutter="10" class="mb8">
      <el-col :span="1"></el-col>
      <el-col :span="1.5">
        <el-button
          type="success"
          plain
          icon="el-icon-edit"
          size="small"
          @click="handleUpdate"
          v-hasPermi="['mes:pro:protask:edit']"
        >保存</el-button>
      </el-col>
      <el-col :span="1.5">
        <el-button
          type="danger"
          plain
          icon="el-icon-delete"
          size="small"
          @click="handleClose"
        >关闭</el-button>
      </el-col>
      <el-col :span="1.5">
        <el-button
          type="warning"
          plain
          icon="el-icon-refresh"
          size="small"
          @click="handleRefresh"
        >重新加载</el-button>
      </el-col>
    </el-row>
    <div class="wrapper">
      <div class="container">
        <GanttChar class="left-container" ref="ganttChar" optType="edit" :tasks="tasks" @task-changed="onTaskChanged"></GanttChar>
      </div>      
    </div> 
    </div>
</template>

<script>
import GanttChar from "./ganttx.vue";
import {listGanttTaskList,updateProtask} from "@/yunshu-ui/api/mes/pro/protask";
export default {
    name:'',
    components:{GanttChar},
    data(){
        return {
            tasks:{
                data: [],
                links: []
            },
            changedTaskIds:[],
            optType:'edit',
            // 查询参数
            queryParams: {
                workorderCode: null,
                workorderName: null,
                orderSource: null,
                sourceCode: null,
                productId: null,
                productCode: null,
                productName: null,
                productSpc: null,
                unitOfMeasure: null,
                quantity: null,
                quantityProduced: null,
                quantityChanged: null,
                quantityScheduled: null,
                clientId: null,
                clientCode: null,
                clientName: null,
                requestDate: null,
                parentId: null,
                ancestors: null,
                status: null,
            },
            form:{
                taskId: null,                              
                startTime: null,
                duration: null,
                endTime: null            
            }
        }
    },
    watch: {
        tasks(){
            console.log("TaskChanged");
        }
    },
    created(){
        this.getGanttTasks();
    },
    methods: {
        onTaskChanged(id) {
            if (!this.changedTaskIds.includes(id)) {
                this.changedTaskIds.push(id);
            }
        },
        getGanttTasks(){
            listGanttTaskList(this.queryParams).then(response => {
                const payload = response?.data || {};
                this.tasks.data = payload.data || [];
                this.tasks.links = payload.links || [];
                this.$nextTick(() => {
                    this.$refs.ganttChar?.reload();
                });
            }).catch(() => {
                this.tasks.data = [];
                this.tasks.links = [];
            });
        },
        handleUpdate(){
            debugger;
            if(this.changedTaskIds.length>0){
                this.reset();
                this.changedTaskIds.forEach(id => {
                    debugger;                    
                    let nTask = this.tasks.data.filter(task => task.id == id)[0];
                    if(nTask.id.startsWith("MO")){
                        return; //如果是工单改变，不进行保存。
                    }
                    let startDate = new Date(nTask.start_date);
                    let endDate = new Date(nTask.end_date);
                    this.form = nTask;
                    this.form.taskId = nTask.id;
                    this.form.startTime = startDate.getFullYear()+'-'+(startDate.getMonth()+1)+'-'+startDate.getDate()+' '+startDate.getHours()+':'+startDate.getMinutes()+':'+startDate.getSeconds();
                    this.form.duration = nTask.duration;
                    this.form.endTime = endDate.getFullYear()+'-'+(endDate.getMonth()+1)+'-'+endDate.getDate()+' '+endDate.getHours()+':'+endDate.getMinutes()+':'+endDate.getSeconds();
                    updateProtask(this.form).then(response =>{
                        console.log("update success:"+id);
                        this.$modal.msgSuccess("保存成功");
                    },err =>{
                        debugger;
                        console.log("update failed:"+err.msg);
                        this.$modal.msgSuccess("保存失败");
                    });
                });
                //this.changedTaskIds =[];                
                
            }            
        },
        handleRefresh(){
            this.$refs.ganttChar.reload();
        },
        handleClose(){
            this.$router.push({ path: "/app/planning/scheduling" });
        },
        reset() {
            this.form = {
                taskId: null,
                startTime: null,
                duration: null,
                endTime: null,
            };
        },

    }
}
</script>

<style scoped>
/* 布局由 @/styles/yunshu-ui.css 统一控制 */
</style>