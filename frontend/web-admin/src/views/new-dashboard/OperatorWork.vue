<template>
  <div class="op-root">
    <h2>我的工位</h2>
    <div class="op-section">
      <h3>待处理任务</h3>
      <div class="op-table-wrap"><table class="op-table"><thead><tr><th>派工编号</th><th>工序</th><th>工位</th><th>计划量</th><th>已完成</th><th>状态</th><th>操作</th></tr></thead>
      <tbody><tr v-for="t in tasks" :key="t.dispatchId||t.dispatch_id"><td class="mono">{{t.dispatchNo||t.dispatch_no}}</td><td>{{t.stepName||t.step_name||'—'}}</td><td>{{t.stationName||t.station_name||'—'}}</td><td>{{t.plannedQty||t.planned_qty}}</td><td>{{t.completedQty||t.completed_qty||0}}</td><td><span :class="tagC(t.status)">{{statusLabel(t.status)}}</span></td><td><button v-if="t.status!=='COMPLETED'" class="op-btn sm" @click="openReport(t)">报工</button></td></tr></tbody></table></div>
    </div>

    <Teleport to="body"><div v-if="rm.open" class="op-overlay" @click.self="rm.open=false"><div class="op-modal"><div class="op-modal-head"><h3>生产报工</h3><button class="op-btn ghost sm" @click="rm.open=false">关闭</button></div><div class="op-modal-body">
      <label class="op-field"><span>良品数量 *</span><input v-model="rf.goodQty" type="number"/></label>
      <label class="op-field"><span>不良数量</span><input v-model="rf.defectQty" type="number"/></label>
      <label class="op-field"><span>备注</span><input v-model="rf.remark"/></label>
    </div><div class="op-modal-foot"><button class="op-btn sec" @click="rm.open=false">取消</button><button class="op-btn pri" @click="submitReport">提交报工</button></div></div></div></Teleport>
  </div>
</template>

<script setup>
import {onMounted,reactive,ref} from "vue";import {request} from "@/api/request";import { getAuthUser } from "@/utils/auth-context";
const tasks=ref([]);const rf=reactive({goodQty:0,defectQty:0,remark:""});const rm=reactive({open:false});const currentTask=ref(null);
onMounted(async()=>{try{const r=await request.get("/planning/dispatch-tasks");const u=getAuthUser()||{};tasks.value=(r?.data||[]).filter(t=>String(t.operatorId)===String(u.userId))}catch{tasks.value=[]}});
function openReport(t){currentTask.value=t;rf.goodQty=0;rf.defectQty=0;rf.remark="";rm.open=true}
async function submitReport(){try{await request.post("/production/reports",{reportNo:"RPT-"+Date.now(),workOrderId:currentTask.value.workOrderId,stepId:currentTask.value.stepId,operatorId:currentTask.value.operatorId,reportType:"NORMAL",goodQty:Number(rf.goodQty),defectQty:Number(rf.defectQty),remark:rf.remark});alert("报工成功");rm.open=false;const r=await request.get("/planning/dispatch-tasks");const u=getAuthUser()||{};tasks.value=(r?.data||[]).filter(t=>String(t.operatorId)===String(u.userId))}catch(e){alert(e?.message||"报工失败")}}
function statusLabel(s){return s==='CREATED'?'新建':s==='RUNNING'?'进行中':s==='COMPLETED'?'已完成':s}
function tagC(s){return s==='CREATED'?'blue':s==='RUNNING'?'amber':s==='COMPLETED'?'green':''}
</script>
<style scoped>
.op-root{display:grid;gap:16px}.op-root h2{margin:0;font-size:20px;font-weight:700}.op-section h3{margin:0 0 8px;font-size:14px;font-weight:600}.op-table-wrap{background:#fff;border:1px solid #e0e0e0;overflow:hidden}.op-table{width:100%;border-collapse:collapse}.op-table th,.op-table td{text-align:left;padding:10px 14px;font-size:13px;border-bottom:1px solid #eee}.op-table th{font-size:11px;color:#999;font-weight:600;text-transform:uppercase;background:#fafafa}.mono{font-family:"JetBrains Mono",monospace;font-size:12px}.blue{color:#2563eb;font-weight:600}.amber{color:#d97706;font-weight:600}.green{color:#059669;font-weight:600}.op-btn{height:34px;padding:0 16px;border-radius:0;border:none;font-size:13px;cursor:pointer}.op-btn.pri{background:#1a1a1a;color:#fff}.op-btn.sec{background:#fff;border:1px solid #d0d0d0;color:#555}.op-btn.ghost{background:transparent;color:#999}.op-btn.sm{height:26px;font-size:12px;padding:0 10px}.op-overlay{position:fixed;inset:0;background:rgba(0,0,0,0.2);display:grid;place-items:center;z-index:100}.op-modal{width:min(400px,calc(100vw-32px));background:#fff;border:1px solid #e0e0e0}.op-modal-head{display:flex;align-items:center;justify-content:space-between;padding:14px 20px;border-bottom:1px solid #e8e8e8}.op-modal-head h3{margin:0;font-size:15px}.op-modal-body{padding:16px 20px;display:grid;gap:12px}.op-modal-foot{display:flex;justify-content:flex-end;gap:10px;padding:12px 20px;border-top:1px solid #e8e8e8}.op-field{display:grid;gap:5px}.op-field span{font-size:12px;color:#777}.op-field input{height:36px;padding:0 10px;border:1px solid #d0d0d0;font-size:13px;outline:none}
</style>
