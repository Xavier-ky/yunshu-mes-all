<template>
  <div class="andon-panel">
    <div class="andon-panel-toolbar">
      <el-button type="primary" icon="Plus" @click="openCreate">新增类型</el-button>
      <el-button icon="Refresh" @click="load">刷新</el-button>
    </div>
    <div class="andon-panel-split">
      <el-table v-loading="loading" class="yunshu-data-table" stripe border :data="list" highlight-current-row @current-change="select">
        <el-table-column label="编码" prop="typeCode" min-width="120" show-overflow-tooltip />
        <el-table-column label="名称" prop="typeName" min-width="140" show-overflow-tooltip />
        <el-table-column label="状态" width="90" align="center">
          <template #default="s"><el-tag size="small" :type="s.row.status === 'ENABLED' ? 'success' : 'info'">{{ s.row.status }}</el-tag></template>
        </el-table-column>
      </el-table>
      <el-form v-if="sel" :model="form" label-width="80px" class="andon-panel-form">
        <h4>{{ editing ? '编辑类型' : '类型详情' }}</h4>
        <el-form-item label="编码"><el-input v-model="form.typeCode" :readonly="!editing" /></el-form-item>
        <el-form-item label="名称"><el-input v-model="form.typeName" :readonly="!editing" /></el-form-item>
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
      <el-empty v-else description="请选择类型" />
    </div>
    <el-dialog v-model="modal" title="新增安灯类型" width="420px" append-to-body>
      <el-form :model="form" label-width="80px">
        <el-form-item label="编码"><el-input v-model="form.typeCode" /></el-form-item>
        <el-form-item label="名称"><el-input v-model="form.typeName" /></el-form-item>
      </el-form>
      <template #footer><el-button @click="modal = false">取消</el-button><el-button type="primary" @click="create">确定</el-button></template>
    </el-dialog>
  </div>
</template>

<script>
import { request } from "@/api/request";

export default {
  name: "AndonTypePanel",
  data() {
    return { loading: false, list: [], sel: null, editing: false, modal: false, form: { typeCode: "", typeName: "", status: "ENABLED" } };
  },
  methods: {
    async load() {
      this.loading = true;
      try { this.list = (await request.get("/andon/types"))?.data || []; if (this.list.length && !this.sel) this.select(this.list[0]); }
      finally { this.loading = false; }
    },
    select(row) { if (!row) return; this.sel = row; Object.assign(this.form, row); this.editing = false; },
    openCreate() { Object.assign(this.form, { typeCode: "", typeName: "", status: "ENABLED" }); this.modal = true; },
    async create() { await request.post("/andon/types", this.form); this.modal = false; await this.load(); },
    async save() { await request.put(`/andon/types/${this.sel.typeId}`, this.form); this.editing = false; await this.load(); },
    cancelEdit() { Object.assign(this.form, this.sel); this.editing = false; },
    async remove() {
      if (!this.sel || !confirm("确认删除该类型？")) return;
      await request.delete(`/andon/types/${this.sel.typeId}`);
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
