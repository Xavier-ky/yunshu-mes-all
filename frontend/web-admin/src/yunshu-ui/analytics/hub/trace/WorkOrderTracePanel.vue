<template>
  <div class="inbound-doc-panel wo-trace-panel">
    <section class="inbound-filter-panel">
      <el-form :inline="true" class="inbound-filter-form" label-width="0" @submit.prevent>
        <el-form-item>
          <el-input
            v-model="workOrderCode"
            placeholder="工单号 / 客户订单号 / 入库单号 / 报工单号"
            clearable
            class="wo-trace-code-input"
            @keyup.enter="handleSearch"
          />
        </el-form-item>
        <el-form-item class="filter-actions wo-trace-toolbar">
          <el-select
            v-model="recentPick"
            class="wo-trace-recent-select"
            placeholder="最近可追溯工单"
            clearable
            filterable
            :loading="recentLoading"
            :disabled="!recentWorkOrders.length"
            @change="onRecentPick"
          >
            <el-option
              v-for="item in recentWorkOrders"
              :key="item.workOrderId"
              :label="formatRecentLabel(item)"
              :value="item.workOrderNo"
            >
              <div class="wo-trace-option">
                <span class="wo-trace-option__code">{{ item.workOrderNo }}</span>
                <span class="wo-trace-option__meta">{{ item.orderNo || "—" }} · {{ lifecycleLabel(item.lifecycleStatus) }}</span>
              </div>
            </el-option>
          </el-select>
          <el-button type="primary" icon="el-icon-search" @click="handleSearch" :loading="loading">追溯</el-button>
          <el-button icon="el-icon-refresh" @click="resetSearch">重置</el-button>
        </el-form-item>
      </el-form>
    </section>

    <div v-if="summary" class="wo-trace-summary">
      <div class="wo-trace-summary__head">
        <strong>{{ summary.workOrderNo }}</strong>
        <el-tag size="small" type="success">{{ lifecycleLabel(summary.lifecycleStatus) }}</el-tag>
        <span class="wo-trace-summary__meta">客户订单 {{ summary.orderNo || '—' }} · {{ summary.customerName || '—' }}</span>
        <span class="wo-trace-summary__meta">{{ summary.productCode }} {{ summary.productName }}</span>
        <span class="wo-trace-summary__meta">计划 {{ summary.planQty }} 件 · 完工 {{ summary.completedQty ?? '—' }} 件</span>
      </div>
    </div>

    <div v-loading="loading" class="wo-trace-sections">
      <template v-if="traceData">
        <section v-for="block in traceBlocks" :key="block.key" class="wo-trace-block">
          <div class="wo-trace-block__head">
            <span class="wo-trace-block__title">{{ block.title }}</span>
            <span class="wo-trace-count">{{ block.rows.length }}</span>
          </div>
          <el-table
            v-if="block.rows.length"
            class="yunshu-data-table wo-trace-table"
            stripe
            border
            size="small"
            :data="block.rows"
          >
            <el-table-column
              v-for="col in block.columns"
              :key="col.prop"
              :label="col.label"
              :prop="col.prop"
              :min-width="col.minWidth || 100"
              align="center"
              header-align="center"
              show-overflow-tooltip
            />
          </el-table>
          <p v-else class="wo-trace-empty">暂无记录</p>
        </section>
      </template>
      <div v-else-if="!loading && searched" class="wo-trace-empty-wrap">
        <el-empty description="未找到该工单的追溯数据" />
      </div>
      <div v-else-if="!loading && !recentLoading" class="wo-trace-empty-wrap">
        <el-empty description="暂无可追溯工单，请先完成领料或报工流程" />
      </div>
    </div>
  </div>
</template>

<script>
import { traceWorkOrder, listRecentTraceWorkOrders } from "@/yunshu-ui/api/traceability/workorder";

const LIFECYCLE_LABELS = {
  RELEASED: "已下达",
  KITTED: "已齐套",
  SCHEDULED: "已排产",
  MATERIAL_ISSUED: "已领料",
  IN_PROGRESS: "生产中",
  QC_PENDING: "待质检",
  QC_PASSED: "质检通过",
  COMPLETED: "已完工",
};

function snakeToCamelKey(key) {
  if (!key || !key.includes("_")) {
    return key;
  }
  return key.replace(/_([a-z])/g, (_, c) => c.toUpperCase());
}

