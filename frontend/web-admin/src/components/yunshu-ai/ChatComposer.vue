<template>
  <div
    class="composer-card"
    :class="{ focused, 'menus-up': menuPlacement === 'up' }"
  >
    <div class="composer-body">
      <textarea
        ref="textareaRef"
        :value="modelValue"
        class="composer-input"
        :placeholder="placeholder"
        rows="1"
        @input="onInput"
        @focus="focused = true"
        @blur="onBlur"
        @keydown.enter.exact.prevent="$emit('send')"
      />
    </div>

    <div class="composer-toolbar">
      <div class="toolbar-left">
        <button type="button" class="tool-btn" title="添加附件" @click="pickFile">
          <svg viewBox="0 0 24 24" aria-hidden="true">
            <path d="M12 5v14M5 12h14" />
          </svg>
        </button>
        <input ref="fileRef" type="file" class="hidden-file" multiple @change="onFiles" />

        <div ref="approvalRef" class="menu-anchor">
          <button
            type="button"
            class="pill-trigger"
            :class="{ open: approvalOpen }"
            @click.stop="toggleApproval"
          >
            <span class="status-dot pill-dot" :class="approvalMode"></span>
            <span class="pill-text">{{ currentApprovalLabel }}</span>
            <svg class="chevron" viewBox="0 0 24 24" :class="{ open: approvalOpen }">
              <path d="M6 9l6 6 6-6" />
            </svg>
          </button>

          <Transition name="menu-fade">
            <div v-if="approvalOpen" class="menu-panel approval-panel" @click.stop>
              <button
                v-for="opt in approvalOptions"
                :key="opt.key"
                type="button"
                class="menu-option menu-option--plain menu-option--approval"
                :class="{ selected: approvalMode === opt.key }"
                @click="selectApproval(opt.key)"
              >
                <span class="option-leading">
                  <span class="status-dot" :class="opt.key"></span>
                  <span class="option-title">{{ opt.label }}</span>
                </span>
                <span class="option-check" aria-hidden="true">
                  <svg v-if="approvalMode === opt.key" viewBox="0 0 24 24"><path d="M5 12l5 5L20 7"/></svg>
                </span>
              </button>
            </div>
          </Transition>
        </div>
      </div>

      <div class="toolbar-spacer"></div>

      <div class="toolbar-right">
        <div ref="modelRef" class="menu-anchor">
          <button type="button" class="pill-trigger model-trigger" @click.stop="toggleModel">
            <span class="pill-text">{{ currentModelShort }}</span>
            <svg class="chevron" viewBox="0 0 24 24" :class="{ open: modelOpen }">
              <path d="M6 9l6 6 6-6" />
            </svg>
          </button>

          <Transition name="menu-fade">
            <div v-if="modelOpen" class="menu-panel model-panel" @click.stop>
              <button
                v-for="m in models"
                :key="m.key"
                type="button"
                class="menu-option menu-option--plain menu-option--model"
                :class="{ selected: selectedModel === m.key }"
                @click="selectModel(m.key)"
              >
                <span class="option-title">{{ m.label }}</span>
                <span class="option-check" aria-hidden="true">
                  <svg v-if="selectedModel === m.key" viewBox="0 0 24 24"><path d="M5 12l5 5L20 7"/></svg>
                </span>
              </button>
            </div>
          </Transition>
        </div>

        <button type="button" class="tool-btn mic" :class="{ listening }" title="语音输入" @click="toggleVoice">
          <svg viewBox="0 0 24 24" aria-hidden="true">
            <path d="M12 3a3 3 0 0 0-3 3v6a3 3 0 0 0 6 0V6a3 3 0 0 0-3-3z" />
            <path d="M19 10v2a7 7 0 0 1-14 0v-2" />
            <path d="M12 19v3" />
          </svg>
        </button>

        <button
          type="button"
          class="send-btn"
          :disabled="disabled || !modelValue.trim()"
          aria-label="发送"
          @click="$emit('send')"
        >
          <svg viewBox="0 0 24 24" aria-hidden="true">
            <path d="M12 19V5" />
            <path d="M6.5 10.5L12 5l5.5 5.5" />
          </svg>
        </button>
      </div>
    </div>
  </div>
</template>

<script setup>
import { computed, onBeforeUnmount, onMounted, ref, watch } from "vue";

const props = defineProps({
  modelValue: { type: String, default: "" },
  selectedModel: { type: String, default: "deepseek" },
  approvalMode: { type: String, default: "risk_only" },
  models: { type: Array, default: () => [] },
  placeholder: { type: String, default: "随心输入" },
  disabled: { type: Boolean, default: false },
  /** down = 欢迎页居中时向下展开；up = 底部输入时向上展开 */
  menuPlacement: { type: String, default: "down", validator: (v) => ["down", "up"].includes(v) },
});

