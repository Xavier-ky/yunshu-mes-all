<template>
  <aside
    v-if="visible"
    ref="hostRef"
    class="ys-companion"
    :class="{
      'is-open': open,
      'is-restoring': restoring,
      'is-thinking': isReplying,
      'has-notice': hasNotice,
    }"
    aria-label="云枢小智陪伴助手"
  >
    <Transition name="ys-chat-panel">
      <section
        v-if="open"
        id="ys-companion-chat"
        class="ys-chat"
        role="dialog"
        aria-modal="false"
        aria-labelledby="ys-chat-title"
      >
        <header class="ys-chat__topbar">
          <div class="ys-chat__brand">
            <span class="ys-chat__brand-mark" aria-hidden="true"></span>
            <span>云枢小智</span>
          </div>
          <div class="ys-chat__tools">
            <button type="button" aria-label="新建对话" title="新建对话" @click="startNewConversation">
              <svg viewBox="0 0 24 24" aria-hidden="true"><path d="M12 5v14M5 12h14" /></svg>
            </button>
            <button type="button" aria-label="关闭问答面板" title="关闭" @click="close">
              <svg viewBox="0 0 24 24" aria-hidden="true"><path d="m6 6 12 12M18 6 6 18" /></svg>
            </button>
          </div>
        </header>

        <main ref="conversationRef" class="ys-chat__content">
          <template v-if="messages.length === 0">
            <section class="ys-chat__hero">
              <div class="ys-chat__hero-copy">
                <p class="ys-chat__hello">hi ~</p>
                <h2 id="ys-chat-title">我是云枢小智<span>✦</span></h2>
                <p>遇到生产、质量或现场问题，直接告诉我就好。</p>
              </div>
              <div class="ys-chat__mascot" aria-hidden="true">
                <i></i><b></b>
              </div>
            </section>

            <section class="ys-chat__suggestions" aria-label="推荐问题">
              <button
                v-for="prompt in currentPrompts"
                :key="prompt"
                type="button"
                @click="ask(prompt)"
              >
                <span>{{ prompt }}</span>
                <svg viewBox="0 0 24 24" aria-hidden="true"><path d="m9 5 7 7-7 7" /></svg>
              </button>
            </section>

            <button type="button" class="ys-chat__refresh" @click="nextPromptSet">
              <svg viewBox="0 0 24 24" aria-hidden="true"><path d="M20 11a8 8 0 1 0 2 5.5M20 4v7h-7" /></svg>
              换一批
            </button>
          </template>

          <section v-else class="ys-chat__thread" aria-live="polite">
            <div class="ys-chat__thread-title">
              <span>本次对话</span>
              <button type="button" @click="startNewConversation">重新开始</button>
            </div>
            <article
              v-for="message in messages"
              :key="message.id"
              class="ys-chat__message"
              :class="`is-${message.role}`"
            >
              <div v-if="message.role === 'assistant'" class="ys-chat__message-avatar" aria-hidden="true">智</div>
              <p>{{ message.content }}</p>
            </article>
            <article v-if="isReplying && !messages.some((message) => message.role === 'assistant')" class="ys-chat__message is-assistant is-loading" aria-label="云枢小智正在思考">
              <div class="ys-chat__message-avatar" aria-hidden="true">智</div>
              <p><i></i><i></i><i></i></p>
            </article>
          </section>
        </main>

        <form class="ys-chat__composer" @submit.prevent="submitQuestion">
          <textarea
            ref="composerRef"
            v-model="draft"
            rows="3"
            maxlength="500"
            placeholder="说说你遇到的情况…"
            aria-label="输入想咨询的问题"
            @keydown.enter.exact.prevent="submitQuestion"
          ></textarea>
          <div class="ys-chat__composer-actions">
            <button type="button" class="ys-chat__add" aria-label="附件功能即将开放" title="附件功能即将开放">
              <svg viewBox="0 0 24 24" aria-hidden="true"><path d="M12 5v14M5 12h14" /></svg>
            </button>
            <div class="ys-chat__model-picker" role="group" aria-label="选择回答模型">
              <button
                type="button"
                :class="{ 'is-active': selectedModel === 'qwen3' }"
                :disabled="isReplying"
                @click="selectedModel = 'qwen3'"
              >Qwen3</button>
              <button
                type="button"
                :class="{ 'is-active': selectedModel === 'qwen3l' }"
                :disabled="isReplying"
                @click="selectedModel = 'qwen3l'"
              >Qwen3L</button>
            </div>
            <span>{{ draft.length }}/500</span>
            <button
              type="submit"
              class="ys-chat__send"
              :disabled="!draft.trim() || isReplying"
              aria-label="发送问题"
            >
              <svg viewBox="0 0 24 24" aria-hidden="true"><path d="m12 19 0-14M6 11l6-6 6 6" /></svg>
            </button>
          </div>
        </form>
        <p class="ys-chat__disclaimer">云枢小智 · 回答仅供参考</p>
      </section>
    </Transition>

    <button
      type="button"
      class="ys-companion__orb"
      :class="shapeClass"
      :style="lookStyle"
      :aria-expanded="open"
      aria-controls="ys-companion-chat"
      title="打开云枢小智"
      @click="toggle"
      @mouseenter="onMouseEnter"
      @mouseleave="onMouseLeave"
    >
      <span class="ys-companion__orb-bg" aria-hidden="true"></span>
      <span class="ys-companion__halo" aria-hidden="true"></span>
      <span class="ys-companion__glow" aria-hidden="true"></span>
      <span class="ys-companion__eye ys-companion__eye--left" aria-hidden="true"></span>
      <span class="ys-companion__eye ys-companion__eye--right" aria-hidden="true"></span>
      <span v-if="hasNotice" class="ys-companion__notice" aria-label="有新的提示"></span>
      <span class="ys-companion__sr">打开云枢小智陪伴助手</span>
    </button>
  </aside>
