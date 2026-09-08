<template>
  <div class="app-container inventory-doc-shell">
    <el-tabs v-model="activeTab" class="inventory-doc-tabs" @tab-change="onTabChange">
      <el-tab-pane label="生产领料" name="issue" lazy>
        <IssueDoc v-if="activeTab === 'issue'" />
      </el-tab-pane>
      <el-tab-pane label="销售出库" name="product-sales" lazy>
        <ProductSales v-if="activeTab === 'product-sales'" />
      </el-tab-pane>
      <el-tab-pane label="供应商退货" name="rt-vendor" lazy>
        <RtVendor v-if="activeTab === 'rt-vendor'" />
      </el-tab-pane>
    </el-tabs>
  </div>
</template>

<script>
import IssueDoc from "@/yunshu-ui/pro/wm/issue/index.vue";
import ProductSales from "@/yunshu-ui/pro/wm/productsales/index.vue";
import RtVendor from "@/yunshu-ui/pro/wm/rtvendor/index.vue";

const TAB_NAMES = ["issue", "product-sales", "rt-vendor"];

export default {
  name: "InventoryOutbound",
  components: { IssueDoc, ProductSales, RtVendor },
  data() {
    return {
      activeTab: "issue",
    };
  },
  created() {
    this.syncTabFromRoute();
  },
  watch: {
    "$route.query.tab"() {
      this.syncTabFromRoute();
    },
  },
  methods: {
    syncTabFromRoute() {
      const tab = this.$route.query.tab;
      if (TAB_NAMES.includes(tab)) {
        this.activeTab = tab;
      }
    },
    onTabChange(name) {
      if (this.$route.query.tab === name) return;
      this.$router.replace({
        path: "/app/inventory/outbound",
        query: { ...this.$route.query, tab: name },
      });
    },
  },
};
</script>
