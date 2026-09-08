<template>
  <section v-if="visibleItems.length" class="order-incoming-panel">
    <div class="order-incoming-panel__row">
      <span class="order-incoming-panel__title">
        <i class="order-incoming-panel__mark" aria-hidden="true"></i>
        待接订单
      </span>
      <div class="order-incoming-panel__track" role="list">
        <button
          v-for="item in visibleItems"
          :key="item.id"
          type="button"
          class="order-incoming-panel__chip"
          role="listitem"
          :title="chipTitle(item)"
          @click="$emit('select', item)"
        >
          <strong class="order-incoming-panel__code">{{ item.refNo }}</strong>
          <el-tag size="small" type="warning" effect="plain">待录入</el-tag>
          <span class="order-incoming-panel__customer">{{ item.customerName }}</span>
          <span class="order-incoming-panel__product">{{ item.productLabel }} × {{ item.orderQty }}</span>
          <span class="order-incoming-panel__time">{{ formatReceivedAt(item.receivedAt) }}</span>
        </button>
      </div>
      <span class="order-incoming-panel__hint">点击录入</span>
    </div>
  </section>
</template>

<script>
import { buildIncomingOrderPresets } from "@/yunshu-ui/planning/orders/incoming-order-presets";
import { parseTime } from "@/yunshu-ui/utils/yunshu-utils";

export default {
  name: "OrderIncomingPanel",
  props: {
    consumedIds: {
      type: Array,
      default: () => [],
    },
  },
  emits: ["select"],
  computed: {
    visibleItems() {
      const consumed = new Set(this.consumedIds || []);
      return buildIncomingOrderPresets().filter((item) => !consumed.has(item.id));
    },
  },
  methods: {
    formatReceivedAt(value) {
      if (!value) return "";
      return parseTime(value, "{m}-{d} {h}:{i}") || "";
    },
    chipTitle(item) {
      return [
        item.refNo,
        item.customerName,
        `${item.productLabel} × ${item.orderQty} 台`,
        item.deliveryDate ? `交付 ${item.deliveryDate}` : "",
        item.sourceLabel,
      ]
        .filter(Boolean)
        .join(" · ");
    },
  },
};
</script>

<style scoped>
.order-incoming-panel {
  position: relative;
  display: flex;
  align-items: center;
  min-height: 50px;
  margin-bottom: 10px;
  padding: 0 12px 0 14px;
  border: 1px solid rgba(23, 105, 224, 0.14);
  border-radius: 10px;
  background:
    linear-gradient(118deg, rgba(255, 255, 255, 0.82) 0%, rgba(241, 247, 255, 0.96) 42%, rgba(232, 242, 252, 0.94) 100%);
  box-shadow:
    0 1px 2px rgba(23, 67, 120, 0.04),
    0 6px 18px rgba(23, 105, 224, 0.07),
    inset 0 1px 0 rgba(255, 255, 255, 0.9);
  overflow: hidden;
  box-sizing: border-box;
}

.order-incoming-panel::before {
  content: "";
  position: absolute;
  left: 0;
  top: 0;
  bottom: 0;
  width: 3px;
  background: linear-gradient(180deg, #5b9cf5 0%, #1769e0 100%);
  opacity: 0.9;
}

.order-incoming-panel__row {
  display: flex;
  align-items: center;
  gap: 10px;
  width: 100%;
  height: 32px;
}

.order-incoming-panel__title {
  flex-shrink: 0;
  display: inline-flex;
  align-items: center;
  gap: 7px;
  height: 32px;
  margin: 0;
  line-height: 32px;
  font-size: 13px;
  font-weight: 600;
  color: #1e3a5f;
  letter-spacing: 0.02em;
  white-space: nowrap;
}

.order-incoming-panel__mark {
  width: 7px;
  height: 7px;
  border-radius: 50%;
  background: radial-gradient(circle at 35% 35%, #7eb6ff, #1769e0);
  box-shadow: 0 0 0 3px rgba(23, 105, 224, 0.12);
}

.order-incoming-panel__track {
  flex: 1;
  min-width: 0;
  display: flex;
  flex-wrap: nowrap;
  align-items: center;
  gap: 8px;
  height: 32px;
  overflow-x: auto;
  overflow-y: hidden;
  -ms-overflow-style: none;
  scrollbar-width: none;
}

.order-incoming-panel__track::-webkit-scrollbar {
  display: none;
}

.order-incoming-panel__chip {
  flex-shrink: 0;
  display: inline-flex;
  align-items: center;
  gap: 6px;
  max-width: 420px;
  height: 32px;
  padding: 0 11px;
  margin: 0;
  border: 1px solid rgba(23, 105, 224, 0.12);
  border-radius: 999px;
  background: rgba(255, 255, 255, 0.88);
  cursor: pointer;
  color: #1a1a1a;
  white-space: nowrap;
  box-shadow: 0 1px 2px rgba(23, 67, 120, 0.05);
  transition: border-color 0.15s ease, box-shadow 0.15s ease, background 0.15s ease;
}

.order-incoming-panel__chip :deep(.el-tag) {
  height: 20px;
  padding: 0 6px;
  line-height: 18px;
  border-radius: 4px;
  vertical-align: middle;
}

.order-incoming-panel__chip:hover {
  border-color: rgba(23, 105, 224, 0.34);
  background: #fff;
  box-shadow: 0 2px 10px rgba(23, 105, 224, 0.12);
}

.order-incoming-panel__code {
  font-size: 12px;
  font-weight: 600;
  color: #173355;
}

.order-incoming-panel__customer {
  font-size: 12px;
  color: #303133;
  max-width: 148px;
  overflow: hidden;
  text-overflow: ellipsis;
}

.order-incoming-panel__product {
  font-size: 12px;
  color: #4a607a;
}

.order-incoming-panel__time {
  font-size: 11px;
  color: #6b84a0;
}

.order-incoming-panel__hint {
  flex-shrink: 0;
  font-size: 12px;
  color: #6b84a0;
  white-space: nowrap;
}
</style>
