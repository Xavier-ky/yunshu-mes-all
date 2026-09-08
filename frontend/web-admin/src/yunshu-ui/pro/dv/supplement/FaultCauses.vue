<template>
  <div :class="embedded ? 'dv-fault-causes-embedded' : 'app-container'">
    <el-form :inline="true" size="small" class="mb8">
      <el-form-item>
        <el-button type="primary" icon="Plus" @click="openForm()">新增</el-button>
      </el-form-item>
    </el-form>
    <el-table v-loading="loading" class="yunshu-data-table" stripe border :data="list">
      <el-table-column label="编码" prop="causeCode" min-width="120" show-overflow-tooltip />
      <el-table-column label="名称" prop="causeName" min-width="160" show-overflow-tooltip />
      <el-table-column label="设备类别" prop="categoryName" min-width="120" show-overflow-tooltip />
      <el-table-column label="预防措施" prop="preventMeasure" min-width="200" show-overflow-tooltip />
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

    <el-dialog v-model="open" :title="form.faultCauseId ? '编辑故障原因' : '新增故障原因'" width="560px" append-to-body>
      <el-form :model="form" label-width="90px">
        <el-form-item label="编码"><el-input v-model="form.causeCode" /></el-form-item>
        <el-form-item label="名称"><el-input v-model="form.causeName" /></el-form-item>
        <el-form-item label="类别ID"><el-input v-model="form.categoryId" placeholder="可选 device_category_id" /></el-form-item>
        <el-form-item label="预防措施"><el-input v-model="form.preventMeasure" type="textarea" /></el-form-item>
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
  name: "EqFaultCauses",
  props: {
    embedded: { type: Boolean, default: false },
  },
  data() {
    return {
      loading: false,
      list: [],
      open: false,
      form: { faultCauseId: null, causeCode: "", causeName: "", categoryId: null, preventMeasure: "", status: "ENABLED" },
    };
  },
  created() {
    this.load();
  },
  methods: {
    async load() {
      this.loading = true;
      try {
        this.list = (await request.get("/equipment/fault-causes"))?.data || [];
      } finally {
        this.loading = false;
      }
    },
    openForm(row) {
      this.form = row
        ? { ...row }
        : { faultCauseId: null, causeCode: "", causeName: "", categoryId: null, preventMeasure: "", status: "ENABLED" };
      this.open = true;
    },
    async submit() {
      const p = { ...this.form, categoryId: this.form.categoryId ? Number(this.form.categoryId) : null };
      if (p.faultCauseId) {
        await request.put(`/equipment/fault-causes/${p.faultCauseId}`, p);
      } else {
        await request.post("/equipment/fault-causes", p);
      }
      this.open = false;
      await this.load();
    },
    async handleDelete(row) {
      await this.$modal.confirm(`确认删除 ${row.causeName}？`);
      await request.delete(`/equipment/fault-causes/${row.faultCauseId}`);
      await this.load();
    },
  },
};
</script>
