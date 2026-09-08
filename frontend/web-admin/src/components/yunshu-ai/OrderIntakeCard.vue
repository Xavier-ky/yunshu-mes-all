<template>
  <section class="order-intake-card" aria-label="订单接收 Agent 表单">
    <header class="card-head">
      <div>
        <span class="eyebrow">ORDER INTAKE AGENT</span>
        <h4>确认待排产订单</h4>
      </div>
      <span class="step-tag">排产 1 / 7</span>
    </header>

    <div v-if="loadingProducts" class="card-loading">正在从订单中心读取最新订单…</div>
    <p v-else-if="loadError" class="card-error">{{ loadError }}</p>

    <form v-else class="order-form" @submit.prevent="submit">
      <p class="source-note">已自动回填订单中心最新一条真实订单；确认后将直接进入原有智能排产流程。</p>
      <label>
        <span>订单号 <b>*</b></span>
        <input v-model.trim="form.order_no" maxlength="64" :readonly="prefilled" :disabled="completed || submitting" required />
      </label>
      <label>
        <span>客户名称 <b>*</b></span>
        <input v-model.trim="form.customer_name" maxlength="100" :readonly="prefilled" :disabled="completed || submitting" required />
      </label>
      <label>
        <span>产品 <b>*</b></span>
        <select v-model.number="form.product_id" :disabled="prefilled || completed || submitting" required>
          <option :value="null" disabled>请选择产品</option>
          <option v-for="product in products" :key="product.product_id" :value="product.product_id">
            {{ product.product_code ? `${product.product_code} · ` : "" }}{{ product.product_name }}
          </option>
        </select>
      </label>
      <label>
        <span>订单数量 <b>*</b></span>
        <input v-model.number="form.order_qty" type="number" min="1" step="1" :readonly="prefilled" :disabled="completed || submitting" required />
      </label>
      <label class="full-width">
        <span>交付日期 <b>*</b></span>
        <div class="date-picker-field">
          <input v-model="form.delivery_date" type="date" :min="today" :readonly="prefilled" :disabled="prefilled || completed || submitting" required title="订单中心交付日期" @click="openDatePicker" />
          <span class="date-picker-hint" aria-hidden="true">⌄</span>
        </div>
      </label>

      <p v-if="submitError" class="card-error">{{ submitError }}</p>
      <section v-if="submitting" class="execution-wait" role="status" aria-live="polite">
        <span class="execution-orb" aria-hidden="true"><i></i><b></b></span>
        <div class="execution-copy">
          <strong>智能排产正在执行</strong>
          <span>{{ activeStage.label }}</span>
        </div>
        <ol class="execution-stages" aria-label="智能排产执行阶段">
          <li v-for="(stage, index) in executionStages" :key="stage.label" :class="{ active: index === activeStageIndex, passed: index < activeStageIndex }">
            <i aria-hidden="true"></i><span>{{ stage.short }}</span>
          </li>
        </ol>
      </section>
      <div v-if="completed" class="success-note">
        <strong>订单 {{ completed.order.order_no }} 已自动确认，并已生成工单 {{ completed.work_order.work_order_no }}。</strong>
        <span>已完成 BOM 与工艺路线、齐套与物料风险分析：{{ completed.analysis.summary.recommendation }}</span>
        <span v-if="completed.scheduling_execution">已自动排产 {{ completed.scheduling_execution.productionTaskCount }} 道任务，并同步 {{ completed.scheduling_execution.dispatchTaskCount }} 条派工。</span>
        <span v-if="completed.production_issue_execution?.readyForShopFloor">已完成生产领料、拣货出库和物料消耗追溯，工单已准备好进入现场作业。</span>
        <span v-else-if="completed.scheduling_execution" class="issue-pending">排产已完成，当前未能自动领料：{{ completed.production_issue_execution?.error || '请检查实际库存与领料单状态。' }}</span>
      </div>
      <footer v-else>
        <span>确认后会确认此订单、生成工单，并继续后续 BOM、齐套、排产与领料流程</span>
        <button type="submit" :disabled="submitting || !ready">
          {{ submitting ? "正在启动…" : "确认并启动智能排产" }}
        </button>
      </footer>
    </form>
  </section>
</template>

<script setup>
import { computed, onBeforeUnmount, onMounted, reactive, ref } from "vue";
import { createOrderFromIntake, fetchLatestOrderForIntake, fetchOrderIntakeProducts } from "@/api/yunshuAgent";

