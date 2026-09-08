<template>
  <div class="chatgpt-page">
    <aside class="sidebar">
      <div class="agent-brand">
        <div class="brand-mark" aria-hidden="true"><img :src="cloudLogo" alt="" /></div>
        <div class="brand-copy">
          <div class="brand-name">云枢小智</div>
          <div class="brand-subtitle">YUNSHU AGENT</div>
        </div>
        <span class="brand-spark" aria-hidden="true">✦</span>
      </div>

      <div class="sidebar-scroll">
        <nav class="agent-tools" aria-label="云枢小智工具栏">
          <button
            v-for="tool in agentTools"
            :key="tool.key"
            type="button"
            class="agent-tool"
            :class="{ active: tool.key === 'chat', 'is-trace-active': tool.key === 'trace' && traceOpen, 'is-overview-active': tool.key === 'agents' && agentOverviewOpen }"
            :aria-current="tool.key === 'chat' ? 'page' : undefined"
            @click="handleTool(tool.key)"
          >
            <span class="tool-icon" aria-hidden="true">{{ tool.glyph }}</span>
            <span>{{ tool.label }}</span>
            <span v-if="tool.key === 'chat'" class="tool-active-dot" aria-hidden="true"></span>
          </button>
        </nav>

        <section class="history-section task-history-section" aria-label="历史任务">
          <div class="history-heading"><span>历史任务</span><span class="history-count">0</span></div>
          <div class="task-empty-state">
            <span class="task-empty-icon" aria-hidden="true">⌁</span>
            <span>运行过的任务会沉淀在这里</span>
          </div>
        </section>

        <section class="history-section conversation-section" aria-label="历史对话">
          <div class="history-heading"><span>历史对话</span><span v-if="sessions.length" class="history-count">{{ sessions.length }}</span></div>
          <div v-if="!sessions.length" class="empty-chat">从一个新任务开始对话</div>
          <ul v-else class="session-list">
            <li
              v-for="s in sessions"
              :key="s.id"
              :class="{ active: s.id === sessionId }"
              @click="loadSession(s.id)"
            >
              <span class="session-title">{{ s.title }}</span>
              <span class="session-status" :class="statusClass(s.status)">{{ s.status }}</span>
            </li>
          </ul>
        </section>
      </div>

      <div class="user-bar">
        <img
          v-if="!logoBroken"
          src="/images/my-logo.jpg"
          alt=""
          class="user-avatar-img"
          @error="logoBroken = true"
        />
        <div v-else class="user-avatar-fallback">{{ userInitial }}</div>
        <div class="user-text">
          <div class="user-name">{{ user.display_name }}</div>
          <div class="user-plan">{{ roleLabel }}</div>
        </div>
        <button type="button" class="user-settings" title="设置（即将开放）" aria-label="打开设置">
          <span class="settings-icon" aria-hidden="true">⚙</span>
          <span class="settings-label">设置</span>
        </button>
      </div>
    </aside>

    <main class="main">
      <header class="main-top">
        <span v-if="runningStatus" class="status-pill" :class="statusClass(runningStatus)">{{ runningStatus }}</span>
        <span v-if="lastIntent" class="intent-tag">{{ lastIntent }}</span>
        <button
          type="button"
          class="workflow-toggle"
          :class="{ active: traceOpen }"
          :aria-expanded="traceOpen"
          :aria-label="traceOpen ? '收起 Agent 工作流程' : '打开 Agent 工作流程'"
          :title="traceOpen ? '收起 Agent 工作流程' : '打开 Agent 工作流程'"
          aria-controls="agent-workflow-panel"
          @click="toggleTrace"
        >
          <span class="workflow-toggle-icon" aria-hidden="true"><i></i><b></b></span>
        </button>
      </header>

      <!-- 欢迎页 -->
      <section v-if="!messages.length" class="center-stage">
        <h1>今天想处理什么？</h1>

        <ChatComposer
          v-model="input"
          :selected-model="selectedModel"
          :approval-mode="approvalMode"
          :models="models"
          :disabled="loading"
          menu-placement="down"
          placeholder="随心输入"
          @update:selected-model="selectedModel = $event"
          @update:approval-mode="approvalMode = $event"
          @send="send"
          @attach="onAttach"
        />

        <div class="quick-cards">
          <button v-for="q in quickCards" :key="q.text" type="button" class="quick-card" @click="quickSend(q.text)">
            <span class="quick-icon" :class="q.icon">{{ q.emoji }}</span>
            <span class="card-title">{{ q.title }}</span>
            <span class="card-desc">{{ q.desc }}</span>
          </button>
        </div>
      </section>

      <!-- 对话区 -->
      <section v-else class="chat-stage">
        <div ref="scrollEl" class="messages" @scroll="onMessagesScroll">
          <article v-for="(m, i) in messages" :key="i" class="msg" :class="[m.role, { 'msg--subagent': m.agentName && m.agentName !== '云枢小智' }]">
            <div class="msg-avatar" :class="[`msg-avatar--${m.role}`, `msg-avatar--${agentVisualKey(m.agentName)}`]" aria-hidden="true">
              <img v-if="m.role === 'user'" :src="userAvatarSrc" alt="" />
              <span v-else class="assistant-orb"><i></i><b></b></span>
            </div>
            <div class="msg-content">
              <div class="msg-role" :class="`msg-role--${m.role}`">
                <template v-if="m.role === 'user'">你</template>
                <template v-else>
                  <span class="agent-name" :class="{ 'agent-name--subagent': m.agentName && m.agentName !== '云枢小智' }">{{ agentDisplayName(m.agentName) }}</span><span class="agent-sparkle" aria-hidden="true">✦</span>
                </template>
              </div>
              <div class="msg-body" :class="{ 'is-streaming': m.streaming }">
                <QualityReportCard
                  v-if="m.report"
                  :report="m.report"
                  :exporting="reportExportingId === m.report.report_id"
                  @export="onExportQualityReport"
                />
                <div v-else-if="m.role === 'assistant' && !m.content && m.streaming" class="typing-indicator" aria-label="云枢小智正在生成回答">
                  <i></i><i></i><i></i>
                </div>
                <div v-else class="msg-rendered" v-html="formatAnswer(m.content)"></div>
              </div>
              <ConfirmationCard
                v-for="c in m.confirmations || []"
                :key="c.confirmation_id"
                :data="c"
                :loading="confirmLoading"
                @approve="onApprove"
                @reject="onReject"
              />
              <OrderIntakeCard
                v-if="m.role === 'assistant' && !m.streaming && m.interaction?.type === 'scheduling_order_intake'"
                :session-id="sessionId"
                :model-key="selectedModel"
                @execution-state="onOrderIntakeExecutionState"
                @created="onOrderIntakeCreated"
              />
            </div>
          </article>
          <Transition name="stream-wait">
            <div v-if="loading && streamWaitVisible" class="stream-waiting" role="status" aria-live="polite">
              <span class="stream-wait-orb"><i></i><b></b></span>
              <span>{{ streamWaitHint }}</span>
              <em><i></i><i></i><i></i></em>
            </div>
          </Transition>
        </div>

        <div class="composer-dock">
          <ChatComposer
            v-model="input"
            :selected-model="selectedModel"
            :approval-mode="approvalMode"
            :models="models"
            :disabled="loading"
            menu-placement="up"
            placeholder="随心输入"
            @update:selected-model="selectedModel = $event"
            @update:approval-mode="approvalMode = $event"
            @send="send"
            @attach="onAttach"
          />
        </div>
      </section>

      <TracePanel
        :open="traceOpen"
        :status="traceData.status"
        :plan="traceData.plan"
        :agents="traceData.selected_agents"
        :steps="traceData.steps"
        :tool-calls="traceData.tool_calls"
        :events="traceData.events"
        @close="traceOpen = false"
      />
      <AgentOverviewPanel
        :open="agentOverviewOpen"
        :agents="agentOverviewAgents"
        @close="agentOverviewOpen = false"
      />
    </main>
  </div>
</template>

