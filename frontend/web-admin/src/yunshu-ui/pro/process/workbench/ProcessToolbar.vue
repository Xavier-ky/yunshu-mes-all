<template>
  <div class="cal-workbench-toolbar">
    <div class="cal-workbench-toolbar__period">
      <strong>工序库</strong>
    </div>
    <div class="cal-workbench-toolbar__filters">
      <el-input
        :model-value="queryParams.processCode"
        clearable
        placeholder="工序编码"
        class="cal-workbench-toolbar__select"
        @keyup.enter="$emit('search')"
        @update:model-value="$emit('update:processCode', $event)"
      />
      <el-input
        :model-value="queryParams.processName"
        clearable
        placeholder="工序名称"
        class="cal-workbench-toolbar__select"
        @keyup.enter="$emit('search')"
        @update:model-value="$emit('update:processName', $event)"
      />
      <el-select
        :model-value="queryParams.enableFlag"
        clearable
        placeholder="是否启用"
        class="cal-workbench-toolbar__select"
        @update:model-value="$emit('update:enableFlag', $event || '')"
      >
        <el-option v-for="dict in yesNoOptions" :key="dict.value" :label="dict.label" :value="dict.value" />
      </el-select>
      <el-button type="primary" @click="$emit('search')">搜索</el-button>
      <el-button @click="$emit('reset')">重置</el-button>
      <el-button type="primary" plain @click="$emit('create')">新增</el-button>
      <el-button plain @click="$emit('export')">导出</el-button>
      <el-button :loading="loading" @click="$emit('refresh')">刷新</el-button>
    </div>
  </div>
</template>

<script setup>
defineProps({
  queryParams: { type: Object, required: true },
  yesNoOptions: { type: Array, default: () => [] },
  loading: Boolean,
});

defineEmits([
  "search",
  "reset",
  "create",
  "export",
  "refresh",
  "update:processCode",
  "update:processName",
  "update:enableFlag",
]);
</script>
