<template>
  <div class="visited-tabs-bar" aria-label="已访问页面快捷切换">
    <div class="visited-tabs-brand" aria-hidden="true">
      <Layers3 :size="17" :stroke-width="1.8" />
    </div>

    <button
      class="tabs-arrow"
      type="button"
      title="向左滚动"
      aria-label="向左滚动页签"
      @click="scrollTabs(-240)"
    >
      <ChevronLeft :size="18" />
    </button>

    <div ref="scroller" class="tabs-scroller" role="tablist" @wheel.passive="onWheel">
      <div
        v-for="tab in tagsView.tabs"
        :key="tab.key"
        :ref="(el) => setTabRef(tab.key, el)"
        class="visited-tab"
        :class="{ active: isActive(tab), affix: tab.affix }"
        @mousedown.middle.prevent="closeTab(tab)"
        @contextmenu.prevent="openTabMenu($event, tab)"
      >
        <button
          type="button"
          class="tab-open"
          role="tab"
          :title="tab.title"
          :aria-selected="isActive(tab)"
          @click="openTab(tab)"
        >
          <Home v-if="tab.affix" :size="15" :stroke-width="1.8" class="tab-home" />
          <span class="tab-title">{{ tab.title }}</span>
        </button>
        <button
          v-if="!tab.affix"
          type="button"
          class="tab-close"
          :aria-label="`关闭${tab.title}`"
          @click="closeTab(tab)"
        >
          <X :size="14" :stroke-width="2" />
        </button>
      </div>
    </div>

    <button
      class="tabs-arrow"
      type="button"
      title="向右滚动"
      aria-label="向右滚动页签"
      @click="scrollTabs(240)"
    >
      <ChevronRight :size="18" />
    </button>

    <div class="tabs-tools">
      <button type="button" class="tabs-tool" title="刷新当前页面" aria-label="刷新当前页面" @click="refreshPage">
        <RotateCw :size="17" :stroke-width="1.8" />
      </button>
      <button
        ref="menuTrigger"
        type="button"
        class="tabs-tool"
        title="页签管理"
        aria-label="打开页签管理菜单"
        :aria-expanded="menuOpen"
        @click="toggleMenu"
      >
        <MoreHorizontal :size="19" :stroke-width="1.8" />
      </button>
    </div>
    <button
      type="button"
      class="yunshu-ai-shortcut"
      :class="{ active: route.path === '/app/yunshu-ai' }"
      title="打开云枢小智"
      aria-label="打开云枢小智"
      @click="openYunshuAi"
    >
      <span class="yunshu-ai-orbit" aria-hidden="true"></span>
      <img src="/images/open-a-i.svg" alt="" class="yunshu-ai-logo" />
      <span class="yunshu-ai-label">云枢小智</span>
      <Sparkles :size="15" :stroke-width="1.8" class="yunshu-ai-sparkle" aria-hidden="true" />
    </button>

    <div v-if="menuOpen" ref="menuRef" class="tabs-menu" role="menu" aria-label="页签管理">
      <button type="button" role="menuitem" @click="refreshPage">
        <RotateCw :size="16" />
        刷新当前页
      </button>
      <button type="button" role="menuitem" :disabled="!canCloseOthers" @click="closeOthers()">
        <PanelTopClose :size="16" />
        关闭其他页签
      </button>
      <button type="button" role="menuitem" :disabled="tagsView.tabs.length <= 1" @click="closeAll">
        <XCircle :size="16" />
        关闭全部页签
      </button>
    </div>

    <div
      v-if="contextMenu.open"
      ref="contextMenuRef"
      class="tabs-menu tabs-context-menu"
      role="menu"
      aria-label="页签快捷菜单"
      :style="{ left: `${contextMenu.x}px`, top: `${contextMenu.y}px` }"
    >
      <button type="button" role="menuitem" :disabled="contextMenu.tab?.affix" @click="closeContextTab">
        <X :size="16" />
        关闭当前页签
      </button>
      <button type="button" role="menuitem" @click="closeOthers(contextMenu.tab?.key)">
        <PanelTopClose :size="16" />
        关闭其他页签
      </button>
      <button type="button" role="menuitem" @click="closeAll">
        <XCircle :size="16" />
        关闭全部页签
      </button>
    </div>
  </div>
