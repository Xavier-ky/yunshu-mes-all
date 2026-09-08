<template>
  <Teleport to="body">
    <div v-if="client.checkoutOpen" class="drawer-mask" @click.self="client.checkoutOpen = false">
      <aside class="drawer-panel">
        <header class="drawer-head">
          <h3>购物车 · 结算</h3>
          <button type="button" class="drawer-close" @click="client.checkoutOpen = false">×</button>
        </header>

        <div v-if="!client.cart.length" class="drawer-empty">
          <p>购物车为空</p>
          <p class="hint">去商城挑选风扇产品吧</p>
        </div>

        <template v-else>
          <ul class="cart-lines">
            <li v-for="line in client.cart" :key="line.productId" class="cart-line">
              <img :src="line.imageUrl" :alt="line.productName" class="line-img" @error="onImgError" />
              <div class="line-info">
                <div class="line-name">{{ line.productName }}</div>
                <div class="line-code">{{ line.productCode }}</div>
                <div class="line-price">{{ line.displayPrice }}</div>
              </div>
              <div class="line-qty">
                <button type="button" @click="client.updateCartQty(line.productId, line.qty - 1)">−</button>
                <input
                  type="number"
                  min="1"
                  :value="line.qty"
                  @change="client.updateCartQty(line.productId, Number($event.target.value))"
                />
                <button type="button" @click="client.updateCartQty(line.productId, line.qty + 1)">+</button>
              </div>
              <button type="button" class="line-remove" @click="client.removeFromCart(line.productId)">删除</button>
            </li>
          </ul>

          <div class="checkout-form">
            <label>
              <span>期望交期 <em>*</em></span>
              <input v-model="deliveryDate" type="date" :min="minDate" />
            </label>
            <label>
              <span>备注</span>
              <textarea v-model="remark" rows="3" placeholder="采购说明、收货要求等（选填）" />
            </label>
          </div>

          <footer class="drawer-foot">
            <div class="total">
              合计 <strong>¥{{ client.cartTotal }}</strong>
              <span class="total-hint">（展示价，以 MES 订单为准）</span>
            </div>
            <button
              type="button"
              class="submit-btn"
              :disabled="client.submitting"
              @click="submit"
            >
              {{ client.submitting ? "提交中…" : "提交采购订单" }}
            </button>
          </footer>
        </template>
      </aside>
    </div>

    <div v-if="successOrder" class="success-mask" @click.self="successOrder = null">
      <div class="success-card">
        <div class="success-icon">✓</div>
        <h3>订单提交成功</h3>
        <p class="order-no">订单号：<strong>{{ successOrder.orderNo }}</strong></p>
        <p class="success-hint">订单已进入 MES 计划系统，生产主管可在订单管理中查看。</p>
        <div class="success-actions">
          <button type="button" @click="goOrders">查看我的订单</button>
          <button type="button" class="primary" @click="successOrder = null">继续采购</button>
        </div>
      </div>
    </div>
  </Teleport>
</template>

<script setup>
import { ref, computed, watch } from "vue";
import { useRouter } from "vue-router";
import { useClientStore } from "@/stores/client";

const router = useRouter();
const client = useClientStore();
const deliveryDate = ref(defaultDeliveryDate());
const remark = ref("");
const successOrder = ref(null);

const minDate = computed(() => {
  const d = new Date();
  return d.toISOString().slice(0, 10);
});

function defaultDeliveryDate() {
  const d = new Date();
  d.setDate(d.getDate() + 14);
  return d.toISOString().slice(0, 10);
}

watch(
  () => client.checkoutOpen,
  (open) => {
    if (open && !deliveryDate.value) deliveryDate.value = defaultDeliveryDate();
  },
);

function onImgError(e) {
  e.target.src = "/images/client-catalog/placeholder.png";
}

async function submit() {
  if (!deliveryDate.value) {
    alert("请选择期望交期");
    return;
  }
  try {
    const order = await client.submitOrder({
      deliveryDate: deliveryDate.value,
      remark: remark.value.trim(),
    });
    successOrder.value = order;
    remark.value = "";
    deliveryDate.value = defaultDeliveryDate();
  } catch (e) {
    alert(e.message || "提交失败");
  }
}

function goOrders() {
  successOrder.value = null;
  router.push("/client/orders");
}
</script>

<style scoped>
.drawer-mask {
  position: fixed;
  inset: 0;
  background: rgba(0, 0, 0, 0.45);
  z-index: 200;
  display: flex;
  justify-content: flex-end;
}

.drawer-panel {
  color-scheme: light;
  width: min(440px, 100vw);
  background: #fff;
  height: 100%;
  display: flex;
  flex-direction: column;
  box-shadow: -4px 0 24px rgba(0, 0, 0, 0.12);
}

