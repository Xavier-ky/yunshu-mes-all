<template>
  <aside class="cal-workbench-list process-workbench-list">
    <div class="cal-workbench-list__body" v-loading="loading">
      <button
        v-for="item in processes"
        :key="item.processId"
        type="button"
        class="cal-workbench-list__item process-workbench-list__item"
        :class="{ 'is-active': item.processId === selectedProcessId }"
        @click="$emit('select', item)"
      >
        <img
          v-if="coverOf(item)"
          :src="coverOf(item)"
          alt=""
          class="process-cover-thumb"
          loading="lazy"
        />
        <span v-else class="process-cover-thumb process-cover-thumb--empty">工序</span>
        <span class="process-workbench-list__text">
          <span class="cal-workbench-list__item-head">
            <b>{{ item.processName || "未命名工序" }}</b>
            <small>{{ item.processCode }}</small>
          </span>
          <span class="cal-workbench-list__item-meta">
            <span>{{ item.enableFlag === "N" ? "停用" : "启用" }}</span>
          </span>
        </span>
      </button>
      <div v-if="!loading && !processes.length" class="cal-workbench-list__empty">
        <b>暂无工序</b>
        <span>调整筛选条件或点击新增</span>
      </div>
    </div>
    <div v-if="total > 0" class="cal-workbench-list__footer">
      <span class="cal-workbench-list__footer-total">共 {{ total }} 条</span>
      <pagination
        :total="total"
        :page="page"
        :limit="pageSize"
        layout="prev, pager, next"
        :pager-count="5"
        :auto-scroll="false"
        @update:page="$emit('update:page', $event)"
        @update:limit="$emit('update:pageSize', $event)"
      />
    </div>
  </aside>
</template>

<script setup>
import { resolveProcessCover } from "../processImageMap.js";

defineProps({
  processes: { type: Array, default: () => [] },
  selectedProcessId: { type: [Number, String], default: null },
  loading: Boolean,
  total: { type: Number, default: 0 },
  page: { type: Number, default: 1 },
  pageSize: { type: Number, default: 10 },
});

defineEmits(["select", "update:page", "update:pageSize"]);

function coverOf(item) {
  return resolveProcessCover(item);
}
</script>