const emit = defineEmits([
  "update:modelValue",
  "update:selectedModel",
  "update:approvalMode",
  "send",
  "attach",
]);

const focused = ref(false);
const approvalOpen = ref(false);
const modelOpen = ref(false);
const listening = ref(false);
const textareaRef = ref(null);
const fileRef = ref(null);
const approvalRef = ref(null);
const modelRef = ref(null);
let recognition = null;

const approvalOptions = [
  { key: "always_ask", label: "请求批准" },
  { key: "risk_only", label: "替我审批" },
];

const currentApprovalLabel = computed(() => {
  const o = approvalOptions.find((x) => x.key === props.approvalMode);
  return o ? o.label : "请求批准";
});

const currentModel = computed(() => props.models.find((m) => m.key === props.selectedModel));

const currentModelShort = computed(() => {
  const m = currentModel.value;
  if (!m) return "模型";
  return m.label || m.short;
});

function onInput(e) {
  emit("update:modelValue", e.target.value);
  autoResize();
}

function autoResize() {
  const el = textareaRef.value;
  if (!el) return;
  el.style.height = "auto";
  el.style.height = `${Math.min(el.scrollHeight, 180)}px`;
}

watch(() => props.modelValue, () => autoResize());

function onBlur() {
  focused.value = false;
}

function closeMenus() {
  approvalOpen.value = false;
  modelOpen.value = false;
}

function toggleApproval() {
  approvalOpen.value = !approvalOpen.value;
  modelOpen.value = false;
}

function toggleModel() {
  modelOpen.value = !modelOpen.value;
  approvalOpen.value = false;
}

function selectApproval(key) {
  emit("update:approvalMode", key);
  closeMenus();
}

function selectModel(key) {
  emit("update:selectedModel", key);
  closeMenus();
}

function pickFile() {
  fileRef.value?.click();
}

function onFiles(e) {
  const files = Array.from(e.target.files || []);
  if (files.length) emit("attach", files);
  e.target.value = "";
}

function toggleVoice() {
  const SR = window.SpeechRecognition || window.webkitSpeechRecognition;
  if (!SR) {
    alert("当前浏览器不支持语音输入");
    return;
  }
  if (listening.value && recognition) {
    recognition.stop();
    return;
  }
  recognition = new SR();
  recognition.lang = "zh-CN";
  recognition.interimResults = false;
  recognition.onstart = () => { listening.value = true; };
  recognition.onend = () => { listening.value = false; };
  recognition.onerror = () => { listening.value = false; };
  recognition.onresult = (ev) => {
    const text = ev.results?.[0]?.[0]?.transcript || "";
    if (text) emit("update:modelValue", props.modelValue ? `${props.modelValue}${text}` : text);
  };
  recognition.start();
}

function onDocClick() {
  closeMenus();
}

onMounted(() => {
  document.addEventListener("click", onDocClick);
  autoResize();
});

onBeforeUnmount(() => {
  document.removeEventListener("click", onDocClick);
  if (recognition) recognition.stop();
});
</script>

<style scoped>
.composer-card {
  --card-radius: 24px;
  --border: rgba(15, 23, 42, 0.08);
  --shadow-rest:
    0 1px 2px rgba(15, 23, 42, 0.04),
    0 8px 24px rgba(15, 23, 42, 0.06),
    0 24px 48px rgba(15, 23, 42, 0.04);
  --shadow-focus:
    0 2px 4px rgba(15, 23, 42, 0.05),
    0 12px 32px rgba(15, 23, 42, 0.08),
    0 28px 56px rgba(15, 23, 42, 0.06);

  width: min(840px, 100%);
  border-radius: var(--card-radius);
  background: #ffffff;
  border: 1px solid var(--border);
  box-shadow: var(--shadow-rest);
  display: flex;
  flex-direction: column;
  overflow: visible;
  transition: box-shadow 0.22s ease, border-color 0.22s ease, transform 0.22s ease;
}

.composer-card.focused {
  border-color: rgba(15, 23, 42, 0.12);
  box-shadow: var(--shadow-focus);
  transform: translateY(-1px);
}

.composer-body {
  flex: 1;
  min-height: 64px;
  padding: 20px 28px 0;
  box-sizing: border-box;
}

.composer-input {
  width: 100%;
  min-height: 28px;
  max-height: 180px;
  border: none;
  resize: none;
  padding: 0;
  margin: 0;
  font-size: 16px;
  line-height: 1.55;
  font-family: inherit;
  color: #0f172a;
  background: transparent;
  outline: none;
  box-shadow: none;
  display: block;
  appearance: none;
  -webkit-appearance: none;
}

.composer-input:focus,
.composer-input:focus-visible {
  border: none;
  outline: none;
  box-shadow: none;
}

.composer-input::placeholder {
  color: #94a3b8;
}

