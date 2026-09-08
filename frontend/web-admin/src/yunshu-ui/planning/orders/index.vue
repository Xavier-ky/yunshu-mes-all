<template>
  <div class="app-container orders-command-center">
    <OrderIncomingPanel :consumed-ids="consumedIncomingIds" @select="onIncomingOrderSelect" />
    <section v-show="showSearch" class="orders-filter-panel">
      <el-form ref="queryForm" :model="queryParams" :inline="true" class="orders-filter-form" @submit.prevent>
        <el-form-item label="订单号" prop="orderNo">
          <el-input v-model="queryParams.orderNo" placeholder="输入订单编号" clearable @keyup.enter="handleQuery" />
        </el-form-item>
        <el-form-item label="客户名称" prop="customerName">
          <el-input v-model="queryParams.customerName" placeholder="输入客户名称" clearable @keyup.enter="handleQuery" />
        </el-form-item>
        <el-form-item label="订单状态" prop="status">
          <el-select v-model="queryParams.status" clearable placeholder="全部状态">
            <el-option label="待确认" value="CREATED" />
            <el-option label="执行中" value="CONFIRMED" />
            <el-option label="已完成" value="COMPLETED" />
          </el-select>
        </el-form-item>
        <el-form-item label="交付日期" prop="deliveryRange">
          <el-date-picker
            v-model="queryParams.deliveryRange"
            type="daterange"
            value-format="YYYY-MM-DD"
            range-separator="至"
            start-placeholder="开始日期"
            end-placeholder="结束日期"
            placement="bottom-end"
            clearable
          />
        </el-form-item>
        <el-form-item class="filter-actions">
          <el-button type="primary" size="small" @click="handleQuery"><Search :size="14" /> 搜索</el-button>
          <el-button size="small" @click="resetQuery"><RotateCcw :size="14" /> 重置</el-button>
        </el-form-item>
      </el-form>
    </section>

    <div class="orders-toolbar">
      <div class="toolbar-left">
        <el-button type="primary" size="small" class="primary-action" @click="handleAdd"><Plus :size="15" /> 新建订单</el-button>
        <span class="toolbar-divider"></span>
        <span class="result-count">共 <b>{{ filteredOrders.length }}</b> 笔订单</span>
      </div>
      <div class="toolbar-right">
        <right-toolbar
          :show-search="showSearch"
          @update:showSearch="showSearch = $event"
          @queryTable="getList"
        />
      </div>
    </div>

    <div class="orders-table-frame">
      <span class="frame-corner frame-corner--tl" aria-hidden="true"></span>
      <span class="frame-corner frame-corner--tr" aria-hidden="true"></span>
      <span class="frame-corner frame-corner--bl" aria-hidden="true"></span>
      <span class="frame-corner frame-corner--br" aria-hidden="true"></span>
      <el-table
        v-loading="loading"
        class="yunshu-data-table orders-table"
        stripe
        border
        :data="pagedOrders"
        row-key="orderId"
      >
        <el-table-column label="订单编号" prop="orderNo" min-width="180" align="center" header-align="center" fixed="left" show-overflow-tooltip>
          <template #default="scope">
            <div class="order-code-cell"><span class="order-code-dot"></span><strong>{{ scope.row.orderNo }}</strong></div>
          </template>
        </el-table-column>
        <el-table-column label="客户名称" prop="customerName" min-width="180" align="center" header-align="center" show-overflow-tooltip />
        <el-table-column label="订单数量" prop="orderQty" min-width="120" align="center" header-align="center">
          <template #default="scope"><span class="quantity-cell">{{ formatNumber(scope.row.orderQty) }} <em>件</em></span></template>
        </el-table-column>
        <el-table-column label="交付日期" prop="deliveryDate" min-width="140" align="center" header-align="center" show-overflow-tooltip>
          <template #default="scope">
            <span :class="{ 'delivery-soon': isUrgent(scope.row) }">{{ scope.row.deliveryDate || "—" }}</span>
          </template>
        </el-table-column>
        <el-table-column label="订单状态" prop="status" min-width="120" align="center" header-align="center">
          <template #default="scope"><span class="order-status" :class="`status-${String(scope.row.status || '').toLowerCase()}`">{{ statusLabel(scope.row.status) }}</span></template>
        </el-table-column>
        <el-table-column label="操作" min-width="240" align="center" header-align="center" fixed="right" class-name="col-actions">
          <template #default="scope">
            <div class="yunshu-row-actions">
              <el-button size="small" link @click="handleUpdate(scope.row)"><Edit3 :size="14" /> 修改</el-button>
              <el-button v-if="scope.row.status === 'CREATED'" size="small" link type="primary" @click="confirmOrder(scope.row)"><CircleCheck :size="14" /> 确认订单</el-button>
              <el-button v-if="scope.row.status === 'CONFIRMED'" size="small" link type="primary" @click="createWorkOrder(scope.row)"><Factory :size="14" /> 生成工单</el-button>
            </div>
          </template>
        </el-table-column>
        <template #empty><div class="orders-empty"><div class="empty-mark"><ClipboardList :size="28" /></div><strong>暂无符合条件的订单</strong><span>调整筛选条件后再试试</span></div></template>
      </el-table>
    </div>

    <pagination
      v-show="filteredOrders.length > 0"
      :total="filteredOrders.length"
      :page="page.page"
      :limit="page.limit"
      :page-sizes="[10, 20, 50]"
      @update:page="page.page = $event"
      @update:limit="page.limit = $event"
      @pagination="handlePagination"
    />

    <el-dialog v-model="open" :title="title" width="600px" append-to-body class="orders-dialog">
      <div class="dialog-intro"><span class="dialog-intro-icon"><ClipboardList :size="18" /></span><div><strong>{{ title }}</strong><small>{{ dialogIntroText }}</small></div></div>
      <el-form ref="form" :model="form" :rules="rules" label-width="92px" class="orders-edit-form">
        <el-form-item label="订单号" prop="orderNo"><el-input v-model="form.orderNo" placeholder="请输入订单编号" /></el-form-item>
        <el-form-item label="客户名称" prop="customerName"><el-input v-model="form.customerName" placeholder="请输入客户名称" /></el-form-item>
        <el-form-item label="产品" prop="productId">
          <el-select v-model="form.productId" placeholder="请选择产品" filterable style="width: 100%">
            <el-option
              v-for="p in productOptions"
              :key="p.productId"
              :label="`${p.productCode} · ${p.productName}`"
              :value="p.productId"
            />
          </el-select>
        </el-form-item>
        <el-form-item label="订单数量" prop="orderQty"><el-input-number v-model="form.orderQty" :min="1" controls-position="right" /></el-form-item>
        <el-form-item label="交付日期" prop="deliveryDate"><el-date-picker v-model="form.deliveryDate" type="date" value-format="YYYY-MM-DD" placeholder="请选择交付日期" style="width: 100%" /></el-form-item>
      </el-form>
      <template #footer><div class="dialog-footer"><el-button @click="open = false">取消</el-button><el-button type="primary" @click="submitForm">保存订单</el-button></div></template>
    </el-dialog>
  </div>
