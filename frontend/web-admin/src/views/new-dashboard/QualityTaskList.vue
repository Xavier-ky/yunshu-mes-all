<template>
  <CrudManager
    entity-label="质检任务"
    row-key="qualityTaskId"
    :columns="columns"
    :tag-map="tagMap"
    :creatable="false"
    :editable="false"
    :fetch-api="fetchQualityTasks"
  />
</template>

<script setup>
import CrudManager from "@/components/CrudManager.vue";
import { fetchQualityTasks } from "@/api/quality";

const columns = [
  { key: "qualityTaskNo", label: "任务编号", mono: true },
  { key: "productName", label: "产品" },
  { key: "inspectType", label: "检验类型" },
  { key: "status", label: "状态", type: "tag" },
];
const tagMap = { PENDING: "待检验", IN_PROGRESS: "检验中", COMPLETED: "已完成" };
</script>
