<template>
  <PlanningPageShell
    title="订单中心"
    description="客户订单接入、明细管理与工单转化"
    theme="orders"
    :refreshed-at="refreshedAt"
    :breadcrumbs="planBreadcrumbs"
    :chips="heroChips"
    :planning-nav="{ current: 'order', query: stepperQuery }"
    agent-context="new-orders"
  >
    <template #kpi>
      <PlanningKpiBento :layout="kpiLayout" accent-color="#0052d9" />
    </template>

    <ListDetailLayout title="" description="">
      <template #toolbar>
        <div class="plan-search">
          <svg width="16" height="16" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2">
            <circle cx="11" cy="11" r="8" /><line x1="21" y1="21" x2="16.65" y2="16.65" />
          </svg>
          <input v-model="kw" placeholder="搜索订单号或客户..." @input="applyFilter" />
        </div>
        <select v-model="fs" class="plan-select" @change="applyFilter">
          <option value="">全部状态</option>
          <option value="CREATED">已创建</option>
          <option value="CONFIRMED">已确认</option>
          <option value="COMPLETED">已完成</option>
        </select>
        <button type="button" class="plan-btn primary" @click="openCreate">+ 新建订单</button>
      </template>

      <template #list>
        <div v-if="!filtered.length" class="list-empty">暂无订单</div>
        <PlanningListCard
          v-for="o in filtered"
          :key="o.orderId"
          :title="o.orderNo"
          :subtitle="o.customerName"
          :active="selectedId === o.orderId"
          :border-class="listBorderClass(o)"
          :chips="orderChips(o)"
          @click="selectItem(o)"
        >
          <template #badge><MesStatusBadge :value="o.status" /></template>
          <template #meta>交期 {{ o.deliveryDate || "—" }} · {{ o.orderQty }} 台</template>
          <template #footer>
            <MesProgressCell :completed="woQtyForOrder(o.orderId)" :total="Number(o.orderQty) || 0" />
          </template>
        </PlanningListCard>
      </template>

      <template #detail>
        <div v-if="!selected" class="detail-empty"><p>请选择订单或新建</p></div>
        <template v-else>
          <header class="detail-head">
            <div>
              <h3>{{ isCreating ? "新建订单" : selected.orderNo }}</h3>
              <p v-if="!isCreating">{{ selected.customerName }}</p>
            </div>
            <div class="detail-actions">
              <label v-if="!isCreating" class="edit-toggle">
                <span>编辑</span>
                <input v-model="editMode" type="checkbox" />
              </label>
              <button
                v-if="!isCreating && selected.status === 'CONFIRMED'"
                type="button"
                class="plan-btn primary"
                @click="createWoFromOrder"
              >
                生成工单
              </button>
              <button
                v-if="!isCreating && selected.status === 'CREATED'"
                type="button"
                class="plan-btn"
                @click="confirmOrder"
              >
                确认订单
              </button>
            </div>
          </header>

          <DetailTabs v-model="activeTab" :tabs="detailTabs" />

          <div v-show="activeTab === 'basic'" class="tab-panel">
            <div class="field-grid">
              <label class="field"><span>订单号 *</span><input v-model="form.orderNo" :readonly="!canEdit" /></label>
              <label class="field"><span>客户 *</span><input v-model="form.customerName" :readonly="!canEdit" /></label>
              <label class="field">
                <span>产品</span>
                <select v-model="form.productId" :disabled="!canEdit">
                  <option :value="null">请选择</option>
                  <option v-for="p in products" :key="p.productId" :value="p.productId">{{ p.productName }}</option>
                </select>
              </label>
              <label class="field"><span>数量 *</span><input v-model="form.orderQty" type="number" :readonly="!canEdit" /></label>
              <label class="field"><span>交期</span><input v-model="form.deliveryDate" :readonly="!canEdit" /></label>
              <label class="field">
                <span>状态</span>
                <select v-model="form.status" :disabled="!canEdit">
                  <option value="CREATED">已创建</option>
                  <option value="CONFIRMED">已确认</option>
                  <option value="COMPLETED">已完成</option>
                </select>
              </label>
            </div>
            <footer v-if="canEdit" class="detail-foot">
              <button type="button" class="plan-btn" @click="cancelEdit">取消</button>
              <button type="button" class="plan-btn primary" :disabled="saving" @click="submit">
                {{ saving ? "保存中…" : "保存" }}
              </button>
            </footer>
          </div>

          <div v-show="activeTab === 'items'" class="tab-panel">
            <MesDataTable
              :columns="itemColumns"
              :rows="orderItems"
              row-key-field="orderItemId"
            />
            <p v-if="!orderItems.length" class="hint">订单明细将随创建时产品行写入；编辑时可更新主产品数量。</p>
          </div>

          <div v-show="activeTab === 'workorders'" class="tab-panel">
            <MesDataTable
              :columns="woColumns"
              :rows="linkedWorkOrders"
              row-key-field="workOrderId"
              @row-click="goWorkOrder"
            />
          </div>
        </template>
      </template>
    </ListDetailLayout>

    <PlanningInsightPanel :panels="insightPanels" />
  </PlanningPageShell>