const emit = defineEmits(["created", "execution-state"]);
const props = defineProps({
  sessionId: { type: String, default: "" },
  modelKey: { type: String, default: "deepseek" },
});
const products = ref([]);
const loadingProducts = ref(true);
const loadError = ref("");
const submitError = ref("");
const submitting = ref(false);
const completed = ref(null);
const prefilled = ref(false);
const activeStageIndex = ref(0);
const executionStartedAt = ref(0);
let stageTimer = 0;
const form = reactive({ source_order_id: null, order_no: "", customer_name: "", product_id: null, order_qty: null, delivery_date: "" });
const today = new Date().toISOString().slice(0, 10);
const ready = computed(() => form.order_no && form.customer_name && form.product_id && Number(form.order_qty) > 0 && form.delivery_date);
const executionStages = [
  { after: 0, short: "订单", label: "正在确认订单并生成工单…" },
  { after: 2, short: "BOM", label: "正在读取 BOM、工艺与齐套数据…" },
  { after: 5, short: "排产", label: "正在执行甘特排产、派工与领料流程…" },
  { after: 9, short: "总结", label: "正在生成排产总结与风险建议…" },
];
const activeStage = computed(() => executionStages[activeStageIndex.value]);

function openDatePicker(event) {
  // Native date pickers are accessible and prevent manually-entered date
  // formats from varying by browser. `showPicker` is supported by Chromium;
  // other browsers keep their standard date control behaviour.
  event.currentTarget?.showPicker?.();
}

function updateExecutionStage() {
  const elapsedSeconds = (Date.now() - executionStartedAt.value) / 1000;
  const nextIndex = executionStages.reduce(
    (index, stage, current) => (elapsedSeconds >= stage.after ? current : index),
    0,
  );
  if (nextIndex !== activeStageIndex.value) {
    activeStageIndex.value = nextIndex;
    emit("execution-state", { active: true, label: executionStages[nextIndex].label });
  }
}

function startExecutionFeedback() {
  activeStageIndex.value = 0;
  executionStartedAt.value = Date.now();
  emit("execution-state", { active: true, label: executionStages[0].label });
  window.clearInterval(stageTimer);
  stageTimer = window.setInterval(updateExecutionStage, 500);
}

function stopExecutionFeedback() {
  window.clearInterval(stageTimer);
  stageTimer = 0;
}

async function loadOrderForConfirmation() {
  loadingProducts.value = true;
  loadError.value = "";
  try {
    const [productResponse, latestOrderResponse] = await Promise.all([
      fetchOrderIntakeProducts(),
      fetchLatestOrderForIntake(),
    ]);
    products.value = productResponse.products || [];
    if (!products.value.length) loadError.value = "当前没有可选择的产品，请先维护产品主数据。";
    const order = latestOrderResponse.order;
    if (!order) throw new Error("订单中心当前没有可用于智能排产的订单");
    Object.assign(form, {
      source_order_id: order.order_id,
      order_no: order.order_no,
      customer_name: order.customer_name,
      product_id: order.product_id,
      order_qty: order.order_qty,
      delivery_date: order.delivery_date,
    });
    prefilled.value = true;
  } catch (error) {
    loadError.value = error.message || "订单中心数据读取失败，请稍后重试。";
  } finally {
    loadingProducts.value = false;
  }
}

async function submit() {
  if (!ready.value || submitting.value || completed.value) return;
  submitting.value = true;
  submitError.value = "";
  startExecutionFeedback();
  try {
    const response = await createOrderFromIntake({
      ...form,
      session_id: props.sessionId || null,
      model: props.modelKey || null,
      confirmed: true,
    });
    completed.value = response;
    emit("created", response);
  } catch (error) {
    submitError.value = error.message || "智能排产启动失败，请稍后重试。";
    emit("execution-state", { active: false });
  } finally {
    stopExecutionFeedback();
    submitting.value = false;
  }
}

onMounted(loadOrderForConfirmation);
onBeforeUnmount(stopExecutionFeedback);
</script>

