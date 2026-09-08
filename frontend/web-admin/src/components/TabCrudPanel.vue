<template>
  <div class="crud-root">
    <header v-if="title" class="crud-hero">
      <div>
        <span v-if="eyebrow" class="crud-eyebrow">{{ eyebrow }}</span>
        <h2>{{ title }}</h2>
        <p v-if="subtitle">{{ subtitle }}</p>
      </div>
    </header>

    <nav v-if="tabs.length > 1" class="crud-tabs">
      <button
        v-for="tab in tabs"
        :key="tab.key"
        type="button"
        :class="['crud-tab', { on: activeTab === tab.key }]"
        @click="switchTab(tab.key)"
      >
        {{ tab.label }}
      </button>
    </nav>

    <div class="crud-stats">
      <div class="crud-stat"><span>总数</span><strong>{{ rows.length }}</strong></div>
      <div class="crud-stat"><span>当前页</span><strong>{{ paged.length }}</strong></div>
    </div>

    <div class="crud-bar">
      <div class="crud-s">
        <svg width="16" height="16" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2"><circle cx="11" cy="11" r="8"/><line x1="21" y1="21" x2="16.65" y2="16.65"/></svg>
        <input v-model="filter.keyword" :placeholder="searchPlaceholder" @keyup.enter="load" @input="pg = 1" />
      </div>
      <select v-if="showStatusFilter" v-model="filter.status" @change="pg = 1">
        <option value="">全部状态</option>
        <option v-for="opt in statusOptions" :key="opt.value" :value="opt.value">{{ opt.label }}</option>
      </select>
      <button v-if="canCreate" class="crud-add" type="button" @click="openCreate">+ 新增{{ itemLabel }}</button>
    </div>

    <div class="crud-tbl-w">
      <table class="crud-tbl">
        <thead>
          <tr>
            <th v-for="col in currentColumns" :key="col.key">{{ col.label }}</th>
            <th v-if="showActions" class="th-act">操作</th>
          </tr>
        </thead>
        <tbody>
          <tr v-if="loading"><td :colspan="colspan" class="crud-empty">加载中…</td></tr>
          <tr v-else-if="!paged.length"><td :colspan="colspan" class="crud-empty">暂无{{ itemLabel }}数据</td></tr>
          <tr v-for="row in paged" v-else :key="rowId(row)">
            <td v-for="col in currentColumns" :key="col.key" :class="{ 'mono-s': col.mono }">
              <span v-if="col.type === 'status'" class="st-dot" :class="statusOn(row[col.key]) ? 'on' : ''"></span>
              <template v-if="col.type === 'status'">{{ statusLabel(row[col.key]) }}</template>
              <template v-else>{{ row[col.key] ?? '—' }}</template>
            </td>
            <td v-if="showActions" class="td-act">
              <button v-if="canEdit" class="ab" type="button" title="编辑" @click="openEdit(row)">
                <svg width="14" height="14" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2"><path d="M11 4H4a2 2 0 0 0-2 2v14a2 2 0 0 0 2 2h14a2 2 0 0 0 2-2v-7"/><path d="M18.5 2.5a2.121 2.121 0 0 1 3 3L12 15l-4 1 1-4 9.5-9.5z"/></svg>
              </button>
              <button v-if="canDelete" class="ab da" type="button" title="删除" @click="remove(row)">
                <svg width="14" height="14" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2"><polyline points="3 6 5 6 21 6"/><path d="M19 6v14a2 2 0 0 1-2 2H7a2 2 0 0 1-2-2V6m3 0V4a2 2 0 0 1 2-2h4a2 2 0 0 1 2 2v2"/></svg>
              </button>
            </td>
          </tr>
        </tbody>
      </table>
    </div>

    <div v-if="tp > 1" class="crud-pager">
      <span>{{ pg }}/{{ tp }} 页</span>
      <div class="pg-btns">
        <button type="button" :disabled="pg <= 1" @click="pg = 1">&laquo;</button>
        <button type="button" :disabled="pg <= 1" @click="pg--">&lsaquo;</button>
        <button v-for="p in vp" :key="p" type="button" :class="{ on: p === pg }" @click="pg = p">{{ p }}</button>
        <button type="button" :disabled="pg >= tp" @click="pg++">&rsaquo;</button>
        <button type="button" :disabled="pg >= tp" @click="pg = tp">&raquo;</button>
      </div>
    </div>

    <Teleport to="body">
      <div v-if="modal.open" class="mod-o" @click.self="modal.open = false">
        <div class="mod">
          <div class="mod-h">
            <h3>{{ modal.mode === 'create' ? `新增${itemLabel}` : `编辑${itemLabel}` }}</h3>
            <button class="mod-x" type="button" @click="modal.open = false">&times;</button>
          </div>
          <div class="mod-b">
            <label v-for="f in currentFormFields" :key="f.key" class="mf">
              <span>{{ f.label }}{{ f.required !== false ? ' *' : '' }}</span>
              <input v-model="form[f.key]" :type="f.inputType || 'text'" :placeholder="f.placeholder || ''" />
            </label>
            <p v-if="modal.error" class="mod-err">{{ modal.error }}</p>
          </div>
          <div class="mod-f">
            <button class="mb s" type="button" @click="modal.open = false">取消</button>
            <button class="mb p" type="button" :disabled="modal.saving" @click="submit">{{ modal.saving ? '保存中…' : '保存' }}</button>
          </div>
        </div>
      </div>
    </Teleport>
  </div>
