<template>
  <div class="report-studio">
    <header class="studio-command">
      <div class="command-identity">
        <div class="command-mark"><LayoutTemplate :size="18" /></div>
        <div class="command-title">
          <div class="command-kicker">REPORT STUDIO / 报表设计</div>
          <div class="command-file">
            <span>{{ currentTemplate?.label || "未选择模板" }}</span>
            <span v-if="currentTemplate" class="command-category">{{ currentTemplate.categoryLabel }}</span>
          </div>
        </div>
      </div>

      <div class="save-state" :class="{ 'save-state--dirty': dirty, 'save-state--error': loadError }">
        <span class="save-state__dot"></span>
        <span>{{ loadError ? "设计器异常" : dirty ? "有未保存修改" : designerReady ? "已保存" : "正在连接设计器" }}</span>
      </div>

      <div class="command-actions">
        <el-button class="command-btn" @click="openCreateDialog">
          <Plus :size="16" /> 新建
        </el-button>
        <el-button class="command-btn command-btn--primary" :disabled="!designerReady" @click="saveDesign">
          <Save :size="16" /> 保存
        </el-button>
        <el-button class="command-btn" :disabled="!selectedFile" @click="previewDesign">
          <Eye :size="16" /> 预览
        </el-button>
        <el-button class="command-btn" :disabled="!selectedFile" @click="exportDesign">
          <Download :size="16" /> 导出
        </el-button>
        <el-button class="command-btn" @click="openArchiveDrawer">
          <FolderArchive :size="16" /> 报表档案
        </el-button>
        <el-button class="command-icon-btn" :disabled="!designerReady" title="刷新设计器" @click="refreshDesigner">
          <RefreshCw :size="16" />
        </el-button>
        <el-dropdown trigger="click" @command="handleMoreCommand">
          <el-button class="command-icon-btn" title="更多模板操作">
            <MoreHorizontal :size="18" />
          </el-button>
          <template #dropdown>
            <el-dropdown-menu>
              <el-dropdown-item command="duplicate" :disabled="!currentTemplate">
                <Copy :size="15" /> 复制为新模板
              </el-dropdown-item>
              <el-dropdown-item command="rename" :disabled="!currentTemplate || currentTemplate.protected">
                <Pencil :size="15" /> 重命名
              </el-dropdown-item>
              <el-dropdown-item command="delete" :disabled="!currentTemplate || currentTemplate.protected" divided>
                <Trash2 :size="15" /> 删除模板
              </el-dropdown-item>
              <el-dropdown-item command="manage" divided>
                <ExternalLink :size="15" /> 报表管理
              </el-dropdown-item>
            </el-dropdown-menu>
          </template>
        </el-dropdown>
      </div>
    </header>

    <div class="studio-body">
      <aside class="template-dock">
        <div class="dock-heading">
          <div>
            <span class="dock-eyebrow">TEMPLATE LIBRARY</span>
            <h2>模板库</h2>
          </div>
          <span class="dock-total">{{ templateCatalog.length }}</span>
        </div>

        <div class="dock-search">
          <Search :size="15" />
          <input v-model="templateSearch" type="search" placeholder="搜索模板" aria-label="搜索报表模板" />
        </div>

        <nav class="dock-categories" aria-label="模板分类">
          <button
            v-for="item in categories"
            :key="item.value"
            type="button"
            :class="{ active: activeCategory === item.value }"
            @click="activeCategory = item.value"
          >
            <span>{{ item.label }}</span>
            <span>{{ item.count }}</span>
          </button>
        </nav>

        <div class="template-list">
          <button
            v-for="item in filteredTemplates"
            :key="item.fileName"
            type="button"
            class="template-card"
            :class="{ active: selectedFile === item.fileName }"
            @click="selectTemplate(item)"
          >
            <span class="template-paper" :class="`template-paper--${item.preview}`" aria-hidden="true">
              <span class="paper-head"></span>
              <span class="paper-rule"></span>
              <span class="paper-rule paper-rule--short"></span>
              <span class="paper-grid"></span>
            </span>
            <span class="template-card__body">
              <span class="template-card__topline">
                <span class="template-card__name">{{ item.label }}</span>
                <span v-if="item.protected" class="template-builtin">内置</span>
              </span>
              <span class="template-card__desc">{{ item.description }}</span>
              <span class="template-card__meta">
                <span>{{ item.categoryLabel }}</span>
                <span>{{ item.paper }}</span>
              </span>
            </span>
          </button>

          <div v-if="!filteredTemplates.length" class="template-empty">
            <FileText :size="24" />
            <span>没有匹配的模板</span>
          </div>
        </div>

        <button type="button" class="dock-create" @click="openCreateDialog">
          <Plus :size="16" />
          从模板创建报表
        </button>
      </aside>

      <main class="designer-workspace">
        <div class="workspace-strip">
          <div class="workspace-file">
            <span class="workspace-file__led"></span>
            <span>{{ selectedFile || "请选择模板" }}</span>
          </div>
          <div class="workspace-hint">A4 画布 · MySQL 模板存储 · 自动保存状态检测</div>
        </div>

        <div class="designer-host">
          <div v-if="loading" class="designer-loading" aria-live="polite">
            <div class="loading-paper">
              <span></span><span></span><span></span><span></span>
            </div>
            <strong>正在装配报表设计器</strong>
            <small>加载模板、数据源与属性面板</small>
          </div>

          <div v-else-if="loadError" class="designer-error">
            <div class="designer-error__icon"><AlertTriangle :size="24" /></div>
            <strong>设计器未能正常加载</strong>
            <p>{{ loadError }}</p>
            <el-button type="primary" @click="reloadDesigner">重新加载</el-button>
          </div>

          <iframe
            v-show="!loadError"
            ref="iframeRef"
            :key="iframeKey"
            :src="designerSrc"
            frameborder="0"
            class="designer-frame"
            title="UReport 报表设计器"
            @load="onIframeLoad"
          />
        </div>
      </main>
    </div>

    <el-dialog
      v-model="templateDialog.open"
      :title="dialogTitle"
      width="520px"
      append-to-body
      destroy-on-close
    >
      <div class="template-dialog-intro">
        <span class="template-dialog-mark"><FileText :size="18" /></span>
        <div>
          <strong>{{ dialogTitle }}</strong>
          <p>{{ dialogDescription }}</p>
        </div>
      </div>
      <el-form label-position="top" @submit.prevent>
        <el-form-item label="模板名称" :error="templateDialog.error">
          <el-input
            v-model="templateDialog.name"
            maxlength="80"
            show-word-limit
            placeholder="例如：七月生产日报"
            @keyup.enter="submitTemplateDialog"
          />
        </el-form-item>
        <el-form-item v-if="templateDialog.mode === 'create'" label="起始模板">
          <el-select v-model="templateDialog.baseFile" style="width: 100%">
            <el-option
              v-for="item in presetCatalog"
              :key="item.fileName"
              :label="`${item.label} · ${item.description}`"
              :value="item.fileName"
            />
          </el-select>
        </el-form-item>
      </el-form>
      <template #footer>
        <el-button @click="templateDialog.open = false">取消</el-button>
        <el-button type="primary" :loading="templateDialog.submitting" @click="submitTemplateDialog">
          {{ templateDialog.mode === "rename" ? "确认重命名" : "创建并打开" }}
        </el-button>
      </template>
    </el-dialog>

    <el-drawer
      v-model="archiveDrawerOpen"
      title="报表档案"
      size="640px"
      :append-to-body="false"
      destroy-on-close
      class="report-archive-drawer"
      @close="closeArchiveDrawer"
    >
      <ReportArchivePanel
        v-if="archiveDrawerOpen"
        embed-mode
        @design="openReportFromArchive"
        @changed="refreshArchiveState"
      />
    </el-drawer>
  </div>
