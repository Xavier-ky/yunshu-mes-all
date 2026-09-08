<template>
  <div class="app-container inventory-doc-shell">
    <el-tabs v-model="hubMode" class="inventory-doc-tabs" @tab-change="onModeChange">
      <el-tab-pane label="物料清单" name="item" lazy>
        <MdItem v-if="hubMode === 'item'" />
      </el-tab-pane>
      <el-tab-pane label="BOM" name="bom" lazy>
        <BomManage v-if="hubMode === 'bom'" />
      </el-tab-pane>
      <el-tab-pane label="物料分类" name="type" lazy>
        <ItemType v-if="hubMode === 'type'" />
      </el-tab-pane>
      <el-tab-pane label="计量单位" name="unit" lazy>
        <UnitMeasure v-if="hubMode === 'unit'" />
      </el-tab-pane>
      <el-tab-pane label="客户" name="client" lazy>
        <ClientManage v-if="hubMode === 'client'" />
      </el-tab-pane>
      <el-tab-pane label="供应商" name="vendor" lazy>
        <VendorManage v-if="hubMode === 'vendor'" />
      </el-tab-pane>
    </el-tabs>
  </div>
</template>

<script>
import MdItem from "@/yunshu-ui/md/mditem/index.vue";
import ItemType from "@/yunshu-ui/md/itemtype/index.vue";
import UnitMeasure from "@/yunshu-ui/md/unitmeasure/index.vue";
import BomManage from "@/yunshu-ui/md/bom/index.vue";
import ClientManage from "@/yunshu-ui/md/client/index.vue";
import VendorManage from "@/yunshu-ui/md/vendor/index.vue";

const MODES = ["item", "bom", "type", "unit", "client", "vendor"];

export default {
  name: "MdItemHub",
  components: { MdItem, ItemType, UnitMeasure, BomManage, ClientManage, VendorManage },
  data() {
    return { hubMode: "item" };
  },
  created() {
    this.syncFromRoute();
  },
  watch: {
    "$route.query.mode"() {
      this.syncFromRoute();
    },
  },
  methods: {
    syncFromRoute() {
      const mode = this.$route.query.mode;
      const next = MODES.includes(mode) ? mode : "item";
      this.hubMode = next;
      if (mode !== next) {
        this.$router.replace({ path: "/app/master-data/mditem", query: { ...this.$route.query, mode: next } });
      }
    },
    onModeChange(mode) {
      if (this.$route.query.mode === mode) return;
      this.$router.replace({ path: "/app/master-data/mditem", query: { ...this.$route.query, mode } });
    },
  },
};
</script>