</template>

<script setup>
import { computed, onMounted, reactive, ref, watch } from "vue";

const props = defineProps({
  title: { type: String, default: "" },
  subtitle: { type: String, default: "" },
  eyebrow: { type: String, default: "" },
  tabs: { type: Array, required: true },
  defaultTab: { type: String, default: "" },
  statusOptions: {
    type: Array,
    default: () => [
      { value: "ENABLED", label: "启用" },
      { value: "DISABLED", label: "停用" },
    ],
  },
  statusLabels: {
    type: Object,
    default: () => ({
      ENABLED: "启用",
      DISABLED: "停用",
      CREATED: "已创建",
      PRODUCING: "生产中",
      COMPLETED: "已完成",
      PASS: "合格",
      FAIL: "不合格",
      SUCCESS: "成功",
      FAILED: "失败",
    }),
  },
  statusOnValues: {
    type: Array,
    default: () => ["ENABLED", "COMPLETED", "PASS", "SUCCESS"],
  },
});

const activeTab = ref(props.defaultTab || props.tabs[0]?.key || "");
const rows = ref([]);
const loading = ref(false);
const filter = reactive({ keyword: "", status: "" });
const modal = reactive({ open: false, mode: "create", saving: false, error: "" });
const form = reactive({});
const pg = ref(1);
const ps = 10;

const currentTab = computed(() => props.tabs.find((t) => t.key === activeTab.value) || props.tabs[0]);
const itemLabel = computed(() => currentTab.value?.itemLabel || "");
const currentColumns = computed(() => currentTab.value?.columns || []);
const currentFormFields = computed(() => currentTab.value?.formFields || []);
const searchPlaceholder = computed(() => currentTab.value?.searchPlaceholder || "搜索…");
const showStatusFilter = computed(() => currentTab.value?.showStatusFilter !== false && currentTab.value?.canEdit !== false);
const canCreate = computed(() => currentTab.value?.canEdit !== false && currentTab.value?.create);
const canEdit = computed(() => currentTab.value?.canEdit !== false && currentTab.value?.update);
const canDelete = computed(() => currentTab.value?.canDelete !== false && currentTab.value?.del);
const showActions = computed(() => canEdit.value || canDelete.value);

const filtered = computed(() => {
  let a = rows.value;
  const kw = filter.keyword.trim().toLowerCase();
  if (kw) {
    a = a.filter((row) =>
      currentColumns.value.some((col) =>
        String(row[col.key] ?? "").toLowerCase().includes(kw)
      )
    );
  }
  if (filter.status) {
    a = a.filter((row) =>
      currentColumns.value.some((col) => col.type === "status" && row[col.key] === filter.status)
    );
  }
  return a;
});

const tp = computed(() => Math.ceil(filtered.value.length / ps) || 1);
const vp = computed(() => {
  let s = Math.max(1, pg.value - 2);
  let e = Math.min(tp.value, s + 4);
  s = Math.max(1, e - 4);
  const r = [];
  for (let i = s; i <= e; i++) r.push(i);
  return r;
});
const paged = computed(() => filtered.value.slice((pg.value - 1) * ps, pg.value * ps));
const colspan = computed(() => currentColumns.value.length + (showActions.value ? 1 : 0));