<script setup>
import { computed, nextTick, onBeforeUnmount, onMounted, reactive, ref, watch } from "vue";
import cloudLogo from "../../../../images/云.svg";
import ChatComposer from "@/components/yunshu-ai/ChatComposer.vue";
import ConfirmationCard from "@/components/yunshu-ai/ConfirmationCard.vue";
import OrderIntakeCard from "@/components/yunshu-ai/OrderIntakeCard.vue";
import QualityReportCard from "@/components/yunshu-ai/QualityReportCard.vue";
import TracePanel from "@/components/yunshu-ai/TracePanel.vue";
import AgentOverviewPanel from "@/components/yunshu-ai/AgentOverviewPanel.vue";
import {
  agentHealth,
  approveConfirmation,
  currentAgentUser,
  exportQualityReport,
  fetchAgentSession,
  fetchAgentSessions,
  fetchModels,
  fetchTrace,
  rejectConfirmation,
  streamAgentChat,
} from "@/api/yunshuAgent";

const APPROVAL_KEY = "yunshu_ai_approval_mode";
const reportExportingId = ref("");
const MODEL_KEY = "yunshu_ai_model";

const user = currentAgentUser();
const logoBroken = ref(false);
const userAvatarSrc = computed(() => user.avatar || "/images/my-logo.jpg");
const input = ref("");
const loading = ref(false);
const streamWaitVisible = ref(false);
const streamWaitHint = ref("云枢小智正在思考");
const confirmLoading = ref(false);
const sessionId = ref(null);
const messages = ref([]);
const sessions = ref([]);
const models = ref([]);
const selectedModel = ref(localStorage.getItem(MODEL_KEY) || "deepseek");
const approvalMode = ref(localStorage.getItem(APPROVAL_KEY) || "risk_only");
const traceOpen = ref(false);
const agentOverviewOpen = ref(false);
const runningStatus = ref("");
const lastIntent = ref("");
const scrollEl = ref(null);
const stickToBottom = ref(true);
let scrollFrame = 0;
const traceData = reactive({
  status: "",
  plan: [],
  selected_agents: [],
  steps: [],
  tool_calls: [],
  events: [],
});

const quickCards = [
  { title: "智能排产", desc: "读取最新订单并安排生产", text: "我有一个新订单，帮我智能排产", icon: "schedule", emoji: "✦" },
  { title: "质量分析", desc: "读取实时质量数据并识别风险", text: "帮我分析今天的质量情况", icon: "quality", emoji: "◌" },
  { title: "系统总览", desc: "今日 KPI 与待办事项", text: "查询今天系统总览", icon: "overview", emoji: "▦" },
  { title: "生产追溯", desc: "按产品 SN 快速定位生产链路", text: "如何快速定位一张产品 SN？", icon: "trace", emoji: "⌁" },
];

const agentTools = [
  { key: "new", label: "新建任务", glyph: "✎" },
  { key: "scheduled", label: "已安排", glyph: "◷" },
  { key: "agents", label: "Agent 总览", glyph: "◉" },
  { key: "sites", label: "站点", glyph: "▦" },
  { key: "chat", label: "聊天", glyph: "＋" },
  { key: "trace", label: "执行轨迹", glyph: "⌁" },
];

const agentOverviewAgents = [
  { id: "IntentRoutingAgent", label: "意图识别", module: "智能排产入口", tone: "intent", status: "active" },
  { id: "OrderIntakeAgent", label: "订单接收", module: "订单与排产准备", tone: "intake", status: "active" },
  { id: "BomRouteAgent", label: "BOM 与工艺", module: "主数据与工艺", tone: "bom", status: "active" },
  { id: "KittingRiskAgent", label: "齐套风险", module: "物料齐套", tone: "risk", status: "active" },
  { id: "KittingExecutionAgent", label: "锁料执行", module: "物料锁定", tone: "lock", status: "active" },
  { id: "CapacitySchedulingAgent", label: "产能排产", module: "计划调度", tone: "schedule", status: "active" },
  { id: "DispatchExecutionAgent", label: "派工同步", module: "现场派工", tone: "dispatch", status: "active" },
  { id: "SchedulingValidationAgent", label: "排产校验", module: "计划校验", tone: "validate", status: "active" },
  { id: "ProductionIssueAgent", label: "生产领料", module: "仓储出库", tone: "issue", status: "active" },
  { id: "ProductionExecutionAgent", label: "生产执行", module: "生产报工", tone: "production", status: "active" },
  { id: "QualityManagementAgent", label: "质量管理", module: "质量与放行", tone: "quality", status: "active" },
  { id: "WarehouseLogisticsAgent", label: "仓储物流", module: "库存与入库", tone: "warehouse", status: "active" },
  { id: "TraceabilityAgent", label: "追溯管理", module: "批次与 SN", tone: "trace", status: "active" },
  { id: "EquipmentMaintenanceAgent", label: "设备维护", module: "点检与维修", tone: "equipment", status: "active" },
  { id: "AndonResponseAgent", label: "安灯响应", module: "现场异常", tone: "andon", status: "active" },
  { id: "OperationsInsightAgent", label: "经营洞察", module: "经营总览", tone: "insight", status: "active" },
];

const userInitial = computed(() => (user.display_name || "U").slice(0, 1));

const roleLabel = computed(() => {
  const map = {
    TESTER: "测试人员",
    MANAGER: "管理员",
    PROD_SUPERVISOR: "生产主管",
    WAREHOUSE_CLERK: "仓库员",
    QUALITY_INSPECTOR: "质检员",
    EQUIPMENT_MAINTAINER: "设备维护",
    LINE_OPERATOR: "产线操作员",
  };
  return map[user.user_role] || user.user_role;
});

function statusClass(s) {
  const map = {
    Done: "done",
    "Waiting Approval": "wait",
    Error: "error",
    Planning: "plan",
    Routing: "plan",
    Running: "run",
  };
  return map[s] || "done";
}

function formatAnswer(text) {
  if (!text) return "";
  const plainText = String(text)
    .replace(/\r/g, "")
    .replace(/!\[([^\]]*)\]\([^)]*\)/g, "$1")
    .replace(/\[([^\]]+)\]\([^)]*\)/g, "$1")
    .replace(/```[^\n]*\n?/g, "")
    .replace(/`/g, "")
    .replace(/^\s*#{1,6}\s*/gm, "")
    .replace(/^\s*>\s?/gm, "")
    .replace(/^\s*[-+*]\s+/gm, "")
    .replace(/(\*\*|__|~~|\*|_)/g, "")
    .replace(/^\s*[-=]{3,}\s*$/gm, "")
    .replace(/\n{3,}/g, "\n\n")
    .trim();
  return plainText
    .replace(/&/g, "&amp;")
    .replace(/</g, "&lt;")
    .replace(/>/g, "&gt;")
    .replace(/\n/g, "<br/>");
}

const AGENT_LABELS = {
  MainAgent: "云枢小智",
  IntentRoutingAgent: "意图识别 Agent",
  OrderIntakeAgent: "订单接收 Agent",
  BomRouteAgent: "BOM 与工艺路线 Agent",
  KittingRiskAgent: "齐套与物料风险 Agent",
  KittingExecutionAgent: "锁料执行 Agent",
  CapacitySchedulingAgent: "产能排产 Agent",
  DispatchExecutionAgent: "派工同步 Agent",
  SchedulingValidationAgent: "排产校验 Agent",
  ProductionIssueAgent: "生产领料 Agent",
  ProductionExecutionAgent: "生产执行 Agent",
  QualityManagementAgent: "质量管理 Agent",
  WarehouseLogisticsAgent: "仓储物流 Agent",
  TraceabilityAgent: "追溯管理 Agent",
  EquipmentMaintenanceAgent: "设备维护 Agent",
  AndonResponseAgent: "安灯响应 Agent",
  OperationsInsightAgent: "经营洞察 Agent",
};

function agentDisplayName(agentName) {
  return AGENT_LABELS[agentName] || agentName || "云枢小智";
}

function agentVisualKey(agentName) {
  const map = {
    IntentRoutingAgent: "intent",
    OrderIntakeAgent: "intake",
    BomRouteAgent: "bom",
    KittingRiskAgent: "risk",
    KittingExecutionAgent: "lock",
    CapacitySchedulingAgent: "schedule",
    DispatchExecutionAgent: "dispatch",
    SchedulingValidationAgent: "validate",
    ProductionIssueAgent: "issue",
    ProductionExecutionAgent: "production",
    QualityManagementAgent: "quality",
    WarehouseLogisticsAgent: "warehouse",
    TraceabilityAgent: "trace",
    EquipmentMaintenanceAgent: "equipment",
    AndonResponseAgent: "andon",
    OperationsInsightAgent: "insight",
  };
  return map[agentName] || "main";
}

