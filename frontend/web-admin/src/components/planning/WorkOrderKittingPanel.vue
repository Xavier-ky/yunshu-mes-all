<template>
  <div class="kit-panel">
    <div v-if="!workOrderId" class="kit-empty">请先选择工单</div>
    <template v-else>
      <div class="kit-toolbar">
        <button type="button" class="plan-btn primary" :disabled="loading" @click="analyze">
          {{ loading ? "分析中…" : "分析齐套" }}
        </button>
        <button v-if="result && !result.allSufficient" type="button" class="plan-btn" @click="reserve">
          锁定库存
        </button>
        <button v-if="result" type="button" class="plan-btn" @click="release">释放锁定</button>
      </div>

      <div v-if="result" class="kit-summary" :class="result.allSufficient ? 'ok' : 'warn'">
        <strong>{{ result.allSufficient ? "齐套充足" : "存在欠料" }}</strong>
        <span>{{ result.workOrderNo }} · {{ result.productName }} · 计划 {{ result.planQty }}</span>
      </div>

      <MesDataTable
        v-if="result?.details?.length"
        :columns="columns"
        :rows="result.details"
        row-key-field="materialId"
        :row-class-fn="rowClassFn"
      >
        <template #cell-shortage="{ row }">
          <span :class="row.sufficient ? '' : 'short'">{{ row.sufficient ? "0" : num(row.shortage) }}</span>
        </template>
        <template #cell-sufficient="{ row }">
          <span :class="row.sufficient ? 'tag-ok' : 'tag-warn'">{{ row.sufficient ? "充足" : "短缺" }}</span>
        </template>
      </MesDataTable>
      <div v-else-if="analyzed && !loading" class="kit-empty">暂无 BOM 明细或库存数据</div>
    </template>
  </div>
</template>

<script setup>
import { ref, watch } from "vue";
import MesDataTable from "@/components/MesDataTable.vue";
import { fetchKitting, reserveKitting, releaseKitting } from "@/api/planning";

const props = defineProps({
  workOrderId: { type: Number, default: null },
});

const result = ref(null);
const loading = ref(false);
const analyzed = ref(false);

const columns = [
  { key: "materialCode", label: "物料编码", type: "code" },
  { key: "materialName", label: "物料名称" },
  { key: "isKey", label: "关键", type: "text" },
  { key: "qtyPer", label: "单耗", align: "right", type: "number" },
  { key: "required", label: "需求量", align: "right", type: "number" },
  { key: "available", label: "可用", align: "right", type: "number" },
  { key: "shortage", label: "短缺", align: "right" },
  { key: "sufficient", label: "状态" },
];

watch(
  () => props.workOrderId,
  (id) => {
    result.value = null;
    analyzed.value = false;
    if (id) analyze();
  },
  { immediate: true }
);

function rowClassFn(row) {
  return row.sufficient ? "" : "row-warn";
}

function num(v) {
  return Number(v || 0).toFixed(2);
}

async function analyze() {
  if (!props.workOrderId) return;
  loading.value = true;
  try {
    const r = await fetchKitting(props.workOrderId);
    result.value = r?.data || null;
    analyzed.value = true;
  } catch (e) {
    alert(e?.message || "齐套分析失败");
    result.value = null;
  } finally {
    loading.value = false;
  }
}

async function reserve() {
  try {
    await reserveKitting(props.workOrderId);
    alert("库存已锁定");
    await analyze();
  } catch (e) {
    alert(e?.message || "锁定失败");
  }
}

async function release() {
  try {
    await releaseKitting(props.workOrderId);
    alert("锁定已释放");
    await analyze();
  } catch (e) {
    alert(e?.message || "释放失败");
  }
}

defineExpose({ analyze });
</script>

<style scoped>
.kit-panel {
  display: grid;
  gap: 12px;
}
.kit-toolbar {
  display: flex;
  gap: 8px;
  flex-wrap: wrap;
}
.kit-summary {
  padding: 12px 16px;
  border: 1px solid #e7e7e7;
  display: flex;
  flex-direction: column;
  gap: 4px;
}
.kit-summary.ok {
  border-left: 3px solid #0abf5b;
  background: #f0fdf4;
}
.kit-summary.warn {
  border-left: 3px solid #ff9d00;
  background: #fffbeb;
}
.kit-summary strong {
  font-size: 15px;
}
.kit-summary span {
  font-size: 12px;
  color: #666;
}
.kit-empty {
  padding: 32px;
  text-align: center;
  color: #bbb;
  font-size: 13px;
}
.short {
  color: #e34d59;
  font-weight: 600;
}
.tag-ok {
  color: #0abf5b;
  font-weight: 600;
  font-size: 12px;
}
.tag-warn {
  color: #ff9d00;
  font-weight: 600;
  font-size: 12px;
}
</style>
