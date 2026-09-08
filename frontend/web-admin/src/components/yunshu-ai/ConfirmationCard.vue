<template>
  <div class="conf-card">
    <div class="conf-head">
      <span class="conf-badge" :class="data.risk_level">{{ riskLabel }}</span>
      <strong>{{ data.action_name }}</strong>
    </div>
    <p class="conf-module">{{ data.business_module }} · {{ data.agent_name }}</p>
    <dl class="conf-dl">
      <dt>影响对象</dt><dd>{{ data.target }}</dd>
      <dt>风险说明</dt><dd>{{ data.risk_note || "—" }}</dd>
      <dt>预期结果</dt><dd>{{ data.expected_result || "—" }}</dd>
    </dl>
    <div v-if="data.status === 'pending'" class="conf-actions">
      <button class="btn-approve" :disabled="loading" @click="$emit('approve', data.confirmation_id)">批准执行</button>
      <button class="btn-reject" :disabled="loading" @click="$emit('reject', data.confirmation_id)">取消</button>
    </div>
    <p v-else class="conf-done">状态：{{ data.status === "approved" ? "已批准" : "已取消" }}</p>
  </div>
</template>

<script setup>
import { computed } from "vue";

const props = defineProps({
  data: { type: Object, required: true },
  loading: { type: Boolean, default: false },
});
defineEmits(["approve", "reject"]);

const riskLabel = computed(() => ({
  low: "低风险", medium: "中风险", high: "高风险",
}[props.data.risk_level] || props.data.risk_level));
</script>

<style scoped>
.conf-card { border: 1px solid #e8e8e8; border-radius: 14px; padding: 16px 18px; background: #fafafa; margin-top: 12px; }
.conf-head { display: flex; align-items: center; gap: 10px; font-size: 15px; color: #222; }
.conf-badge { font-size: 11px; padding: 2px 8px; border-radius: 999px; background: #fff3e0; color: #e65100; }
.conf-badge.high { background: #ffebee; color: #c62828; }
.conf-badge.low { background: #e8f5e9; color: #2e7d32; }
.conf-module { margin: 6px 0 10px; font-size: 13px; color: #888; }
.conf-dl { margin: 0; display: grid; grid-template-columns: 72px 1fr; gap: 4px 8px; font-size: 13px; }
.conf-dl dt { color: #999; }
.conf-dl dd { margin: 0; color: #333; }
.conf-actions { display: flex; gap: 10px; margin-top: 14px; }
.btn-approve { height: 34px; padding: 0 16px; border: none; background: #1a1a1a; color: #fff; border-radius: 8px; cursor: pointer; font-size: 13px; }
.btn-reject { height: 34px; padding: 0 16px; border: 1px solid #ddd; background: #fff; border-radius: 8px; cursor: pointer; font-size: 13px; }
.conf-done { margin: 12px 0 0; font-size: 13px; color: #666; }
</style>