function normalizeRow(row) {
  if (!row || typeof row !== "object") {
    return row;
  }
  const out = {};
  Object.entries(row).forEach(([key, value]) => {
    out[snakeToCamelKey(key)] = value;
  });
  return out;
}

function normalizeTracePayload(res) {
  const raw = !res
    ? null
    : res.workOrder
      ? res
      : res.data?.workOrder
        ? res.data
        : res.data || null;
  if (!raw?.workOrder) {
    return null;
  }
  return {
    workOrder: normalizeRow(raw.workOrder),
    customerOrder: (raw.customerOrder || []).map(normalizeRow),
    issues: (raw.issues || []).map(normalizeRow),
    dispatches: (raw.dispatches || []).map(normalizeRow),
    proCardProcesses: (raw.proCardProcesses || []).map(normalizeRow),
    consumes: (raw.consumes || []).map(normalizeRow),
    feedbacks: (raw.feedbacks || []).map(normalizeRow),
    reports: (raw.reports || []).map(normalizeRow),
    ipqc: (raw.ipqc || []).map(normalizeRow),
    recpts: (raw.recpts || []).map(normalizeRow),
    proCards: (raw.proCards || []).map(normalizeRow),
    productSns: (raw.productSns || []).map(normalizeRow),
  };
}

export default {
  name: "WorkOrderTracePanel",
  data() {
    return {
      workOrderCode: "",
      loading: false,
      recentLoading: false,
      searched: false,
      traceData: null,
      recentWorkOrders: [],
      recentPick: "",
    };
  },
  computed: {
    summary() {
      return this.traceData?.workOrder || null;
    },
    traceBlocks() {
      const d = this.traceData;
      if (!d) {
        return [];
      }
      return [
        {
          key: "customerOrder",
          title: "客户订单",
          rows: d.customerOrder || [],
          columns: [
            { label: "订单号", prop: "orderNo", minWidth: 160 },
            { label: "客户", prop: "customerName", minWidth: 120 },
            { label: "数量", prop: "orderQty", minWidth: 80 },
            { label: "状态", prop: "status", minWidth: 90 },
          ],
        },
        {
          key: "issues",
          title: "生产领料",
          rows: d.issues || [],
          columns: [
            { label: "领料单号", prop: "issueCode", minWidth: 120 },
            { label: "状态", prop: "status", minWidth: 90 },
          ],
        },
        {
          key: "dispatches",
          title: "派工执行",
          rows: d.dispatches || [],
          columns: [
            { label: "派工单号", prop: "dispatchNo", minWidth: 150 },
            { label: "工序", prop: "stepName", minWidth: 100 },
            { label: "工位", prop: "stationName", minWidth: 100 },
            { label: "完成", prop: "completedQty", minWidth: 70 },
            { label: "计划", prop: "plannedQty", minWidth: 70 },
            { label: "状态", prop: "status", minWidth: 90 },
          ],
        },
        {
          key: "consumes",
          title: "物料消耗",
          rows: d.consumes || [],
          columns: [
            { label: "消耗单", prop: "recordId", minWidth: 80 },
            { label: "关联领料", prop: "issueIdRef", minWidth: 100 },
            { label: "状态", prop: "status", minWidth: 90 },
          ],
        },
        {
          key: "feedbacks",
          title: "生产报工",
          rows: d.feedbacks || [],
          columns: [
            { label: "报工单号", prop: "feedbackCode", minWidth: 140 },
            { label: "工序", prop: "processName", minWidth: 100 },
            { label: "良品", prop: "quantityQualified", minWidth: 80 },
            { label: "状态", prop: "status", minWidth: 90 },
          ],
        },
        {
          key: "reports",
          title: "产量报告",
          rows: d.reports || [],
          columns: [
            { label: "报告编号", prop: "reportNo", minWidth: 140 },
            { label: "合格数量", prop: "goodQty", minWidth: 100 },
          ],
        },
        {
          key: "ipqc",
          title: "过程质检 IPQC",
          rows: d.ipqc || [],
          columns: [
            { label: "检验单号", prop: "ipqcCode", minWidth: 130 },
            { label: "工序", prop: "processName", minWidth: 100 },
            { label: "合格数", prop: "quantityQualified", minWidth: 80 },
            { label: "结果", prop: "checkResult", minWidth: 90 },
            { label: "状态", prop: "status", minWidth: 90 },
          ],
        },
        {
          key: "recpts",
          title: "成品入库",
          rows: d.recpts || [],
          columns: [
            { label: "入库单号", prop: "recptCode", minWidth: 120 },
            { label: "数量", prop: "quantity", minWidth: 80 },
            { label: "状态", prop: "status", minWidth: 90 },
          ],
        },
        {
          key: "proCards",
          title: "流转卡",
          rows: d.proCards || [],
          columns: [
            { label: "流转卡号", prop: "cardCode", minWidth: 160 },
            { label: "批次", prop: "batchCode", minWidth: 150 },
            { label: "产品", prop: "itemCode", minWidth: 110 },
            { label: "数量", prop: "quantityTransfered", minWidth: 80 },
            { label: "状态", prop: "status", minWidth: 90 },
          ],
        },
        {
          key: "proCardProcesses",
          title: "流转卡工序",
          rows: d.proCardProcesses || [],
          columns: [
            { label: "工序", prop: "processName", minWidth: 120 },
            { label: "产出", prop: "quantityOutput", minWidth: 80 },
            { label: "完工时间", prop: "outputTime", minWidth: 140 },
          ],
        },
      ];
    },
  },
  watch: {
    "$route.query.wo"(wo) {
      const code = wo ? String(wo) : "";
      if (code && code !== this.workOrderCode) {
        this.workOrderCode = code;
        this.syncRecentPick(code);
        this.handleSearch();
      }
    },
  },
  mounted() {
    this.bootstrap();
  },
  methods: {
    async bootstrap() {
      await this.loadRecentWorkOrders();
      const wo = this.$route?.query?.wo;
      if (wo) {
        this.workOrderCode = String(wo);
        this.syncRecentPick(this.workOrderCode);
        this.handleSearch();
        return;
      }
      const first = this.recentWorkOrders[0];
      if (first?.workOrderNo) {
        this.workOrderCode = first.workOrderNo;
        this.recentPick = first.workOrderNo;
        this.handleSearch();
      }
    },
    async loadRecentWorkOrders() {
      this.recentLoading = true;
      try {
        const res = await listRecentTraceWorkOrders(20);
        const rows = res?.data || res || [];
        this.recentWorkOrders = rows.map(normalizeRow);
      } catch {
        this.recentWorkOrders = [];
      } finally {
        this.recentLoading = false;
      }
    },
    onRecentPick(code) {
      if (!code) {
        return;
      }
      this.workOrderCode = code;
      this.handleSearch();
    },
    syncRecentPick(code) {
      const key = (code || "").trim();
      const hit = this.recentWorkOrders.find(
        (item) => item.workOrderNo === key || item.orderNo === key,
      );
      this.recentPick = hit ? hit.workOrderNo : "";
    },
    formatRecentLabel(item) {
      if (!item) {
        return "";
      }
      return `${item.workOrderNo}${item.orderNo ? " · " + item.orderNo : ""} · ${this.lifecycleLabel(item.lifecycleStatus)}`;
    },
    lifecycleLabel(v) {
      return LIFECYCLE_LABELS[v] || v || "—";
    },
    syncRouteQuery(code) {
      if (this.$route.query?.wo === code && this.$route.query?.tab !== "batch" && this.$route.query?.tab !== "product") {
        return;
      }
      const query = { ...this.$route.query, tab: "workorder", wo: code };
      this.$router.replace({ query });
    },
    handleSearch() {
      const code = (this.workOrderCode || "").trim();
      if (!code) {
        this.$modal?.msgWarning?.("请输入工单号、客户订单号、入库单号或报工单号");
        return;
      }
      this.loading = true;
      this.searched = true;
      traceWorkOrder(code)
        .then((res) => {
          const payload = normalizeTracePayload(res);
          this.traceData = payload;
          if (!payload?.workOrder) {
            this.traceData = null;
            this.$modal?.msgError?.("未找到该工单");
          } else {
            const woNo = payload.workOrder.workOrderNo || code;
            this.workOrderCode = woNo;
            this.syncRecentPick(woNo);
            this.syncRouteQuery(woNo);
          }
        })
        .catch((err) => {
          this.traceData = null;
          this.$modal?.msgError?.(err?.message || "追溯查询失败");
        })
        .finally(() => {
          this.loading = false;
        });
    },
    resetSearch() {
      this.workOrderCode = "";
      this.recentPick = "";
      this.traceData = null;
      this.searched = false;
    },
  },
};
</script>