function rowId(row) {
  return row[currentTab.value.idKey];
}

function statusLabel(v) {
  if (!v) return "—";
  return props.statusLabels[v] || v;
}

function statusOn(v) {
  return props.statusOnValues.includes(v);
}

async function load() {
  loading.value = true;
  try {
    const r = await currentTab.value.fetchList({
      keyword: filter.keyword,
      status: filter.status,
    });
    const d = r?.data ?? r;
    rows.value = Array.isArray(d) ? d : [];
  } catch {
    rows.value = [];
  } finally {
    loading.value = false;
  }
}

function switchTab(key) {
  activeTab.value = key;
  filter.keyword = "";
  filter.status = "";
  pg.value = 1;
  load();
}

function openCreate() {
  modal.mode = "create";
  modal.open = true;
  modal.error = "";
  Object.keys(form).forEach((k) => delete form[k]);
  currentFormFields.value.forEach((f) => {
    form[f.key] = f.default ?? "";
  });
}

function openEdit(row) {
  modal.mode = "edit";
  modal.open = true;
  modal.error = "";
  Object.keys(form).forEach((k) => delete form[k]);
  const tab = currentTab.value;
  currentFormFields.value.forEach((f) => {
    form[f.key] = row[f.key] !== undefined && row[f.key] !== null ? row[f.key] : "";
  });
  form[tab.idKey] = row[tab.idKey];
}

async function submit() {
  const tab = currentTab.value;
  for (const f of currentFormFields.value.filter((f) => f.required !== false)) {
    if (!form[f.key] && form[f.key] !== 0) {
      modal.error = `${f.label}不能为空`;
      return;
    }
  }
  modal.saving = true;
  modal.error = "";
  try {
    const payload = {};
    currentFormFields.value.forEach((f) => {
      if (form[f.key] !== undefined && form[f.key] !== "") payload[f.key] = form[f.key];
    });
    if (modal.mode === "create") {
      await tab.create(payload);
    } else {
      await tab.update(form[tab.idKey], payload);
    }
    modal.open = false;
    await load();
  } catch (e) {
    modal.error = e?.message || "保存失败";
  } finally {
    modal.saving = false;
  }
}

async function remove(row) {
  const tab = currentTab.value;
  const id = row[tab.idKey];
  const nc = tab.columns.find((c) => !c.type || c.type !== "status");
  const label = nc ? row[nc.key] : `ID:${id}`;
  if (!window.confirm(`确认删除「${label}」？不可撤销。`)) return;
  try {
    await tab.del(id);
    await load();
  } catch (e) {
    window.alert(e?.message || "删除失败");
  }
}

watch(
  () => props.tabs,
  () => {
    if (!props.tabs.find((t) => t.key === activeTab.value)) {
      activeTab.value = props.tabs[0]?.key || "";
    }
  }
);

onMounted(load);

defineExpose({ load, switchTab });
</script>