</template>

<script setup>
import { computed, nextTick, onBeforeUnmount, onMounted, reactive, ref, watch } from "vue";
import { useRoute } from "vue-router";
import { streamCompanionReply } from "@/api/companion";

const route = useRoute();
const hostRef = ref(null);
const composerRef = ref(null);
const conversationRef = ref(null);
const open = ref(false);
const restoring = ref(false);
const isReplying = ref(false);
const hasNotice = ref(true);
const draft = ref("");
const messages = ref([]);
const sessionId = ref(null);
const selectedModel = ref("qwen3");
const promptSetIndex = ref(1);
const currentShape = ref("");

const IDLE_EYE_LOOK = Object.freeze({ x: -2.1, y: -1.65 });
const eyeX = ref(IDLE_EYE_LOOK.x);
const eyeY = ref(IDLE_EYE_LOOK.y);
let restoreTimer;
let eyeLookTimer;
let idleEyeTimer;
let scrollFrame;
let nextLookAt = Date.now() + randomLookDelay();

const promptSets = [
  [
    "为什么生产任务进度滞后？",
    "当前订单的物料齐套情况如何？",
    "哪些设备需要优先点检？",
    "今天还有哪些未关闭安灯？",
  ],
  [
    "帮我查看今日生产概览",
    "质量任务有没有积压？",
    "库存不足的常见原因是什么？",
    "如何快速定位一张产品 SN？",
  ],
];

const LIVE_PRESET_IDS = Object.freeze({
  "帮我查看今日生产概览": "today-production-overview",
  "质量任务有没有积压？": "quality-task-backlog",
});

const visible = computed(() => !route.meta.fullBleed || route.path === "/app/yunshu-ai");
const currentPrompts = computed(() => promptSets[promptSetIndex.value]);
const shapeClass = computed(() => currentShape.value ? `shape-${currentShape.value}` : "");
const lookStyle = computed(() => ({
  "--look-x": `${eyeX.value}px`,
  "--look-y": `${eyeY.value}px`,
}));

