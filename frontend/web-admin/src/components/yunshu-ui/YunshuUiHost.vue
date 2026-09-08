<template>
  <el-config-provider :locale="zhCn" size="default">
    <div class="yunshu-ui-root">
      <slot />
    </div>
  </el-config-provider>
</template>

<script setup>
import { getCurrentInstance } from "vue";
import ElementPlus from "element-plus";
import zhCn from "element-plus/dist/locale/zh-cn.mjs";
import Pagination from "@/yunshu-ui/components/Pagination/index.vue";
import RightToolbar from "@/yunshu-ui/components/RightToolbar/index.vue";
import DictTag from "@/yunshu-ui/components/DictTag/index.vue";
import SvgIcon from "@/yunshu-ui/components/SvgIcon/index.vue";
import { ElMessage } from "element-plus";
import modal from "@/yunshu-ui/plugins/modal";
import tab from "@/yunshu-ui/plugins/tab";
import { parseTime, resetForm, handleTree, addDateRange, selectDictLabel, selectDictLabels } from "@/yunshu-ui/utils/yunshu-utils";
import { installDict } from "@/yunshu-ui/utils/dict/install";
import { getDicts } from "@/yunshu-ui/api/system/dict/data";
import { getConfigKey } from "@/yunshu-ui/api/system/config";
import hasPermi from "@/yunshu-ui/directives/hasPermi";
import { checkPermi } from "@/yunshu-ui/utils/permission";
import { installLegacyElement } from "@/yunshu-ui/compat/legacyElement";
import { installHiprintRuntime } from "@/yunshu-ui/print/print-bootstrap";
import download from "@/yunshu-ui/utils/download";
import webSite from "@/config/website";
import "element-plus/dist/index.css";
import "@/yunshu-ui/styles/element-icons.css";
import "@/yunshu-ui/styles/yunshu-ui-content.scss";
import "@/yunshu-ui/styles/yunshu-compat.scss";
import "@/styles/yunshu-ui.css";

const inst = getCurrentInstance();
if (inst && !globalThis.__YUNSHU_UI_RUNTIME__) {
  const app = inst.appContext.app;
  app.use(ElementPlus, { locale: zhCn, size: "default" });
  installLegacyElement(app);
  app.component("Pagination", Pagination);
  app.component("RightToolbar", RightToolbar);
  app.component("DictTag", DictTag);
  app.component("SvgIcon", SvgIcon);
  app.component("svg-icon", SvgIcon);
  app.config.globalProperties.$modal = modal;
  app.config.globalProperties.$message = ElMessage;
  app.config.globalProperties.$tab = tab;
  app.config.globalProperties.parseTime = parseTime;
  app.config.globalProperties.resetForm = resetForm;
  app.config.globalProperties.handleTree = handleTree;
  app.config.globalProperties.addDateRange = addDateRange;
  app.config.globalProperties.selectDictLabel = selectDictLabel;
  app.config.globalProperties.selectDictLabels = selectDictLabels;
  app.config.globalProperties.getDicts = getDicts;
  app.config.globalProperties.getConfigKey = (key) =>
    getConfigKey(key).catch(() => ({ code: 200, msg: "123456" }));
  app.config.globalProperties.checkPermission = checkPermi;
  app.config.globalProperties.download = download;
  installHiprintRuntime(app);
  if (!app.config.globalProperties.website) {
    app.config.globalProperties.website = {
      reportUrl: webSite.reportUrl || "/ureport",
      print_transfer_url: webSite.print_transfer_url || "",
      print_transfer_token: webSite.print_transfer_token || "",
    };
  }
  app.mixin({
    methods: {
      checkPermission: checkPermi,
    },
  });
  installDict(app, {
    metas: {
      "*": {
        labelField: "dictLabel",
        valueField: "dictValue",
        request(dictMeta) {
          return getDicts(dictMeta.type).then((res) => res.data);
        },
      },
    },
  });
  app.directive("hasPermi", hasPermi);
  globalThis.__YUNSHU_UI_RUNTIME__ = true;
}
</script>

<style scoped>
.yunshu-ui-root {
  width: 100%;
  height: 100%;
  min-height: 0;
  display: flex;
  flex-direction: column;
}
</style>
