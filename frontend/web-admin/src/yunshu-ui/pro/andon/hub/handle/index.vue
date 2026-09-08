<template>
  <div class="app-container andon-handle-center">
    <HandleHeroBar :summary="summary" :loading="loading" @refresh="refresh" @board="goBoard" />

    <el-row :gutter="0" class="andon-handle-row operator-hub-row">
      <el-col :span="6" class="andon-handle-aside operator-hub-aside">
        <PendingAside
          :rows="pendingRows"
          :selected-id="selectedId"
          :loading="loading"
          @select="onSelect"
          @history="showHistory = true"
        />
      </el-col>
      <el-col :span="18" class="andon-handle-main operator-hub-main-col">
        <HandleDetailPanel
          :record-id="selectedId"
          :initial-tab="detailTab"
          @saved="onSaved"
          ref="detailPanel"
        />
      </el-col>
    </el-row>

    <el-drawer v-model="showHistory" title="已处置历史" size="70%" append-to-body>
      <el-table class="yunshu-data-table" stripe border :data="historyRows" v-loading="historyLoading">
        <el-table-column label="工位" prop="workstationName" min-width="100" />
        <el-table-column label="原因" prop="andonReason" min-width="140" show-overflow-tooltip />
        <el-table-column label="处置人" prop="handlerNickName" width="100" />
        <el-table-column label="处置时间" prop="handleTime" width="160" />
      </el-table>
    </el-drawer>
  </div>
</template>

<script>
import { getAuthUser } from "@/utils/auth-context";
import HandleHeroBar from "./components/HandleHeroBar.vue";
import PendingAside from "./components/PendingAside.vue";
import HandleDetailPanel from "./components/HandleDetailPanel.vue";
import { listAndonrecord } from "@/yunshu-ui/api/mes/pro/andonrecord";
import { getAndonBoardSummary } from "@/yunshu-ui/api/mes/pro/andonboard";

export default {
  name: "AndonHandleHub",
  components: { HandleHeroBar, PendingAside, HandleDetailPanel },
  data() {
    return {
      loading: false,
      pendingRows: [],
      selectedId: null,
      summary: {},
      detailTab: "detail",
      showHistory: false,
      historyRows: [],
      historyLoading: false,
      authUser: {},
    };
  },
  mounted() {
    this.authUser = getAuthUser() || {};
    this.syncFromRoute();
    this.refresh();
  },
  watch: {
    "$route.query"() {
      this.syncFromRoute();
    },
    showHistory(v) {
      if (v) this.loadHistory();
    },
  },
  methods: {
    syncFromRoute() {
      const rid = this.$route.query?.recordId;
      if (rid) this.selectedId = Number(rid) || rid;
      const tab = this.$route.query?.tab;
      if (tab === "tasks" || tab === "detail") this.detailTab = tab;
    },
    async refresh() {
      this.loading = true;
      try {
        const [activeRes, board] = await Promise.all([
          listAndonrecord({ status: "ACTIVE", pageNum: 1, pageSize: 200 }),
          getAndonBoardSummary(),
        ]);
        this.pendingRows = activeRes?.rows || [];
        const bs = board?.data || {};
        const mine = this.pendingRows.filter(
          (r) => String(r.handlerUserId) === String(this.authUser.userId)
        ).length;
        this.summary = {
          activeCount: bs.activeRecords ?? this.pendingRows.length,
          mineCount: mine,
          todayClosed: bs.shiftClosed ?? 0,
          avgMinutes: bs.avgResponseMinutes ?? 0,
        };
        if (!this.selectedId && this.pendingRows.length) {
          this.selectedId = this.pendingRows[0].recordId;
        }
      } finally {
        this.loading = false;
      }
    },
    onSelect(row) {
      this.selectedId = row.recordId;
      this.$router.replace({ query: { ...this.$route.query, recordId: String(row.recordId) } });
    },
    onSaved() {
      this.refresh();
      this.$refs.detailPanel?.refreshTasks?.();
    },
    goBoard() {
      this.$router.push("/app/andon");
    },
    async loadHistory() {
      this.historyLoading = true;
      try {
        const res = await listAndonrecord({ status: "HANDLED", pageNum: 1, pageSize: 100 });
        this.historyRows = res?.rows || [];
      } finally {
        this.historyLoading = false;
      }
    },
  },
};
</script>