</template>

<script setup>
import { computed, nextTick, onBeforeUnmount, onMounted, reactive, ref, watch } from "vue";
import { useRoute, useRouter } from "vue-router";
import {
  ChevronLeft,
  ChevronRight,
  Home,
  Layers3,
  MoreHorizontal,
  PanelTopClose,
  RotateCw,
  Sparkles,
  X,
  XCircle,
} from "lucide-vue-next";
import { useTagsViewStore } from "@/stores/tags-view";

const route = useRoute();
const router = useRouter();
const tagsView = useTagsViewStore();
const scroller = ref(null);
const menuTrigger = ref(null);
const menuRef = ref(null);
const contextMenuRef = ref(null);
const menuOpen = ref(false);
const tabRefs = new Map();
const contextMenu = reactive({ open: false, x: 0, y: 0, tab: null });
const canCloseOthers = computed(() =>
  tagsView.tabs.some((tab) => !tab.affix && tab.key !== route.fullPath),
);

function isActive(tab) {
  return tab.key === route.fullPath;
}

function setTabRef(key, element) {
  if (element) tabRefs.set(key, element);
  else tabRefs.delete(key);
}

function openTab(tab) {
  if (!isActive(tab)) router.push(tab.fullPath);
  closeMenus();
}

function closeTab(tab) {
  const nextPath = tagsView.closeTab(tab.key, route.fullPath);
  if (nextPath) router.push(nextPath);
  closeMenus();
}

function scrollTabs(distance) {
  scroller.value?.scrollBy({ left: distance, behavior: "smooth" });
}

function onWheel(event) {
  if (Math.abs(event.deltaY) > Math.abs(event.deltaX)) {
    scroller.value?.scrollBy({ left: event.deltaY });
  }
}

function refreshPage() {
  window.location.reload();
}

function openYunshuAi() {
  if (route.path !== "/app/yunshu-ai") router.push("/app/yunshu-ai");
}

async function toggleMenu() {
  contextMenu.open = false;
  menuOpen.value = !menuOpen.value;
  if (menuOpen.value) {
    await nextTick();
    menuRef.value?.querySelector("button:not(:disabled)")?.focus();
  }
}

async function openTabMenu(event, tab) {
  menuOpen.value = false;
  contextMenu.open = true;
  contextMenu.tab = tab;
  contextMenu.x = Math.max(6, Math.min(event.clientX, window.innerWidth - 180));
  contextMenu.y = Math.max(6, Math.min(event.clientY, window.innerHeight - 130));
  await nextTick();
  contextMenuRef.value?.querySelector("button:not(:disabled)")?.focus();
}

function closeContextTab() {
  if (contextMenu.tab) closeTab(contextMenu.tab);
}

function closeOthers(targetKey = route.fullPath) {
  tagsView.closeOthers(targetKey);
  if (targetKey && targetKey !== route.fullPath) router.push(targetKey);
  closeMenus();
}

function closeAll() {
  const nextPath = tagsView.closeAll(route.fullPath);
  if (nextPath) router.push(nextPath);
  closeMenus();
}

function closeMenus(restoreFocus = false) {
  const contextTabKey = contextMenu.tab?.key;
  menuOpen.value = false;
  contextMenu.open = false;
  contextMenu.tab = null;
  if (restoreFocus) {
    nextTick(() => {
      const contextTabButton = contextTabKey
        ? tabRefs.get(contextTabKey)?.querySelector(".tab-open")
        : null;
      (contextTabButton || menuTrigger.value)?.focus();
    });
  }
}

function onDocumentPointerDown(event) {
  if (!(event.target instanceof Element)) return;
  if (!event.target.closest(".tabs-tools") && !event.target.closest(".tabs-menu")) {
    closeMenus();
  }
}