.composer-toolbar {
  display: flex;
  align-items: center;
  gap: 10px;
  padding: 6px 20px 12px;
  flex-shrink: 0;
}

.toolbar-left,
.toolbar-right {
  display: flex;
  align-items: center;
  gap: 6px;
  min-width: 0;
}

.toolbar-spacer {
  flex: 1;
  min-width: 8px;
}

.hidden-file {
  display: none;
}

.tool-btn {
  width: 38px;
  height: 38px;
  border: 0;
  background: transparent;
  color: #64748b;
  border-radius: 10px;
  display: grid;
  place-items: center;
  cursor: pointer;
  padding: 0;
  flex-shrink: 0;
  transition: background 0.15s, color 0.15s;
}

.tool-btn:hover {
  background: #f1f5f9;
  color: #334155;
}

.tool-btn svg {
  width: 18px;
  height: 18px;
  stroke: currentColor;
  stroke-width: 1.8;
  fill: none;
  stroke-linecap: round;
}

.tool-btn.mic.listening {
  color: #dc2626;
  background: #fef2f2;
}

.pill-trigger {
  height: 38px;
  padding: 0 12px 0 10px;
  border: 1px solid transparent;
  border-radius: 10px;
  background: transparent;
  color: #475569;
  font-size: 14px;
  font-family: inherit;
  cursor: pointer;
  display: inline-flex;
  align-items: center;
  gap: 6px;
  max-width: 180px;
  transition: background 0.15s, border-color 0.15s, color 0.15s;
}

.pill-trigger:hover,
.pill-trigger.open {
  background: #f8fafc;
  border-color: #e2e8f0;
  color: #0f172a;
}

.pill-trigger.active {
  color: #0f172a;
}

.pill-icon {
  width: 22px;
  height: 22px;
  border-radius: 7px;
  background: #f1f5f9;
  display: grid;
  place-items: center;
  flex-shrink: 0;
  color: #64748b;
}

.status-dot {
  width: 8px;
  height: 8px;
  border-radius: 50%;
  flex-shrink: 0;
}

.status-dot.always_ask {
  background: #22c55e;
  box-shadow: 0 0 0 2px rgba(34, 197, 94, 0.2);
}

.status-dot.risk_only {
  background: #eab308;
  box-shadow: 0 0 0 2px rgba(234, 179, 8, 0.22);
}

.pill-dot {
  width: 7px;
  height: 7px;
}

.pill-icon svg {
  width: 14px;
  height: 14px;
}

.pill-text {
  white-space: nowrap;
  overflow: hidden;
  text-overflow: ellipsis;
}

.model-trigger {
  max-width: 220px;
  font-size: 15px;
}

.model-trigger .pill-text {
  font-size: 15px;
  font-weight: 500;
  color: #0f172a;
}

.chevron {
  width: 14px;
  height: 14px;
  stroke: currentColor;
  stroke-width: 2;
  fill: none;
  stroke-linecap: round;
  stroke-linejoin: round;
  flex-shrink: 0;
  opacity: 0.55;
  transition: transform 0.18s ease;
}

.chevron.open {
  transform: rotate(180deg);
}

.menu-anchor {
  position: relative;
}

.menu-panel {
  position: absolute;
  left: 0;
  z-index: 200;
  min-width: 300px;
  background: #ffffff;
  border: 1px solid rgba(15, 23, 42, 0.08);
  border-radius: 16px;
  padding: 8px;
  box-shadow:
    0 0 0 1px rgba(255, 255, 255, 0.8) inset,
    0 4px 16px rgba(15, 23, 42, 0.08),
    0 20px 48px rgba(15, 23, 42, 0.12);
}

/* 欢迎页：向下展开 */
.composer-card:not(.menus-up) .menu-panel {
  top: calc(100% + 10px);
  bottom: auto;
}

/* 底部输入：向上展开 */
.composer-card.menus-up .menu-panel {
  bottom: calc(100% + 10px);
  top: auto;
}

.model-panel {
  right: 0;
  left: auto;
  width: 182px;
  min-width: unset;
  max-width: 182px;
  box-sizing: border-box;
  padding: 5px;
}

.menu-option.menu-option--model {
  padding: 9px 5px 9px 10px;
}

.menu-option.menu-option--model .option-title {
  font-size: 15px;
  font-weight: 400;
  color: #334155;
  -webkit-font-smoothing: antialiased;
}

.menu-option.menu-option--model.selected .option-title {
  font-weight: 600;
  color: #0f172a;
}

.menu-option.menu-option--model .option-check svg {
  width: 15px;
  height: 15px;
}

.approval-panel {
  min-width: unset;
  width: 148px;
  max-width: 148px;
  box-sizing: border-box;
  padding: 3px;
}

