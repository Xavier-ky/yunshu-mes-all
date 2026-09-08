<template>
  <div class="app-container print-command-center sys-hub-page">
    <PrintHeroBar
      :summary="printSummary"
      :loading="summaryLoading"
      @refresh="refreshAll"
      @create-template="openCreateTemplate"
    />

    <header class="print-command-top">
      <div class="print-command-top__meta">
        <el-tag :type="clientConnected ? 'success' : 'info'" size="small" effect="plain">
          {{ clientConnected ? "打印客户端已连接" : "打印客户端未连接" }}
        </el-tag>
        <span v-if="transferHint" class="print-command-top__hint">{{ transferHint }}</span>
      </div>
      <div class="print-command-top__actions">
        <el-button size="default" :loading="dockRefreshing" @click="refreshDock">刷新模板</el-button>
        <el-button size="default" @click="openClientDrawer">打印客户端</el-button>
      </div>
    </header>

    <div class="print-command-body">
      <PrintTemplateDock
        ref="dockRef"
        :selected-template-id="selectedTemplateId"
        @select="onSelectTemplate"
        @created="onTemplateCreated"
        @deleted="onTemplateDeleted"
        @loaded="onTemplatesLoaded"
        @loading-change="dockLoading = $event"
      />

      <main class="print-design-main">
        <PrintDesignPanel
          v-if="selectedTemplateId"
          ref="designRef"
          :key="String(selectedTemplateId)"
          embed-mode
          :template-id="selectedTemplateId"
          :paper-type="selectedPaperType"
          @dirty-change="designDirty = $event"
        />
        <div v-else-if="!dockLoading" class="print-design-main__empty">
          <el-empty description="请从左侧选择模板，或新建模板开始设计" />
        </div>
        <div v-else class="print-design-main__empty">
          <el-icon class="is-loading" :size="28"><Loading /></el-icon>
        </div>
      </main>
    </div>

    <el-drawer
      v-model="clientDrawerOpen"
      title="打印客户端"
      size="640px"
      :append-to-body="false"
      destroy-on-close
      class="print-client-drawer"
      @close="closeClientDrawer"
    >
      <ClientPanel v-if="clientDrawerOpen" embed-mode />
    </el-drawer>
  </div>
</template>

<script>
import PrintTemplateDock from "../components/PrintTemplateDock.vue";
import PrintHeroBar from "../components/PrintHeroBar.vue";
import PrintDesignPanel from "@/yunshu-ui/print/template/index.vue";
import ClientPanel from "@/yunshu-ui/print/client/index.vue";
import { getPrintSummary } from "@/yunshu-ui/api/print/summary";
import webSite from "@/config/website";
import { ElMessageBox } from "element-plus";
import { Loading } from "@element-plus/icons-vue";