function workflowEventMessage(event) {
  const sourceText = (event.sources || []).join("；") || "MES 业务服务";
  const status = event.status === "completed" ? "已完成" : event.status === "waiting" ? "等待输入" : "已阻断";
  return `调用结果 · ${event.title}\n${event.summary}\n数据来源：${sourceText}\n节点状态：${status}`;
}

async function playPersistedConversationMessage(payload) {
  const event = payload.workflow_event || payload.workflowEvent || null;
  const message = reactive({
    role: "assistant",
    agentName: payload.agent_name || payload.agentName || event?.agent || "云枢小智",
    content: "",
    streaming: true,
    workflowEvent: event,
  });
  messages.value.push(message);
  await scrollBottom();
  const writer = createSmoothStreamWriter((chunk) => {
    message.content += chunk;
    scheduleScrollBottom();
  });
  writer.push(payload.content || workflowEventMessage(event || {}));
  await writer.drain();
  message.streaming = false;
  await new Promise((resolve) => window.setTimeout(resolve, 420));
}

function newChat() {
  sessionId.value = null;
  messages.value = [];
  runningStatus.value = "";
  lastIntent.value = "";
  Object.assign(traceData, { status: "", plan: [], selected_agents: [], steps: [], tool_calls: [], events: [] });
}

function handleTool(key) {
  if (key === "new" || key === "chat") {
    newChat();
    return;
  }
  if (key === "agents") {
    agentOverviewOpen.value = !agentOverviewOpen.value;
    if (agentOverviewOpen.value) traceOpen.value = false;
    return;
  }
  if (key === "trace") toggleTrace();
}

function toggleTrace() {
  traceOpen.value = !traceOpen.value;
  if (traceOpen.value) agentOverviewOpen.value = false;
}

async function loadSession(id) {
  const s = sessions.value.find((x) => x.id === id);
  if (!s) return;
  try {
    const detail = await fetchAgentSession(id);
    sessionId.value = detail.id;
    messages.value = (detail.messages || []).map((message) => ({
      role: message.role,
      content: message.content,
      agentName: message.agent_name || "云枢小智",
      workflowEvent: message.workflow_event || null,
      report: message.report || null,
      streaming: false,
    }));
    runningStatus.value = detail.status || "";
    lastIntent.value = "";
    await scrollBottom();
  } catch (e) {
    alert(`历史对话加载失败：${e.message}`);
  }
}

function upsertSession(title, status, intent) {
  const id = sessionId.value || crypto.randomUUID();
  sessionId.value = id;
  const existing = sessions.value.find((s) => s.id === id);
  const payload = {
    id,
    title: title.slice(0, 24),
    status,
    intent,
  };
  if (existing) Object.assign(existing, payload);
  else sessions.value.unshift(payload);
}

async function scrollBottom() {
  await nextTick();
  if (scrollEl.value) {
    scrollEl.value.scrollTop = scrollEl.value.scrollHeight;
    stickToBottom.value = true;
  }
}

function onMessagesScroll() {
  const element = scrollEl.value;
  if (!element) return;
  stickToBottom.value = element.scrollHeight - element.scrollTop - element.clientHeight < 72;
}

function scheduleScrollBottom() {
  // Keep the natural chat follow behaviour only while the reader is already
  // at the bottom.  Once they scroll up to inspect a chart or earlier card,
  // incoming stream fragments must not pull the viewport back down.
  if (!stickToBottom.value) return;
  if (scrollFrame) return;
  scrollFrame = window.requestAnimationFrame(() => {
    scrollFrame = 0;
    if (scrollEl.value && stickToBottom.value) scrollEl.value.scrollTop = scrollEl.value.scrollHeight;
  });
}

function createSmoothStreamWriter(onText) {
  let queue = "";
  let timer = 0;
  let drainResolver;

  const schedule = () => {
    if (timer || !queue) return;
    const queueLength = Array.from(queue).length;
    // Keep a constant cadence even when the network delivers several SSE
    // fragments in one browser read; this is what prevents a final text burst.
    const batchSize = queueLength > 180 ? 2 : 1;
    const delay = queueLength > 180 ? 24 : 28;
    timer = window.setTimeout(() => {
      timer = 0;
      const characters = Array.from(queue);
      const text = characters.splice(0, batchSize).join("");
      queue = characters.join("");
      if (text) onText(text);
      if (queue) schedule();
      else if (drainResolver) {
        drainResolver();
        drainResolver = undefined;
      }
    }, delay);
  };

  return {
    push(text) {
      queue += text || "";
      schedule();
    },
    drain() {
      if (!queue && !timer) return Promise.resolve();
      return new Promise((resolve) => {
        drainResolver = resolve;
      });
    },
    dispose() {
      if (timer) window.clearTimeout(timer);
      timer = 0;
      queue = "";
      drainResolver?.();
      drainResolver = undefined;
    },
  };
}

function quickSend(text) {
  input.value = text;
  send();
}

async function onOrderIntakeCreated(result) {
  const issue = result.production_issue_execution;
  const conversationMessages = result.conversation_messages || [];
  if (!conversationMessages.length) {
    throw new Error("排产结果未附带已持久化的 Agent 过程消息，已停止展示以避免出现不可审计输出。");
  }
  if (result.session_id) sessionId.value = String(result.session_id);
  loading.value = true;
  runningStatus.value = "Running";
  traceData.events = [];
  for (const message of conversationMessages) {
    const event = message.workflow_event || null;
    if (event) traceData.events.push(event);
    await playPersistedConversationMessage(message);
  }
  runningStatus.value = issue?.readyForShopFloor ? "Done" : "Error";
  loading.value = false;
  await scrollBottom();
}

function onOrderIntakeExecutionState(state) {
  if (state?.active) {
    loading.value = true;
    runningStatus.value = "Running";
    // The confirmation card owns this wait state so users can keep seeing
    // which MES stage is pending without a second, duplicate typing bubble.
    streamWaitVisible.value = false;
    scheduleScrollBottom();
    return;
  }
  loading.value = false;
  runningStatus.value = "Error";
  streamWaitVisible.value = false;
}

