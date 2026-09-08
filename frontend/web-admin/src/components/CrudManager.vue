<template>
  <div class="crud-root">
    <div class="crud-toolbar">
      <div class="crub-filters">
        <input v-model="filter.keyword" class="crud-input" :placeholder="'搜索' + entityLabel" @keyup.enter="load" />
        <button class="crud-btn sec" @click="load">查询</button>
        <button class="crud-btn ghost" @click="clearFilter">重置</button>
      </div>
      <button class="crud-btn pri" @click="openCreate" v-if="creatable !== false">新增{{ entityLabel }}</button>
    </div>

    <div class="crud-table-wrap">
      <table class="crud-table">
        <thead>
          <tr>
            <th v-for="col in columns" :key="col.key" :style="col.width ? { width: col.width } : {}">{{ col.label }}</th>
            <th v-if="editable !== false" style="width:100px">操作</th>
          </tr>
        </thead>
        <tbody>
          <tr v-for="row in rows" :key="row[rowKey]">
            <td v-for="col in columns" :key="col.key" :class="col.mono ? 'mono' : ''">
              <template v-if="col.type === 'tag'">
                <span class="crud-tag" :class="tagTone(row[col.key])">{{ formatCell(row, col) }}</span>
              </template>
              <template v-else>{{ formatCell(row, col) }}</template>
            </td>
            <td v-if="editable !== false" class="crud-actions">
              <button class="crud-link" @click="openEdit(row)">编辑</button>
              <button class="crud-link danger" @click="remove(row)">删除</button>
            </td>
          </tr>
          <tr v-if="!rows.length && !loading">
            <td :colspan="columns.length + (editable !== false ? 1 : 0)" class="crud-empty">暂无数据</td>
          </tr>
        </tbody>
      </table>
    </div>

    <Teleport to="body">
      <div v-if="modal.open" class="crud-overlay" @click.self="closeModal">
        <div class="crud-modal">
          <header class="crud-modal-head">
            <h3>{{ modal.mode === "create" ? "新增" : "编辑" }}{{ entityLabel }}</h3>
            <button class="crud-btn ghost sm" @click="closeModal">关闭</button>
          </header>
          <div class="crud-modal-body">
            <label v-for="field in formFields" :key="field.key" class="crud-field">
              <span>{{ field.label }}<em v-if="field.required" class="req">*</em></span>
              <input v-if="!field.type || field.type === 'text'" v-model="form[field.key]" :placeholder="field.placeholder" />
              <select v-else-if="field.type === 'select'" v-model="form[field.key]">
                <option value="">请选择</option>
                <option v-for="opt in fieldOptions(field)" :key="opt.value" :value="opt.value">{{ opt.label }}</option>
              </select>
            </label>
            <p v-if="modal.error" class="crud-error">{{ modal.error }}</p>
          </div>
          <footer class="crud-modal-foot">
            <button class="crud-btn sec" @click="closeModal">取消</button>
            <button class="crud-btn pri" :disabled="modal.saving" @click="submit">{{ modal.saving ? "保存中…" : "保存" }}</button>
          </footer>
        </div>
      </div>
    </Teleport>
  </div>
</template>

<script setup>
import { onMounted, reactive, ref } from "vue";

const props = defineProps({
  columns: { type: Array, required: true },
  entityLabel: { type: String, default: "记录" },
  rowKey: { type: String, default: "id" },
  creatable: { type: Boolean, default: true },
  editable: { type: Boolean, default: true },
  formFields: { type: Array, default: () => [] },
  fieldOptionMap: { type: Object, default: () => ({}) },
  tagMap: { type: Object, default: () => ({}) },
  fetchApi: { type: Function, required: true },
  createApi: { type: Function },
  updateApi: { type: Function },
  deleteApi: { type: Function },
});

const rows = ref([]);
const loading = ref(false);
const filter = reactive({ keyword: "" });
const modal = reactive({ open: false, mode: "create", saving: false, error: "" });
const form = reactive({});