export default {
  name: "AnalyticsPrintHub",
  components: { PrintTemplateDock, PrintHeroBar, PrintDesignPanel, ClientPanel, Loading },
  data() {
    return {
      selectedTemplateId: null,
      selectedPaperType: "A4",
      designDirty: false,
      clientDrawerOpen: false,
      dockRefreshing: false,
      dockLoading: true,
      dockTemplateList: [],
      pendingRouteTemplateId: null,
      printSummary: {},
      summaryLoading: false,
      clientConnected: false,
      connectionTimer: null,
    };
  },
  computed: {
    transferHint() {
      if (!webSite.print_transfer_url) {
        return "未配置打印中转服务，静默打印需安装 hiPrint 客户端";
      }
      return "";
    },
  },
  mounted() {
    this.syncFromRoute();
    if (this.$route.query?.drawer === "clients") {
      this.clientDrawerOpen = true;
    }
    this.loadSummary();
    this.pollClientConnection();
  },
  beforeUnmount() {
    if (this.connectionTimer) {
      clearInterval(this.connectionTimer);
      this.connectionTimer = null;
    }
  },
  watch: {
    "$route.query"() {
      this.syncFromRoute();
    },
  },
  methods: {
    pollClientConnection() {
      const check = () => {
        this.clientConnected = Boolean(
          window.hiwebSocket?.opened ||
          (window.hiwebSocket?.clients && Object.keys(window.hiwebSocket.clients).length > 0),
        );
      };
      check();
      this.connectionTimer = setInterval(check, 3000);
    },
    async loadSummary() {
      this.summaryLoading = true;
      try {
        const res = await getPrintSummary();
        this.printSummary = res?.data || {};
      } catch {
        this.printSummary = {};
      } finally {
        this.summaryLoading = false;
      }
    },
    async refreshAll() {
      await Promise.all([this.refreshDock(), this.loadSummary()]);
    },
    openCreateTemplate() {
      this.$refs.dockRef?.handleAdd?.();
    },
    syncFromRoute() {
      const drawer = this.$route.query?.drawer;
      const tab = this.$route.query?.tab;
      this.clientDrawerOpen = drawer === "clients" || tab === "clients";

      const routeTemplateId = this.$route.query?.templateId;
      if (routeTemplateId && String(routeTemplateId) !== String(this.selectedTemplateId)) {
        this.pendingRouteTemplateId = routeTemplateId;
        this.tryApplyRouteTemplate();
      }
    },
    tryApplyRouteTemplate() {
      if (!this.pendingRouteTemplateId) return false;
      const dock = this.$refs.dockRef;
      const list = dock?.templateList?.length ? dock.templateList : this.dockTemplateList;
      if (!list?.length) return false;
      const hit = list.find(
        (item) => String(item.templateId) === String(this.pendingRouteTemplateId),
      );
      if (hit) {
        this.applySelection(hit, true);
        this.pendingRouteTemplateId = null;
        return true;
      }
      this.pendingRouteTemplateId = null;
      return false;
    },
    onTemplatesLoaded(list) {
      this.dockTemplateList = list || [];
      this.dockLoading = false;
      if (this.tryApplyRouteTemplate()) return;
      this.ensureDefaultSelection(this.dockTemplateList);
    },
    ensureDefaultSelection(list) {
      if (!list?.length) {
        this.selectedTemplateId = null;
        this.updateRouteQuery(true);
        return;
      }
      if (this.selectedTemplateId != null && this.selectedTemplateId !== "") {
        const stillExists = list.some(
          (item) => String(item.templateId) === String(this.selectedTemplateId),
        );
        if (stillExists) return;
      }
      const routeTemplateId = this.$route.query?.templateId;
      let target = null;
      if (routeTemplateId) {
        target = list.find((item) => String(item.templateId) === String(routeTemplateId));
      }
      if (!target) target = list[0];
      this.applySelection(target, true);
    },
    async confirmSwitchDesign() {
      if (!this.designDirty) return true;
      try {
        await ElMessageBox.confirm("当前模板还有未保存修改，切换后将丢失这些修改。", "未保存修改", {
          confirmButtonText: "放弃修改",
          cancelButtonText: "返回保存",
          type: "warning",
        });
        return true;
      } catch {
        return false;
      }
    },
    async onSelectTemplate(payload) {
      if (String(payload.templateId) === String(this.selectedTemplateId)) return;
      const ok = await this.confirmSwitchDesign();
      if (!ok) return;
      this.selectedTemplateId = payload.templateId;
      this.selectedPaperType = payload.paperType || "A4";
      this.designDirty = false;
      this.updateRouteQuery();
    },
    onTemplateCreated(item) {
      this.selectedTemplateId = item.templateId;
      this.selectedPaperType = item.paperType || "A4";
      this.designDirty = false;
      this.updateRouteQuery();
      this.loadSummary();
    },
    async onTemplateDeleted(deletedId) {
      if (String(deletedId) !== String(this.selectedTemplateId)) {
        this.loadSummary();
        return;
      }
      const list = this.$refs.dockRef?.templateList || [];
      if (list.length) {
        this.applySelection(list[0], true);
      } else {
        this.selectedTemplateId = null;
        this.designDirty = false;
        this.updateRouteQuery();
      }
      this.loadSummary();
    },
    applySelection(item, pushRoute = true) {
      this.selectedTemplateId = item.templateId;
      this.selectedPaperType = item.paperType || "A4";
      this.designDirty = false;
      if (pushRoute) this.updateRouteQuery();
      else this.updateRouteQuery(true);
    },
    updateRouteQuery() {
      const query = { ...this.$route.query };
      if (this.selectedTemplateId) {
        query.templateId = String(this.selectedTemplateId);
        if (this.selectedPaperType) query.paperType = this.selectedPaperType;
      } else {
        delete query.templateId;
        delete query.paperType;
      }
      if (this.clientDrawerOpen) query.drawer = "clients";
      else delete query.drawer;
      this.$router.replace({ query });
    },
    openClientDrawer() {
      this.clientDrawerOpen = true;
      this.$router.replace({ query: { ...this.$route.query, drawer: "clients" } });
    },
    closeClientDrawer() {
      if (this.$route.query?.drawer !== "clients") return;
      const query = { ...this.$route.query };
      delete query.drawer;
      this.$router.replace({ query });
    },
    async refreshDock() {
      this.dockRefreshing = true;
      this.dockLoading = true;
      try {
        await this.$refs.dockRef?.refresh?.();
        await this.loadSummary();
      } finally {
        this.dockRefreshing = false;
      }
    },
  },
};
</script>
