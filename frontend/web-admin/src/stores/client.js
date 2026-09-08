import { defineStore } from "pinia";
import { ref, computed } from "vue";
import { fetchClientCatalog, submitClientOrder, fetchClientOrders } from "@/api/client";
import { FIXED_CLIENT_CUSTOMER } from "@/utils/client-session";
import {
  VIRTUAL_CATALOG_PRODUCT_ID,
  appendVirtualCatalogItem,
} from "@/constants/client-catalog-extra";

export const useClientStore = defineStore("client", () => {
  const catalog = ref([]);
  const loadingCatalog = ref(false);
  const cart = ref([]);
  const checkoutOpen = ref(false);
  const submitting = ref(false);
  const lastOrder = ref(null);
  const orderHistory = ref([]);

  const cartCount = computed(() =>
    cart.value.reduce((sum, line) => sum + line.qty, 0),
  );

  const cartTotal = computed(() =>
    cart.value.reduce((sum, line) => sum + (line.unitPrice || 0) * line.qty, 0),
  );

  async function loadCatalog() {
    loadingCatalog.value = true;
    try {
      const res = await fetchClientCatalog();
      catalog.value = appendVirtualCatalogItem(res?.data);
    } finally {
      loadingCatalog.value = false;
    }
  }

  function parsePrice(displayPrice) {
    const n = parseInt(String(displayPrice || "").replace(/[^\d]/g, ""), 10);
    return Number.isFinite(n) ? n : 0;
  }

  function addToCart(product, qty = 1) {
    const existing = cart.value.find((c) => c.productId === product.productId);
    if (existing) {
      existing.qty += qty;
      return;
    }
    cart.value.push({
      productId: product.productId,
      productCode: product.productCode,
      productName: product.productName,
      imageUrl: product.imageUrl,
      displayPrice: product.displayPrice,
      unitPrice: parsePrice(product.displayPrice),
      qty,
    });
  }

  function updateCartQty(productId, qty) {
    const line = cart.value.find((c) => c.productId === productId);
    if (!line) return;
    line.qty = Math.max(1, qty);
  }

  function removeFromCart(productId) {
    cart.value = cart.value.filter((c) => c.productId !== productId);
  }

  function clearCart() {
    cart.value = [];
  }

  async function submitOrder({ deliveryDate, remark }) {
    if (!cart.value.length) throw new Error("购物车为空");
    const virtualLine = cart.value.find((c) => c.productId === VIRTUAL_CATALOG_PRODUCT_ID);
    if (virtualLine) {
      throw new Error(`「${virtualLine.productName}」为展示商品，请移除后再提交订单`);
    }
    submitting.value = true;
    try {
      const res = await submitClientOrder({
        deliveryDate,
        remark,
        items: cart.value.map((c) => ({ productId: c.productId, orderQty: c.qty })),
      });
      lastOrder.value = res?.data || null;
      clearCart();
      checkoutOpen.value = false;
      return lastOrder.value;
    } finally {
      submitting.value = false;
    }
  }

  async function loadOrderHistory() {
    const res = await fetchClientOrders();
    orderHistory.value = res?.data || [];
  }

  return {
    customerName: FIXED_CLIENT_CUSTOMER,
    catalog,
    loadingCatalog,
    cart,
    checkoutOpen,
    submitting,
    lastOrder,
    orderHistory,
    cartCount,
    cartTotal,
    loadCatalog,
    addToCart,
    updateCartQty,
    removeFromCart,
    clearCart,
    submitOrder,
    loadOrderHistory,
  };
});
