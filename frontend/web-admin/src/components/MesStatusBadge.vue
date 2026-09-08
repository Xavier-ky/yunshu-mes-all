<template>
  <span class="mes-badge" :class="toneClass">{{ displayLabel }}</span>
</template>

<script setup>
import { computed } from "vue";

const props = defineProps({
  value: { type: String, default: "" },
  label: { type: String, default: "" },
});

const STATUS_MAP = {
  CREATED: { label: "已创建", tone: "blue" },
  CONFIRMED: { label: "已确认", tone: "blue" },
  DISPATCHED: { label: "已派工", tone: "amber" },
  RUNNING: { label: "进行中", tone: "green" },
  COMPLETED: { label: "已完成", tone: "green" },
  CANCELLED: { label: "已取消", tone: "red" },
};

const toneClass = computed(() => {
  const m = STATUS_MAP[props.value];
  return m ? `tone-${m.tone}` : "tone-gray";
});

const displayLabel = computed(() => {
  if (props.label) return props.label;
  const m = STATUS_MAP[props.value];
  return m?.label || props.value || "—";
});
</script>

<style scoped>
.mes-badge {
  display: inline-flex;
  align-items: center;
  gap: 5px;
  font-size: 11px;
  font-weight: 500;
  padding: 2px 8px;
  border: 1px solid #e7e7e7;
  background: #fafafa;
  color: #666;
}

.mes-badge::before {
  content: "";
  width: 6px;
  height: 6px;
  border-radius: 50%;
  background: currentColor;
}

.tone-green {
  color: #0abf5b;
  background: #f0fdf4;
  border-color: #bbf7d0;
}

.tone-blue {
  color: #0052d9;
  background: #eff6ff;
  border-color: #bfdbfe;
}

.tone-amber {
  color: #ff9d00;
  background: #fffbeb;
  border-color: #fde68a;
}

.tone-red {
  color: #e34d59;
  background: #fef2f2;
  border-color: #fecaca;
}

.tone-gray {
  color: #888;
}
</style>
