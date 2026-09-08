<template>
  <div class="cal-workbench-toolbar">
    <div class="cal-workbench-toolbar__period">
      <el-button-group>
        <el-button :icon="ArrowLeft" title="上一月" @click="$emit('navigate', -1)" />
        <el-button @click="$emit('navigate', 0)">今天</el-button>
        <el-button :icon="ArrowRight" title="下一月" @click="$emit('navigate', 1)" />
      </el-button-group>
      <strong>{{ periodLabel }}</strong>
    </div>

    <div class="cal-workbench-toolbar__filters">
      <el-button :icon="Refresh" :loading="loading" title="刷新" @click="$emit('refresh')" />
      <el-button link type="primary" @click="$emit('navigate-tab', 'overview')">日历总览</el-button>
    </div>
  </div>
</template>

<script setup>
import { ArrowLeft, ArrowRight, Refresh } from "@element-plus/icons-vue";

defineProps({
  periodLabel: { type: String, default: "" },
  loading: Boolean,
});

defineEmits(["navigate", "refresh", "navigate-tab"]);
</script>
