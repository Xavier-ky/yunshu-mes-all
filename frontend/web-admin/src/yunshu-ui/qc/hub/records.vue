<template>
  <el-drawer
    v-model="visible"
    title="检验记录"
    size="88%"
    class="qc-hub-drawer"
    body-class="qc-hub-drawer-body"
    append-to-body
    destroy-on-close
    @closed="activeTab = 'iqc'"
  >
    <el-tabs v-model="activeTab" class="qc-hub-tabs">
      <el-tab-pane label="来料 IQC" name="iqc" lazy>
        <IqcList v-if="loaded.iqc" />
      </el-tab-pane>
      <el-tab-pane label="过程 IPQC" name="ipqc" lazy>
        <IpqcList v-if="loaded.ipqc" />
      </el-tab-pane>
      <el-tab-pane label="出货 OQC" name="oqc" lazy>
        <OqcList v-if="loaded.oqc" />
      </el-tab-pane>
      <el-tab-pane label="退料 RQC" name="rqc" lazy>
        <RqcList v-if="loaded.rqc" />
      </el-tab-pane>
    </el-tabs>
  </el-drawer>
</template>

<script>
import IqcList from "@/yunshu-ui/qc/iqc/iqc.vue";
import IpqcList from "@/yunshu-ui/qc/ipqc/index.vue";
import OqcList from "@/yunshu-ui/qc/oqc/index.vue";
import RqcList from "@/yunshu-ui/qc/rqc/index.vue";

export default {
  name: "QcRecordsHub",
  components: { IqcList, IpqcList, OqcList, RqcList },
  data() {
    return {
      visible: false,
      activeTab: "iqc",
      loaded: { iqc: false, ipqc: false, oqc: false, rqc: false },
    };
  },
  methods: {
    open(tab = "iqc") {
      this.activeTab = tab || "iqc";
      this.loaded[this.activeTab] = true;
      this.visible = true;
    },
  },
  watch: {
    activeTab(tab) {
      if (tab) this.loaded[tab] = true;
    },
  },
};
</script>