.drawer-head {
  display: flex;
  align-items: center;
  justify-content: space-between;
  padding: 16px 20px;
  border-bottom: 1px solid #eee;
}

.drawer-head h3 {
  margin: 0;
  font-size: 16px;
  color: #333;
}

.drawer-close {
  border: none;
  background: none;
  font-size: 24px;
  color: #999;
  cursor: pointer;
  line-height: 1;
}

.drawer-empty {
  flex: 1;
  display: flex;
  flex-direction: column;
  align-items: center;
  justify-content: center;
  color: #999;
}

.drawer-empty .hint {
  font-size: 13px;
  margin-top: 8px;
}

.cart-lines {
  list-style: none;
  margin: 0;
  padding: 12px 16px;
  flex: 1;
  overflow-y: auto;
}

.cart-line {
  display: grid;
  grid-template-columns: 64px 1fr auto auto;
  gap: 10px;
  align-items: center;
  padding: 12px 0;
  border-bottom: 1px solid #f0f0f0;
}

.line-img {
  width: 64px;
  height: 64px;
  object-fit: contain;
  background: #fafafa;
  border-radius: 4px;
}

.line-name {
  font-size: 13px;
  color: #333;
  font-weight: 500;
}

.line-code {
  font-size: 11px;
  color: #999;
  margin-top: 2px;
}

.line-price {
  font-size: 14px;
  color: #e4393c;
  margin-top: 4px;
}

.line-qty {
  display: flex;
  align-items: center;
  gap: 4px;
}

.line-qty button {
  width: 28px;
  height: 28px;
  border: 1px solid #ddd;
  background: #fff;
  color: #333;
  cursor: pointer;
  border-radius: 4px;
}

.line-qty input {
  width: 44px;
  height: 28px;
  text-align: center;
  border: 1px solid #ddd;
  border-radius: 4px;
  padding: 0 4px;
  color: #1a1a1a;
  background: #fff;
  -moz-appearance: textfield;
  appearance: textfield;
}

.line-qty input::-webkit-outer-spin-button,
.line-qty input::-webkit-inner-spin-button {
  -webkit-appearance: none;
  margin: 0;
}

.line-remove {
  border: none;
  background: none;
  color: #999;
  font-size: 12px;
  cursor: pointer;
}

.line-remove:hover {
  color: #e4393c;
}

.checkout-form {
  padding: 16px 20px;
  border-top: 1px solid #eee;
  display: grid;
  gap: 12px;
}

.checkout-form label {
  display: grid;
  gap: 6px;
  font-size: 13px;
  color: #666;
}

.checkout-form label em {
  color: #e4393c;
  font-style: normal;
}

.checkout-form input,
.checkout-form textarea {
  border: 1px solid #ddd;
  border-radius: 4px;
  padding: 8px 10px;
  font-size: 14px;
  color: #1a1a1a;
  background: #fff;
  color-scheme: light;
}

.drawer-foot {
  padding: 16px 20px;
  border-top: 1px solid #eee;
  background: #fafafa;
}

.total {
  font-size: 14px;
  color: #333;
  margin-bottom: 12px;
}

.total strong {
  color: #e4393c;
  font-size: 20px;
}

.total-hint {
  display: block;
  font-size: 11px;
  color: #999;
  margin-top: 4px;
}

.submit-btn {
  width: 100%;
  height: 44px;
  background: #e4393c;
  color: #fff;
  border: none;
  border-radius: 4px;
  font-size: 15px;
  font-weight: 600;
  cursor: pointer;
}

.submit-btn:hover:not(:disabled) {
  background: #c81623;
}

.submit-btn:disabled {
  opacity: 0.6;
  cursor: not-allowed;
}

.success-mask {
  position: fixed;
  inset: 0;
  background: rgba(0, 0, 0, 0.5);
  z-index: 300;
  display: flex;
  align-items: center;
  justify-content: center;
}

.success-card {
  color-scheme: light;
  background: #fff;
  border-radius: 8px;
  padding: 32px 40px;
  max-width: 420px;
  text-align: center;
}

.success-icon {
  width: 56px;
  height: 56px;
  background: #52c41a;
  color: #fff;
  border-radius: 50%;
  font-size: 28px;
  line-height: 56px;
  margin: 0 auto 16px;
}

.success-card h3 {
  margin: 0 0 12px;
  color: #333;
}

.order-no {
  font-size: 14px;
  color: #666;
}

.success-hint {
  font-size: 13px;
  color: #999;
  margin: 12px 0 24px;
}

.success-actions {
  display: flex;
  gap: 12px;
  justify-content: center;
}

.success-actions button {
  padding: 10px 20px;
  border: 1px solid #ddd;
  background: #fff;
  border-radius: 4px;
  cursor: pointer;
  font-size: 14px;
}

.success-actions button.primary {
  background: #e4393c;
  color: #fff;
  border-color: #e4393c;
}
</style>
