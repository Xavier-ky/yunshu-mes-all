<template>
  <div class="ad-root">
    <div class="ad-top"><div class="ad-hero"><h2>安灯中心</h2><p>实时异常事件监控与处理</p></div><button class="ad-add" @click="openCreate">+ 发起安灯</button></div>
    <div class="ad-grid"><div v-for="e in list" :key="e.andonId||e.andon_id" class="ad-card" :class="e.status==='OPEN'?'warn':e.status==='CLOSED'?'done':''"><div class="ad-card-hd"><span class="ad-no mono">{{e.andonNo||e.andon_no||'—'}}</span><span class="ad-tag" :class="tagC(e.status)">{{e.status==='OPEN'?'OPEN':e.status==='CLOSED'?'CLOSED':'PROCESSING'}}</span></div><div class="ad-card-bd"><p class="ad-desc">{{e.exceptionDesc||e.exception_desc||'—'}}</p><div class="ad-meta"><span>{{e.typeName||e.type_name||'—'}}</span><span>{{e.lineName||e.line_name||'—'}}</span><span class="ad-time">{{fmt(e.occurTime||e.occur_time)}}</span></div></div><div class="ad-card-ft"><button v-if="e.status!=='CLOSED'" class="ad-btn sm" @click="closeEvent(e)">Close</button></div></div></div>

    <Teleport to="body"><div v-if="modal" class="ad-overlay" @click.self="modal=false"><div class="ad-modal"><div class="ad-modal-head"><h3>发起安灯</h3><button class="ad-close" @click="modal=false">&times;</button></div><div class="ad-modal-body">
      <label class="ad-field"><span>类型</span><select v-model="f.andonTypeId"><option :value="null">选择</option><option v-for="t in types" :key="t.typeId" :value="t.typeId">{{t.typeName}}</option></select></label>
      <label class="ad-field"><span>原因</span><select v-model="f.reasonId"><option :value="null">选择</option><option v-for="r in reasons" :key="r.reasonId||r.andon_reason_id" :value="r.reasonId||r.andon_reason_id">{{r.reasonName||r.reason_name}}</option></select></label>
      <label class="ad-field"><span>产线</span><select v-model="f.lineId"><option :value="null">选择</option><option v-for="l in lines" :key="l.lineId" :value="l.lineId">{{l.lineName}}</option></select></label>
      <label class="ad-field"><span>描述</span><input v-model="f.exceptionDesc"/></label>
    </div><div class="ad-modal-foot"><button class="ad-btn sec" @click="modal=false">取消</button><button class="ad-btn pri" @click="submit">发起</button></div></div></div></Teleport>
  </div>
</template>
<script setup>
import {onMounted,reactive,ref} from "vue";import {request} from "@/api/request";
const list=ref([]),types=ref([]),reasons=ref([]),lines=ref([]),modal=ref(false);
const f=reactive({andonTypeId:null,reasonId:null,lineId:null,exceptionDesc:""});
async function load(){try{list.value=(await request.get("/andon/events"))?.data||[]}catch{list.value=[]};try{types.value=(await request.get("/andon/types"))?.data||[]}catch{};try{reasons.value=(await request.get("/andon/reasons"))?.data||[]}catch{};try{lines.value=(await request.get("/factory/lines"))?.data||[]}catch{}}
function openCreate(){Object.assign(f,{andonTypeId:null,reasonId:null,lineId:null,exceptionDesc:""});modal.value=true}
async function submit(){try{await request.post("/andon/events/create",{...f,andonTypeId:Number(f.andonTypeId),reasonId:Number(f.reasonId),lineId:Number(f.lineId),reportUserId:1});modal.value=false;await load()}catch(e){alert(e?.message)}}
async function closeEvent(e){if(confirm("Close?")){try{await request.put(`/andon/events/${e.andonId||e.andon_id}/close`,{status:"CLOSED"});await load()}catch(ex){alert(ex?.message)}}}
function tagC(s){return s==='OPEN'?'rd':s==='CLOSED'?'gn':'am'}
function fmt(t){return t?t.substring(0,16):'—'}
onMounted(load)
</script>
<style scoped>
.ad-root{display:grid;gap:16px}.ad-top{display:flex;align-items:flex-start;justify-content:space-between}.ad-hero h2{margin:0;font-size:22px;font-weight:700}.ad-hero p{margin:4px 0 0;color:#999;font-size:13px}.ad-add{height:38px;padding:0 16px;border:none;background:#1a1a1a;color:#fff;font-size:13px;font-weight:500;cursor:pointer}.ad-add:hover{background:#333}
.ad-grid{display:grid;grid-template-columns:repeat(auto-fill,minmax(340px,1fr));gap:10px}
.ad-card{background:#fff;border:1px solid #e0e0e0;padding:16px;display:flex;flex-direction:column;gap:12px}.ad-card.warn{border-left:3px solid #f59e0b}.ad-card.done{border-left:3px solid #10b981;opacity:.6}
.ad-card-hd{display:flex;align-items:center;justify-content:space-between}.ad-no{font-family:"JetBrains Mono",monospace;font-size:12px;color:#999}.ad-tag{font-size:10px;font-weight:700}.ad-tag.rd{color:#dc2626}.ad-tag.gn{color:#059669}.ad-tag.am{color:#d97706}
.ad-card-bd .ad-desc{margin:0 0 8px;font-size:13px;line-height:1.5}.ad-meta{display:flex;gap:16px;font-size:11px;color:#999}.ad-time{margin-left:auto}.ad-card-ft{display:flex;justify-content:flex-end}.ad-btn{height:32px;padding:0 14px;border:1px solid #1a1a1a;background:#fff;font-size:12px;cursor:pointer}.ad-btn.pri{background:#1a1a1a;color:#fff;border:none}.ad-btn.sec{background:#fff;border:1px solid #e0e0e0;color:#555}.ad-btn.sm{height:28px;font-size:11px;padding:0 10px}.ad-btn:hover{background:#f5f5f5}
.ad-overlay{position:fixed;inset:0;background:rgba(0,0,0,.15);display:grid;place-items:center;z-index:100}.ad-modal{width:min(440px,calc(100vw-32px));background:#fff;border:1px solid #e0e0e0}.ad-modal-head{display:flex;align-items:center;justify-content:space-between;padding:16px 24px;border-bottom:1px solid #eee}.ad-modal-head h3{margin:0;font-size:16px}.ad-close{border:none;background:none;font-size:22px;color:#bbb;cursor:pointer}.ad-modal-body{padding:20px 24px;display:grid;gap:14px}.ad-modal-foot{display:flex;justify-content:flex-end;gap:10px;padding:14px 24px;border-top:1px solid #eee}.ad-field{display:grid;gap:5px}.ad-field span{font-size:12px;color:#888}.ad-field input,.ad-field select{height:36px;padding:0 12px;border:1px solid #e0e0e0;font-size:13px;outline:none;background:#fafafa;color:#1a1a1a}.ad-field select option{color:#1a1a1a;background:#fff}
</style>
