<template>
  <div class="andon-panel">
    <div class="andon-panel-toolbar">
      <el-button type="primary" icon="Plus" @click="openCreate">新增原因</el-button>
      <el-button icon="Refresh" @click="load">刷新</el-button>
    </div>
    <div class="andon-panel-split">
      <el-table v-loading="loading" class="yunshu-data-table" stripe border :data="list" highlight-current-row @current-change="select">
        <el-table-column label="编码" prop="reasonCode" min-width="110" show-overflow-tooltip />
        <el-table-column label="名称" prop="reasonName" min-width="140" show-overflow-tooltip />
        <el-table-column label="分类" min-width="100" show-overflow-tooltip>
          <template #default="s">{{ s.row.typeName || s.row.reasonCategory || '—' }}</template>
        </el-table-column>
        <el-table-column label="状态" width="90" align="center">
          <template #default="s"><el-tag size="small" :type="s.row.status === 'ENABLED' ? 'success' : 'info'">{{ s.row.status }}</el-tag></template>
        </el-table-column>
      </el-table>
      <el-form v-if="sel" :model="form" label-width="80px" class="andon-panel-form">
        <h4>{{ editing ? '编辑原因' : '原因详情' }}</h4>
        <el-form-item label="编码"><el-input v-model="form.reasonCode" :readonly="!editing" /></el-form-item>
        <el-form-item label="名称"><el-input v-model="form.reasonName" :readonly="!editing" /></el-form-item>
        <el-form-item label="分类"><el-input v-model="form.reasonCategory" :readonly="!editing" /></el-form-item>
        <el-form-item label="状态">
          <el-select v-model="form.status" :disabled="!editing" style="width:100%">
            <el-option label="启用" value="ENABLED" /><el-option label="停用" value="DISABLED" />
          </el-select>
        </el-form-item>
        <el-form-item>
          <el-button v-if="!editing" type="primary" @click="editing = true">编辑</el-button>
          <el-button v-if="editing" type="primary" @click="save">保存</el-button>
          <el-button v-if="editing" @click="cancelEdit">取消</el-button>
          <el-button type="danger" @click="remove">删除</el-button>
        </el-form-item>
      </el-form>
      <el-empty v-else description="请选择原因" />
    </div>
    <el-dialog v-model="modal" title="新增异常原因" width="420px" append-to-body>
      <el-form :model="form" label-width="80px">
        <el-form-item label="编码"><el-input v-model="form.reasonCode" /></el-form-item>
        <el-form-item label="名称"><el-input v-model="form.reasonName" /></el-form-item>
        <el-form-item label="分类"><el-input v-model="form.reasonCategory" /></el-form-item>
      </el-form>
      <template #footer><el-button @click="modal = false">取消</el-button><el-button type="primary" @click="create">确定</el-button></template>
    </el-dialog>
  </div>
</template>

<script>
import { request } from "@/api/request";

export default {
  name: "AndonReasonPanel",
  data() {
    return { loading: false, list: [], sel: null, editing: false, modal: false, form: { reasonCode: "", reasonName: "", reasonCategory: "GENERAL", status: "ENABLED" } };
  },
  methods: {
    async load() {
      this.loading = true;
      try { this.list = (await request.get("/andon/reasons"))?.data || []; if (this.list.length && !this.sel) this.select(this.list[0]); }
      finally { this.loading = false; }
    },
    select(row) {
      if (!row) return;
      this.sel = row;
      Object.assign(this.form, { ...row, reasonCategory: row.typeName || row.reasonCategory || "GENERAL" });
      this.editing = false;
    },
    openCreate() { Object.assign(this.form, { reasonCode: "", reasonName: "", reasonCategory: "GENERAL", status: "ENABLED" }); this.modal = true; },
    async create() { await request.post("/andon/reasons", this.form); this.modal = false; await this.load(); },
    async save() { await request.put(`/andon/reasons/${this.sel.reasonId}`, this.form); this.editing = false; await this.load(); },
    cancelEdit() { this.select(this.sel); },
    async remove() {
      if (!this.sel || !confirm("确认删除该原因？")) return;
      await request.delete(`/andon/reasons/${this.sel.reasonId}`);
      this.sel = null; await this.load();
    },
  },
};
</script>

<style scoped>
.andon-panel { display: flex; flex-direction: column; gap: 12px; min-height: 55vh; }
.andon-panel-toolbar { display: flex; gap: 8px; }
.andon-panel-split { display: grid; grid-template-columns: 1fr 360px; gap: 12px; min-height: 0; flex: 1; }
.andon-panel-form { border: 1px solid #ebeef5; padding: 16px; border-radius: 4px; }
.andon-panel-form h4 { margin: 0 0 12px; }
</style>