</template>

<script setup>
import { computed, onMounted, reactive, ref, watch } from "vue";
import { useRouter } from "vue-router";
import PlanningPageShell from "@/components/planning/PlanningPageShell.vue";
import PlanningKpiBento from "@/components/planning/PlanningKpiBento.vue";
import PlanningListCard from "@/components/planning/PlanningListCard.vue";
import PlanningInsightPanel from "@/components/planning/PlanningInsightPanel.vue";
import DetailTabs from "@/components/planning/DetailTabs.vue";
import ListDetailLayout from "@/components/ListDetailLayout.vue";
import MesDataTable from "@/components/MesDataTable.vue";
import MesStatusBadge from "@/components/MesStatusBadge.vue";
import MesProgressCell from "@/components/MesProgressCell.vue";
import {
  createOrder,
  createWorkOrder,
  fetchOrderItems,
  fetchOrders,
  fetchPlanningSummary,
  fetchWorkOrders,
  updateOrder,
} from "@/api/planning";
import { request } from "@/api/request";

const router = useRouter();
const orders = ref([]);
const products = ref([]);
const allWorkOrders = ref([]);
const kw = ref("");
const fs = ref("");
const selectedId = ref(null);
const isCreating = ref(false);
const editMode = ref(false);
const saving = ref(false);
const activeTab = ref("basic");
const refreshedAt = ref("");
const summary = ref(null);
const orderItems = ref([]);

const planBreadcrumbs = [
  { label: "计划调度", to: "/app/planning/orders" },
  { label: "订单中心" },
];

const heroChips = computed(() => {
  const overdue = summary.value?.orders?.overdue ?? orders.value.filter((o) => isOverdue(o)).length;
  if (!overdue) return [];
  return [{ label: `交期预警 ${overdue}`, tone: "alert" }];
});

const detailTabs = [
  { key: "basic", label: "基本信息" },
  { key: "items", label: "订单明细" },
  { key: "workorders", label: "关联工单" },
];

const form = reactive({
  orderId: null,
  orderNo: "",
  customerName: "",
  productId: null,
  orderQty: "",
  deliveryDate: "",
  status: "CREATED",
});

const itemColumns = [
  { key: "productCode", label: "产品编码", type: "code" },
  { key: "productName", label: "产品名称" },
  { key: "orderQty", label: "数量", align: "right", type: "number" },
  { key: "technicalRequirement", label: "技术要求" },
];

const woColumns = [
  { key: "workOrderNo", label: "工单号", type: "code" },
  { key: "productName", label: "产品" },
  { key: "planQty", label: "计划", align: "right", type: "number" },
  { key: "completedQty", label: "完成", align: "right", type: "number" },
  { key: "status", label: "状态", type: "status" },
];

const filtered = computed(() => {
  let list = [...orders.value];
  if (kw.value) {
    const k = kw.value.toLowerCase();
    list = list.filter(
      (o) => (o.orderNo || "").toLowerCase().includes(k) || (o.customerName || "").toLowerCase().includes(k)
    );
  }
  if (fs.value) list = list.filter((o) => o.status === fs.value);
  return list.sort((a, b) => String(b.orderNo || "").localeCompare(String(a.orderNo || "")));
});

const selected = computed(() => {
  if (isCreating.value) return { orderNo: "新建", customerName: "" };
  return orders.value.find((o) => o.orderId === selectedId.value) || null;
});

const canEdit = computed(() => isCreating.value || editMode.value);