function initForm() {
  Object.keys(form).forEach(k => delete form[k]);
  props.formFields.forEach(f => { form[f.key] = ""; });
}

function fieldOptions(field) {
  const opts = props.fieldOptionMap[field.key];
  if (!opts) return [];
  if (typeof opts === "function") return opts();
  if (typeof opts === "object" && opts !== null && "value" in opts) return opts.value;
  return opts;
}

function formatCell(row, col) {
  const val = row[col.key];
  if (col.type === "tag") {
    return props.tagMap[val] || val;
  }
  return val ?? "—";
}

function tagTone(val) {
  const t = (props.tagMap[val] || "").toLowerCase();
  if (t.includes("生产") || t.includes("运行")) return "blue";
  if (t.includes("完成") || t.includes("合格") || t.includes("启用")) return "green";
  if (t.includes("待") || t.includes("处理") || t.includes("下达")) return "amber";
  if (t.includes("停用") || t.includes("删除")) return "red";
  return "gray";
}

async function load() {
  loading.value = true;
  try {
    const res = await props.fetchApi({ keyword: filter.keyword });
    const data = res?.data || res;
    rows.value = Array.isArray(data) ? data : [];
  } catch { rows.value = []; }
  finally { loading.value = false; }
}

function clearFilter() { filter.keyword = ""; load(); }
function openCreate() {
  initForm();
  modal.mode = "create"; modal.open = true; modal.error = "";
}
function openEdit(row) {
  initForm();
  props.formFields.forEach(f => { form[f.key] = row[f.key] ?? row[f.sourceKey] ?? ""; });
  form._id = row[props.rowKey];
  modal.mode = "edit"; modal.open = true; modal.error = "";
}
function closeModal() { modal.open = false; }

async function submit() {
  const required = props.formFields.filter(f => f.required);
  if (required.some(f => !form[f.key])) { modal.error = "请填写必填字段"; return; }

  modal.saving = true; modal.error = "";
  try {
    if (modal.mode === "create") {
      await props.createApi({ ...form });
    } else {
      await props.updateApi(form._id, { ...form });
    }
    delete form._id;
    modal.open = false;
    await load();
  } catch (e) { modal.error = e?.message || "操作失败"; }
  finally { modal.saving = false; }
}

async function remove(row) {
  if (!confirm(`确认删除该${props.entityLabel}？`)) return;
  try {
    await props.deleteApi(row[props.rowKey]);
    await load();
  } catch (e) { alert(e?.message || "删除失败"); }
}

onMounted(load);
</script>

