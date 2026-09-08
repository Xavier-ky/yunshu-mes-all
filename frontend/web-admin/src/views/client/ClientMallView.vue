<template>
  <div class="mall-root">
    <aside class="mall-sidebar">
      <h3 class="sidebar-title">商品分类</h3>
      <ul class="category-list">
        <li>
          <button
            type="button"
            class="cat-item"
            :class="{ active: activeCategory === 'ALL' }"
            @click="activeCategory = 'ALL'"
          >
            全部商品
            <span class="cat-count">{{ client.catalog.length }}</span>
          </button>
        </li>
        <li v-for="cat in categories" :key="cat.key">
          <button
            type="button"
            class="cat-item"
            :class="{ active: activeCategory === cat.key }"
            @click="activeCategory = cat.key"
          >
            {{ cat.label }}
            <span class="cat-count">{{ cat.count }}</span>
          </button>
        </li>
      </ul>
    </aside>

    <section class="mall-content">
      <div v-if="client.loadingCatalog" class="mall-loading">加载商品目录…</div>

      <div v-else-if="!filteredProducts.length" class="mall-empty">
        <p>暂无匹配商品</p>
      </div>

      <div v-else class="product-grid">
        <article v-for="product in filteredProducts" :key="product.productId" class="product-card">
          <div class="card-img-wrap">
            <img :src="product.imageUrl" :alt="product.productName" @error="onImgError" />
          </div>
          <div class="card-body">
            <h4 class="card-name" :title="product.productName">{{ product.productName }}</h4>
            <p class="card-model">{{ product.productModel }}</p>
            <p class="card-tagline">{{ product.tagline }}</p>
            <div class="card-specs">
              <span v-for="(spec, i) in product.specs?.slice(0, 3)" :key="i" class="spec-tag">{{ spec }}</span>
            </div>
            <div class="card-foot">
              <span class="card-price">{{ product.displayPrice }}</span>
              <div class="card-actions">
                <div class="qty-stepper">
                  <button type="button" @click="decQty(product.productId)">−</button>
                  <span>{{ qtyMap[product.productId] || 1 }}</span>
                  <button type="button" @click="incQty(product.productId)">+</button>
                </div>
                <button type="button" class="add-btn" @click="addProduct(product)">加入购物车</button>
              </div>
            </div>
          </div>
        </article>
      </div>
    </section>
  </div>
</template>

<script setup>
import { ref, computed, reactive, onMounted, watch } from "vue";
import { useClientStore } from "@/stores/client";

const props = defineProps({
  searchKeyword: { type: String, default: "" },
});

const client = useClientStore();
const activeCategory = ref("ALL");
const qtyMap = reactive({});

const categories = computed(() => {
  const map = new Map();
  for (const p of client.catalog) {
    const key = p.category || "OTHER";
    const label = p.categoryLabel || "其他";
    if (!map.has(key)) map.set(key, { key, label, count: 0 });
    map.get(key).count += 1;
  }
  return [...map.values()];
});

const filteredProducts = computed(() => {
  const kw = (props.searchKeyword || "").trim().toLowerCase();
  return client.catalog.filter((p) => {
    const matchCat = activeCategory.value === "ALL" || p.category === activeCategory.value;
    const matchKw =
      !kw ||
      p.productName?.toLowerCase().includes(kw) ||
      p.productCode?.toLowerCase().includes(kw) ||
      p.productModel?.toLowerCase().includes(kw);
    return matchCat && matchKw;
  });
});

onMounted(() => client.loadCatalog());

watch(
  () => client.catalog,
  (list) => {
    for (const p of list) {
      if (!qtyMap[p.productId]) qtyMap[p.productId] = 1;
    }
  },
  { immediate: true },
);

function incQty(id) {
  qtyMap[id] = (qtyMap[id] || 1) + 1;
}

function decQty(id) {
  qtyMap[id] = Math.max(1, (qtyMap[id] || 1) - 1);
}

function addProduct(product) {
  client.addToCart(product, qtyMap[product.productId] || 1);
  client.checkoutOpen = true;
}

function onImgError(e) {
  e.target.src = "/images/client-catalog/placeholder.png";
}
</script>

<style scoped>
.mall-root {
  display: flex;
  gap: 20px;
  align-items: flex-start;
}

