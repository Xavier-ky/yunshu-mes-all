<template>
  <el-dialog
    :title="dialogTitle"
    v-model="showFlag"
    append-to-body
    :modal="true"
    width="1280px"
    class="yunshu-picker-dialog workorder-picker-dialog"
    :z-index="3100"
    destroy-on-close
    @opened="onDialogOpened"
  >
    <p v-if="dialogHint" class="picker-dialog-hint">{{ dialogHint }}</p>
    <section class="picker-filter-panel workorder-picker-filter">
      <el-form :model="queryParams" ref="queryForm" size="default" class="picker-filter-form picker-filter-form--single-row" label-width="0" @submit.prevent>
        <el-form-item prop="workorderCode" class="picker-field picker-field--code">
          <el-input v-model="queryParams.workorderCode" placeholder="工单编码" clearable @keyup.enter="handleQuery" />
        </el-form-item>
        <el-form-item prop="workorderName" class="picker-field">
          <el-input v-model="queryParams.workorderName" placeholder="工单名称" clearable @keyup.enter="handleQuery" />
        </el-form-item>
        <el-form-item prop="productCode" class="picker-field">
          <el-input v-model="queryParams.productCode" placeholder="产品编号" clearable @keyup.enter="handleQuery" />
        </el-form-item>
        <el-form-item prop="productName" class="picker-field">
          <el-input v-model="queryParams.productName" placeholder="产品名称" clearable @keyup.enter="handleQuery" />
        </el-form-item>
        <el-form-item prop="clientName" class="picker-field">
          <el-input v-model="queryParams.clientName" placeholder="客户名称" clearable @keyup.enter="handleQuery" />
        </el-form-item>
        <el-form-item class="picker-filter-actions">
          <el-button type="primary" icon="el-icon-search" @click="handleQuery">搜索</el-button>
          <el-button icon="el-icon-refresh" @click="resetQuery">重置</el-button>
        </el-form-item>
      </el-form>
    </section>
    <div class="picker-table-frame">
      <el-table
        v-loading="loading"
        class="yunshu-data-table picker-table"
        stripe
        border
        height="360"
        highlight-current-row
        :data="workorderList"
        @current-change="handleCurrent"
        @row-dblclick="handleRowDbClick"
      >
        <el-table-column width="52" align="center" fixed="left">
          <template #default="scope">
            <el-radio
              v-model="selectedWorkorderId"
              :label="normalizeId(scope.row.workorderId)"
              @change="handleRowChange(scope.row)"
            >{{ "" }}</el-radio>
          </template>
        </el-table-column>
        <el-table-column label="工单编码" min-width="168" prop="workorderCode" show-overflow-tooltip />
        <el-table-column label="工单名称" min-width="140" align="center" prop="workorderName" show-overflow-tooltip />
        <el-table-column label="流程状态" width="100" align="center" prop="lifecycleStatus">
          <template #default="scope">
            <span>{{ lifecycleLabel(scope.row.lifecycleStatus) }}</span>
          </template>
        </el-table-column>
        <el-table-column label="产品编号" min-width="110" align="center" prop="productCode" show-overflow-tooltip />
        <el-table-column label="产品名称" min-width="130" align="center" prop="productName" show-overflow-tooltip />
        <el-table-column label="数量" width="72" align="center" prop="quantity" />
        <el-table-column label="需求日期" align="center" prop="requestDate" width="110">
          <template #default="scope">
            <span>{{ parseTime(scope.row.requestDate, '{y}-{m}-{d}') }}</span>
          </template>
        </el-table-column>
        <el-table-column label="客户" min-width="100" align="center" prop="clientName" show-overflow-tooltip />
      </el-table>
    </div>
    <pagination
      v-show="total > 0"
      :total="total"
      :page="queryParams.pageNum"
      :limit="queryParams.pageSize"
      @update:page="queryParams.pageNum = $event"
      @update:limit="queryParams.pageSize = $event"
      @pagination="getList"
    />
    <template #footer>
      <div class="dialog-footer">
        <el-button type="primary" @click="confirmSelect">确 定</el-button>
        <el-button @click="showFlag = false">取 消</el-button>
      </div>
    </template>
  </el-dialog>
</template>
<script>
import { listWorkorder } from "@/yunshu-ui/api/mes/pro/workorder";

const LIFECYCLE_LABELS = {
  RELEASED: "已下达",
  KITTING_OK: "已齐套",
  SCHEDULED: "已排产",
  MATERIAL_ISSUED: "已领料",
  IN_PROGRESS: "生产中",
};

