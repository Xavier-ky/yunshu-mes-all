<template>
  <div class="kt-root">
    <div class="kt-hero"><h2>齐套分析</h2><p>基于 BOM 物料清单对比库存，计算齐套缺口并支持库存锁定</p></div>
    <div class="kt-selector">
      <div class="kt-f"><span>选择工单</span><select v-model="woId" @change="analyze"><option :value="null">— 请选择 —</option><option v-for="w in wos" :key="w.workOrderId" :value="w.workOrderId">{{ w.workOrderNo }} · {{ w.productName||'' }}</option></select></div>
      <button class="kt-btn-pri" @click="analyze" :disabled="!woId">分析齐套</button>
    </div>
    <div v-if="result" class="kt-result">
      <div class="kt-summary">
        <div class="kt-sum-card" :class="result.allSufficient?'ok':'warn'">
          <strong>{{ result.allSufficient?'齐套充足':'存在欠料' }}</strong>
          <span>{{ result.workOrderNo }} · {{ result.productName }} · 计划 {{ result.planQty }}</span>
        </div>
        <div class="kt-actions" v-if="!result.allSufficient">
          <button class="kt-btn-pri sm" @click="reserve">锁定库存</button>
          <button class="kt-btn-sec sm" @click="release">释放锁定</button>
        </div>
      </div>
      <div class="kt-tbl-w"><table class="kt-tbl"><thead><tr><th>物料编码</th><th>物料名称</th><th>关键</th><th>单台用量</th><th>损耗率</th><th>需求量</th><th>可用库存</th><th>短缺量</th><th>状态</th></tr></thead>
      <tbody><tr v-for="d in result.details" :key="d.materialId" :class="d.sufficient?'':'row-warn'">
        <td class="mono-s">{{ d.materialCode }}</td><td>{{ d.materialName }}</td><td>{{ d.isKey?'是':'否' }}</td><td>{{ d.qtyPer }}</td><td>{{ num(d.lossRate) }}%</td><td>{{ num(d.required) }}</td><td>{{ num(d.available) }}</td><td class="short">{{ d.sufficient?'0':num(d.shortage) }}</td>
        <td><span :class="d.sufficient?'tag-ok':'tag-warn'">{{ d.sufficient?'充足':'短缺' }}</span></td>
      </tr></tbody></table></div>
    </div>
    <div v-if="!result && woId" class="kt-empty">点击"分析齐套"开始计算</div>
  </div>
</template>
<script setup>
import {onMounted,ref} from "vue";import {request} from "@/api/request";
const wos=ref([]),woId=ref(null),result=ref(null);
onMounted(async()=>{try{wos.value=(await request.get("/planning/work-orders"))?.data||[]}catch{wos.value=[]};const q=new URLSearchParams(location.search);const w=q.get("wo");if(w){woId.value=Number(w);analyze()}});
async function analyze(){if(!woId.value)return;try{const r=await request.get(`/planning/kitting/${woId.value}`);result.value=r?.data||null}catch(e){alert(e?.message);result.value=null}}
async function reserve(){try{await request.post(`/planning/kitting/${woId.value}/reserve`);alert("库存已锁定");analyze()}catch(e){alert(e?.message)}}
async function release(){try{await request.post(`/planning/kitting/${woId.value}/release`);alert("锁定已释放");analyze()}catch(e){alert(e?.message)}}
function num(v){return Number(v||0).toFixed(2)}
</script>
<style scoped>
.kt-root{display:grid;gap:16px}.kt-hero h2{margin:0;font-size:20px;font-weight:700}.kt-hero p{margin:4px 0 0;color:#999;font-size:13px}.mono-s{font-family:"JetBrains Mono",monospace;font-size:12px;color:#666}
.kt-selector{display:flex;gap:12px;align-items:flex-end;padding:16px;background:#fff;border:1px solid #e0e0e0}.kt-f{display:grid;gap:5px;flex:1;max-width:400px}.kt-f span{font-size:12px;color:#888;font-weight:500}.kt-f select{height:38px;padding:0 12px;border:1px solid #e0e0e0;font-size:13px;background:#fff;color:#1a1a1a;outline:none}.kt-f select option{color:#1a1a1a;background:#fff}.kt-btn-pri{height:38px;padding:0 18px;border:none;background:#1a1a1a;color:#fff;font-size:13px;font-weight:500;cursor:pointer}.kt-btn-pri:hover:not(:disabled){background:#333}.kt-btn-pri:disabled{opacity:.3;cursor:default}.kt-btn-pri.sm{height:32px;font-size:12px;padding:0 14px}.kt-btn-sec{height:32px;padding:0 14px;border:1px solid #e0e0e0;background:#fff;font-size:12px;cursor:pointer;color:#555}.kt-btn-sec:hover{background:#f5f5f5}
.kt-summary{display:flex;align-items:center;justify-content:space-between;gap:16px}.kt-sum-card{padding:14px 18px;border:1px solid #e0e0e0;display:flex;flex-direction:column;gap:4px}.kt-sum-card.ok{border-left:3px solid #059669;background:#f0fdf4}.kt-sum-card.warn{border-left:3px solid #f59e0b;background:#fffbeb}.kt-sum-card strong{font-size:16px}.kt-sum-card span{font-size:12px;color:#777}.kt-actions{display:flex;gap:8px}
.kt-tbl-w{background:#fff;border:1px solid #e0e0e0;overflow:auto}.kt-tbl{width:100%;border-collapse:collapse;min-width:800px}.kt-tbl th{text-align:left;padding:10px 14px;font-size:11px;color:#999;font-weight:600;background:#fafafa;border-bottom:1px solid #eee;white-space:nowrap}.kt-tbl td{padding:8px 14px;font-size:13px;border-bottom:1px solid #f5f5f5}.kt-tbl tbody tr.row-warn{background:#fff9f0}.tag-ok{color:#059669;font-weight:600;font-size:12px}.tag-warn{color:#d97706;font-weight:600;font-size:12px}.short{color:#dc2626;font-weight:600}.kt-empty{padding:48px;text-align:center;color:#ccc;font-size:13px}
</style>