.mall-sidebar {
  width: 210px;
  flex-shrink: 0;
  background: #fff;
  border-radius: 6px;
  padding: 18px 0;
  box-shadow: 0 1px 4px rgba(0, 0, 0, 0.06);
  font-family: "PingFang SC", "Microsoft YaHei", "Helvetica Neue", sans-serif;
}

.sidebar-title {
  margin: 0 18px 14px;
  font-size: 16px;
  font-weight: 600;
  letter-spacing: 0.04em;
  color: #1a1a1a;
  padding-bottom: 12px;
  border-bottom: 1px solid #f0f0f0;
}

.category-list {
  list-style: none;
  margin: 0;
  padding: 0;
}

.cat-item {
  width: 100%;
  display: flex;
  align-items: center;
  justify-content: space-between;
  padding: 11px 18px;
  border: none;
  background: transparent;
  text-align: left;
  font-size: 15px;
  line-height: 1.45;
  letter-spacing: 0.02em;
  color: #555;
  cursor: pointer;
  transition: background 0.15s, color 0.15s;
}

.cat-item:hover,
.cat-item.active {
  background: #fff5f5;
  color: #e4393c;
  font-weight: 600;
}

.cat-item.active .cat-count {
  color: #e4393c;
  font-weight: 500;
}

.cat-count {
  font-size: 12px;
  color: #aaa;
  font-variant-numeric: tabular-nums;
}

.mall-content {
  flex: 1;
  min-width: 0;
}

.mall-loading,
.mall-empty {
  background: #fff;
  border-radius: 6px;
  padding: 60px;
  text-align: center;
  color: #999;
}

.product-grid {
  display: grid;
  grid-template-columns: repeat(auto-fill, minmax(220px, 1fr));
  gap: 16px;
}

.product-card {
  background: #fff;
  border-radius: 6px;
  overflow: hidden;
  box-shadow: 0 1px 4px rgba(0, 0, 0, 0.06);
  transition: box-shadow 0.2s, transform 0.2s;
  display: flex;
  flex-direction: column;
}

.product-card:hover {
  box-shadow: 0 4px 16px rgba(228, 57, 60, 0.12);
  transform: translateY(-2px);
}

.card-img-wrap {
  aspect-ratio: 1;
  background: #fafafa;
  display: flex;
  align-items: center;
  justify-content: center;
  padding: 16px;
}

.card-img-wrap img {
  max-width: 100%;
  max-height: 100%;
  object-fit: contain;
}

.card-body {
  padding: 12px 14px 14px;
  flex: 1;
  display: flex;
  flex-direction: column;
}

.card-name {
  margin: 0;
  font-size: 14px;
  color: #333;
  line-height: 1.4;
  display: -webkit-box;
  -webkit-line-clamp: 2;
  -webkit-box-orient: vertical;
  overflow: hidden;
}

.card-model {
  margin: 4px 0 0;
  font-size: 12px;
  color: #999;
}

.card-tagline {
  margin: 6px 0 0;
  font-size: 11px;
  color: #b0b0b0;
  line-height: 1.4;
}

.card-specs {
  display: flex;
  flex-wrap: wrap;
  gap: 4px;
  margin-top: 8px;
}

.spec-tag {
  font-size: 10px;
  color: #e4393c;
  background: #fff5f5;
  padding: 2px 6px;
  border-radius: 2px;
  border: 1px solid #ffd4d4;
}

.card-foot {
  margin-top: auto;
  padding-top: 10px;
}

.card-price {
  font-size: 18px;
  font-weight: 700;
  color: #e4393c;
}

.card-actions {
  display: flex;
  align-items: center;
  gap: 8px;
  margin-top: 8px;
}

.qty-stepper {
  display: flex;
  align-items: center;
  border: 1px solid #ddd;
  border-radius: 4px;
  overflow: hidden;
}

.qty-stepper button {
  width: 28px;
  height: 28px;
  border: none;
  background: #f5f5f5;
  cursor: pointer;
  font-size: 14px;
}

.qty-stepper span {
  width: 32px;
  text-align: center;
  font-size: 13px;
  color: #333;
}

.add-btn {
  flex: 1;
  height: 30px;
  background: #e4393c;
  color: #fff;
  border: none;
  border-radius: 4px;
  font-size: 12px;
  cursor: pointer;
}

.add-btn:hover {
  background: #c81623;
}
</style>