<style scoped>
.crud-root { display: grid; gap: 12px; }
.crud-toolbar { display: flex; align-items: center; justify-content: space-between; gap: 12px; }
.crub-filters { display: flex; align-items: center; gap: 8px; }
.crud-input { height: 34px; padding: 0 12px; border: 1px solid rgba(255,255,255,0.5); border-radius: 0; font-size: 13px; outline: none; width: 200px; background: rgba(255,255,255,0.4); backdrop-filter: blur(10px); color: #1a1a1a; box-shadow: 0 0 6px rgba(180,190,205,0.08); }
.crud-input:focus { border-color: rgba(255,255,255,0.7); background: rgba(255,255,255,0.55); box-shadow: 0 0 0 1px rgba(255,255,255,0.7), 0 0 12px rgba(180,190,205,0.15); }
.crud-btn { height: 34px; padding: 0 16px; border-radius: 0; border: none; font-size: 13px; font-weight: 500; cursor: pointer; white-space: nowrap; }
.crud-btn.pri { background: #1a1a1a; color: #fff; }
.crud-btn.pri:hover { background: #333; }
.crud-btn.sec { background: rgba(255,255,255,0.5); backdrop-filter: blur(10px); border: 1px solid rgba(255,255,255,0.5); box-shadow: 0 0 6px rgba(180,190,205,0.1); color: #4b5563; }
.crud-btn.sec:hover { background: rgba(255,255,255,0.7); }
.crud-btn.ghost { background: transparent; color: rgba(0,0,0,0.4); }
.crud-btn.ghost:hover { color: #1a1a1a; }
.crud-btn.sm { height: 30px; font-size: 12px; padding: 0 12px; }
.crud-table-wrap { background: rgba(255,255,255,0.45); backdrop-filter: blur(16px); -webkit-backdrop-filter: blur(16px); border: 1px solid rgba(255,255,255,0.5); box-shadow: 0 0 0 1px rgba(255,255,255,0.5), 0 0 24px rgba(180,190,205,0.12); border-radius: 0; overflow: hidden; }
.crud-table { width: 100%; border-collapse: collapse; }
.crud-table th, .crud-table td { text-align: left; padding: 10px 14px; font-size: 13px; border-bottom: 1px solid rgba(255,255,255,0.35); }
.crud-table th { font-size: 11px; color: rgba(0,0,0,0.45); font-weight: 600; text-transform: uppercase; letter-spacing: 0.04em; background: rgba(255,255,255,0.2); }
.crud-table tbody tr:hover { background: rgba(255,255,255,0.25); }
.mono { font-family: "JetBrains Mono","SF Mono",monospace; font-size: 12px; }
.crud-tag { display: inline-block; font-size: 11px; font-weight: 600; }
.crud-actions { white-space: nowrap; text-align: right; }
.crud-link { border: none; background: none; color: #4b5563; font-size: 12px; cursor: pointer; padding: 2px 6px; border-radius: 4px; }
.crud-link:hover { color: #1a1a1a; background: #f3f4f6; }
.crud-link.danger:hover { color: #dc2626; }
.crud-empty { text-align: center; color: #d1d5db; padding: 48px 0; }
.crud-overlay { position: fixed; inset: 0; background: rgba(0,0,0,0.15); backdrop-filter: blur(6px); display: grid; place-items: center; z-index: 100; }
.crud-modal { width: min(480px, calc(100vw - 32px)); background: rgba(255,255,255,0.55); backdrop-filter: blur(28px); -webkit-backdrop-filter: blur(28px); border-radius: 0; border: 1px solid rgba(255,255,255,0.5); box-shadow: 0 0 0 1px rgba(255,255,255,0.5), 0 20px 60px rgba(0,0,0,0.08); overflow: hidden; }
.crud-modal-head { display: flex; align-items: center; justify-content: space-between; padding: 14px 20px; border-bottom: 1px solid rgba(255,255,255,0.5); }
.crud-modal-head h3 { margin: 0; font-size: 15px; font-weight: 600; }
.crud-modal-body { padding: 16px 20px; display: grid; gap: 12px; max-height: 60vh; overflow-y: auto; }
.crud-field { display: grid; gap: 5px; }
.crud-field span { font-size: 12px; font-weight: 500; color: rgba(0,0,0,0.5); }
.crud-field .req { color: #dc2626; margin-left: 2px; font-style: normal; }
.crud-field input, .crud-field select { height: 36px; padding: 0 10px; border: 1px solid rgba(255,255,255,0.5); border-radius: 0; font-size: 13px; background: rgba(255,255,255,0.4); color: #1a1a1a; outline: none; box-shadow: 0 0 6px rgba(180,190,205,0.08); }
.crud-field input:focus, .crud-field select:focus { border-color: rgba(255,255,255,0.7); background: rgba(255,255,255,0.55); box-shadow: 0 0 0 1px rgba(255,255,255,0.7), 0 0 12px rgba(180,190,205,0.15); }
.crud-field select option { color: #1a1a1a; background: #fff; }
.crud-error { margin: 0; color: #dc2626; font-size: 12px; }
.crud-modal-foot { display: flex; justify-content: flex-end; gap: 10px; padding: 12px 20px; border-top: 1px solid rgba(255,255,255,0.5); }
</style>