function randomLookDelay() {
  return 2600 + Math.random() * 2600;
}

function randomIdleDelay() {
  return 700 + Math.random() * 750;
}

function resetEyeLook() {
  eyeX.value = IDLE_EYE_LOOK.x;
  eyeY.value = IDLE_EYE_LOOK.y;
}

function clearTimer(timer) {
  if (timer) window.clearTimeout(timer);
}

function clearEyeTimers() {
  clearTimer(eyeLookTimer);
  clearTimer(idleEyeTimer);
  eyeLookTimer = undefined;
  idleEyeTimer = undefined;
}

function scheduleIdleEyeMotion() {
  clearTimer(idleEyeTimer);
  if (open.value || isReplying.value || eyeLookTimer) return;
  idleEyeTimer = window.setTimeout(() => {
    eyeX.value = IDLE_EYE_LOOK.x - 0.85 + Math.random() * 1.3;
    eyeY.value = IDLE_EYE_LOOK.y - 0.7 + Math.random() * 1.25;
    idleEyeTimer = undefined;
    scheduleIdleEyeMotion();
  }, randomIdleDelay());
}

function moveEyesTowardPointer(event) {
  const rect = hostRef.value?.getBoundingClientRect();
  if (!rect) return;
  const dx = event.clientX - (rect.left + rect.width / 2);
  const dy = event.clientY - (rect.top + rect.height / 2);
  const distance = Math.hypot(dx, dy);
  if (distance < 5) return;
  const maxOffset = 3.2;
  eyeX.value = (dx / distance) * maxOffset;
  eyeY.value = (dy / distance) * maxOffset;
}

function beginPointerLook(event) {
  moveEyesTowardPointer(event);
  clearEyeTimers();
  eyeLookTimer = window.setTimeout(() => {
    resetEyeLook();
    eyeLookTimer = undefined;
    scheduleIdleEyeMotion();
  }, 1450);
  nextLookAt = Date.now() + randomLookDelay();
}

function onMouseEnter() {
  if (!open.value) currentShape.value = "square";
}

function onMouseLeave() {
  if (!open.value) currentShape.value = "";
}

function clearRestoreTimer() {
  clearTimer(restoreTimer);
  restoreTimer = undefined;
}

function toggle() {
  clearRestoreTimer();
  if (open.value) {
    close();
    return;
  }
  currentShape.value = "square";
  restoring.value = false;
  open.value = true;
  hasNotice.value = false;
  clearEyeTimers();
  resetEyeLook();
  restoreTimer = window.setTimeout(() => {
    currentShape.value = "";
    restoring.value = true;
    restoreTimer = undefined;
  }, 180);
  nextTick(() => composerRef.value?.focus());
}

function close() {
  clearRestoreTimer();
  open.value = false;
  restoring.value = false;
  currentShape.value = "";
  resetEyeLook();
  nextLookAt = Date.now() + randomLookDelay();
  scheduleIdleEyeMotion();
}

function nextPromptSet() {
  promptSetIndex.value = (promptSetIndex.value + 1) % promptSets.length;
}

function startNewConversation() {
  messages.value = [];
  sessionId.value = null;
  draft.value = "";
  nextTick(() => composerRef.value?.focus());
}

function makeLocalReply(question) {
  return `已收到“${question}”。问答能力正在完善中；模型服务接入后，我会结合当前页面与授权数据，为你提供可追溯的分析。`;
}

function wait(milliseconds) {
  return new Promise((resolve) => window.setTimeout(resolve, milliseconds));
}

function makeUnavailableReply() {
  return "本地模型暂时不可用。你的问题没有被当作普通回复处理，请稍后重试或检查小精灵服务。";
}

