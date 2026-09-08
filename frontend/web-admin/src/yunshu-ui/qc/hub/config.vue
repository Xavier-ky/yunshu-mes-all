<template>
  <el-drawer
    v-model="visible"
    title="检验标准配置"
    size="88%"
    class="qc-hub-drawer"
    body-class="qc-hub-drawer-body"
    append-to-body
    destroy-on-close
    @closed="activeTab = 'template'"
  >
    <el-tabs v-model="activeTab" class="qc-hub-tabs">
      <el-tab-pane label="检测模板" name="template" lazy>
        <TemplatePanel v-if="loaded.template" />
      </el-tab-pane>
      <el-tab-pane label="检测项设置" name="index" lazy>
        <IndexPanel v-if="loaded.index" />
      </el-tab-pane>
      <el-tab-pane label="常见缺陷" name="defect" lazy>
        <DefectPanel v-if="loaded.defect" />
      </el-tab-pane>
    </el-tabs>
  </el-drawer>
</template>

<script>
import TemplatePanel from "@/yunshu-ui/qc/qctemplate/index.vue";
import IndexPanel from "@/yunshu-ui/qc/qcindex/index.vue";
import DefectPanel from "@/yunshu-ui/qc/qcdefect/index.vue";

export default {
  name: "QcConfigHub",
  components: { TemplatePanel, IndexPanel, DefectPanel },
  data() {
    return {
      visible: false,
      activeTab: "template",
      loaded: { template: false, index: false, defect: false },
    };
  },
  methods: {
    open(tab = "template") {
      this.activeTab = tab || "template";
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