const kpiLayout = computed(() => {
  const s = summary.value?.orders;
  const statusMap = { CREATED: "待确认", CONFIRMED: "已确认", COMPLETED: "已完成", CANCELLED: "已取消" };
  const statusColors = { CREATED: "#0052d9", CONFIRMED: "#0abf5b", COMPLETED: "#ff9d00", CANCELLED: "#e34d59" };

  const dist = s?.statusDistribution || [];
  let segments = dist
    .filter((d) => ["CREATED", "CONFIRMED", "COMPLETED"].includes(d.status))
    .map((d) => ({
      name: statusMap[d.status] || d.status,
      value: Number(d.count) || 0,
      itemStyle: { color: statusColors[d.status] },
    }));
  if (!segments.length) {
    ["CREATED", "CONFIRMED", "COMPLETED"].forEach((st) => {
      const cnt = orders.value.filter((o) => o.status === st).length;
      if (cnt) segments.push({ name: statusMap[st], value: cnt, itemStyle: { color: statusColors[st] } });
    });
  }
  if (!segments.length) segments = [{ name: "暂无", value: 1 }];

  const total = s?.total ?? orders.value.length;
  const pending = s?.pendingConfirm ?? orders.value.filter((o) => o.status === "CREATED").length;
  const weekDue = orders.value.filter((o) => daysToDelivery(o) <= 7 && daysToDelivery(o) >= 0).length;
  const totalQty = orders.value.reduce((sum, o) => sum + Number(o.orderQty || 0), 0);
  const overdue = s?.overdue ?? orders.value.filter((o) => isOverdue(o)).length;

  const weekData = s?.deliveryByWeek || [];
  const barLabels = weekData.length ? weekData.map((w) => w.week_key) : ["W-4", "W-3", "W-2", "W-1"];
  const barValues = weekData.length ? weekData.map((w) => Number(w.cnt) || 0) : [2, 3, 1, 4];

  return [
    {
      type: "hero-donut",
      key: "hero",
      span: 5,
      label: "订单总数",
      value: total,
      segments,
      colors: ["#0052d9", "#0abf5b", "#ff9d00"],
    },
    {
      type: "mini-bars",
      key: "weeks",
      span: 7,
      title: "近4周交期",
      labels: barLabels,
      values: barValues,
      horizontal: false,
    },
    {
      type: "connected",
      key: "strip",
      span: 12,
      items: [
        { key: "pending", label: "待确认", value: pending, tone: pending ? "warn" : "" },
        { key: "week", label: "本周交期", value: weekDue, tone: weekDue ? "warn" : "" },
        { key: "qty", label: "总订货量", value: totalQty, unit: "台" },
        { key: "overdue", label: "交期预警", value: overdue, tone: overdue ? "alert" : "" },
      ],
    },
  ];
});

const insightPanels = computed(() => {
  const dist = summary.value?.orders?.statusDistribution || [];
  const statusMap = { CREATED: "待确认", CONFIRMED: "已确认", COMPLETED: "已完成", CANCELLED: "已取消" };
  const pieData = dist.map((d) => ({ name: statusMap[d.status] || d.status, value: Number(d.count) || 0 }));
  if (!pieData.length) {
    ["CREATED", "CONFIRMED", "COMPLETED"].forEach((st) => {
      const cnt = orders.value.filter((o) => o.status === st).length;
      if (cnt) pieData.push({ name: statusMap[st], value: cnt });
    });
  }
  const weekData = summary.value?.orders?.deliveryByWeek || [];
  const barLabels = weekData.length ? weekData.map((w) => w.week_key) : ["W-4", "W-3", "W-2", "W-1"];
  const barValues = weekData.length ? weekData.map((w) => Number(w.cnt) || 0) : [2, 3, 1, 4];
  return [
    {
      title: "订单状态分布",
      type: "chart",
      option: {
        color: ["#0052d9", "#0abf5b", "#ff9d00", "#e34d59"],
        tooltip: { trigger: "item" },
        series: [{ type: "pie", radius: ["42%", "68%"], data: pieData.length ? pieData : [{ name: "暂无", value: 1 }] }],
      },
    },
    {
      title: "近4周交期订单",
      type: "chart",
      option: {
        color: ["#0052d9"],
        grid: { left: 40, right: 16, top: 16, bottom: 28 },
        xAxis: { type: "category", data: barLabels },
        yAxis: { type: "value", minInterval: 1 },
        series: [{ type: "bar", data: barValues, barWidth: 24 }],
      },
    },
  ];
});

const stepperQuery = computed(() => (selectedId.value ? { orderId: selectedId.value } : {}));