</template>

<script setup>
import { computed, nextTick, onBeforeUnmount, onMounted, reactive, ref, watch } from "vue";
import { useRoute, useRouter } from "vue-router";
import { ElMessage, ElMessageBox } from "element-plus";
import {
  AlertTriangle,
  Copy,
  Download,
  ExternalLink,
  Eye,
  FileText,
  FolderArchive,
  LayoutTemplate,
  MoreHorizontal,
  Pencil,
  Plus,
  RefreshCw,
  Save,
  Search,
  Trash2,
} from "lucide-vue-next";
import { addReport, delReport, getReport, listReport, updateReport } from "@/yunshu-ui/api/mes/report/ureport";
import ReportArchivePanel from "@/yunshu-ui/analytics/report/index.vue";
import { patchUreportDesignerIframe } from "./ureport-iframe-patch";
import {
  PRESET_TEMPLATES,
  buildTemplateCatalog,
  normalizeReportFileName,
  replaceTemplateTitle,
  reportDisplayName,
  validateReportName,
} from "./model";

const DEFAULT_TEMPLATE = PRESET_TEMPLATES[0].fileName;
const route = useRoute();
const router = useRouter();

const selectedFile = ref(DEFAULT_TEMPLATE);
const templateCatalog = ref([]);
const loading = ref(true);
const loadError = ref("");
const dirty = ref(false);
const designerReady = ref(false);
const iframeKey = ref(0);
const iframeRef = ref(null);
const templateSearch = ref("");
const activeCategory = ref("all");
const savePending = ref(false);
const archiveDrawerOpen = ref(false);

