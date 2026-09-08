<template>
  <div class="mes-table-wrap">
    <table class="mes-table">
      <thead>
        <tr>
          <th
            v-for="col in columns"
            :key="col.key"
            :class="[col.align === 'right' ? 'num' : '', col.sortable ? 'sortable' : '']"
            :style="col.width ? { width: col.width } : undefined"
            @click="col.sortable && onSort(col.key)"
          >
            {{ col.label }}
            <span v-if="sortKey === col.key">{{ sortDir === 'asc' ? ' ↑' : ' ↓' }}</span>
          </th>
        </tr>
      </thead>
      <tbody>
        <tr v-if="!rows.length">
          <td :colspan="columns.length" class="mes-table-empty">
            <slot name="empty">暂无数据</slot>
          </td>
        </tr>
        <tr
          v-for="(row, ri) in rows"
          :key="rowKey(row, ri)"
          :class="rowClass(row)"
          @click="$emit('row-click', row)"
        >
          <td
            v-for="col in columns"
            :key="col.key"
            :class="[col.align === 'right' ? 'num' : '', col.type === 'code' ? 'cell-code' : '']"
          >
            <slot :name="`cell-${col.key}`" :row="row" :value="row[col.key]">
              <MesStatusBadge v-if="col.type === 'status'" :value="String(row[col.key] || '')" />
              <MesProgressCell
                v-else-if="col.type === 'progress'"
                :completed="row[col.completedKey || 'completedQty']"
                :total="row[col.totalKey || 'planQty']"
              />
              <span v-else>{{ formatCell(row[col.key], col) }}</span>
            </slot>
          </td>
        </tr>
      </tbody>
    </table>
    <div v-if="showPager && totalPages > 1" class="mes-table-foot">
      <span>共 {{ total }} 条 · 第 {{ page }} / {{ totalPages }} 页</span>
      <div class="mes-pager-btns">
        <button :disabled="page <= 1" @click="$emit('page-change', 1)">&laquo;</button>
        <button :disabled="page <= 1" @click="$emit('page-change', page - 1)">&lsaquo;</button>
        <button
          v-for="p in visiblePages"
          :key="p"
          :class="{ on: p === page }"
          @click="$emit('page-change', p)"
        >
          {{ p }}
        </button>
        <button :disabled="page >= totalPages" @click="$emit('page-change', page + 1)">&rsaquo;</button>
        <button :disabled="page >= totalPages" @click="$emit('page-change', totalPages)">&raquo;</button>
      </div>
    </div>
  </div>
</template>

<script setup>
import { computed } from "vue";
import MesStatusBadge from "./MesStatusBadge.vue";
import MesProgressCell from "./MesProgressCell.vue";

const props = defineProps({
  columns: { type: Array, default: () => [] },
  rows: { type: Array, default: () => [] },
  rowKeyField: { type: String, default: "id" },
  rowClassFn: { type: Function, default: null },
  sortKey: { type: String, default: "" },
  sortDir: { type: String, default: "asc" },
  page: { type: Number, default: 1 },
  pageSize: { type: Number, default: 20 },
  total: { type: Number, default: 0 },
  showPager: { type: Boolean, default: false },
});

const emit = defineEmits(["sort-change", "page-change", "row-click"]);

const totalPages = computed(() =>
  Math.max(1, Math.ceil((props.total || props.rows.length) / props.pageSize))
);

const visiblePages = computed(() => {
  const t = totalPages.value;
  const p = props.page;
  const pages = [];
  const start = Math.max(1, p - 2);
  const end = Math.min(t, p + 2);
  for (let i = start; i <= end; i++) pages.push(i);
  return pages;
});

function rowKey(row, index) {
  return row[props.rowKeyField] ?? row.id ?? index;
}

function rowClass(row) {
  return props.rowClassFn ? props.rowClassFn(row) : "";
}

function onSort(key) {
  const dir = props.sortKey === key && props.sortDir === "asc" ? "desc" : "asc";
  emit("sort-change", { key, dir });
}

function formatCell(val, col) {
  if (val == null || val === "") return "—";
  if (col.type === "number") return Number(val).toLocaleString();
  return val;
}
</script>