const linkedWorkOrders = computed(() => {
  if (!selectedId.value || isCreating.value) return [];
  return allWorkOrders.value.filter((w) => w.orderId === selectedId.value);
});

onMounted(loadAll);

async function loadAll() {
  refreshedAt.value = new Date().toLocaleTimeString("zh-CN", { hour: "2-digit", minute: "2-digit", second: "2-digit" });
  try {
    orders.value = (await fetchOrders())?.data || [];
  } catch {
    orders.value = [];
  }
  try {
    products.value = (await request.get("/master-data/products"))?.data || [];
  } catch {
    products.value = [];
  }
  try {
    allWorkOrders.value = (await fetchWorkOrders())?.data || [];
  } catch {
    allWorkOrders.value = [];
  }
  try {
    summary.value = (await fetchPlanningSummary())?.data || null;
  } catch {
    summary.value = null;
  }
  if (!selectedId.value && orders.value.length && !isCreating.value) selectItem(orders.value[0]);
}

async function loadOrderItems() {
  if (!selectedId.value || isCreating.value) {
    orderItems.value = [];
    return;
  }
  try {
    orderItems.value = (await fetchOrderItems(selectedId.value))?.data || [];
  } catch {
    orderItems.value = [];
  }
}

function daysToDelivery(o) {
  if (!o.deliveryDate) return 999;
  const d = new Date(o.deliveryDate);
  const today = new Date();
  today.setHours(0, 0, 0, 0);
  return Math.ceil((d - today) / 86400000);
}

function orderChips(o) {
  const chips = [];
  const days = daysToDelivery(o);
  if (isOverdue(o)) chips.push({ text: "已超期", tone: "red" });
  else if (days <= 7 && days >= 0) chips.push({ text: `剩${days}天`, tone: "orange" });
  return chips;
}

function listBorderClass(o) {
  if (isOverdue(o)) return "alert-border";
  if (daysToDelivery(o) <= 7) return "warn-border";
  return "";
}

function woQtyForOrder(orderId) {
  return allWorkOrders.value.filter((w) => w.orderId === orderId).reduce((s, w) => s + Number(w.planQty || 0), 0);
}

function isOverdue(o) {
  if (!o.deliveryDate || o.status === "COMPLETED") return false;
  return new Date(o.deliveryDate) < new Date() && o.status !== "CANCELLED";
}

function applyFilter() {
  if (selectedId.value && !filtered.value.some((o) => o.orderId === selectedId.value)) {
    selectedId.value = filtered.value[0]?.orderId ?? null;
    isCreating.value = false;
    editMode.value = false;
    if (selected.value && !isCreating.value) fillForm(selected.value);
  }
}

function fillForm(o) {
  Object.assign(form, {
    orderId: o.orderId,
    orderNo: o.orderNo || "",
    customerName: o.customerName || "",
    productId: null,
    orderQty: o.orderQty ?? "",
    deliveryDate: o.deliveryDate || "",
    status: o.status || "CREATED",
  });
}

function selectItem(o) {
  isCreating.value = false;
  editMode.value = false;
  selectedId.value = o.orderId;
  fillForm(o);
  loadOrderItems();
}

function openCreate() {
  isCreating.value = true;
  editMode.value = true;
  selectedId.value = null;
  Object.assign(form, {
    orderId: null,
    orderNo: "",
    customerName: "",
    productId: null,
    orderQty: "",
    deliveryDate: "",
    status: "CREATED",
  });
}

function cancelEdit() {
  if (isCreating.value) {
    isCreating.value = false;
    editMode.value = false;
    if (orders.value.length) selectItem(orders.value[0]);
    return;
  }
  editMode.value = false;
  if (selected.value) fillForm(selected.value);
}

async function submit() {
  if (!form.orderNo?.trim() || !form.customerName?.trim()) {
    alert("请填写订单号和客户");
    return;
  }
  const body = {
    orderNo: form.orderNo.trim(),
    customerName: form.customerName.trim(),
    productId: form.productId ? Number(form.productId) : null,
    orderQty: Number(form.orderQty) || 0,
    deliveryDate: form.deliveryDate,
    status: form.status,
  };
  saving.value = true;
  try {
    if (isCreating.value) {
      await createOrder(body);
    } else {
      await updateOrder(form.orderId, body);
    }
    await loadAll();
    isCreating.value = false;
    editMode.value = false;
  } catch (e) {
    alert(e?.message || "保存失败");
  } finally {
    saving.value = false;
  }
}

