<template>
  <div class="plan-timeline">
    <div v-if="!items.length" class="plan-tl-empty">暂无记录</div>
    <div v-for="(item, i) in items" :key="item.id || i" class="plan-timeline-item">
      <div class="plan-timeline-dot" :class="item.tone || ''" />
      <div class="plan-timeline-body">
        <div class="plan-timeline-title">{{ item.title }}</div>
        <div class="plan-timeline-meta">{{ item.meta }}</div>
        <div v-if="item.remark" class="plan-timeline-meta">{{ item.remark }}</div>
      </div>
    </div>
  </div>
</template>

<script setup>
defineProps({
  items: { type: Array, default: () => [] },
});
</script>

<style scoped>
.plan-tl-empty {
  padding: 24px;
  text-align: center;
  color: #ccc;
  font-size: 13px;
}
</style>
