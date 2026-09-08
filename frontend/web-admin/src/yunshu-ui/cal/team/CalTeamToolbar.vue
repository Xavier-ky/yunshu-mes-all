<template>
  <div class="cal-workbench-toolbar">
    <div class="cal-workbench-toolbar__period">
      <strong>班组维护</strong>
    </div>
    <div class="cal-workbench-toolbar__filters">
      <el-select
        :model-value="queryParams.calendarType"
        clearable
        placeholder="班组类型"
        class="cal-workbench-toolbar__select"
        @update:model-value="$emit('update:calendarType', $event || '')"
      >
        <el-option
          v-for="item in calendarTypes"
          :key="item.value"
          :label="item.label"
          :value="item.value"
        />
      </el-select>
      <el-input
        :model-value="queryParams.teamCode"
        clearable
        placeholder="班组编号"
        class="cal-workbench-toolbar__select"
        @keyup.enter="$emit('search')"
        @update:model-value="$emit('update:teamCode', $event)"
      />
      <el-input
        :model-value="queryParams.teamName"
        clearable
        placeholder="班组名称"
        class="cal-workbench-toolbar__select"
        @keyup.enter="$emit('search')"
        @update:model-value="$emit('update:teamName', $event)"
      />
      <el-button type="primary" @click="$emit('search')">搜索</el-button>
      <el-button @click="$emit('reset')">重置</el-button>
      <el-button type="primary" plain @click="$emit('create')">新增</el-button>
      <el-button plain @click="$emit('export')">导出</el-button>
      <el-button :icon="Refresh" :loading="loading" title="刷新" @click="$emit('refresh')" />
      <el-button link type="primary" @click="$emit('navigate-tab', 'overview')">日历总览</el-button>
      <el-button link type="primary" @click="$emit('navigate-tab', 'holiday')">节假日</el-button>
      <el-button link type="primary" @click="$emit('navigate-tab', 'plan')">排班计划</el-button>
    </div>
  </div>
</template>

<script setup>
import { Refresh } from "@element-plus/icons-vue";

defineProps({
  queryParams: { type: Object, required: true },
  calendarTypes: { type: Array, default: () => [] },
  loading: Boolean,
});

defineEmits([
  "search",
  "reset",
  "create",
  "export",
  "refresh",
  "navigate-tab",
  "update:calendarType",
  "update:teamCode",
  "update:teamName",
]);
</script>
