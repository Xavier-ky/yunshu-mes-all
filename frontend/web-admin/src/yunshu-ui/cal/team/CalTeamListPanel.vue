<template>
  <aside class="cal-workbench-list">
    <div class="cal-workbench-list__body" v-loading="loading">
      <button
        v-for="team in teams"
        :key="team.teamId"
        type="button"
        class="cal-workbench-list__item"
        :class="{ 'is-active': team.teamId === selectedTeamId }"
        @click="$emit('select', team)"
      >
        <span class="cal-workbench-list__item-head">
          <b>{{ team.teamName || "未命名班组" }}</b>
          <small>{{ team.teamCode }}</small>
        </span>
        <span class="cal-workbench-list__item-meta">
          <dict-tag :options="calendarTypes" :value="team.calendarType" />
          <span>{{ team.enableFlag === "N" ? "停用" : "启用" }}</span>
        </span>
      </button>
      <div v-if="!loading && !teams.length" class="cal-workbench-list__empty">
        <b>暂无班组</b>
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
defineProps({
  teams: { type: Array, default: () => [] },
  calendarTypes: { type: Array, default: () => [] },
  selectedTeamId: { type: [Number, String], default: null },
  loading: Boolean,
  total: { type: Number, default: 0 },
  page: { type: Number, default: 1 },
  pageSize: { type: Number, default: 10 },
});

defineEmits(["select", "update:page", "update:pageSize"]);
</script>
