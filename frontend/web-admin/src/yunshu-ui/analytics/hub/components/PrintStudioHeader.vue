<template>
  <header v-if="!embedMode" class="print-studio-header">
    <div class="print-studio-header__identity">
      <el-button class="print-studio-header__back" link @click="goBack">
        <el-icon><ArrowLeft /></el-icon>
        返回模板库
      </el-button>
      <div class="print-studio-header__title">
        <span class="print-studio-header__kicker">PRINT STUDIO / 模板设计</span>
        <strong>{{ templateName || "未命名模板" }}</strong>
      </div>
    </div>
    <div class="print-studio-header__state" :class="{ 'print-studio-header__state--dirty': dirty }">
      <span class="print-studio-header__dot"></span>
      <span>{{ dirty ? "有未保存修改" : "已保存" }}</span>
    </div>
  </header>
</template>

<script>
import { ArrowLeft } from "@element-plus/icons-vue";

export default {
  name: "PrintStudioHeader",
  components: { ArrowLeft },
  props: {
    templateName: { type: String, default: "" },
    dirty: { type: Boolean, default: false },
    embedMode: { type: Boolean, default: false },
  },
  methods: {
    goBack() {
      this.$router.push({ path: "/app/analytics/print", query: { tab: "templates" } });
    },
  },
};
</script>