const templateDialog = reactive({
  open: false,
  mode: "create",
  name: "",
  baseFile: DEFAULT_TEMPLATE,
  error: "",
  submitting: false,
});

let designerBridge = null;
let loadFallbackTimer = null;

const currentTemplate = computed(
  () => templateCatalog.value.find((item) => item.fileName === selectedFile.value) || null,
);
const presetCatalog = computed(() => templateCatalog.value.filter((item) => item.protected));
const designerSrc = computed(() => {
  const u = encodeURIComponent(`mysql:${selectedFile.value}`);
  return `/ureport/designer?_u=${u}`;
});
const categories = computed(() => {
  const definitions = [
    ["all", "全部"],
    ["general", "通用"],
    ["production", "生产"],
    ["quality", "质量"],
    ["print", "打印"],
    ["custom", "自定义"],
  ];
  return definitions.map(([value, label]) => ({
    value,
    label,
    count: value === "all"
      ? templateCatalog.value.length
      : templateCatalog.value.filter((item) => item.category === value).length,
  }));
});
const filteredTemplates = computed(() => {
  const keyword = templateSearch.value.trim().toLowerCase();
  return templateCatalog.value.filter((item) => {
    const matchesCategory = activeCategory.value === "all" || item.category === activeCategory.value;
    const matchesKeyword = !keyword
      || item.label.toLowerCase().includes(keyword)
      || item.description.toLowerCase().includes(keyword);
    return matchesCategory && matchesKeyword;
  });
});
const dialogTitle = computed(() => ({
  create: "新建报表模板",
  duplicate: "复制报表模板",
  rename: "重命名模板",
}[templateDialog.mode]));
const dialogDescription = computed(() => ({
  create: "选择一个经过验证的 MES 模板作为起点。",
  duplicate: "保留当前布局、数据源和表达式，生成独立副本。",
  rename: "修改模板文件名，不改变当前报表内容。",
}[templateDialog.mode]));

function clearLoadFallback() {
  if (loadFallbackTimer) {
    clearTimeout(loadFallbackTimer);
    loadFallbackTimer = null;
  }
}

function scheduleLoadFallback() {
  clearLoadFallback();
  loadFallbackTimer = setTimeout(() => {
    if (!designerReady.value) {
      loading.value = false;
      loadError.value = "连接 UReport 服务超时，请确认后端服务与 /ureport 代理可用。";
    }
  }, 15000);
}

async function confirmDiscardChanges() {
  if (!dirty.value) return true;
  try {
    await ElMessageBox.confirm("当前模板还有未保存修改，继续操作将丢失这些修改。", "未保存修改", {
      confirmButtonText: "放弃修改",
      cancelButtonText: "返回保存",
      type: "warning",
    });
    return true;
  } catch {
    return false;
  }
}

async function updateRouteFile(fileName) {
  if (route.query.file === fileName) return;
  await router.replace({ query: { ...route.query, file: fileName } });
}

async function selectTemplate(item) {
  if (!item || item.fileName === selectedFile.value) return;
  if (!await confirmDiscardChanges()) return;
  selectedFile.value = item.fileName;
  await updateRouteFile(item.fileName);
  reloadDesigner();
}