async function send() {
  const text = input.value.trim();
  if (!text || loading.value) return;
  input.value = "";
  messages.value.push({ role: "user", content: text });
  const assistantMessage = reactive({
    role: "assistant",
    agentName: "云枢小智",
    content: "",
    confirmations: [],
    streaming: true,
  });
  loading.value = true;
  streamWaitVisible.value = true;
  streamWaitHint.value = "云枢小智正在思考";
  runningStatus.value = "Planning";
  await scrollBottom();
  const writer = createSmoothStreamWriter((chunk) => {
    assistantMessage.content += chunk;
    scheduleScrollBottom();
  });
  const ensureMainAssistantVisible = () => {
    if (!messages.value.includes(assistantMessage)) {
      messages.value.push(assistantMessage);
      scheduleScrollBottom();
    }
  };
  const streamedNodes = new Map();
  const streamedReports = new Map();
  const reportWriters = new Map();
  let hasStructuredReport = false;
  const addNodeMessage = (event) => {
    const key = event.node_id || event.workflow_event?.event_id || `${event.agent_name || "QualityManagementAgent"}-${streamedNodes.size}`;
    let node = streamedNodes.get(key);
    if (!node) {
      node = reactive({
        role: "assistant",
        agentName: event.agent_name || event.workflow_event?.agent || "QualityManagementAgent",
        content: "",
        streaming: true,
        workflowEvent: event.workflow_event || null,
      });
      const mainIndex = messages.value.indexOf(assistantMessage);
      messages.value.splice(mainIndex >= 0 ? mainIndex : messages.value.length, 0, node);
      streamedNodes.set(key, node);
      streamWaitVisible.value = false;
      scrollBottom();
    }
    return node;
  };
  const handleStreamNode = async (event) => {
    // `node_start` only announces backend work. Rendering it as a chat message
    // created an empty, long-lived typing card while a model was still
    // preparing its first token. A card is now created only with actual text
    // (`node`, `node_delta`, or `node_complete`).
    if (event.type === "node_start") {
      const label = agentDisplayName(event.agent_name || event.workflow_event?.agent);
      const title = String(event.workflow_event?.title || "");
      // The final MainAgent node is the external-model summary. Its first
      // token can take noticeably longer than a local rule node, so surface a
      // compact live status instead of leaving the completed cards silent.
      streamWaitHint.value = event.agent_name === "MainAgent" || /总体分析|总结|报告结论/.test(title)
        ? "云枢小智正在汇总质量结论"
        : `${label}正在准备分析`;
      streamWaitVisible.value = true;
      scheduleScrollBottom();
      if (event.workflow_event) {
        const index = traceData.events.findIndex((item) => item.event_id === event.workflow_event.event_id);
        if (index >= 0) traceData.events[index] = event.workflow_event;
        else traceData.events.push(event.workflow_event);
      }
      return;
    }
    const node = addNodeMessage(event);
    let nodeWriter = node._streamWriter;
    if (!nodeWriter) {
      nodeWriter = createSmoothStreamWriter((chunk) => {
        node.content += chunk;
        scheduleScrollBottom();
      });
      node._streamWriter = nodeWriter;
    }
    if (event.type === "node") {
      nodeWriter.push(event.content || "");
      await nodeWriter.drain();
      node.streaming = false;
    } else if (event.type === "node_delta") {
      nodeWriter.push(event.content || "");
      await nodeWriter.drain();
    } else if (event.type === "node_complete") {
      if (!node.content && event.content) nodeWriter.push(event.content);
      await nodeWriter.drain();
      node.streaming = false;
    }
    if (event.workflow_event) {
      const index = traceData.events.findIndex((item) => item.event_id === event.workflow_event.event_id);
      if (index >= 0) traceData.events[index] = event.workflow_event;
      else traceData.events.push(event.workflow_event);
    }
  };
  const updateTraceEvent = (event) => {
    if (!event?.workflow_event) return;
    const index = traceData.events.findIndex((item) => item.event_id === event.workflow_event.event_id);
    if (index >= 0) traceData.events[index] = event.workflow_event;
    else traceData.events.push(event.workflow_event);
  };
  const ensureReport = (event) => {
    const id = event.report_id;
    let report = streamedReports.get(id);
    if (!report) {
      report = reactive({
        report_id: id,
        title: event.title || "质量分析报告",
        window_days: event.window_days || 7,
        queried_at: event.queried_at || "",
        source: event.source || "MES 实时只读快照",
        facts: event.facts || {},
        sections: [], charts: [], tables: [], export_ready: false,
      });
      const message = reactive({ role: "assistant", agentName: "QualityManagementAgent", content: "", report, streaming: false });
      const mainIndex = messages.value.indexOf(assistantMessage);
      messages.value.splice(mainIndex >= 0 ? mainIndex : messages.value.length, 0, message);
      streamedReports.set(id, report);
      hasStructuredReport = true;
      streamWaitVisible.value = false;
      scrollBottom();
    }
    return report;
  };
  const ensureReportSection = (report, event) => {
    const id = event.section || "analysis";
    let section = report.sections.find((item) => item.id === id);
    if (!section) {
      section = reactive({ id, title: event.title || "质量分析", content: "", streaming: false });
      report.sections.push(section);
    }
    return section;
  };
  const handleReport = async (event) => {
    const report = ensureReport(event);
    if (event.type === "report_start") {
      Object.assign(report, event);
    } else if (event.type === "report_section") {
      const section = ensureReportSection(report, event);
      section.title = event.title || section.title;
      section.content = event.content || "";
      section.streaming = false;
      // Rule fallback sections do not emit deltas.  They are still real
      // content, so end the compact model-wait state as soon as they arrive.
      streamWaitVisible.value = false;
    } else if (event.type === "report_section_delta") {
      const section = ensureReportSection(report, event);
      section.title = event.title || section.title;
      section.streaming = true;
      // The report's final external summary streams through this event.  Its
      // first visible fragment is the hand-off point from the wait indicator
      // to the report itself.
      streamWaitVisible.value = false;
      let sectionWriter = reportWriters.get(`${report.report_id}:${section.id}`);
      if (!sectionWriter) {
        sectionWriter = createSmoothStreamWriter((chunk) => {
          section.content += chunk;
          scheduleScrollBottom();
        });
        reportWriters.set(`${report.report_id}:${section.id}`, sectionWriter);
      }
      sectionWriter.push(event.content || "");
      await sectionWriter.drain();
    } else if (event.type === "report_chart") {
      const index = report.charts.findIndex((item) => item.id === event.chart?.id);
      if (index >= 0) report.charts[index] = event.chart;
      else report.charts.push(event.chart);
    } else if (event.type === "report_table") {
      const index = report.tables.findIndex((item) => item.id === event.table?.id);
      if (index >= 0) report.tables[index] = event.table;
      else report.tables.push(event.table);
    } else if (event.type === "report_final_complete") {
      const section = ensureReportSection(report, {
        ...event,
        section: event.section || "analysis",
        title: event.title || "风险与行动建议",
      });
      const sectionWriter = reportWriters.get(`${report.report_id}:${section.id}`);
      await sectionWriter?.drain();
      // Be resilient to a provider that returns the whole conclusion only in
      // its completion event: never leave a chart-only report before export.
      if (!section.content && event.content) section.content = event.content;
      section.streaming = false;
      streamWaitVisible.value = false;
      updateTraceEvent(event);
    } else if (event.type === "report_export_ready") {
      report.export_ready = true;
      report.status = "READY";
      report.export_message = event.message;
      streamWaitVisible.value = false;
    }
    scheduleScrollBottom();
  };

  try {
    let responseMeta = null;
    await streamAgentChat({
      message: text,
      session_id: sessionId.value,
      user_id: user.user_id,
      user_role: user.user_role,
      model: selectedModel.value,
      approval_mode: approvalMode.value,
    }, {
      onMeta: (meta) => {
        responseMeta = { ...(responseMeta || {}), ...meta };
        if (meta.session_id) sessionId.value = meta.session_id;
        if (meta.status) runningStatus.value = meta.status;
        if (meta.intent) lastIntent.value = meta.intent;
        if (meta.intent === "quality_daily_analysis") streamWaitHint.value = "正在读取实时质量数据";
        if (meta.intent === "quality_analysis_report") streamWaitHint.value = "正在准备质量报告";
        if (meta.agent_name) assistantMessage.agentName = meta.agent_name;
        if (meta.confirmations) assistantMessage.confirmations = meta.confirmations;
        if (meta.interaction?.type) assistantMessage.interaction = meta.interaction;
        Object.assign(traceData, {
          status: meta.status || traceData.status,
          plan: meta.plan || traceData.plan,
          selected_agents: meta.selected_agents || traceData.selected_agents,
          steps: meta.steps || traceData.steps,
          tool_calls: meta.tool_calls || traceData.tool_calls,
          events: meta.workflow_events || traceData.events,
        });
      },
      onNode: handleStreamNode,
      onReport: handleReport,
      onDelta: async (chunk) => {
        streamWaitVisible.value = false;
        writer.push(chunk);
        // Do not expose a typing bubble before the first visible character.
        // The smooth writer preserves the gentle cadence after it is shown.
        ensureMainAssistantVisible();
        await writer.drain();
      },
      onDone: (done) => {
        responseMeta = { ...(responseMeta || {}), ...done };
        // `report_export_ready` is emitted immediately before `done`, but a
        // browser can receive both SSE records in its final read.  Treat the
        // durable report id on `done` as the authoritative completion signal
        // so the Word action never depends on one transient UI event.
        if (done?.report_id) {
          const report = streamedReports.get(done.report_id);
          if (report) {
            report.export_ready = true;
            report.status = "READY";
            report.export_message ||= "报告已完成。是否需要导出 Word？";
          }
        }
      },
    });
    await writer.drain();
    assistantMessage.streaming = false;
    if (!assistantMessage.content.trim() && !hasStructuredReport) {
      ensureMainAssistantVisible();
      assistantMessage.content = "模型暂未返回有效内容，请稍后再试。";
    }
    const resp = responseMeta || {};
    upsertSession(text, resp.status || "Done", resp.intent || "");
    try {
      const history = await fetchAgentSessions();
      sessions.value = history.sessions || [];
    } catch {
      /* current conversation is already visible; history retry on next page load */
    }
    Object.assign(traceData, {
      status: resp.status || "Done",
      plan: resp.plan || [],
      selected_agents: resp.selected_agents || [],
      steps: resp.steps || [],
      tool_calls: resp.tool_calls || [],
      events: resp.workflow_events || traceData.events,
    });
    if (resp.trace_id) {
      try {
        const t = await fetchTrace(resp.trace_id);
        Object.assign(traceData, t);
      } catch {
        /* trace optional */
      }
    }
  } catch (e) {
    writer.dispose();
    runningStatus.value = "Error";
    streamWaitVisible.value = false;
    ensureMainAssistantVisible();
    assistantMessage.streaming = false;
    assistantMessage.content = `请求失败：${e.message}。请确认 Agent 后端已启动（端口 8090）。`;
  } finally {
    assistantMessage.streaming = false;
    streamWaitVisible.value = false;
    loading.value = false;
    await scrollBottom();
  }
}