function onDocumentKeyDown(event) {
  if (event.key === "Escape" && (menuOpen.value || contextMenu.open)) {
    event.preventDefault();
    closeMenus(true);
    return;
  }
  if (!["ArrowDown", "ArrowUp"].includes(event.key)) return;
  const activeMenu = document.activeElement?.closest(".tabs-menu");
  if (!activeMenu) return;
  const items = [...activeMenu.querySelectorAll("button:not(:disabled)")];
  const index = items.indexOf(document.activeElement);
  if (index < 0 || !items.length) return;
  event.preventDefault();
  const step = event.key === "ArrowDown" ? 1 : -1;
  items[(index + step + items.length) % items.length]?.focus();
}

watch(
  () => route.fullPath,
  async (key) => {
    closeMenus();
    await nextTick();
    tabRefs.get(key)?.scrollIntoView({ behavior: "smooth", block: "nearest", inline: "nearest" });
  },
  { immediate: true },
);

onMounted(() => {
  document.addEventListener("pointerdown", onDocumentPointerDown);
  document.addEventListener("keydown", onDocumentKeyDown);
});
onBeforeUnmount(() => {
  document.removeEventListener("pointerdown", onDocumentPointerDown);
  document.removeEventListener("keydown", onDocumentKeyDown);
});
</script>

<style scoped>
.visited-tabs-bar {
  position: relative;
  z-index: 30;
  display: flex;
  align-items: stretch;
  height: 54px;
  flex: 0 0 54px;
  min-width: 0;
  color: #4b5565;
  background:
    linear-gradient(180deg, rgba(255, 255, 255, 0.98), rgba(249, 251, 254, 0.98)),
    #fff;
  border-bottom: 1px solid #cbd4e0;
  box-shadow: 0 4px 12px rgba(17, 32, 57, 0.1);
  font-family: var(--tc-font-nav, "钉钉进步体", "DingTalk JinBuTi", "Microsoft YaHei", sans-serif);
  font-size: 16px;
}

.visited-tabs-bar::after {
  content: "";
  position: absolute;
  left: 0;
  right: 0;
  bottom: -1px;
  height: 1px;
  pointer-events: none;
  background: linear-gradient(90deg, rgba(0, 82, 217, 0.34), rgba(0, 82, 217, 0.08) 28%, rgba(0, 82, 217, 0.04) 72%, rgba(0, 82, 217, 0.2));
}