function reloadDesigner() {
  designerBridge?.cleanup?.();
  designerBridge = null;
  designerReady.value = false;
  dirty.value = false;
  loadError.value = "";
  loading.value = true;
  iframeKey.value += 1;
  scheduleLoadFallback();
}

function onIframeLoad() {
  designerBridge?.cleanup?.();
  designerBridge = patchUreportDesignerIframe(iframeRef.value, {
    onReady() {
      designerReady.value = true;
      loading.value = false;
      loadError.value = "";
      clearLoadFallback();
      window.setTimeout(() => designerBridge?.selectDefaultCell?.(), 320);
      window.setTimeout(() => designerBridge?.selectDefaultCell?.(), 900);
    },
    onDirtyChange(value) {
      dirty.value = value;
      // 不要求 wasDirty：强制保存时可能先从未标记脏 → 保存成功再变回干净
      if (savePending.value && !value) {
        savePending.value = false;
        ElMessage.success("报表已保存");
        loadTemplateList(selectedFile.value);
      }
    },
    onError(message) {
      designerReady.value = false;
      loading.value = false;
      loadError.value = message;
      clearLoadFallback();
    },
  });
  scheduleLoadFallback();
}

function saveDesign() {
  if (!designerBridge?.ready) {
    ElMessage.warning("设计器尚未就绪");
    return;
  }
  savePending.value = true;
  // 强制触发：不依赖 UReport 工具栏 dirty 标记（属性面板改动有时未点亮保存）
  if (!designerBridge.save(true)) {
    savePending.value = false;
    ElMessage.error("无法触发设计器保存，请刷新后重试");
    return;
  }
  window.setTimeout(() => {
    if (!savePending.value) return;
    // AJAX 已发出但 dirty 未回落时仍给反馈（内容可能已写入）
    savePending.value = false;
    ElMessage.success("已提交保存");
    loadTemplateList(selectedFile.value);
  }, 2800);
}

function previewDesign() {
  if (designerBridge?.ready && designerBridge.preview()) return;
  const u = encodeURIComponent(`mysql:${selectedFile.value}`);
  window.open(`/ureport/preview?_u=${u}`, "_blank", "noopener,noreferrer");
}

async function exportDesign() {
  if (!selectedFile.value) {
    ElMessage.warning("请选择需要导出的报表");
    return;
  }
  if (dirty.value) {
    try {
      await ElMessageBox.confirm(
        "当前模板有未保存修改，导出的是最近一次保存的版本。",
        "导出报表",
        {
          confirmButtonText: "继续导出",
          cancelButtonText: "返回保存",
          type: "warning",
        },
      );
    } catch {
      return;
    }
  }
  const u = encodeURIComponent(`mysql:${selectedFile.value}`);
  window.open(`/ureport/excel?_u=${u}`, "_blank", "noopener,noreferrer");
}

async function refreshDesigner() {
  if (!await confirmDiscardChanges()) return;
  reloadDesigner();
}

function resetDialog(mode) {
  templateDialog.mode = mode;
  templateDialog.name = "";
  templateDialog.baseFile = selectedFile.value || DEFAULT_TEMPLATE;
  templateDialog.error = "";
  templateDialog.submitting = false;
  templateDialog.open = true;
}

function openCreateDialog() {
  resetDialog("create");
}

function openDuplicateDialog() {
  resetDialog("duplicate");
  templateDialog.name = `${currentTemplate.value?.label || "报表"} 副本`;
}

function openRenameDialog() {
  resetDialog("rename");
  templateDialog.name = currentTemplate.value?.label || "";
}

async function getTemplateDetail(fileName) {
  const target = templateCatalog.value.find((item) => item.fileName === fileName);
  if (!target?.id) throw new Error("未找到模板记录");
  const response = await getReport(target.id);
  return response?.data || response;
}