</template>

<script>
import { CircleCheck, ClipboardList, Edit3, Factory, Plus, RotateCcw, Search } from "lucide-vue-next";
import { fetchOrders, createOrder, updateOrder } from "@/api/planning";
import { fetchProducts } from "@/api/domain";
import { filterOrders, getOrderMetrics, normalizeOrderResponse, paginateOrders } from "./model";
import {
  loadConsumedIncomingIds,
  presetToOrderForm,
  saveConsumedIncomingIds,
} from "./incoming-order-presets";
import OrderIncomingPanel from "@/yunshu-ui/components/OrderIncomingPanel.vue";

export default {
  name: "PlanningOrders",
  components: { CircleCheck, ClipboardList, Edit3, Factory, Plus, RotateCcw, Search, OrderIncomingPanel },
  data() {
    return {
      loading: false,
      showSearch: true,
      allOrders: [],
      productOptions: [],
      open: false,
      title: "",
      dialogIntroText: "填写订单基本信息，保存后可继续协同排产",
      consumedIncomingIds: loadConsumedIncomingIds(),
      form: {},
      queryParams: { orderNo: "", customerName: "", status: "", deliveryRange: [] },
      page: { page: 1, limit: 20 },
      rules: {
        orderNo: [{ required: true, message: "订单号不能为空", trigger: "blur" }],
        customerName: [{ required: true, message: "客户不能为空", trigger: "blur" }],
        productId: [{ required: true, message: "请选择产品", trigger: "change" }],
        orderQty: [{ required: true, message: "数量不能为空", trigger: "blur" }],
        deliveryDate: [{ required: true, message: "请选择交付日期", trigger: "change" }],
      },
    };
  },
  computed: {
    filteredOrders() {
      return filterOrders(this.allOrders, this.queryParams);
    },
    pagedOrders() {
      return paginateOrders(this.filteredOrders, this.page.page, this.page.limit).rows;
    },
  },
  watch: {
    filteredOrders() {
      const maxPage = Math.max(1, Math.ceil(this.filteredOrders.length / this.page.limit));
      if (this.page.page > maxPage) this.page.page = maxPage;
    },
  },
  created() {
    this.loadProducts();
    this.getList();
  },
  methods: {
    loadProducts() {
      fetchProducts()
        .then((response) => {
          this.productOptions = Array.isArray(response?.data) ? response.data : [];
          if (this.open && !this.form.orderId && !this.form.productId && this.productOptions.length) {
            this.form.productId = this.productOptions[0].productId;
          }
        })
        .catch(() => {
          this.productOptions = [];
        });
    },
    buildOrderPayload(form) {
      return {
        orderNo: form.orderNo,
        customerName: form.customerName,
        productId: form.productId,
        orderQty: form.orderQty,
        deliveryDate: form.deliveryDate,
        status: form.status || "CREATED",
      };
    },
    getList() {
      this.loading = true;
      fetchOrders().then((response) => {
        this.allOrders = normalizeOrderResponse(response);
        this.page.page = 1;
      }).catch(() => {
        this.allOrders = [];
      }).finally(() => {
        this.loading = false;
      });
    },
    handleQuery() {
      this.page.page = 1;
    },
    resetQuery() {
      this.queryParams = { orderNo: "", customerName: "", status: "", deliveryRange: [] };
      this.page.page = 1;
    },
    handlePagination({ page, limit }) {
      this.page.page = page;
      this.page.limit = limit;
    },
    handleAdd() {
      const defaultDelivery = new Date();
      defaultDelivery.setDate(defaultDelivery.getDate() + 14);
      this.form = {
        orderQty: 1,
        status: "CREATED",
        deliveryDate: defaultDelivery.toISOString().slice(0, 10),
        productId: this.productOptions[0]?.productId ?? null,
      };
      this.dialogIntroText = "填写订单基本信息，保存后可继续协同排产";
      this.open = true;
      this.title = "新建订单";
    },
    onIncomingOrderSelect(preset) {
      this.form = presetToOrderForm(preset, this.productOptions);
      if (!this.form.productId) {
        this.$modal.msgWarning(`未找到产品「${preset.productCode}」，请在表单中手动选择`);
      }
      this.dialogIntroText = `已从待接订单带入：${preset.customerName} · ${preset.productLabel} × ${preset.orderQty} 台 · 交付 ${preset.deliveryDate}`;
      this.open = true;
      this.title = `新建订单 · ${preset.customerName}`;
      this.$nextTick(() => {
        this.$refs.form?.clearValidate?.();
      });
    },
    markIncomingPresetConsumed(presetId) {
      if (!presetId || this.consumedIncomingIds.includes(presetId)) return;
      this.consumedIncomingIds = [...this.consumedIncomingIds, presetId];
      saveConsumedIncomingIds(this.consumedIncomingIds);
    },
    handleUpdate(row) {
      this.form = { ...row };
      this.open = true;
      this.title = "修改订单";
    },
    submitForm() {
      this.$refs.form.validate((valid) => {
        if (!valid) return;
        const payload = this.buildOrderPayload(this.form);
        const action = this.form.orderId
          ? updateOrder(this.form.orderId, payload)
          : createOrder(payload);
        action
          .then(() => {
            if (this.form._incomingPresetId) {
              this.markIncomingPresetConsumed(this.form._incomingPresetId);
            }
            this.$modal.msgSuccess("订单保存成功");
            this.open = false;
            this.getList();
          })
          .catch((err) => {
            this.$modal.msgError(err?.message || "订单保存失败");
          });
      });
    },
    confirmOrder(row) {
      const payload = this.buildOrderPayload({ ...row, status: "CONFIRMED" });
      updateOrder(row.orderId, payload)
        .then(() => {
          this.$modal.msgSuccess("订单已确认");
          this.getList();
        })
        .catch((err) => {
          this.$modal.msgError(err?.message || "订单确认失败");
        });
    },
    createWorkOrder(row) {
      this.$router.push({
        path: "/app/planning/work-orders",
        query: { orderId: String(row.orderId), orderNo: row.orderNo },
      });
    },
    statusLabel(status) {
      return { CREATED: "待确认", CONFIRMED: "执行中", COMPLETED: "已完成" }[status] || status || "未知";
    },
    formatNumber(value) {
      return Number(value || 0).toLocaleString("zh-CN");
    },
    isUrgent(row) {
      return getOrderMetrics([row]).urgent > 0;
    },
  },
};
</script>