async function onExportQualityReport(reportId) {
  if (!reportId || reportExportingId.value) return;
  reportExportingId.value = reportId;
  try {
    await exportQualityReport(reportId);
  } catch (error) {
    alert(`质量报告导出失败：${error.message}`);
  } finally {
    reportExportingId.value = "";
  }
}

async function onApprove(id) {
  confirmLoading.value = true;
  try {
    await approveConfirmation(id, user.user_id);
    for (const m of messages.value) {
      if (m.confirmations) {
        for (const c of m.confirmations) {
          if (c.confirmation_id === id) c.status = "approved";
        }
      }
    }
    runningStatus.value = "Done";
    messages.value.push({ role: "assistant", content: "已批准并执行，安灯事件已创建（Mock）。" });
    await scrollBottom();
  } catch (e) {
    alert(e.message);
  } finally {
    confirmLoading.value = false;
  }
}

async function onReject(id) {
  confirmLoading.value = true;
  try {
    await rejectConfirmation(id);
    for (const m of messages.value) {
      if (m.confirmations) {
        for (const c of m.confirmations) {
          if (c.confirmation_id === id) c.status = "rejected";
        }
      }
    }
    runningStatus.value = "Done";
  } catch (e) {
    alert(e.message);
  } finally {
    confirmLoading.value = false;
  }
}

onMounted(async () => {
  try {
    const history = await fetchAgentSessions();
    sessions.value = history.sessions || [];
  } catch {
    sessions.value = [];
  }
  try {
    await agentHealth();
    const data = await fetchModels();
    models.value = data.models || [];
    const def = models.value.find((m) => m.default);
    if (def && !localStorage.getItem(MODEL_KEY)) selectedModel.value = def.key;
  } catch {
    models.value = [
      { key: "deepseek", label: "DeepSeek V4-Pro", short: "DeepSeek V4-Pro", default: true },
      { key: "qwen", label: "Qwen 3.7-Max", short: "Qwen 3.7-Max" },
      { key: "glm", label: "智谱 GLM 5.2", short: "智谱 GLM 5.2" },
    ];
  }
});

function onAttach(files) {
  const names = files.map((f) => f.name).join("、");
  input.value = input.value ? `${input.value}\n[附件: ${names}]` : `[附件: ${names}]`;
}

watch(selectedModel, (v) => localStorage.setItem(MODEL_KEY, v));
watch(approvalMode, (v) => localStorage.setItem(APPROVAL_KEY, v));

onBeforeUnmount(() => {
  if (scrollFrame) window.cancelAnimationFrame(scrollFrame);
});
</script>

<style scoped>
.chatgpt-page {
  width: 100%;
  height: 100%;
  min-height: 0;
  align-self: stretch;
  position: relative;
  overflow: hidden;
  background: #fff;
  color: #1f1f1f;
  font-family: "Segoe UI", "Microsoft YaHei", Arial, sans-serif;
  display: flex;
}

