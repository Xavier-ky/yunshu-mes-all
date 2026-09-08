<template>
  <div class="app-container bom-workbench-root">
    <el-row :gutter="0" class="bom-workbench-row">
      <el-col :span="6" :xs="24" class="bom-workbench-aside">
        <div class="bom-aside-summary">
          <div class="aside-summary-head">
            <span>BOM 概览</span>
          </div>
          <div class="aside-stat-grid">
            <div><b>{{ total }}</b><span>产品数</span></div>
            <div><b>{{ bomLineCount }}</b><span>当前子件</span></div>
          </div>
        </div>

        <div class="bom-aside-panel bom-aside-panel--grow">
          <div class="aside-section-title"><span>产品列表</span><small>选择维护 BOM</small></div>
          <el-form :model="queryParams" ref="queryForm" size="small" :inline="true" class="bom-aside-search" label-width="0" @submit.prevent>
            <el-form-item prop="itemCode">
              <el-input v-model="queryParams.itemCode" placeholder="产品编码" clearable @keyup.enter="handleQuery" />
            </el-form-item>
            <el-form-item prop="itemName">
              <el-input v-model="queryParams.itemName" placeholder="产品名称" clearable @keyup.enter="handleQuery" />
            </el-form-item>
            <el-form-item>
              <el-button type="primary" icon="el-icon-search" size="default" @click="handleQuery">搜索</el-button>
              <el-button icon="el-icon-refresh" size="default" @click="resetQuery">重置</el-button>
            </el-form-item>
          </el-form>

          <div v-loading="loading" class="bom-product-list">
            <button
              v-for="item in productList"
              :key="item.itemId"
              type="button"
              class="bom-product-list__item"
              :class="{ 'is-active': selectedProduct && selectedProduct.itemId === item.itemId }"
              @click="handleProductSelect(item)"
            >
              <span class="bom-product-list__code">{{ item.itemCode }}</span>
              <span class="bom-product-list__name">{{ item.itemName }}</span>
              <span v-if="item.specification" class="bom-product-list__spec">{{ item.specification }}</span>
            </button>
            <el-empty v-if="!loading && !productList.length" description="暂无产品" :image-size="64" />
          </div>
        </div>

        <pagination
          v-show="total > 0"
          :total="total"
          :page="queryParams.pageNum"
          :limit="queryParams.pageSize"
          layout="prev, pager, next"
          :pager-count="5"
          :auto-scroll="false"
          @update:page="queryParams.pageNum = $event"
          @update:limit="queryParams.pageSize = $event"
          @pagination="getList"
        />
      </el-col>

      <el-col :span="18" :xs="24" class="bom-workbench-main">
        <el-empty v-if="!selectedProduct" class="bom-empty" description="请从左侧选择产品以维护 BOM" />
        <template v-else>
          <div class="bom-product-strip">
            <div class="bom-product-strip__main">
              <span class="bom-product-strip__label">当前产品</span>
              <span class="bom-product-strip__code">{{ selectedProduct.itemCode }}</span>
              <span class="bom-product-strip__name">{{ selectedProduct.itemName }}</span>
              <span v-if="selectedProduct.specification" class="bom-product-strip__spec">{{ selectedProduct.specification }}</span>
            </div>
            <div class="bom-product-strip__meta">
              <span><em>单位</em>{{ selectedProduct.unitName || "—" }}</span>
              <span><em>子件</em>{{ bomLineCount }} 项</span>
            </div>
          </div>
          <ItemBom
            embedded
            optType="edit"
            :itemId="selectedProduct.itemId"
            @lines-changed="onBomLinesChanged"
          />
        </template>
      </el-col>
    </el-row>
  </div>
</template>

<script>
import { listMdItem } from "@/yunshu-ui/api/mes/md/mdItem";
import { listBom } from "@/yunshu-ui/api/mes/md/bom";
import ItemBom from "@/yunshu-ui/md/mditem/components/itembom.vue";

export default {
  name: "ProductBom",
  components: { ItemBom },
  data() {
    return {
      loading: true,
      total: 0,
      productList: [],
      selectedProduct: null,
      bomLineCount: 0,
      queryParams: {
        pageNum: 1,
        pageSize: 10,
        itemCode: undefined,
        itemName: undefined,
        itemOrProduct: "PRODUCT",
      },
    };
  },
  created() {
    this.getList();
  },
  methods: {
    getList() {
      this.loading = true;
      listMdItem(this.queryParams)
        .then((response) => {
          this.productList = response.rows || [];
          this.total = response.total || 0;
          if (this.selectedProduct) {
            const hit = this.productList.find((p) => p.itemId === this.selectedProduct.itemId);
            if (hit) {
              this.selectedProduct = hit;
            } else if (this.productList.length) {
              this.handleProductSelect(this.productList[0]);
            } else {
              this.selectedProduct = null;
              this.bomLineCount = 0;
            }
          } else if (this.productList.length) {
            this.handleProductSelect(this.productList[0]);
          }
        })
        .finally(() => {
          this.loading = false;
        });
    },
    handleQuery() {
      this.queryParams.pageNum = 1;
      this.getList();
    },
    resetQuery() {
      this.resetForm("queryForm");
      this.queryParams.itemOrProduct = "PRODUCT";
      this.handleQuery();
    },
    handleProductSelect(row) {
      this.selectedProduct = row || null;
      if (row?.itemId) {
        this.loadBomLineCount(row.itemId);
      } else {
        this.bomLineCount = 0;
      }
    },
    loadBomLineCount(itemId) {
      listBom({ itemId, pageNum: 1, pageSize: 1 }).then((res) => {
        this.bomLineCount = res.total || 0;
      }).catch(() => {
        this.bomLineCount = 0;
      });
    },
    onBomLinesChanged(count) {
      this.bomLineCount = count ?? this.bomLineCount;
    },
  },
};
</script>