<style scoped>
.order-intake-card { margin: 14px 0 4px; border: 1px solid rgba(122, 151, 210, .29); border-radius: 16px; overflow: hidden; background: linear-gradient(145deg, rgba(246,249,255,.96), rgba(255,255,255,.9)); box-shadow: 0 10px 26px rgba(67, 94, 157, .08); }
.card-head { display:flex; align-items:center; justify-content:space-between; gap:12px; padding:15px 17px 13px; border-bottom:1px solid rgba(182,198,231,.42); }
.eyebrow { display:block; color:#8294bf; font-size:10px; font-weight:800; letter-spacing:.12em; }
h4 { margin:4px 0 0; color:#385684; font-size:17px; }
.step-tag { padding:4px 8px; border-radius:999px; color:#687db1; background:#e8efff; font-size:11px; font-weight:700; white-space:nowrap; }
.order-form { display:grid; grid-template-columns:repeat(2,minmax(0,1fr)); gap:12px; padding:15px 17px 16px; }
label { display:grid; gap:6px; color:#62718d; font-size:12px; font-weight:700; }
label b { color:#d86a75; }
input, select { width:100%; min-width:0; box-sizing:border-box; padding:10px 11px; border:1px solid #d7e0f2; border-radius:10px; outline:none; background:rgba(255,255,255,.9); color:#304260; font:inherit; font-size:13px; transition:border-color .16s, box-shadow .16s; }
.date-picker-field { position:relative; }
.date-picker-field input { padding-right:36px; cursor:pointer; }
.date-picker-hint { position:absolute; right:12px; top:50%; color:#7187b8; font-size:18px; font-weight:800; line-height:1; pointer-events:none; transform:translateY(-54%) rotate(0deg); }
.date-picker-field:focus-within .date-picker-hint { color:#5277c5; }
input:focus, select:focus { border-color:#8ba7df; box-shadow:0 0 0 3px rgba(128,156,220,.14); }
input:disabled, select:disabled { color:#8090aa; background:#f2f5fb; }
.full-width, footer, .card-error, .success-note, .source-note, .execution-wait { grid-column:1 / -1; }
.source-note { margin:0; padding:9px 11px; border:1px solid rgba(116,146,206,.2); border-radius:10px; color:#6980a9; background:rgba(233,240,255,.58); font-size:12px; line-height:1.55; }
.execution-wait { display:grid; grid-template-columns:auto minmax(0,1fr); gap:9px 10px; margin:0; padding:12px; border:1px solid rgba(113,145,212,.26); border-radius:12px; color:#6079a8; background:linear-gradient(120deg,rgba(238,244,255,.9),rgba(250,248,255,.86)); box-shadow:inset 0 1px rgba(255,255,255,.8); }
.execution-orb { position:relative; display:block; width:28px; height:28px; overflow:hidden; border-radius:50%; background:radial-gradient(circle at 34% 27%,#eefbff 0 7%,#a8d7fa 34%,#8e93e4 76%); box-shadow:0 4px 11px rgba(103,132,205,.26); }
.execution-orb::after { content:""; position:absolute; inset:-3px; border:1px solid rgba(115,154,221,.42); border-radius:inherit; animation:execution-pulse 1.6s ease-out infinite; }
.execution-orb i, .execution-orb b { position:absolute; z-index:1; top:10px; width:3px; height:6px; border-radius:50%; background:#45618f; transform-origin:center; animation:execution-blink 2.8s ease-in-out infinite; }
.execution-orb i { left:8px; }.execution-orb b { right:8px; }
.execution-copy { display:grid; gap:2px; min-width:0; line-height:1.45; }.execution-copy strong { color:#4e6fa9; font-size:13px; }.execution-copy span { overflow:hidden; color:#7d8fae; font-size:11px; text-overflow:ellipsis; white-space:nowrap; }
.execution-stages { display:flex; grid-column:1 / -1; align-items:center; gap:0; margin:0; padding:0; list-style:none; }.execution-stages li { display:flex; flex:1; align-items:center; gap:5px; min-width:0; color:#afbbcf; font-size:10px; transition:color .25s ease; }.execution-stages li:not(:last-child)::after { content:""; flex:1; height:1px; margin:0 6px; background:#dbe3f3; }.execution-stages li i { display:block; width:6px; height:6px; flex:0 0 auto; border:1px solid #b5c0d7; border-radius:50%; background:#fff; transition:all .25s ease; }.execution-stages li span { white-space:nowrap; }.execution-stages li.active { color:#6681bc; font-weight:700; }.execution-stages li.active i { border-color:#7699dc; background:#7699dc; box-shadow:0 0 0 4px rgba(115,150,221,.12); animation:execution-dot 1.2s ease-in-out infinite; }.execution-stages li.passed { color:#91a6cb; }.execution-stages li.passed i { border-color:#9eb6df; background:#9eb6df; }
@keyframes execution-pulse { 0% { opacity:.8; transform:scale(.86); } 100% { opacity:0; transform:scale(1.34); } } @keyframes execution-dot { 50% { transform:scale(1.25); } } @keyframes execution-blink { 0%,45%,52%,100% { transform:scaleY(1); } 48%,50% { transform:scaleY(.16); } }
footer { display:flex; align-items:center; justify-content:space-between; gap:12px; padding-top:3px; color:#95a1b5; font-size:11px; }
button { border:0; border-radius:10px; padding:10px 14px; background:linear-gradient(135deg,#6687dc,#578eca); color:white; font:inherit; font-size:13px; font-weight:800; cursor:pointer; box-shadow:0 6px 14px rgba(80,121,203,.2); }
button:disabled { cursor:not-allowed; background:#b9c6df; box-shadow:none; }
.card-loading, .card-error, .success-note { margin:0; padding:14px 17px; font-size:13px; line-height:1.6; }
.card-loading { color:#7588ac; }
.card-error { color:#bd5361; background:rgba(255,241,243,.7); }
.success-note { color:#3f755e; background:rgba(240,250,245,.85); }
.success-note span { display:block; margin-top:4px; }
.success-note .issue-pending { color:#a76b37; }
@media (max-width:620px) { .order-form { grid-template-columns:1fr; } footer { align-items:flex-start; flex-direction:column; } }
</style>