function sanitizeCompanionText(text) {
  return text
    .replace(/[\*`_]/g, "")
    .replace(/#{1,6}(?=\s|$)/g, "")
    .replace(/(^|\n)>\s?/g, "$1")
    .replace(/\[([^\]]+)\]\([^)]*\)/g, "$1");
}

function createSmoothStreamWriter(onText) {
  let queue = "";
  let timer;
  let drainResolver;

  const schedule = () => {
    if (timer || !queue) return;
    const queueLength = Array.from(queue).length;
    const batchSize = queueLength > 160 ? 5 : queueLength > 80 ? 3 : 1;
    const delay = queueLength > 160 ? 8 : queueLength > 80 ? 12 : 20;
    timer = window.setTimeout(() => {
      timer = undefined;
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
      queue += text;
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
      timer = undefined;
      queue = "";
      drainResolver?.();
      drainResolver = undefined;
    },
  };
}

async function ask(question) {
  const content = question.trim();
  if (!content || isReplying.value) return;
  messages.value.push({ id: `${Date.now()}-user`, role: "user", content });
  draft.value = "";
  isReplying.value = true;
  await nextTick();
  scrollToLatest();

  const context = {
    message: content,
    session_id: sessionId.value,
    preset_id: LIVE_PRESET_IDS[content],
    model_key: selectedModel.value,
    route_path: route.path,
    route_title: route.meta.title || "当前页面",
  };
  let answer = "";
  let assistantMessage;
  const ensureAssistantMessage = () => {
    if (assistantMessage) return assistantMessage;
    assistantMessage = reactive({
      id: `${Date.now()}-assistant`,
      role: "assistant",
      content: "",
    });
    messages.value.push(assistantMessage);
    return assistantMessage;
  };
  const writer = createSmoothStreamWriter((text) => {
    answer += text;
    ensureAssistantMessage().content = answer;
    scheduleScrollToLatest();
  });

  try {
    await streamCompanionReply(context, {
      onMeta: (payload) => {
        if (payload.session_id) sessionId.value = payload.session_id;
      },
      onDelta: (chunk) => {
        writer.push(sanitizeCompanionText(chunk));
      },
      onDone: (payload) => {
        if (payload.session_id) sessionId.value = payload.session_id;
      },
    });
    await writer.drain();
    if (!answer) ensureAssistantMessage().content = makeUnavailableReply();
  } catch {
    writer.dispose();
    ensureAssistantMessage().content = makeUnavailableReply();
  } finally {
    isReplying.value = false;
  }
  await nextTick();
  scrollToLatest();
}

function submitQuestion() {
  ask(draft.value);
}

function scrollToLatest() {
  const container = conversationRef.value;
  if (container) container.scrollTo({ top: container.scrollHeight, behavior: "smooth" });
}

function scheduleScrollToLatest() {
  if (scrollFrame) return;
  scrollFrame = window.requestAnimationFrame(() => {
    scrollFrame = undefined;
    scrollToLatest();
  });
}

function handleMouseMove(event) {
  if (!hostRef.value || open.value || isReplying.value) return;
  if (Date.now() >= nextLookAt) {
    beginPointerLook(event);
  } else if (eyeLookTimer) {
    moveEyesTowardPointer(event);
  }
}

function onDocumentPointerDown(event) {
  if (open.value && hostRef.value && !hostRef.value.contains(event.target)) close();
}

function onKeydown(event) {
  if (event.key === "Escape") close();
}

watch(() => route.fullPath, () => close());

onMounted(() => {
  scheduleIdleEyeMotion();
  document.addEventListener("pointerdown", onDocumentPointerDown);
  document.addEventListener("keydown", onKeydown);
  document.addEventListener("mousemove", handleMouseMove);
});

onBeforeUnmount(() => {
  clearRestoreTimer();
  clearEyeTimers();
  if (scrollFrame) window.cancelAnimationFrame(scrollFrame);
  document.removeEventListener("pointerdown", onDocumentPointerDown);
  document.removeEventListener("keydown", onKeydown);
  document.removeEventListener("mousemove", handleMouseMove);
});
</script>