<style scoped>
.wo-trace-panel {
  display: flex;
  flex-direction: column;
  height: 100%;
  min-height: 0;
  overflow: hidden;
}
.wo-trace-panel :deep(.inbound-filter-panel) {
  margin-bottom: 8px;
  padding: 8px 10px 2px;
}
.wo-trace-code-input {
  width: min(360px, 100%);
  min-width: 240px;
}
.wo-trace-summary {
  flex: 0 0 auto;
  margin-bottom: 8px;
  padding: 8px 12px;
  border-radius: 0;
  background: rgba(23, 105, 224, 0.06);
  border: 1px solid rgba(23, 105, 224, 0.18);
}
.wo-trace-summary__head {
  display: flex;
  flex-wrap: wrap;
  align-items: center;
  gap: 8px 14px;
}
.wo-trace-summary__head strong {
  font-family: "钉钉进步体", "DingTalk JinBuTi", "Microsoft YaHei", sans-serif;
  font-size: 15px;
  font-weight: 600;
  color: #1e293b;
  letter-spacing: 0.02em;
}
.wo-trace-summary__meta {
  color: #64748b;
  font-size: 12px;
}
.wo-trace-sections {
  flex: 1 1 auto;
  min-height: 0;
  overflow: auto;
  display: grid;
  grid-template-columns: 1fr 1fr;
  gap: 10px 12px;
  align-content: start;
  padding-bottom: 8px;
}
.wo-trace-block {
  display: flex;
  flex-direction: column;
  min-height: 0;
  padding: 8px 10px;
  background: #fff;
  border: 1px solid #dce5f0;
}
.wo-trace-block__head {
  display: flex;
  align-items: center;
  justify-content: space-between;
  gap: 8px;
  margin-bottom: 6px;
  padding-left: 10px;
  position: relative;
}
.wo-trace-block__head::before {
  content: "";
  position: absolute;
  left: 0;
  top: 50%;
  width: 3px;
  height: 14px;
  transform: translateY(-50%);
  background: #1769e0;
}
.wo-trace-block__title {
  font-family: "钉钉进步体", "DingTalk JinBuTi", "Microsoft YaHei", sans-serif;
  font-size: 15px;
  font-weight: 600;
  color: #1e293b;
  letter-spacing: 0.02em;
  line-height: 1.3;
}
.wo-trace-count {
  flex-shrink: 0;
  min-width: 22px;
  height: 20px;
  padding: 0 7px;
  border-radius: 10px;
  background: #f1f5f9;
  color: #64748b;
  font-size: 12px;
  font-weight: 600;
  line-height: 20px;
  text-align: center;
}
.wo-trace-empty {
  margin: 0;
  padding: 6px 0 2px;
  color: #94a3b8;
  font-size: 12px;
  line-height: 1.4;
  min-height: 28px;
}
.wo-trace-empty-wrap {
  grid-column: 1 / -1;
  padding: 32px 0;
  text-align: center;
}
.wo-trace-table {
  width: 100%;
}
.wo-trace-table :deep(.el-table__header-wrapper th.el-table__cell),
.wo-trace-table :deep(.el-table__body-wrapper td.el-table__cell) {
  padding: 4px 0;
}
.wo-trace-table :deep(.el-table__cell .cell) {
  line-height: 1.35;
  white-space: nowrap;
}
.wo-trace-toolbar {
  display: inline-flex;
  flex-wrap: wrap;
  align-items: center;
  gap: 8px;
}
.wo-trace-toolbar :deep(.el-form-item__content) {
  display: inline-flex;
  flex-wrap: wrap;
  align-items: center;
  gap: 8px;
}
.wo-trace-recent-select {
  width: 200px;
}
.wo-trace-option {
  display: flex;
  align-items: center;
  justify-content: space-between;
  gap: 12px;
  width: 100%;
}
.wo-trace-option__code {
  color: #1a1a1a;
  font-size: 13px;
}
.wo-trace-option__meta {
  color: #909399;
  font-size: 12px;
  flex-shrink: 0;
}
@media (max-width: 1100px) {
  .wo-trace-sections {
    grid-template-columns: 1fr;
  }
}
</style>