async function submitTemplateDialog() {
  const existingNames = templateCatalog.value.map((item) => item.fileName);
  const currentName = templateDialog.mode === "rename" ? selectedFile.value : "";
  const error = validateReportName(templateDialog.name, existingNames, currentName);
  if (error) {
    templateDialog.error = error;
    return;
  }
  templateDialog.error = "";
  templateDialog.submitting = true;
  const newFileName = normalizeReportFileName(templateDialog.name);

  try {
    if (templateDialog.mode === "rename") {
      const detail = await getTemplateDetail(selectedFile.value);
      await updateReport({ ...detail, id: currentTemplate.value.id, name: newFileName });
      ElMessage.success("模板已重命名");
    } else {
      const sourceFile = templateDialog.mode === "create"
        ? templateDialog.baseFile
        : selectedFile.value;
      const detail = await getTemplateDetail(sourceFile);
      const content = replaceTemplateTitle(detail.content, reportDisplayName(newFileName));
      await addReport({ name: newFileName, content });
      ElMessage.success(templateDialog.mode === "duplicate" ? "模板副本已创建" : "模板已创建");
    }
    templateDialog.open = false;
    await loadTemplateList(newFileName);
    selectedFile.value = newFileName;
    await updateRouteFile(newFileName);
    reloadDesigner();
  } catch (errorValue) {
    templateDialog.error = errorValue?.message || "模板操作失败";
  } finally {
    templateDialog.submitting = false;
  }
}

async function deleteCurrentTemplate() {
  const item = currentTemplate.value;
  if (!item || item.protected) return;
  if (!await confirmDiscardChanges()) return;
  try {
    await ElMessageBox.confirm(`删除“${item.label}”后无法恢复。`, "删除模板", {
      confirmButtonText: "确认删除",
      cancelButtonText: "取消",
      type: "warning",
    });
    await delReport(item.id);
    ElMessage.success("模板已删除");
    selectedFile.value = DEFAULT_TEMPLATE;
    await loadTemplateList(DEFAULT_TEMPLATE);
    await updateRouteFile(DEFAULT_TEMPLATE);
    reloadDesigner();
  } catch {
    // User cancelled or request failed; API interceptor handles request errors.
  }
}

function handleMoreCommand(command) {
  if (command === "duplicate") openDuplicateDialog();
  if (command === "rename") openRenameDialog();
  if (command === "delete") deleteCurrentTemplate();
  if (command === "manage") openArchiveDrawer();
}

function openArchiveDrawer() {
  archiveDrawerOpen.value = true;
  if (route.query.drawer !== "archive") {
    router.replace({ query: { ...route.query, drawer: "archive" } });
  }
}

function closeArchiveDrawer() {
  if (route.query.drawer !== "archive") return;
  const nextQuery = { ...route.query };
  delete nextQuery.drawer;
  router.replace({ query: nextQuery });
}

async function openReportFromArchive(fileName) {
  if (!fileName) return;
  archiveDrawerOpen.value = false;
  closeArchiveDrawer();
  await loadTemplateList(fileName);
  const item = templateCatalog.value.find((entry) => entry.fileName === fileName);
  if (item) {
    await selectTemplate(item);
    return;
  }
  if (!await confirmDiscardChanges()) return;
  selectedFile.value = fileName;
  await updateRouteFile(fileName);
  reloadDesigner();
}

async function refreshArchiveState() {
  await loadTemplateList(selectedFile.value);
}

async function loadTemplateList(preferredFile = "") {
  try {
    const response = await listReport({ pageNum: 1, pageSize: 200 });
    const rows = response?.rows || response?.data?.rows || [];
    const names = new Set(rows.map((row) => row.name || row.name_));
    const completeRows = [
      ...rows,
      ...PRESET_TEMPLATES
        .filter((item) => !names.has(item.fileName))
        .map((item) => ({ name: item.fileName })),
    ];
    templateCatalog.value = buildTemplateCatalog(completeRows);
    const requested = preferredFile || route.query.file;
    if (requested && templateCatalog.value.some((item) => item.fileName === requested)) {
      selectedFile.value = requested;
    } else if (!templateCatalog.value.some((item) => item.fileName === selectedFile.value)) {
      selectedFile.value = templateCatalog.value[0]?.fileName || DEFAULT_TEMPLATE;
    }
  } catch (error) {
    templateCatalog.value = buildTemplateCatalog(PRESET_TEMPLATES.map((item) => ({ name: item.fileName })));
    loadError.value = error?.message || "模板目录加载失败";
    ElMessage.error("模板目录加载失败，请稍后重试");
  }
}

