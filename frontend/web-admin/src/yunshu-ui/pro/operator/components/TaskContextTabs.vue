<template>
  <div class="operator-context-tabs">
    <el-tabs v-model="activeTab" class="operator-context-tabs__head">
      <el-tab-pane label="今日报工" name="feedback" />
      <el-tab-pane label="工序用料" name="bom" />
      <el-tab-pane label="最近绑定" name="binding" />
    </el-tabs>
    <div class="operator-context-tabs__body inbound-table-frame">
      <span class="frame-corner frame-corner--tl" aria-hidden="true"></span>
      <span class="frame-corner frame-corner--tr" aria-hidden="true"></span>
      <span class="frame-corner frame-corner--bl" aria-hidden="true"></span>
      <span class="frame-corner frame-corner--br" aria-hidden="true"></span>

      <el-table
        v-if="activeTab === 'feedback'"
        v-loading="feedbackLoading"
        class="yunshu-data-table inbound-table operator-context-table"
        stripe
        border
        :data="todayFeedback"
        height="100%"
        size="default"
      >
        <el-table-column label="报工单号" prop="feedbackCode" min-width="130" show-overflow-tooltip align="center" />
        <el-table-column label="派工" prop="taskCode" min-width="110" show-overflow-tooltip align="center" />
        <el-table-column label="良品" prop="quantityQualified" width="72" align="center" />
        <el-table-column label="不良" prop="quantityUnquanlified" width="72" align="center" />
        <el-table-column label="时间" min-width="140" align="center">
          <template #default="scope">{{ formatTime(scope.row.feedbackTime) }}</template>
        </el-table-column>
      </el-table>

      <el-table
        v-else-if="activeTab === 'bom'"
        v-loading="bomLoading"
        class="yunshu-data-table inbound-table operator-context-table"
        stripe
        border
        :data="bomLines"
        height="100%"
        size="default"
      >
        <el-table-column label="物料编码" prop="bomItemCode" min-width="120" show-overflow-tooltip align="center" />
        <el-table-column label="物料名称" prop="bomItemName" min-width="140" show-overflow-tooltip align="center" />
        <el-table-column label="用量" prop="quantity" width="80" align="center" />
        <el-table-column label="单位" prop="unitOfMeasure" width="72" align="center" />
      </el-table>

      <el-table
        v-else
        v-loading="bindingLoading"
        class="yunshu-data-table inbound-table operator-context-table"
        stripe
        border
        :data="bindingRows"
        height="100%"
        size="default"
      >
        <el-table-column label="产品 SN" prop="snCode" min-width="150" show-overflow-tooltip align="center" />
        <el-table-column label="批次" min-width="120" show-overflow-tooltip align="center">
          <template #default="s">{{ s.row.batch_no || s.row.batchNo || "—" }}</template>
        </el-table-column>
        <el-table-column label="物料" min-width="120" show-overflow-tooltip align="center">
          <template #default="s">{{ s.row.material_name || s.row.materialName || "—" }}</template>
        </el-table-column>
        <el-table-column label="绑定时间" min-width="140" align="center">
          <template #default="s">{{ formatTime(s.row.bind_time || s.row.bindTime) }}</template>
        </el-table-column>
      </el-table>
    </div>
  </div>
</template>

<script>
import {
  fetchProductBomByProductCode,
  fetchWorkOrderBindings,
} from "@/yunshu-ui/api/mes/pro/operator";

export default {
  name: "OperatorTaskContextTabs",
  props: {
    task: { type: Object, default: null },
    todayFeedback: { type: Array, default: () => [] },
    feedbackLoading: { type: Boolean, default: false },
  },
  data() {
    return {
      activeTab: "feedback",
      bomLoading: false,
      bindingLoading: false,
      bomLines: [],
      bindingRows: [],
    };
  },
  watch: {
    task: {
      immediate: true,
      handler(t) {
        this.loadBom(t);
        this.loadBindings(t);
      },
    },
    activeTab(tab) {
      if (tab === "bom" && this.task) this.loadBom(this.task);
      if (tab === "binding" && this.task) this.loadBindings(this.task);
    },
  },
  methods: {
    formatTime(v) {
      if (!v) return "—";
      const s = String(v);
      return s.length > 16 ? s.slice(0, 16).replace("T", " ") : s.replace("T", " ");
    },
    async loadBom(task) {
      this.bomLines = [];
      if (!task?.productCode) return;
      this.bomLoading = true;
      try {
        const res = await fetchProductBomByProductCode(task.productCode);
        this.bomLines = res?.rows || res?.data || [];
      } catch {
        this.bomLines = [];
      } finally {
        this.bomLoading = false;
      }
    },
    async loadBindings(task) {
      this.bindingRows = [];
      if (!task?.workOrderId && !task?.workOrderNo) return;
      this.bindingLoading = true;
      try {
        this.bindingRows = await fetchWorkOrderBindings(task.workOrderId, task.workOrderNo);
      } catch {
        this.bindingRows = [];
      } finally {
        this.bindingLoading = false;
      }
    },
  },
};
</script>
