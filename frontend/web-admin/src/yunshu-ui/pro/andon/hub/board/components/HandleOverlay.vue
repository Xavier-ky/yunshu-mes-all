<template>
  <div class="andon-handle-overlay">
    <HandleHeroBar :summary="summary" :loading="loading" @refresh="refresh" @board="$emit('back')" />

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
          ref="detailPanel"
          :record-id="selectedId"
          :initial-tab="detailTab"
          @saved="onSaved"
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
import HandleHeroBar from "../../handle/components/HandleHeroBar.vue";
import PendingAside from "../../handle/components/PendingAside.vue";
import HandleDetailPanel from "../../handle/components/HandleDetailPanel.vue";
import { listAndonrecord } from "@/yunshu-ui/api/mes/pro/andonrecord";
import { getAndonBoardSummary } from "@/yunshu-ui/api/mes/pro/andonboard";

export default {
  name: "AndonHandleOverlay",
  components: { HandleHeroBar, PendingAside, HandleDetailPanel },
  props: {
    recordId: { type: [Number, String], default: null },
    detailTab: { type: String, default: "detail" },
  },
  emits: ["back", "saved"],
  data() {
    return {
      loading: false,
      pendingRows: [],
      selectedId: null,
      summary: {},
      showHistory: false,
      historyRows: [],
      historyLoading: false,
      authUser: {},
    };
  },
  watch: {
    recordId: {
      immediate: true,
      handler(id) {
        if (id) this.selectedId = Number(id) || id;
      },
    },
    showHistory(v) {
      if (v) this.loadHistory();
    },
  },
  mounted() {
    this.authUser = getAuthUser() || {};
    this.refresh();
  },
  methods: {
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
          mineCount: bs.mineCount ?? mine,
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
      this.$emit("select-record", row.recordId);
    },
    onSaved() {
      this.refresh();
      this.$refs.detailPanel?.refreshTasks?.();
      this.$emit("saved");
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