.visited-tabs-brand {
  width: 54px;
  flex: 0 0 54px;
  display: grid;
  place-items: center;
  color: #0052d9;
  border-right: 1px solid #e5eaf1;
  background: linear-gradient(135deg, #f3f7ff, #ffffff);
}

.tabs-scroller {
  position: relative;
  display: flex;
  align-items: stretch;
  flex: 1;
  min-width: 0;
  overflow-x: auto;
  overflow-y: hidden;
  scrollbar-width: none;
}

.tabs-scroller::-webkit-scrollbar {
  display: none;
}

.visited-tab {
  position: relative;
  display: inline-flex;
  align-items: center;
  justify-content: center;
  gap: 8px;
  min-width: 112px;
  max-width: 210px;
  height: 54px;
  padding: 0 10px 0 15px;
  flex: 0 0 auto;
  border-right: 1px solid #e7ebf1;
  background: transparent;
  color: #5c6676;
  font: inherit;
  font-size: 16px;
  cursor: pointer;
  transition: color 160ms ease, background 160ms ease;
}

.tab-open {
  min-width: 0;
  height: 100%;
  padding: 0;
  flex: 1;
  display: inline-flex;
  align-items: center;
  justify-content: center;
  gap: 7px;
  border: 0;
  background: transparent;
  color: inherit;
  font: inherit;
  cursor: pointer;
}

.visited-tab::before {
  content: "";
  position: absolute;
  left: 14px;
  right: 14px;
  bottom: 0;
  height: 3px;
  border-radius: 3px 3px 0 0;
  background: #0052d9;
  transform: scaleX(0);
  opacity: 0;
  transition: transform 180ms ease, opacity 180ms ease;
}

.visited-tab:hover {
  color: #1f2937;
  background: #f4f7fb;
}

.visited-tab.active {
  color: #0048c4;
  font-weight: 600;
  background: linear-gradient(180deg, #f7faff 0%, #edf4ff 100%);
}

.visited-tab.active::before {
  transform: scaleX(1);
  opacity: 1;
}

.tab-open:focus-visible,
.tab-close:focus-visible,
.tabs-arrow:focus-visible,
.tabs-tool:focus-visible,
.tabs-menu button:focus-visible {
  outline: 2px solid rgba(0, 82, 217, 0.55);
  outline-offset: -2px;
}

.visited-tab.affix {
  min-width: 102px;
}

.tab-home {
  flex: 0 0 auto;
  color: #718096;
}

.active .tab-home {
  color: #0052d9;
}

.tab-title {
  overflow: hidden;
  text-overflow: ellipsis;
  white-space: nowrap;
}

.tab-close {
  width: 22px;
  height: 22px;
  margin-right: -4px;
  flex: 0 0 22px;
  display: grid;
  place-items: center;
  padding: 0;
  border: 0;
  border-radius: 4px;
  background: transparent;
  color: #8b95a5;
  cursor: pointer;
  transition: color 140ms ease, background 140ms ease;
}

.tab-close:hover {
  color: #fff;
  background: #e34d59;
}

.tabs-arrow,
.tabs-tool {
  width: 44px;
  flex: 0 0 44px;
  display: grid;
  place-items: center;
  border: 0;
  border-right: 1px solid #e5eaf1;
  background: transparent;
  color: #687386;
  cursor: pointer;
  transition: color 150ms ease, background 150ms ease;
}

.tabs-arrow:hover,
.tabs-tool:hover {
  color: #0052d9;
  background: #f1f6ff;
}

.tabs-tools {
  display: flex;
  flex: 0 0 auto;
  border-left: 1px solid #e5eaf1;
}

.tabs-tools .tabs-tool:last-child {
  border-right: 0;
}

.yunshu-ai-shortcut {
  position: relative;
  isolation: isolate;
  height: 40px;
  min-width: 128px;
  margin: 7px 10px;
  padding: 0 13px 0 10px;
  display: inline-flex;
  align-items: center;
  gap: 6px;
  flex: 0 0 auto;
  overflow: hidden;
  border: 1px solid rgba(73, 121, 224, 0.2);
  border-radius: 8px;
  color: #174ca7;
  background:
    linear-gradient(105deg, rgba(240, 247, 255, 0.98), rgba(233, 242, 255, 0.98) 55%, rgba(246, 250, 255, 0.98)),
    #eff6ff;
  box-shadow: 0 1px 2px rgba(22, 82, 185, 0.08), inset 0 1px 0 rgba(255, 255, 255, 0.95);
  font-family: var(--tc-font-ui, "PingFang SC", "Microsoft YaHei", -apple-system, BlinkMacSystemFont, sans-serif);
  font-size: 16px;
  font-weight: 600;
  letter-spacing: 0.1px;
  white-space: nowrap;
  cursor: pointer;
  transition: transform 180ms ease, box-shadow 180ms ease, border-color 180ms ease, color 180ms ease;
}

.yunshu-ai-shortcut::before {
  content: "";
  position: absolute;
  z-index: -1;
  width: 72px;
  height: 72px;
  top: -34px;
  left: -16px;
  border-radius: 50%;
  background: radial-gradient(circle, rgba(67, 150, 255, 0.2), transparent 68%);
  transition: transform 400ms ease;
}

.yunshu-ai-shortcut:hover {
  color: #073b90;
  border-color: rgba(0, 82, 217, 0.42);
  box-shadow: 0 5px 14px rgba(0, 82, 217, 0.18), inset 0 1px 0 rgba(255, 255, 255, 0.95);
  transform: translateY(-1px);
}

.yunshu-ai-shortcut:hover::before {
  transform: translateX(48px) scale(1.35);
}

.yunshu-ai-shortcut.active {
  color: #fff;
  border-color: #0052d9;
  background: linear-gradient(112deg, #0052d9, #1677e8 58%, #0a9ac8);
  box-shadow: 0 4px 13px rgba(0, 82, 217, 0.3), inset 0 1px 0 rgba(255, 255, 255, 0.25);
}

.yunshu-ai-shortcut.active::before {
  background: radial-gradient(circle, rgba(255, 255, 255, 0.34), transparent 68%);
}

.yunshu-ai-logo {
  position: relative;
  width: 24px;
  height: 24px;
  flex: 0 0 24px;
  object-fit: contain;
  filter: invert(25%) sepia(93%) saturate(1822%) hue-rotate(209deg) brightness(83%) contrast(107%);
  transition: filter 180ms ease, transform 500ms cubic-bezier(0.2, 0.8, 0.2, 1);
}

.yunshu-ai-shortcut:hover .yunshu-ai-logo {
  transform: rotate(12deg) scale(1.08);
}

.yunshu-ai-shortcut.active .yunshu-ai-logo {
  filter: brightness(0) invert(1);
}

.yunshu-ai-label,
.yunshu-ai-sparkle {
  position: relative;
}

.yunshu-ai-sparkle {
  margin-left: -2px;
  color: #2483dc;
  opacity: 0.76;
  transition: transform 240ms ease, opacity 240ms ease;
}

.yunshu-ai-shortcut:hover .yunshu-ai-sparkle {
  opacity: 1;
  transform: rotate(25deg) scale(1.12);
}

.yunshu-ai-shortcut.active .yunshu-ai-sparkle {
  color: #d9f2ff;
}

.yunshu-ai-orbit {
  position: absolute;
  z-index: -1;
  width: 52px;
  height: 18px;
  right: -16px;
  bottom: -12px;
  border: 1px solid rgba(21, 125, 224, 0.22);
  border-radius: 50%;
  transform: rotate(-22deg);
  transition: transform 400ms ease;
}

.yunshu-ai-shortcut:hover .yunshu-ai-orbit {
  transform: rotate(6deg) translateX(-6px);
}

.yunshu-ai-shortcut.active .yunshu-ai-orbit {
  border-color: rgba(255, 255, 255, 0.45);
}

.yunshu-ai-shortcut:focus-visible {
  outline: 2px solid rgba(0, 82, 217, 0.56);
  outline-offset: 2px;
}

.tabs-menu {
  position: absolute;
  top: 58px;
  right: 6px;
  z-index: 320;
  width: 184px;
  padding: 6px;
  border: 1px solid #dce3ed;
  border-radius: 7px;
  background: rgba(255, 255, 255, 0.98);
  box-shadow: 0 12px 32px rgba(22, 40, 72, 0.17);
  backdrop-filter: blur(12px);
}

.tabs-menu button {
  width: 100%;
  height: 38px;
  padding: 0 10px;
  display: flex;
  align-items: center;
  gap: 9px;
  border: 0;
  border-radius: 4px;
  background: transparent;
  color: #374151;
  font-family: var(--tc-font-nav, "钉钉进步体", "DingTalk JinBuTi", "Microsoft YaHei", sans-serif);
  font-size: 16px;
  cursor: pointer;
}

.tabs-menu button:hover:not(:disabled) {
  color: #0052d9;
  background: #edf4ff;
}

.tabs-menu button:disabled {
  color: #b7bec9;
  cursor: not-allowed;
}

.tabs-context-menu {
  position: fixed;
  right: auto;
  top: auto;
}

@media (max-width: 900px) {
  .visited-tabs-brand {
    display: none;
  }

  .visited-tab {
    min-width: 86px;
    max-width: 150px;
  }

  .yunshu-ai-shortcut {
    min-width: 40px;
    width: 40px;
    margin-left: 6px;
    margin-right: 6px;
    padding: 0 5px;
    justify-content: center;
  }

  .yunshu-ai-label,
  .yunshu-ai-sparkle {
    display: none;
  }
}

@media (prefers-reduced-motion: reduce) {
  .visited-tab,
  .visited-tab::before {
    transition: none;
  }
}
</style>
