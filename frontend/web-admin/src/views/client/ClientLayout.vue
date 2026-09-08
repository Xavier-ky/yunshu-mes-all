<template>
  <div class="client-root">
    <header class="client-header">
      <div class="client-header-inner">
        <div class="client-brand" @click="router.push('/client/mall')">
          <span class="client-logo">云枢</span>
          <span class="client-brand-text">采购客户端</span>
        </div>

        <div class="client-search">
          <input
            v-model="searchKeyword"
            type="search"
            placeholder="搜索商品名称 / 编码"
            @keyup.enter.prevent
          />
          <button type="button" class="client-search-btn">搜索</button>
        </div>

        <div class="client-header-right">
          <div class="client-user-block">
            <img class="client-avatar" src="/images/MyLogo.jpg" :alt="client.customerName" />
            <span class="client-user">{{ client.customerName }}</span>
          </div>
          <nav class="client-nav">
            <button type="button" class="client-nav-btn" @click="router.push('/client/orders')">
              我的订单
            </button>
            <button type="button" class="client-nav-btn cart-btn" @click="client.checkoutOpen = true">
              购物车
              <span v-if="client.cartCount" class="cart-badge">{{ client.cartCount }}</span>
            </button>
            <button type="button" class="client-nav-btn logout" @click="logout">退出</button>
          </nav>
        </div>
      </div>
    </header>

    <main class="client-main">
      <router-view :search-keyword="searchKeyword" />
    </main>

    <ClientCheckoutDrawer />
  </div>
</template>

<script setup>
import { ref } from "vue";
import { useRouter } from "vue-router";
import { useClientStore } from "@/stores/client";
import { clearClientSession } from "@/utils/client-session";
import ClientCheckoutDrawer from "./ClientCheckoutDrawer.vue";

const router = useRouter();
const client = useClientStore();
const searchKeyword = ref("");

function logout() {
  clearClientSession();
  client.clearCart();
  router.push("/login");
}
</script>

<style scoped>
.client-root {
  color-scheme: light;
  min-height: 100vh;
  background: #f5f5f5;
  display: flex;
  flex-direction: column;
}

.client-header {
  background: #fff;
  border-bottom: 2px solid #e4393c;
  box-shadow: 0 2px 8px rgba(0, 0, 0, 0.06);
  position: sticky;
  top: 0;
  z-index: 100;
}

.client-header-inner {
  max-width: 1400px;
  margin: 0 auto;
  padding: 12px 24px;
  display: flex;
  align-items: center;
  gap: 20px;
}

.client-header-right {
  margin-left: auto;
  display: flex;
  align-items: center;
  gap: 28px;
  flex-shrink: 0;
}

.client-user-block {
  display: flex;
  align-items: center;
  gap: 10px;
  min-width: 0;
}

.client-avatar {
  width: 36px;
  height: 36px;
  border-radius: 50%;
  object-fit: cover;
  border: 2px solid #f0f0f0;
  flex-shrink: 0;
}

.client-brand {
  display: flex;
  align-items: center;
  gap: 8px;
  cursor: pointer;
  flex-shrink: 0;
}

.client-logo {
  background: #e4393c;
  color: #fff;
  font-weight: 700;
  font-size: 14px;
  padding: 6px 10px;
  border-radius: 4px;
}

.client-brand-text {
  font-size: 18px;
  font-weight: 600;
  color: #333;
}

.client-search {
  flex: 1;
  display: flex;
  max-width: 520px;
  border: 2px solid #e4393c;
  border-radius: 4px;
  overflow: hidden;
}

.client-search input {
  flex: 1;
  border: none;
  padding: 10px 14px;
  font-size: 14px;
  color: #1a1a1a;
  background: #fff;
  outline: none;
}

.client-search-btn {
  background: #e4393c;
  color: #fff;
  border: none;
  padding: 0 20px;
  font-size: 14px;
  cursor: pointer;
}

.client-search-btn:hover {
  background: #c81623;
}

.client-nav {
  display: flex;
  align-items: center;
  gap: 10px;
  flex-shrink: 0;
}

.client-user {
  font-size: 14px;
  font-weight: 500;
  color: #333;
  max-width: 160px;
  overflow: hidden;
  text-overflow: ellipsis;
  white-space: nowrap;
}

.client-nav-btn {
  background: transparent;
  border: 1px solid #ddd;
  color: #333;
  padding: 6px 14px;
  border-radius: 4px;
  font-size: 13px;
  cursor: pointer;
}

.client-nav-btn:hover {
  border-color: #e4393c;
  color: #e4393c;
}

.client-nav-btn.cart-btn {
  position: relative;
  border-color: #e4393c;
  color: #e4393c;
}

.cart-badge {
  position: absolute;
  top: -8px;
  right: -8px;
  background: #e4393c;
  color: #fff;
  font-size: 11px;
  min-width: 18px;
  height: 18px;
  border-radius: 9px;
  display: flex;
  align-items: center;
  justify-content: center;
  padding: 0 4px;
}

.client-nav-btn.logout:hover {
  background: #f5f5f5;
}

.client-main {
  flex: 1;
  max-width: 1400px;
  width: 100%;
  margin: 0 auto;
  padding: 20px 24px 40px;
  box-sizing: border-box;
}
</style>