export default {
  name: "WorkOrderSelectSingle",
  dicts: ["mes_order_status", "mes_workorder_sourcetype"],
  props: {
    workorder: {
      type: Object,
      default() {
        return { workorderType: "SELF" };
      },
    },
  },
  data() {
    return {
      showFlag: false,
      dialogTitle: "工单选择",
      dialogHint: "",
      loading: true,
      selectedWorkorderId: undefined,
      selectedRows: null,
      showSearch: true,
      total: 0,
      workorderList: [],
      queryParams: {
        pageNum: 1,
        pageSize: 10,
        workorderCode: null,
        workorderName: null,
        workorderType: this.workorder.workorderType,
        productCode: null,
        productName: null,
        clientName: null,
        status: "CONFIRMED",
      },
    };
  },
  methods: {
    normalizeId(id) {
      return id == null ? "" : String(id);
    },
    findSelectedRow() {
      const id = this.normalizeId(this.selectedWorkorderId);
      if (!id) {
        return null;
      }
      if (this.selectedRows && this.normalizeId(this.selectedRows.workorderId) === id) {
        return this.selectedRows;
      }
      return this.workorderList.find((r) => this.normalizeId(r.workorderId) === id) || null;
    },
    lifecycleLabel(code) {
      return LIFECYCLE_LABELS[code] || code || "—";
    },
    handleOpen(id, options = {}) {
      this.dialogTitle = options.title || "工单选择";
      this.dialogHint = options.hint || "";
      this.selectedWorkorderId = id != null ? this.normalizeId(id) : "";
      this.selectedRows = null;
      if (options.workorderCode) {
        this.queryParams.workorderCode = options.workorderCode;
      }
      this.showFlag = true;
    },
    onDialogOpened() {
      this.getList();
    },
    getList() {
      this.loading = true;
      listWorkorder(this.queryParams)
        .then((response) => {
          this.workorderList = response.rows || [];
          this.total = response.total || 0;
          if (this.selectedWorkorderId) {
            const hit = this.findSelectedRow();
            if (hit) {
              this.selectedRows = hit;
            }
          }
        })
        .finally(() => {
          this.loading = false;
        });
    },
    handleQuery() {
      this.queryParams.pageNum = 1;
      this.getList();
    },
    resetQuery() {
      this.resetForm("queryForm");
      this.queryParams.status = "CONFIRMED";
      this.handleQuery();
    },
    handleCurrent(row) {
      if (row) {
        this.selectedRows = row;
        this.selectedWorkorderId = this.normalizeId(row.workorderId);
      }
    },
    handleRowChange(row) {
      if (row) {
        this.selectedRows = row;
        this.selectedWorkorderId = this.normalizeId(row.workorderId);
      }
    },
    handleRowDbClick(row) {
      if (row) {
        this.selectedRows = row;
        this.selectedWorkorderId = this.normalizeId(row.workorderId);
        this.$emit("onSelected", this.selectedRows);
        this.showFlag = false;
      }
    },
    confirmSelect() {
      const row = this.findSelectedRow();
      if (!row) {
        this.$modal.msgWarning("请先选择一条工单");
        return;
      }
      this.$emit("onSelected", row);
      this.showFlag = false;
    },
  },
};
</script>

<style>
/* append-to-body 弹窗在 .yunshu-ui-root 外，需全局样式 */
.workorder-picker-dialog.el-dialog {
  width: min(1280px, 98vw) !important;
  max-width: 98vw;
}

.workorder-picker-dialog .picker-filter-form--single-row {
  display: grid !important;
  grid-template-columns: minmax(150px, 1.2fr) minmax(130px, 1fr) minmax(120px, 0.95fr) minmax(120px, 0.95fr) minmax(120px, 0.95fr) auto;
  gap: 10px;
  align-items: center;
  width: 100%;
}

.workorder-picker-dialog .picker-filter-form--single-row .el-form-item {
  margin-bottom: 0 !important;
  margin-right: 0 !important;
  width: 100%;
}

.workorder-picker-dialog .picker-filter-form--single-row .el-form-item__content {
  width: 100%;
}

.workorder-picker-dialog .picker-filter-form--single-row .el-input {
  width: 100% !important;
}

.workorder-picker-dialog .picker-filter-actions {
  display: flex;
  align-items: center;
  gap: 8px;
  white-space: nowrap;
}

.workorder-picker-dialog .picker-filter-actions .el-button + .el-button {
  margin-left: 0;
}
</style>
