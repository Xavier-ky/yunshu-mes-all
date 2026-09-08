<template>
  <div class="app-container">
    <el-form :inline="true" size="small" class="mb8">
      <el-form-item>
        <el-button type="primary" icon="Plus" @click="openForm()">新增</el-button>
      </el-form-item>
    </el-form>
    <el-table v-loading="loading" class="yunshu-data-table" stripe border :data="list">
      <el-table-column label="厂商名称" prop="manufacturerName" min-width="160" show-overflow-tooltip />
      <el-table-column label="联系人" prop="contactPerson" min-width="120" show-overflow-tooltip />
      <el-table-column label="联系电话" prop="contactPhone" min-width="130" show-overflow-tooltip />
      <el-table-column label="状态" prop="status" width="100" align="center" />
      <el-table-column label="操作" align="center" class-name="col-actions" width="140">
        <template #default="scope">
          <div class="yunshu-row-actions">
            <el-button type="primary" link @click="openForm(scope.row)">编辑</el-button>
            <el-button type="danger" link @click="handleDelete(scope.row)">删除</el-button>
          </div>
        </template>
      </el-table-column>
    </el-table>

    <el-dialog v-model="open" :title="form.manufacturerId ? '编辑制造商' : '新增制造商'" width="520px" append-to-body>
      <el-form :model="form" label-width="90px">
        <el-form-item label="厂商名称"><el-input v-model="form.manufacturerName" /></el-form-item>
        <el-form-item label="联系人"><el-input v-model="form.contactPerson" /></el-form-item>
        <el-form-item label="联系电话"><el-input v-model="form.contactPhone" /></el-form-item>
        <el-form-item label="状态">
          <el-select v-model="form.status"><el-option label="启用" value="ENABLED" /><el-option label="停用" value="DISABLED" /></el-select>
        </el-form-item>
      </el-form>
      <template #footer>
        <el-button @click="open = false">取消</el-button>
        <el-button type="primary" @click="submit">确定</el-button>
      </template>
    </el-dialog>
  </div>
</template>

<script>
import { request } from "@/api/request";

export default {
  name: "EqManufacturers",
  data() {
    return {
      loading: false,
      list: [],
      open: false,
      form: { manufacturerId: null, manufacturerName: "", contactPerson: "", contactPhone: "", status: "ENABLED" },
    };
  },
  created() {
    this.load();
  },
  methods: {
    async load() {
      this.loading = true;
      try {
        this.list = (await request.get("/equipment/manufacturers"))?.data || [];
      } finally {
        this.loading = false;
      }
    },
    openForm(row) {
      this.form = row
        ? { ...row }
        : { manufacturerId: null, manufacturerName: "", contactPerson: "", contactPhone: "", status: "ENABLED" };
      this.open = true;
    },
    async submit() {
      const p = { ...this.form };
      if (p.manufacturerId) {
        await request.put(`/equipment/manufacturers/${p.manufacturerId}`, p);
      } else {
        await request.post("/equipment/manufacturers", p);
      }
      this.open = false;
      await this.load();
    },
    async handleDelete(row) {
      await this.$modal.confirm(`确认删除 ${row.manufacturerName}？`);
      await request.delete(`/equipment/manufacturers/${row.manufacturerId}`);
      await this.load();
    },
  },
};
</script>