async function confirmOrder() {
  if (!selectedId.value) return;
  try {
    await updateOrder(selectedId.value, {
      ...form,
      orderNo: selected.value.orderNo,
      customerName: selected.value.customerName,
      orderQty: selected.value.orderQty,
      deliveryDate: selected.value.deliveryDate,
      status: "CONFIRMED",
    });
    await loadAll();
  } catch (e) {
    alert(e?.message || "确认失败");
  }
}

async function createWoFromOrder() {
  if (!selected.value) return;
  const woNo = `WO-${selected.value.orderNo.replace("CO-", "")}`;
  const item = orderItems.value[0];
  try {
    const product = products.value.find((p) => p.productName === selected.value.productName);
    await createWorkOrder({
      workOrderNo: woNo,
      productId: product?.productId || form.productId,
      orderId: selectedId.value,
      orderItemId: item?.orderItemId || null,
      planQty: Number(selected.value.orderQty) || 0,
      status: "CREATED",
    });
    await loadAll();
    const wo = allWorkOrders.value.find((w) => w.workOrderNo === woNo);
    router.push({
      path: "/app/planning/work-orders",
      query: { orderId: selectedId.value, wo: wo?.workOrderId || woNo },
    });
  } catch (e) {
    alert(e?.message || "生成工单失败");
  }
}

function goWorkOrder(row) {
  router.push({
    path: "/app/planning/work-orders",
    query: { orderId: selectedId.value, wo: row.workOrderId },
  });
}

watch(selectedId, (id) => {
  if (!id) return;
  const o = orders.value.find((x) => x.orderId === id);
  if (o && !editMode.value && !isCreating.value) fillForm(o);
  loadOrderItems();
});
</script>

<style scoped>
.list-empty,
.detail-empty {
  padding: 48px 20px;
  text-align: center;
  color: #9ca3af;
}
.list-item {
  width: 100%;
  text-align: left;
  border: none;
  border-bottom: 1px solid #f0f0f0;
  background: transparent;
  padding: 14px 16px;
  cursor: pointer;
}
.list-item:hover {
  background: #f5f7fa;
}
.list-item.active {
  background: rgba(0, 82, 217, 0.06);
  box-shadow: inset 3px 0 0 #0052d9;
}
.list-item.overdue .li-meta {
  color: #e34d59;
}
.li-top {
  display: flex;
  justify-content: space-between;
  align-items: center;
  gap: 8px;
}
.li-no {
  font-family: "JetBrains Mono", monospace;
  font-size: 12px;
  font-weight: 600;
}
.li-sub {
  margin-top: 6px;
  font-size: 13px;
  font-weight: 500;
}
.li-meta {
  margin-top: 4px;
  font-size: 11px;
  color: #888;
}
.detail-head {
  display: flex;
  justify-content: space-between;
  gap: 12px;
  padding: 16px 20px 0;
  flex-wrap: wrap;
}
.detail-head h3 {
  margin: 0;
  font-size: 18px;
}
.detail-head p {
  margin: 4px 0 0;
  font-size: 13px;
  color: #888;
}
.detail-actions {
  display: flex;
  align-items: center;
  gap: 8px;
  flex-wrap: wrap;
}
.edit-toggle {
  display: inline-flex;
  align-items: center;
  gap: 6px;
  font-size: 13px;
  cursor: pointer;
}
.tab-panel {
  padding: 16px 20px;
}
.field-grid {
  display: grid;
  grid-template-columns: 1fr 1fr;
  gap: 14px;
}
.field {
  display: grid;
  gap: 6px;
}
.field span {
  font-size: 12px;
  color: #888;
}
.field input,
.field select {
  height: 38px;
  padding: 0 12px;
  border: 1px solid #dcdcdc;
  font-size: 13px;
  background: #fafafa;
  color: #1a1a1a;
}
.field input:read-only {
  background: #f3f4f6;
}
.field select option {
  color: #1a1a1a;
  background: #fff;
}
.detail-foot {
  display: flex;
  justify-content: flex-end;
  gap: 8px;
  margin-top: 16px;
  padding-top: 16px;
  border-top: 1px solid #eee;
}
.hint {
  font-size: 12px;
  color: #999;
  margin-top: 12px;
}
@media (max-width: 960px) {
  .field-grid {
    grid-template-columns: 1fr;
  }
}
</style>