<style scoped>
.crud-root { display: grid; gap: 14px; }
.crud-hero h2 { margin: 0; font-size: 20px; font-weight: 700; color: #1a1a1a; }
.crud-hero p { margin: 4px 0 0; color: #999; font-size: 13px; }
.crud-eyebrow { display: block; font-size: 11px; font-weight: 600; letter-spacing: 0.08em; color: #bbb; margin-bottom: 4px; }
.crud-tabs { display: flex; gap: 0; border-bottom: 1px solid #e0e0e0; }
.crud-tab { background: none; border: none; border-bottom: 2px solid transparent; color: #999; padding: 10px 16px; font-size: 13px; cursor: pointer; margin-bottom: -1px; }
.crud-tab:hover { color: #555; }
.crud-tab.on { color: #1a1a1a; border-bottom-color: #1a1a1a; font-weight: 600; }
.mono-s { font-family: "JetBrains Mono", monospace; font-size: 12px; color: #666; }
.crud-stats { display: grid; grid-template-columns: repeat(auto-fit, minmax(150px, 1fr)); gap: 10px; }
.crud-stat { background: #fff; border: 1px solid #e0e0e0; padding: 14px 18px; display: flex; flex-direction: column; gap: 4px; }
.crud-stat span { font-size: 11px; color: #999; }
.crud-stat strong { font-size: 26px; font-weight: 700; }
.crud-bar { display: flex; gap: 10px; align-items: center; flex-wrap: wrap; }
.crud-s { position: relative; flex: 1; max-width: 300px; }
.crud-s svg { position: absolute; left: 10px; top: 50%; transform: translateY(-50%); color: #bbb; }
.crud-s input { width: 100%; height: 36px; padding: 0 12px 0 32px; border: 1px solid #e0e0e0; font-size: 13px; outline: none; background: #fff; color: #1a1a1a; }
.crud-bar select { height: 36px; padding: 0 10px; border: 1px solid #e0e0e0; font-size: 12px; background: #fff; color: #1a1a1a; outline: none; min-width: 100px; }
.crud-bar select option { color: #1a1a1a; background: #fff; }
.crud-add { height: 36px; padding: 0 16px; border: none; background: #1a1a1a; color: #fff; font-size: 13px; font-weight: 500; cursor: pointer; white-space: nowrap; }
.crud-add:hover { background: #333; }
.crud-tbl-w { background: #fff; border: 1px solid #e0e0e0; overflow: auto; }
.crud-tbl { width: 100%; border-collapse: collapse; }
.crud-tbl th { text-align: left; padding: 10px 14px; font-size: 11px; color: #999; font-weight: 600; background: #fafafa; border-bottom: 1px solid #eee; white-space: nowrap; }
.crud-tbl td { padding: 8px 14px; font-size: 13px; border-bottom: 1px solid #f5f5f5; }
.crud-tbl tbody tr:hover { background: #f9f9f9; }
.th-act, .td-act { width: 80px; white-space: nowrap; }
.crud-empty { text-align: center; color: #ccc; padding: 40px !important; }
.st-dot { display: inline-block; width: 6px; height: 6px; border-radius: 50%; margin-right: 6px; background: #bbb; }
.st-dot.on { background: #059669; }
.td-act { display: flex; gap: 4px; }
.ab { width: 28px; height: 28px; border: 1px solid #e0e0e0; background: #fff; display: inline-flex; align-items: center; justify-content: center; cursor: pointer; color: #666; }
.ab:hover { background: #f5f5f5; }
.ab.da:hover { background: #fef2f2; border-color: #fecaca; color: #dc2626; }
.crud-pager { display: flex; align-items: center; justify-content: space-between; }
.crud-pager span { font-size: 12px; color: #999; }
.pg-btns { display: flex; gap: 4px; }
.pg-btns button { width: 30px; height: 30px; border: 1px solid #e0e0e0; background: #fff; font-size: 12px; cursor: pointer; color: #555; }
.pg-btns button:hover:not(:disabled) { background: #f5f5f5; }
.pg-btns button.on { background: #1a1a1a; color: #fff; border-color: #1a1a1a; }
.pg-btns button:disabled { opacity: 0.3; cursor: default; }
.mod-o { position: fixed; inset: 0; background: rgba(0, 0, 0, 0.2); display: grid; place-items: center; z-index: 200; }
.mod { width: min(520px, calc(100vw - 32px)); background: #fff; border: 1px solid #e0e0e0; box-shadow: 0 20px 60px rgba(0, 0, 0, 0.1); }
.mod-h { display: flex; align-items: center; justify-content: space-between; padding: 14px 24px; border-bottom: 1px solid #eee; }
.mod-h h3 { margin: 0; font-size: 16px; font-weight: 600; }
.mod-x { border: none; background: none; font-size: 22px; color: #bbb; cursor: pointer; }
.mod-b { padding: 20px 24px; display: grid; gap: 14px; }
.mod-f { display: flex; justify-content: flex-end; gap: 10px; padding: 14px 24px; border-top: 1px solid #eee; }
.mf { display: grid; gap: 5px; }
.mf span { font-size: 12px; color: #888; font-weight: 500; }
.mf input { height: 36px; padding: 0 12px; border: 1px solid #e0e0e0; font-size: 13px; outline: none; background: #fafafa; color: #1a1a1a; }
.mf input:focus { border-color: #888; background: #fff; }
.mod-err { margin: 0; color: #dc2626; font-size: 12px; }
.mb { height: 36px; padding: 0 20px; border: none; font-size: 13px; font-weight: 500; cursor: pointer; }
.mb.p { background: #1a1a1a; color: #fff; }
.mb.s { background: #fff; border: 1px solid #e0e0e0; color: #555; }
.mb:disabled { opacity: 0.6; cursor: default; }
</style>