.sidebar {
  width: 292px;
  flex-shrink: 0;
  align-self: stretch;
  height: 100%;
  min-height: 0;
  background:
    radial-gradient(circle at 8% 4%, rgba(221, 229, 255, 0.62), transparent 33%),
    radial-gradient(circle at 96% 98%, rgba(252, 232, 239, 0.26), transparent 31%),
    linear-gradient(165deg, #fbfcff 0%, #f4f7ff 48%, #faf8fc 100%);
  border-right: 1px solid rgba(197, 206, 232, 0.7);
  display: flex;
  flex-direction: column;
  position: relative;
  overflow: hidden;
  z-index: 2;
}

.sidebar::before {
  content: "";
  position: absolute;
  top: -110px;
  right: -95px;
  width: 230px;
  height: 230px;
  border-radius: 50%;
  background: radial-gradient(circle, rgba(163, 177, 232, 0.15), transparent 67%);
  pointer-events: none;
}

.agent-brand {
  position: relative;
  isolation: isolate;
  min-height: 66px;
  margin: 11px 14px 5px;
  padding: 8px 3px;
  display: flex;
  align-items: center;
  gap: 11px;
  overflow: hidden;
}
.agent-brand::after {
  content: "";
  position: absolute;
  z-index: -1;
  right: -12px;
  bottom: -18px;
  width: 90px;
  height: 90px;
  border-radius: 50%;
  background: radial-gradient(circle, rgba(151, 165, 225, 0.2), transparent 68%);
  filter: blur(2px);
  animation: brand-glow 5s ease-in-out infinite;
}
.brand-mark {
  display: grid;
  width: 43px;
  height: 43px;
  flex: 0 0 43px;
  place-items: center;
  border-radius: 14px;
  background: linear-gradient(135deg, #f1f5ff, #e1e9ff);
  box-shadow: inset 0 1px rgba(255, 255, 255, 0.95), 0 5px 14px rgba(94, 116, 186, 0.12);
}
.brand-mark img {
  width: 28px;
  height: 28px;
  opacity: 0.82;
  filter: sepia(0.1) saturate(0.95) hue-rotate(176deg);
}
.brand-copy { min-width: 0; }
.brand-name {
  color: #405476;
  font-size: 18px;
  font-weight: 800;
  line-height: 1.18;
  letter-spacing: 0.07em;
  text-shadow: 0 2px 12px rgba(99, 118, 184, 0.13);
}
.brand-subtitle {
  margin-top: 5px;
  color: #929db6;
  font-size: 9px;
  font-weight: 700;
  line-height: 1;
  letter-spacing: 0.13em;
}
.brand-spark {
  position: absolute;
  top: 10px;
  right: 11px;
  color: #938bd2;
  font-size: 40px;
  animation: brand-sparkle 3s ease-in-out infinite;
}

.sidebar-scroll {
  flex: 1;
  min-height: 0;
  overflow-x: hidden;
  overflow-y: auto;
  padding: 7px 10px 13px;
  scrollbar-width: thin;
  scrollbar-color: rgba(132, 148, 199, 0.28) transparent;
}
.agent-tools {
  display: grid;
  gap: 1px;
  padding: 0 2px 10px;
  border-bottom: 1px solid rgba(202, 211, 234, 0.76);
}
.agent-tool {
  position: relative;
  width: 100%;
  min-height: 36px;
  border: 1px solid transparent;
  border-radius: 11px;
  background: transparent;
  color: #5b6881;
  display: flex;
  align-items: center;
  gap: 11px;
  padding: 0 10px;
  font: inherit;
  font-size: 14px;
  font-weight: 620;
  letter-spacing: 0.01em;
  text-align: left;
  cursor: pointer;
  transition: background 180ms ease, border-color 180ms ease, color 180ms ease, transform 180ms ease;
}
.agent-tool:hover {
  border-color: rgba(188, 201, 234, 0.82);
  background: rgba(255, 255, 255, 0.7);
  color: #4b5d81;
  transform: translateX(2px);
}
.agent-tool.active {
  border-color: rgba(164, 182, 231, 0.58);
  background: linear-gradient(102deg, rgba(225, 233, 255, 0.9), rgba(248, 248, 255, 0.72));
  color: #526fbb;
  box-shadow: 0 5px 13px rgba(94, 118, 194, 0.07);
}
.agent-tool.is-trace-active { color: #6965bc; }
.agent-tool.is-overview-active {
  border-color: rgba(165, 180, 229, .7);
  background: linear-gradient(102deg, rgba(235, 239, 255, .92), rgba(251, 248, 255, .78));
  color: #5e6fbb;
  box-shadow: 0 5px 13px rgba(94, 118, 194, .08);
}
.tool-icon {
  display: inline-grid;
  width: 19px;
  height: 19px;
  flex: 0 0 19px;
  place-items: center;
  color: #6d7c9b;
  font-size: 19px;
  font-weight: 400;
  line-height: 1;
}
.agent-tool.active .tool-icon { color: #6484d1; }
.tool-active-dot {
  width: 8px;
  height: 8px;
  margin-left: auto;
  border-radius: 50%;
  background: #71a0e9;
  box-shadow: 0 0 0 4px rgba(113, 160, 233, 0.11);
}

.history-section { padding: 16px 2px 0; }
.history-heading {
  display: flex;
  align-items: center;
  justify-content: space-between;
  padding: 0 9px 8px;
  color: #8995ad;
  font-size: 11px;
  font-weight: 750;
  letter-spacing: 0.08em;
}
.task-history-section .history-heading,
.conversation-section .history-heading {
  padding-bottom: 10px;
  color: #65769a;
  font-size: 15px;
  font-weight: 800;
  letter-spacing: 0.045em;
}
.task-history-section .history-count,
.conversation-section .history-count {
  background: rgba(216, 226, 252, 0.8);
  color: #7387b2;
}
.history-count {
  min-width: 18px;
  padding: 2px 5px;
  border-radius: 99px;
  background: rgba(222, 228, 246, 0.68);
  color: #8392af;
  font-size: 10px;
  font-weight: 700;
  text-align: center;
}
.task-empty-state {
  display: flex;
  align-items: center;
  gap: 8px;
  margin: 0 4px;
  padding: 11px 10px;
  border: 1px dashed rgba(191, 202, 232, 0.84);
  border-radius: 12px;
  background: rgba(255, 255, 255, 0.32);
  color: #9ba7bd;
  font-size: 12px;
  line-height: 1.45;
}
.task-empty-icon { color: #8ba2dd; font-size: 16px; }
.empty-chat {
  margin: 0 4px;
  padding: 12px 10px;
  border-radius: 11px;
  color: #9da7b9;
  font-size: 12px;
  line-height: 1.5;
}

.session-list {
  list-style: none;
  margin: 0;
  padding: 0 4px 2px;
  overflow: visible;
}
.session-list li {
  padding: 9px 10px;
  border: 1px solid transparent;
  border-radius: 11px;
  cursor: pointer;
  margin-bottom: 4px;
  transition: background 180ms ease, border-color 180ms ease;
}
.session-list li:hover { background: rgba(255, 255, 255, 0.7); border-color: rgba(201, 210, 236, 0.64); }
.session-list li.active { background: rgba(224, 233, 255, 0.76); border-color: rgba(166, 184, 231, 0.56); }
.session-title {
  display: block;
  font-size: 13px;
  font-weight: 590;
  color: #53627e;
  white-space: nowrap;
  overflow: hidden;
  text-overflow: ellipsis;
}
.session-status {
  display: inline-block;
  margin-top: 3px;
  font-size: 11px;
  color: #94a0b6;
}
.session-status.wait { color: #e65100; }

.user-bar {
  flex: 0 0 auto;
  position: relative;
  z-index: 2;
  border-top: 1px solid rgba(202, 211, 233, 0.72);
  padding: 13px 14px 15px;
  display: flex;
  align-items: center;
  background: rgba(252, 251, 255, 0.9);
}

.user-avatar-img {
  width: 36px;
  height: 36px;
  border-radius: 50%;
  object-fit: cover;
  flex-shrink: 0;
  margin-right: 10px;
  border: 2px solid rgba(255, 255, 255, 0.86);
  background: #fff;
  box-shadow: 0 3px 11px rgba(85, 105, 170, 0.13);
}

.user-avatar-fallback {
  width: 36px;
  height: 36px;
  border-radius: 50%;
  flex-shrink: 0;
  margin-right: 10px;
  background: linear-gradient(135deg, #8fa8e8, #a28bd4);
  color: #fff;
  display: grid;
  place-items: center;
  font-size: 14px;
  font-weight: 600;
}

.user-text {
  min-width: 0;
  flex: 1;
}

.user-name {
  font-size: 13px;
  font-weight: 750;
  color: #465674;
  letter-spacing: 0.025em;
  white-space: nowrap;
  overflow: hidden;
  text-overflow: ellipsis;
}

.user-plan {
  font-size: 10px;
  color: #919db7;
  margin-top: 3px;
  letter-spacing: 0.06em;
}
.user-settings {
  display: inline-flex;
  width: 62px;
  height: 34px;
  flex: 0 0 62px;
  margin-left: 8px;
  padding: 0 9px;
  align-items: center;
  justify-content: center;
  gap: 4px;
  border: 1px solid rgba(194, 204, 234, 0.78);
  border-radius: 11px;
  background: rgba(255, 255, 255, 0.6);
  color: #7e8dab;
  cursor: pointer;
  transition: background 180ms ease, border-color 180ms ease, color 180ms ease, box-shadow 180ms ease;
}
.settings-icon { font-size: 16px; line-height: 1; }
.settings-label { font-size: 12px; font-weight: 700; letter-spacing: 0.04em; line-height: 1; }
.user-settings:hover {
  border-color: rgba(143, 167, 224, 0.86);
  background: rgba(246, 245, 255, 0.94);
  color: #6078b8;
  box-shadow: 0 5px 13px rgba(96, 122, 194, 0.11);
}

@keyframes brand-glow { 50% { opacity: 0.52; transform: scale(1.11); } }
@keyframes brand-sparkle { 0%, 100% { opacity: 0.38; transform: scale(0.8) rotate(0deg); } 50% { opacity: 1; transform: scale(1.14) rotate(25deg); } }

.main {
  flex: 1;
  min-width: 0;
  display: flex;
  flex-direction: column;
  position: relative;
  background:
    radial-gradient(circle at 91% 5%, rgba(255, 218, 198, 0.15), transparent 24%),
    radial-gradient(circle at 72% 16%, rgba(183, 215, 255, 0.19), transparent 33%),
    radial-gradient(circle at 6% 86%, rgba(230, 213, 255, 0.14), transparent 27%),
    linear-gradient(145deg, #ffffff 0%, #f9fbff 52%, #fffaf8 100%);
}

.main-top {
  height: 44px;
  flex-shrink: 0;
  display: flex;
  align-items: center;
  gap: 10px;
  padding: 0 20px;
  border-bottom: 1px solid rgba(184, 202, 231, 0.22);
  background: rgba(255, 255, 255, 0.22);
  backdrop-filter: blur(14px);
}
.workflow-toggle {
  display: inline-flex;
  width: 34px;
  height: 32px;
  align-items: center;
  justify-content: center;
  margin-left: auto;
  padding: 0;
  border: 1px solid rgba(185, 198, 226, 0.48);
  border-radius: 10px;
  background: rgba(255, 255, 255, 0.42);
  color: #637493;
  font: inherit;
  font-size: 12px;
  font-weight: 700;
  letter-spacing: 0.03em;
  cursor: pointer;
  transition: border-color 180ms ease, background 180ms ease, color 180ms ease, box-shadow 180ms ease;
}
.workflow-toggle:hover,
.workflow-toggle.active {
  border-color: rgba(117, 144, 207, 0.74);
  background: rgba(243, 246, 255, 0.88);
  color: #526bb0;
  box-shadow: 0 5px 14px rgba(91, 115, 188, 0.11);
}
.workflow-toggle:focus-visible { outline: 3px solid rgba(124, 151, 219, 0.25); outline-offset: 2px; }
.workflow-toggle-icon {
  display: flex;
  width: 15px;
  height: 15px;
  gap: 3px;
  padding: 2px;
  box-sizing: border-box;
  border: 1.5px solid currentColor;
  border-radius: 4px;
}
.workflow-toggle-icon i,
.workflow-toggle-icon b {
  display: block;
  flex: 1;
  border-radius: 1px;
  background: currentColor;
  opacity: 0.82;
}

.status-pill {
  font-size: 12px;
  padding: 3px 10px;
  border-radius: 999px;
  background: #f0f0f0;
  color: #555;
}
.status-pill.done { background: #e8f5e9; color: #2e7d32; }
.status-pill.wait { background: #fff3e0; color: #e65100; }
.status-pill.error { background: #ffebee; color: #c62828; }
.status-pill.plan, .status-pill.run { background: #e3f2fd; color: #1565c0; }

.intent-tag {
  font-size: 12px;
  color: #999;
}

.center-stage {
  flex: 1;
  display: flex;
  flex-direction: column;
  align-items: center;
  justify-content: center;
  padding: 24px;
  overflow: auto;
}

.center-stage h1 {
  margin: 0 0 40px;
  font-size: 32px;
  font-weight: 400;
  color: #242424;
}

.quick-cards {
  width: min(870px, 100%);
  margin-top: 42px;
  display: grid;
  grid-template-columns: repeat(4, minmax(0, 1fr));
  gap: 12px;
}

.quick-card {
  min-width: 0;
  min-height: 116px;
  border: 1px solid rgba(195, 205, 224, .5);
  border-radius: 17px;
  background: linear-gradient(145deg, rgba(255,255,255,.97), rgba(249,251,255,.9));
  padding: 17px 14px 16px;
  text-align: center;
  color: #263754;
  cursor: pointer;
  box-shadow: 0 9px 23px rgba(66, 86, 126, .085);
  transition: border-color .2s ease, box-shadow .2s ease, transform .2s ease;
}
.quick-card:hover {
  border-color: rgba(127, 154, 211, .48);
  box-shadow: 0 10px 24px rgba(70, 97, 151, .09);
  transform: translateY(-2px);
}
.quick-card:focus-visible { outline: 3px solid rgba(115, 151, 220, .25); outline-offset: 2px; }

.quick-icon {
  display: grid;
  width: 28px;
  height: 28px;
  margin: 0 auto 10px;
  place-items: center;
  border-radius: 9px;
  color: #667fb9;
  background: linear-gradient(145deg, #edf4ff, #f8f7ff);
  font-family: Georgia, "Times New Roman", serif;
  font-size: 18px;
  font-weight: 700;
  line-height: 1;
}
.quick-icon.schedule { color:#5f83cd; background:linear-gradient(145deg,#e8f1ff,#f2efff); }
.quick-icon.quality { color:#c18b40; background:linear-gradient(145deg,#fff6df,#fff9ed); }
.quick-icon.overview { color:#6990a9; background:linear-gradient(145deg,#eaf7fb,#f1f7ff); }
.quick-icon.trace { color:#8a78b8; background:linear-gradient(145deg,#f1edff,#f7f3ff); }

.card-title { display:block; overflow:hidden; color:#33425c; font-size:16px; font-weight:650; letter-spacing:.015em; line-height:1.3; text-overflow:ellipsis; white-space:nowrap; }
.card-desc { display:block; overflow:hidden; margin-top:5px; color:#8c99ad; font-size:12px; letter-spacing:.01em; line-height:1.48; text-overflow:ellipsis; white-space:nowrap; }

.chat-stage {
  flex: 1;
  display: flex;
  flex-direction: column;
  min-height: 0;
  background: transparent;
}

.messages {
  flex: 1;
  overflow: auto;
  box-sizing: border-box;
  padding: clamp(26px, 3.2vh, 42px) clamp(22px, 5vw, 72px) 22px;
  max-width: 1060px;
  width: min(1060px, 100%);
  margin: 0 auto;
  scroll-behavior: smooth;
  scrollbar-gutter: stable;
}
.stream-waiting {
  display: inline-flex;
  align-items: center;
  gap: 8px;
  margin: 10px 0 6px 56px;
  padding: 7px 11px 7px 8px;
  color: #8190a9;
  font-size: 12px;
  letter-spacing: .02em;
  border-radius: 999px;
  background: rgba(247, 250, 255, .76);
  border: 1px solid rgba(213, 225, 245, .82);
  box-shadow: 0 6px 18px rgba(91, 119, 171, .08);
}
.stream-wait-orb {
  position: relative;
  display: inline-block;
  width: 21px;
  height: 21px;
  overflow: hidden;
  border-radius: 48% 52% 50% 50%;
  background: linear-gradient(145deg, #e8f6ff, #a9d9f3 50%, #b4a9e6);
  box-shadow: 0 2px 7px rgba(95, 131, 202, .2);
  animation: stream-wait-float 1.8s ease-in-out infinite;
}
.stream-wait-orb::before { content: ""; position: absolute; top: 3px; left: 4px; width: 7px; height: 4px; border-radius: 50%; background: rgba(255,255,255,.68); }
.stream-wait-orb i,.stream-wait-orb b { position: absolute; top: 9px; width: 2px; height: 4px; border-radius: 50%; background: #587092; }
.stream-wait-orb i { left: 7px; }.stream-wait-orb b { left: 12px; }
.stream-waiting em { display: inline-flex; align-items: center; gap: 3px; font-style: normal; }
.stream-waiting em i { width: 3px; height: 3px; border-radius: 50%; background: #9eb3d9; animation: stream-wait-dot 1s ease-in-out infinite; }
.stream-waiting em i:nth-child(2) { animation-delay: .14s; }.stream-waiting em i:nth-child(3) { animation-delay: .28s; }
.stream-wait-enter-active,.stream-wait-leave-active { transition: opacity .2s ease, transform .2s ease; }
.stream-wait-enter-from,.stream-wait-leave-to { opacity: 0; transform: translateY(4px); }
@keyframes stream-wait-float { 50% { transform: translateY(-2px) rotate(2deg); } }
@keyframes stream-wait-dot { 50% { opacity: .25; transform: translateY(-2px); } }

.msg {
  display: flex;
  align-items: flex-start;
  gap: 11px;
  max-width: 880px;
  margin-bottom: clamp(20px, 2.5vh, 30px);
}
.msg-content {
  min-width: 0;
  flex: 1;
}
.msg-avatar {
  position: relative;
  width: 42px;
  height: 42px;
  flex: 0 0 42px;
  margin-top: 1px;
}
.msg-avatar--user {
  overflow: hidden;
  border-radius: 50%;
  background: #dce7f6;
  box-shadow: 0 3px 12px rgba(60, 89, 132, 0.16);
}
.msg-avatar--user img {
  display: block;
  width: 100%;
  height: 100%;
  object-fit: cover;
}
.assistant-orb {
  position: absolute;
  inset: 0;
  display: block;
  overflow: hidden;
  border-radius: 48% 52% 50% 50% / 52% 49% 51% 48%;
  background:
    radial-gradient(circle at 30% 25%, rgba(255, 255, 255, 0.96), transparent 18%),
    radial-gradient(circle at 72% 78%, rgba(172, 151, 232, 0.42), transparent 49%),
    linear-gradient(142deg, #e7f4ff 0%, #acd8f5 43%, #9eafea 74%, #b7a5e2 100%);
  box-shadow: inset -5px -7px 10px rgba(101, 87, 181, 0.12), 0 4px 13px rgba(101, 133, 205, 0.19);
  animation: assistant-orb-float 3.8s ease-in-out infinite;
}
.assistant-orb::before {
  content: "";
  position: absolute;
  top: 5px;
  left: 7px;
  width: 13px;
  height: 7px;
  border-radius: 50%;
  background: rgba(255, 255, 255, 0.5);
  filter: blur(1px);
  transform: rotate(-28deg);
}
.assistant-orb i,
.assistant-orb b {
  position: absolute;
  top: 18px;
  z-index: 1;
  width: 4px;
  height: 7px;
  border-radius: 50%;
  background: #496484;
  box-shadow: 0 0 1px rgba(255, 255, 255, 0.55);
  animation: assistant-orb-blink 4.6s ease-in-out infinite;
}
.assistant-orb i { left: 14px; transform: rotate(7deg); }
.assistant-orb b { left: 24px; transform: rotate(-7deg); animation-delay: 45ms; }
.msg-avatar--intent .assistant-orb { background: radial-gradient(circle at 30% 25%, rgba(255,255,255,.95), transparent 18%), linear-gradient(145deg, #e9ecff, #b8c5ff 58%, #a996e4); }
.msg-avatar--intake .assistant-orb { background: radial-gradient(circle at 30% 25%, rgba(255,255,255,.95), transparent 18%), linear-gradient(145deg, #e5f8ff, #9bd8ec 55%, #92b7e2); }
.msg-avatar--bom .assistant-orb { background: radial-gradient(circle at 30% 25%, rgba(255,255,255,.95), transparent 18%), linear-gradient(145deg, #eff9ee, #a9d9bd 55%, #84b9ad); }
.msg-avatar--risk .assistant-orb { background: radial-gradient(circle at 30% 25%, rgba(255,255,255,.95), transparent 18%), linear-gradient(145deg, #fff6e6, #f2ca93 55%, #d6a77e); }
.msg-avatar--lock .assistant-orb { background: radial-gradient(circle at 30% 25%, rgba(255,255,255,.95), transparent 18%), linear-gradient(145deg, #edf7ff, #a8d2f0 55%, #8aa8da); }
.msg-avatar--schedule .assistant-orb { background: radial-gradient(circle at 30% 25%, rgba(255,255,255,.95), transparent 18%), linear-gradient(145deg, #f1efff, #c7b6ee 55%, #9da2e4); }
.msg-avatar--dispatch .assistant-orb { background: radial-gradient(circle at 30% 25%, rgba(255,255,255,.95), transparent 18%), linear-gradient(145deg, #eafaff, #9cd9e7 55%, #79adc8); }
.msg-avatar--validate .assistant-orb { background: radial-gradient(circle at 30% 25%, rgba(255,255,255,.95), transparent 18%), linear-gradient(145deg, #edfff5, #a7e2c1 55%, #78bd9f); }
.msg-avatar--issue .assistant-orb { background: radial-gradient(circle at 30% 25%, rgba(255,255,255,.95), transparent 18%), linear-gradient(145deg, #fff0f2, #efb2c1 55%, #c98ead); }
.msg-avatar--production .assistant-orb { background: radial-gradient(circle at 30% 25%, rgba(255,255,255,.95), transparent 18%), linear-gradient(145deg, #fff0e9, #f0b38e 55%, #d88769); }
.msg-avatar--quality .assistant-orb { background: radial-gradient(circle at 30% 25%, rgba(255,255,255,.95), transparent 18%), linear-gradient(145deg, #fff8df, #f0d37d 55%, #d0aa56); }
.msg-avatar--warehouse .assistant-orb { background: radial-gradient(circle at 30% 25%, rgba(255,255,255,.95), transparent 18%), linear-gradient(145deg, #e5fbff, #9adbe4 55%, #6daabd); }
.msg-avatar--trace .assistant-orb { background: radial-gradient(circle at 30% 25%, rgba(255,255,255,.95), transparent 18%), linear-gradient(145deg, #f6eeff, #ceb9ed 55%, #aa8bd2); }
.msg-avatar--equipment .assistant-orb { background: radial-gradient(circle at 30% 25%, rgba(255,255,255,.95), transparent 18%), linear-gradient(145deg, #e8f7fa, #a5ccd4 55%, #789caf); }
.msg-avatar--andon .assistant-orb { background: radial-gradient(circle at 30% 25%, rgba(255,255,255,.95), transparent 18%), linear-gradient(145deg, #fff1e7, #f2b37d 55%, #d98260); }
.msg-avatar--insight .assistant-orb { background: radial-gradient(circle at 30% 25%, rgba(255,255,255,.95), transparent 18%), linear-gradient(145deg, #edf0ff, #b8c5f2 55%, #8999dc); }
.msg-role {
  font-size: 12px;
  color: #8b95a7;
  margin: 0 0 7px 4px;
  font-weight: 600;
  letter-spacing: 0.01em;
}
.msg-role--assistant {
  display: flex;
  align-items: center;
  gap: 5px;
  min-height: 21px;
  margin-left: 2px;
}
.agent-name {
  display: inline-block;
  font-size: 17px;
  font-weight: 800;
  line-height: 1;
  letter-spacing: 0.055em;
  background: linear-gradient(103deg, #4268bd 3%, #7873d2 48%, #3598bd 100%);
  -webkit-background-clip: text;
  background-clip: text;
  color: transparent;
  text-shadow: 0 3px 12px rgba(92, 117, 207, 0.16);
  animation: agent-name-breathe 3.8s ease-in-out infinite;
}
.agent-name--subagent {
  font-size: 14px;
  letter-spacing: .035em;
  background: linear-gradient(103deg, #4e6894 3%, #6378af 48%, #4d93a9 100%);
  -webkit-background-clip: text;
  background-clip: text;
}
.agent-sparkle {
  color: #7585db;
  font-size: 13px;
  line-height: 1;
  animation: agent-sparkle 2.8s ease-in-out infinite;
}
.msg-body {
  position: relative;
  box-sizing: border-box;
  border: 1px solid rgba(148, 163, 184, 0.18);
  border-radius: 18px;
  background: rgba(255, 255, 255, 0.92);
  box-shadow: 0 8px 26px rgba(30, 41, 59, 0.055);
  color: #24334d;
  font-size: 17px;
  line-height: 1.82;
  padding: 15px 18px;
}
.msg-rendered {
  min-height: 1.75em;
  overflow-wrap: anywhere;
}
.msg.assistant .msg-body {
  min-width: min(360px, 100%);
}
.msg.assistant .msg-content {
  max-width: min(820px, calc(100% - 53px));
}
.msg--subagent .msg-body {
  border-color: rgba(117, 145, 204, .24);
  background: linear-gradient(135deg, rgba(255,255,255,.96), rgba(246,249,255,.92));
  box-shadow: 0 9px 25px rgba(76, 105, 162, .07);
}
.msg.user {
  margin-left: auto;
  flex-direction: row-reverse;
  align-items: flex-start;
  max-width: min(760px, 90%);
}
.msg.user .msg-content {
  display: flex;
  flex-direction: column;
  align-items: flex-end;
}
.msg.user .msg-role {
  margin-left: 0;
  margin-right: 4px;
}
.msg.user .msg-body {
  border-color: transparent;
  border-bottom-right-radius: 7px;
  background: linear-gradient(135deg, #7898e9, #5f94d9);
  box-shadow: 0 10px 24px rgba(80, 125, 207, 0.2);
  color: #fff;
}
.msg.user .msg-rendered { color: inherit; }
.msg-body.is-streaming::after {
  content: "";
  display: inline-block;
  width: 2px;
  height: 1.1em;
  margin-left: 3px;
  vertical-align: -0.16em;
  border-radius: 2px;
  background: #7791d9;
  animation: stream-caret 850ms ease-in-out infinite;
}
.msg-body :deep(.md-h3) {
  margin: 10px 0 7px;
  color: #263a62;
  font-size: 16px;
  font-weight: 700;
}
.msg-body :deep(.md-ul) {
  margin: 8px 0;
  padding-left: 22px;
}
.typing-indicator { display: inline-flex; align-items: center; gap: 5px; min-height: 26px; }
.typing-indicator i {
  width: 6px;
  height: 6px;
  border-radius: 50%;
  background: #8095c8;
  animation: typing-bounce 1s ease-in-out infinite;
}
.typing-indicator i:nth-child(2) { animation-delay: 130ms; }
.typing-indicator i:nth-child(3) { animation-delay: 260ms; }

@keyframes stream-caret { 50% { opacity: 0.15; } }
@keyframes typing-bounce { 50% { transform: translateY(-4px); opacity: 0.55; } }
@keyframes assistant-orb-float {
  0%, 100% { transform: translateY(0) rotate(-2deg); }
  50% { transform: translateY(-2px) rotate(2deg); }
}
@keyframes assistant-orb-blink {
  0%, 42%, 47%, 100% { transform: scaleY(1); }
  44%, 45% { transform: scaleY(0.16); }
}
@keyframes agent-name-breathe {
  0%, 100% { filter: saturate(1); transform: translateY(0); }
  50% { filter: saturate(1.15) brightness(1.05); transform: translateY(-1px); }
}
@keyframes agent-sparkle {
  0%, 100% { opacity: 0.45; transform: scale(0.78) rotate(0deg); }
  50% { opacity: 1; transform: scale(1.12) rotate(24deg); }
}

.composer-dock {
  flex-shrink: 0;
  padding: 14px clamp(18px, 4vw, 58px) clamp(18px, 2.6vh, 28px);
  border-top: 1px solid rgba(166, 184, 218, 0.12);
  background: linear-gradient(to top, rgba(255, 253, 252, 0.78) 72%, rgba(255, 255, 255, 0.1));
  box-shadow: 0 -14px 30px rgba(244, 247, 255, 0.22);
  backdrop-filter: blur(10px);
  display: flex;
  justify-content: center;
}

@media (max-width: 900px) {
  .sidebar { width: 230px; }
  .messages { padding-inline: 24px; }
  .quick-cards { grid-template-columns: repeat(2, minmax(0, 1fr)); max-width: 620px; }
}

@media (max-width: 700px) {
  .sidebar { display: none; }
  .main-top { padding-inline: 16px; }
  .messages { padding: 20px 14px 14px; }
  .msg { max-width: 100%; }
  .msg-avatar { width: 38px; height: 38px; flex-basis: 38px; }
  .msg.assistant .msg-body { min-width: 0; }
  .msg.assistant .msg-content { max-width: calc(100% - 49px); }
  .msg.user { max-width: 92%; }
  .msg-body { font-size: 16px; }
  .composer-dock { padding-inline: 12px; }
  .quick-cards { grid-template-columns: 1fr; gap: 10px; margin-top: 28px; }
  .quick-card { min-height: 94px; }
}
</style>
