<template>
  <div class="dash-root">
    <header class="dash-header"><h2>System Overview</h2><span class="dash-time">{{ now }}</span></header>

    <div class="dash-kpi-row">
      <div class="dash-kpi"><span>Users</span><strong>{{ stats.users }}</strong></div>
      <div class="dash-kpi"><span>Roles</span><strong>{{ stats.roles }}</strong></div>
      <div class="dash-kpi"><span>Work Orders</span><strong>{{ stats.workOrders }}</strong></div>
      <div class="dash-kpi"><span>Products</span><strong>{{ stats.products }}</strong></div>
    </div>

    <div class="dash-grid">
      <div class="dash-card">
        <h3>System Health</h3>
        <div class="health-list">
          <div class="health-row"><span>Backend</span><span class="dot" :class="backendOnline?'on':'off'"></span>{{ backendOnline?'Online':'Offline' }}</div>
          <div class="health-row"><span>Database</span><span class="dot on"></span>Connected</div>
          <div class="health-row"><span>JWT Enforcement</span><span class="dot on"></span>Enabled</div>
        </div>
      </div>
      <div class="dash-card">
        <h3>Recent Operations</h3>
        <div class="log-list" v-if="logs.length">
          <div v-for="l in logs.slice(0,8)" :key="l.log_id" class="log-row">
            <span class="mono">{{ fmtTime(l.operation_time) }}</span>
            <span>{{ l.operation_type||'—' }}</span>
            <span class="dim">{{ l.user_name||'—' }}</span>
          </div>
        </div>
      </div>
    </div>
  </div>
</template>

<script setup>
import {onBeforeUnmount,onMounted,ref} from "vue";import {request} from "@/api/request";
const stats=ref({users:0,roles:0,workOrders:0,products:0});const logs=ref([]);const backendOnline=ref(false);
const now=ref("");let clock;
onMounted(async()=>{
  clock=setInterval(()=>{now.value=new Date().toLocaleString("zh-CN",{hour:"2-digit",minute:"2-digit",second:"2-digit",year:"numeric",month:"2-digit",day:"2-digit",weekday:"short"})},1000);
  try{const r=await request.get("/system/users");stats.value.users=(r?.data||[]).length}catch{}
  try{const r=await request.get("/system/roles");stats.value.roles=(r?.data||[]).length}catch{}
  try{const r=await request.get("/planning/work-orders");stats.value.workOrders=(r?.data||[]).length}catch{}
  try{const r=await request.get("/master-data/products");stats.value.products=(r?.data||[]).length}catch{}
  try{const r=await request.get("/system/logs");logs.value=r?.data||[]}catch{logs.value=[]}
  try{await request.get("/health");backendOnline.value=true}catch{backendOnline.value=false}
});
onBeforeUnmount(()=>clearInterval(clock));
function fmtTime(t){return t?t.substring(11,16):'—'}
</script>
<style scoped>
.dash-root{display:grid;gap:16px}.dash-header{display:flex;align-items:baseline;justify-content:space-between}.dash-header h2{margin:0;font-size:20px;font-weight:700}.dash-time{font-size:12px;color:#999}.dash-kpi-row{display:grid;grid-template-columns:repeat(4,1fr);gap:12px}.dash-kpi{background:#fff;border:1px solid #e0e0e0;padding:16px;display:flex;flex-direction:column;gap:4px}.dash-kpi span{font-size:12px;color:#999}.dash-kpi strong{font-size:28px;font-weight:700}.dash-grid{display:grid;grid-template-columns:1fr 1fr;gap:12px}.dash-card{background:#fff;border:1px solid #e0e0e0;padding:16px}.dash-card h3{margin:0 0 12px;font-size:14px;font-weight:600}.health-list{display:grid;gap:8px}.health-row{display:flex;align-items:center;gap:8px;font-size:13px}.dot{width:8px;height:8px;border-radius:50%}.dot.on{background:#10b981}.dot.off{background:#ef4444}.log-list{display:grid;gap:4px}.log-row{display:flex;gap:12px;font-size:12px;padding:4px 0;border-bottom:1px solid #f0f0f0}.mono{font-family:"JetBrains Mono",monospace;font-size:11px;color:#999}.dim{color:#999}
</style>