.menu-option--approval .option-leading {
  display: flex;
  align-items: center;
  gap: 8px;
  min-width: 0;
  overflow: hidden;
}

.menu-option--approval .option-title {
  font-size: 14px;
  font-weight: 400;
  color: #334155;
  white-space: nowrap;
  line-height: 1;
}

.menu-option--approval.selected .option-title {
  font-weight: 600;
  color: #0f172a;
}

.menu-option.menu-option--plain {
  display: grid;
  grid-template-columns: minmax(0, 1fr) 16px;
  align-items: center;
  column-gap: 4px;
  width: 100%;
  min-width: 0;
  box-sizing: border-box;
  padding: 7px 4px 7px 8px;
}

.menu-option--plain .option-title {
  font-size: 14px;
  font-weight: 400;
  color: #334155;
  white-space: nowrap;
  line-height: 1;
  text-align: left;
  overflow: hidden;
  text-overflow: ellipsis;
  justify-self: start;
}

.menu-option--plain.selected .option-title {
  font-weight: 600;
  color: #0f172a;
}

.menu-option--plain .option-check {
  width: 16px;
  height: 16px;
  justify-self: end;
  display: flex;
  align-items: center;
  justify-content: flex-end;
}

.menu-option--plain .option-check svg {
  width: 14px;
  height: 14px;
  stroke: #0f172a;
  stroke-width: 2.2;
  fill: none;
  stroke-linecap: round;
  stroke-linejoin: round;
}

.menu-panel-head {
  padding: 6px 12px 8px;
  font-size: 11px;
  font-weight: 600;
  letter-spacing: 0.06em;
  text-transform: uppercase;
  color: #94a3b8;
}

.menu-option {
  width: 100%;
  border: none;
  border-radius: 12px;
  background: transparent;
  cursor: pointer;
  text-align: left;
  padding: 10px 12px;
  display: grid;
  grid-template-columns: 36px 1fr 20px;
  gap: 10px;
  align-items: center;
  font-family: inherit;
  transition: background 0.15s;
}

.menu-option:hover {
  background: #f8fafc;
}

.menu-option.selected {
  background: #f1f5f9;
}

.option-icon {
  width: 36px;
  height: 36px;
  border-radius: 10px;
  display: grid;
  place-items: center;
  flex-shrink: 0;
}

.option-icon.always_ask {
  background: #eff6ff;
  color: #2563eb;
}

.option-icon.risk_only {
  background: #f0fdf4;
  color: #16a34a;
}

.option-icon.model {
  background: linear-gradient(135deg, #f8fafc, #e2e8f0);
  color: #334155;
}

.option-icon svg {
  width: 18px;
  height: 18px;
}

.model-dot {
  font-size: 13px;
  font-weight: 700;
  letter-spacing: -0.02em;
}

.option-copy {
  min-width: 0;
  display: flex;
  flex-direction: column;
  gap: 2px;
}

.option-title {
  font-size: 14px;
  font-weight: 400;
  color: #0f172a;
  line-height: 1.3;
}

.option-desc {
  font-size: 12px;
  color: #64748b;
  line-height: 1.4;
}

.option-check {
  width: 20px;
  height: 20px;
  display: grid;
  place-items: center;
}

.option-check svg {
  width: 16px;
  height: 16px;
  stroke: #0f172a;
  stroke-width: 2.2;
  fill: none;
  stroke-linecap: round;
  stroke-linejoin: round;
}

.send-btn {
  width: 40px;
  height: 40px;
  border: 0;
  border-radius: 50%;
  background: #1e293b;
  color: #fff;
  display: grid;
  place-items: center;
  cursor: pointer;
  flex-shrink: 0;
  box-shadow: 0 2px 8px rgba(15, 23, 42, 0.18);
  transition: background 0.15s, transform 0.12s, box-shadow 0.15s;
}

.send-btn:hover:not(:disabled) {
  background: #0f172a;
  transform: translateY(-1px);
  box-shadow: 0 4px 12px rgba(15, 23, 42, 0.22);
}

.send-btn:disabled {
  background: #cbd5e1;
  box-shadow: none;
  cursor: not-allowed;
}

.send-btn svg {
  width: 20px;
  height: 20px;
  stroke: currentColor;
  stroke-width: 2.1;
  fill: none;
  stroke-linecap: round;
  stroke-linejoin: round;
}

.menu-fade-enter-active,
.menu-fade-leave-active {
  transition: opacity 0.16s ease, transform 0.16s ease;
}

.composer-card:not(.menus-up) .menu-fade-enter-from,
.composer-card:not(.menus-up) .menu-fade-leave-to {
  opacity: 0;
  transform: translateY(-6px);
}

.composer-card.menus-up .menu-fade-enter-from,
.composer-card.menus-up .menu-fade-leave-to {
  opacity: 0;
  transform: translateY(6px);
}
</style>