function handleBeforeUnload(event) {
  if (!dirty.value) return;
  event.preventDefault();
  event.returnValue = "";
}

onMounted(async () => {
  await loadTemplateList();
  if (route.query.drawer === "archive") {
    archiveDrawerOpen.value = true;
  }
  await nextTick();
  scheduleLoadFallback();
  window.addEventListener("beforeunload", handleBeforeUnload);
});

watch(
  () => route.query.drawer,
  (value) => {
    archiveDrawerOpen.value = value === "archive";
  },
);

onBeforeUnmount(() => {
  designerBridge?.cleanup?.();
  clearLoadFallback();
  window.removeEventListener("beforeunload", handleBeforeUnload);
});
</script>

<style scoped>
.report-studio {
  --studio-ink: #172033;
  --studio-blue: #1769e0;
  --studio-canvas: #e9eef5;
  display: flex;
  flex-direction: column;
  position: relative;
  width: 100%;
  height: 100%;
  min-height: 0;
  overflow: hidden;
  color: #24344d;
  background: var(--studio-canvas);
}

.studio-command {
  position: relative;
  z-index: 5;
  display: grid;
  grid-template-columns: minmax(280px, 1fr) auto minmax(420px, 1fr);
  align-items: center;
  min-height: 62px;
  padding: 0 18px;
  border-bottom: 1px solid #cfd8e5;
  background: #fff;
  box-shadow: 0 3px 14px rgba(27, 43, 67, 0.06);
}

.command-identity,
.command-actions,
.save-state,
.command-file,
.template-card__topline,
.template-card__meta,
.workspace-file,
.template-dialog-intro {
  display: flex;
  align-items: center;
}

.command-identity { gap: 11px; min-width: 0; }
.command-mark {
  display: grid;
  place-items: center;
  width: 34px;
  height: 34px;
  color: var(--studio-blue);
  background: #edf4ff;
  border-left: 3px solid var(--studio-blue);
}
.command-title { min-width: 0; }
.command-kicker {
  margin-bottom: 2px;
  color: #8290a4;
  font-size: 10px;
  font-weight: 700;
  letter-spacing: 0.14em;
}
.command-file { gap: 8px; min-width: 0; font-size: 15px; font-weight: 700; }
.command-file > span:first-child { overflow: hidden; text-overflow: ellipsis; white-space: nowrap; }
.command-category {
  padding: 2px 7px;
  color: #51627a;
  border: 1px solid #d8e0eb;
  background: #f5f7fa;
  font-size: 11px;
  font-weight: 500;
}

