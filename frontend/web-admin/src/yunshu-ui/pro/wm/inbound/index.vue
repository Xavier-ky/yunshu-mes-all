<template>
  <div class="app-container inventory-doc-shell">
    <el-tabs v-model="activeTab" class="inventory-doc-tabs" @tab-change="onTabChange">
      <el-tab-pane label="物料入库" name="item-recpt" lazy>
        <ItemRecpt v-if="activeTab === 'item-recpt'" />
      </el-tab-pane>
      <el-tab-pane label="产品入库" name="product-recpt" lazy>
        <ProductRecpt v-if="activeTab === 'product-recpt'" />
      </el-tab-pane>
      <el-tab-pane label="生产退料" name="rt-issue" lazy>
        <RtIssue v-if="activeTab === 'rt-issue'" />
      </el-tab-pane>
      <el-tab-pane label="销售退货" name="rt-sales" lazy>
        <RtSales v-if="activeTab === 'rt-sales'" />
      </el-tab-pane>
    </el-tabs>
  </div>
</template>

<script>
import ItemRecpt from "@/yunshu-ui/pro/wm/itemrecpt/index.vue";
import ProductRecpt from "@/yunshu-ui/pro/wm/productrecpt/index.vue";
import RtIssue from "@/yunshu-ui/pro/wm/rtissue/index.vue";
import RtSales from "@/yunshu-ui/pro/wm/rtsales/index.vue";

const TAB_NAMES = ["item-recpt", "product-recpt", "rt-issue", "rt-sales"];

export default {
  name: "InventoryInbound",
  components: { ItemRecpt, ProductRecpt, RtIssue, RtSales },
  data() {
    return {
      activeTab: "product-recpt",
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
        path: "/app/inventory/inbound",
        query: { ...this.$route.query, tab: name },
      });
    },
  },
};
</script>
