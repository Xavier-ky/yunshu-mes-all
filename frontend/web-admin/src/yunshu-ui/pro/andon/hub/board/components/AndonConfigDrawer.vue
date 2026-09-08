<template>
  <el-drawer v-model="visible" title="安灯设置" size="78%" append-to-body destroy-on-close @closed="activeTab = 'config'">
    <el-tabs v-model="activeTab" class="sys-hub-tabs">
      <el-tab-pane label="呼叫配置" name="config">
        <AndonConfig v-if="activeTab === 'config'" embed-mode ref="configRef" />
      </el-tab-pane>
      <el-tab-pane label="安灯类型" name="types">
        <TypePanel v-if="activeTab === 'types'" ref="typesRef" />
      </el-tab-pane>
      <el-tab-pane label="异常原因" name="reasons">
        <ReasonPanel v-if="activeTab === 'reasons'" ref="reasonsRef" />
      </el-tab-pane>
    </el-tabs>
  </el-drawer>
</template>

<script>
import AndonConfig from "../../../config.vue";
import TypePanel from "../../../panels/TypePanel.vue";
import ReasonPanel from "../../../panels/ReasonPanel.vue";

export default {
  name: "AndonConfigDrawer",
  components: { AndonConfig, TypePanel, ReasonPanel },
  data() {
    return {
      visible: false,
      activeTab: "config",
    };
  },
  methods: {
    open(tab = "config") {
      this.activeTab = tab;
      this.visible = true;
    },
  },
};
</script>