.save-state {
  justify-self: center;
  gap: 7px;
  padding: 6px 10px;
  color: #627188;
  font-size: 12px;
}
.save-state__dot {
  width: 7px;
  height: 7px;
  border-radius: 50%;
  background: #35a36f;
  box-shadow: 0 0 0 3px rgba(53, 163, 111, 0.12);
}
.save-state--dirty { color: #a96009; }
.save-state--dirty .save-state__dot { background: #d97706; box-shadow: 0 0 0 3px rgba(217, 119, 6, 0.13); }
.save-state--error { color: #c2413b; }
.save-state--error .save-state__dot { background: #d74747; box-shadow: 0 0 0 3px rgba(215, 71, 71, 0.12); }

.command-actions { justify-content: flex-end; gap: 8px; }
.command-btn,
.command-icon-btn {
  gap: 6px;
  min-height: 34px;
  border-radius: 3px;
}
.command-btn--primary {
  color: #fff;
  border-color: var(--studio-blue);
  background: var(--studio-blue);
}
.command-btn--primary:hover { color: #fff; background: #0f57be; border-color: #0f57be; }
.command-icon-btn { width: 34px; padding: 0; }

:global(.report-studio .report-archive-drawer) {
  position: absolute;
}

:global(.report-studio .report-archive-drawer .el-drawer__body) {
  display: flex;
  flex-direction: column;
  min-height: 0;
  padding: 0;
  overflow: auto;
}

:global(.report-studio .report-archive-drawer .hub-embed-panel) {
  flex: 1 1 auto;
  min-height: 0;
  padding: 12px 16px 16px !important;
}

.studio-body {
  display: grid;
  grid-template-columns: 276px minmax(0, 1fr);
  flex: 1;
  min-height: 0;
  overflow: hidden;
}

.template-dock {
  display: flex;
  flex-direction: column;
  min-height: 0;
  padding: 17px 14px 14px;
  color: #33445f;
  border-right: 1px solid #d5deea;
  background: #f7f9fc;
}
.dock-heading { display: flex; align-items: flex-start; justify-content: space-between; padding: 0 4px 13px; }
.dock-eyebrow { color: #8795a9; font-size: 9px; font-weight: 700; letter-spacing: 0.16em; }
.dock-heading h2 { margin: 3px 0 0; color: #22324a; font-size: 18px; font-weight: 700; }
.dock-total {
  display: grid;
  place-items: center;
  min-width: 27px;
  height: 23px;
  padding: 0 6px;
  color: #1769e0;
  border: 1px solid #bed2ef;
  background: #fff;
  font-size: 11px;
}
.dock-search {
  display: flex;
  align-items: center;
  gap: 8px;
  height: 34px;
  padding: 0 10px;
  margin-bottom: 12px;
  color: #8290a7;
  border: 1px solid #d4ddea;
  background: #fff;
}
.dock-search:focus-within { border-color: #4a86d9; box-shadow: 0 0 0 2px rgba(23, 105, 224, 0.16); }
.dock-search input {
  min-width: 0;
  flex: 1;
  color: #26364e;
  border: 0;
  outline: 0;
  background: transparent;
  font-size: 12px;
}
.dock-search input::placeholder { color: #9aa6b7; }

.dock-categories {
  display: grid;
  grid-template-columns: repeat(3, 1fr);
  gap: 4px;
  margin-bottom: 12px;
}
.dock-categories button {
  display: flex;
  align-items: center;
  justify-content: space-between;
  gap: 3px;
  min-width: 0;
  padding: 6px 7px;
  color: #6f7d91;
  border: 1px solid transparent;
  background: transparent;
  font-size: 11px;
  cursor: pointer;
}
.dock-categories button:hover { color: #235ea8; background: #edf3fb; }
.dock-categories button.active { color: #145ab9; border-color: #b8d0ef; background: #e5f0ff; }
.dock-categories button span:last-child { color: #7e94b0; font-size: 10px; }

.template-list {
  display: flex;
  flex-direction: column;
  flex: 1;
  min-height: 0;
  gap: 7px;
  padding-right: 3px;
  overflow: auto;
  scrollbar-width: thin;
  scrollbar-color: #c5cfdd transparent;
}
.template-card {
  display: grid;
  grid-template-columns: 52px minmax(0, 1fr);
  gap: 11px;
  width: 100%;
  padding: 10px;
  color: inherit;
  text-align: left;
  border: 1px solid #d9e1eb;
  border-left: 3px solid transparent;
  background: #fff;
  cursor: pointer;
  transition: border-color .16s ease, background .16s ease, transform .16s ease;
}
.template-card:hover { border-color: #9ebce4; background: #f8fbff; transform: translateX(2px); }
.template-card.active { border-color: #80ace6; border-left-color: #1769e0; background: #eaf3ff; }
.template-paper {
  position: relative;
  display: block;
  width: 46px;
  height: 61px;
  padding: 7px 5px;
  overflow: hidden;
  background: #fff;
  box-shadow: 0 4px 9px rgba(0, 0, 0, .22);
}
.paper-head { display: block; width: 60%; height: 3px; margin: 0 auto 6px; background: #2c4b76; }
.paper-rule { display: block; height: 2px; margin-bottom: 3px; background: #ccd7e6; }
.paper-rule--short { width: 68%; }
.paper-grid {
  display: block;
  height: 29px;
  margin-top: 5px;
  background:
    linear-gradient(#d4dde8 1px, transparent 1px),
    linear-gradient(90deg, #d4dde8 1px, transparent 1px);
  background-size: 100% 7px, 12px 100%;
}
.template-paper--metrics .paper-head { width: 80%; background: #1769e0; }
.template-paper--quality .paper-head { background: #26735b; }
.template-paper--form .paper-grid { background-size: 100% 10px, 19px 100%; }
.template-paper--custom .paper-head { background: #718096; }
.template-card__body { min-width: 0; }
.template-card__topline { justify-content: space-between; gap: 5px; }
.template-card__name { overflow: hidden; color: #23344c; font-size: 13px; font-weight: 650; text-overflow: ellipsis; white-space: nowrap; }
.template-builtin { flex-shrink: 0; padding: 1px 4px; color: #1769e0; border: 1px solid #b8d0ef; background: #f4f8fe; font-size: 9px; }
.template-card__desc { display: block; margin: 5px 0 8px; overflow: hidden; color: #738197; font-size: 10px; text-overflow: ellipsis; white-space: nowrap; }
.template-card__meta { justify-content: space-between; gap: 8px; color: #8a97aa; font-size: 9px; }
.template-empty { display: grid; place-items: center; gap: 8px; min-height: 150px; color: #8693a6; font-size: 12px; }
.dock-create {
  display: flex;
  align-items: center;
  justify-content: center;
  gap: 7px;
  width: 100%;
  min-height: 37px;
  margin-top: 12px;
  color: #1769e0;
  border: 1px solid #9cbce5;
  background: #edf5ff;
  font-size: 12px;
  cursor: pointer;
}
.dock-create:hover { background: #dcecff; }

.designer-workspace { display: flex; flex-direction: column; min-width: 0; min-height: 0; }
.workspace-strip {
  display: flex;
  align-items: center;
  justify-content: space-between;
  flex: 0 0 31px;
  padding: 0 13px;
  color: #6b788c;
  border-bottom: 1px solid #d4dce7;
  background: #f6f8fb;
  font-size: 10px;
}
.workspace-file { gap: 6px; color: #45556d; font-weight: 600; }
.workspace-file__led { width: 5px; height: 5px; border-radius: 50%; background: #1769e0; }
.workspace-hint { letter-spacing: .02em; }
.designer-host { position: relative; flex: 1; min-height: 0; overflow: hidden; background: var(--studio-canvas); }
.designer-frame { display: block; width: 100%; height: 100%; border: 0; background: #fff; }

.designer-loading,
.designer-error {
  position: absolute;
  inset: 0;
  z-index: 3;
  display: flex;
  flex-direction: column;
  align-items: center;
  justify-content: center;
  background: #e9eef5;
}
.loading-paper {
  width: 86px;
  height: 112px;
  padding: 19px 13px;
  margin-bottom: 18px;
  background: #fff;
  box-shadow: 0 12px 30px rgba(32, 49, 75, .13);
}
.loading-paper span { display: block; height: 5px; margin-bottom: 9px; background: #dbe3ed; animation: report-loading 1.2s ease-in-out infinite; }
.loading-paper span:first-child { width: 65%; margin: 0 auto 16px; background: #7ba9e8; }
.loading-paper span:nth-child(3) { width: 76%; }
.designer-loading strong { color: #2c3b52; font-size: 14px; }
.designer-loading small { margin-top: 5px; color: #7b899d; }
@keyframes report-loading { 50% { opacity: .38; } }
.designer-error__icon { display: grid; place-items: center; width: 48px; height: 48px; margin-bottom: 13px; color: #c65c32; border: 1px solid #e5b39f; background: #fff6f1; }
.designer-error strong { font-size: 16px; }
.designer-error p { max-width: 440px; margin: 7px 0 18px; color: #748196; font-size: 12px; text-align: center; }

.template-dialog-intro { gap: 11px; padding: 11px; margin-bottom: 16px; border-left: 3px solid #1769e0; background: #f3f6fa; }
.template-dialog-mark { display: grid; place-items: center; width: 34px; height: 34px; color: #1769e0; background: #e4eefc; }
.template-dialog-intro strong { color: #26364e; font-size: 13px; }
.template-dialog-intro p { margin: 3px 0 0; color: #76849a; font-size: 11px; }

@media (max-width: 1180px) {
  .studio-command { grid-template-columns: minmax(250px, 1fr) auto; }
  .save-state { display: none; }
  .workspace-hint { display: none; }
}
@media (max-width: 900px) {
  .studio-body { grid-template-columns: 224px minmax(0, 1fr); }
  .template-dock { padding-left: 10px; padding-right: 10px; }
  .command-btn { padding-left: 8px; padding-right: 8px; }
}
</style>
