<template>
  <div class="orders-root">
    <header class="orders-head">
      <h2>我的采购订单</h2>
      <button type="button" class="back-btn" @click="router.push('/client/mall')">继续采购</button>
    </header>

    <div v-if="loading" class="orders-loading">加载中…</div>

    <div v-else-if="!orders.length" class="orders-empty">
      <p>暂无订单记录</p>
      <button type="button" @click="router.push('/client/mall')">去商城下单</button>
    </div>

    <div v-else class="orders-list">
      <article v-for="order in orders" :key="order.orderId" class="order-card">
        <div class="order-meta">
          <span class="order-no">{{ order.orderNo }}</span>
          <span class="order-status" :class="statusClass(order.status)">{{ statusLabel(order.status) }}</span>
        </div>
        <div class="order-info">
          <span>交期：{{ order.deliveryDate }}</span>
          <span>客户：{{ order.customerName }}</span>
        </div>
        <ul class="order-lines">
          <li v-for="(line, i) in order.items" :key="i">
            {{ line.productName }} × {{ line.orderQty }}
          </li>
        </ul>
      </article>
    </div>
  </div>
</template>

<script setup>
import { ref, onMounted } from "vue";
import { useRouter } from "vue-router";
import { useClientStore } from "@/stores/client";

const router = useRouter();
const client = useClientStore();
const loading = ref(true);
const orders = ref([]);

onMounted(async () => {
  try {
    await client.loadOrderHistory();
    orders.value = client.orderHistory;
  } finally {
    loading.value = false;
  }
});

function statusLabel(s) {
  const map = { CREATED: "已创建", CONFIRMED: "已确认", IN_PRODUCTION: "生产中", COMPLETED: "已完成", CANCELLED: "已取消" };
  return map[s] || s;
}

function statusClass(s) {
  if (s === "CREATED") return "status-created";
  if (s === "COMPLETED") return "status-done";
  return "";
}
</script>

<style scoped>
.orders-root {
  background: #fff;
  border-radius: 6px;
  padding: 24px;
  box-shadow: 0 1px 4px rgba(0, 0, 0, 0.06);
}

.orders-head {
  display: flex;
  align-items: center;
  justify-content: space-between;
  margin-bottom: 20px;
  padding-bottom: 16px;
  border-bottom: 1px solid #f0f0f0;
}

.orders-head h2 {
  margin: 0;
  font-size: 18px;
  color: #333;
}

.back-btn {
  border: 1px solid #e4393c;
  color: #e4393c;
  background: #fff;
  padding: 8px 16px;
  border-radius: 4px;
  cursor: pointer;
  font-size: 13px;
}

.orders-loading,
.orders-empty {
  text-align: center;
  padding: 48px;
  color: #999;
}

.orders-empty button {
  margin-top: 16px;
  background: #e4393c;
  color: #fff;
  border: none;
  padding: 10px 24px;
  border-radius: 4px;
  cursor: pointer;
}

.orders-list {
  display: grid;
  gap: 16px;
}

.order-card {
  border: 1px solid #eee;
  border-radius: 6px;
  padding: 16px 20px;
}

.order-meta {
  display: flex;
  align-items: center;
  justify-content: space-between;
  margin-bottom: 8px;
}

.order-no {
  font-weight: 600;
  color: #333;
  font-size: 14px;
}

.order-status {
  font-size: 12px;
  padding: 2px 8px;
  border-radius: 4px;
  background: #f5f5f5;
  color: #666;
}

.order-status.status-created {
  background: #fff5f5;
  color: #e4393c;
}

.order-status.status-done {
  background: #f6ffed;
  color: #52c41a;
}

.order-info {
  display: flex;
  gap: 24px;
  font-size: 13px;
  color: #999;
  margin-bottom: 10px;
}

.order-lines {
  list-style: none;
  margin: 0;
  padding: 0;
  font-size: 13px;
  color: #666;
}

.order-lines li {
  padding: 4px 0;
}
</style>
