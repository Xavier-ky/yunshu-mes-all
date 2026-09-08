<template>
  <div class="chart-card">
    <div class="cc-head">
      <h3>{{ title }}</h3>
      <slot name="head-extra"></slot>
    </div>
    <div class="cc-body">
      <slot></slot>
    </div>
  </div>
</template>

<script setup>
defineProps({ title: String });
</script>

<style scoped>
.chart-card {
  background: #fff;
  border: 1px solid #e8eaed;
  border-radius: 8px;
  overflow: hidden;
}
.cc-head {
  display: flex;
  align-items: center;
  justify-content: space-between;
  padding: 16px 20px 0;
}
.cc-head h3 {
  margin: 0;
  font-size: 14px;
  font-weight: 600;
  color: #1a1a1a;
}
.cc-body {
  padding: 8px 12px 16px;
}
</style>
