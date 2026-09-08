<template>
  <el-drawer v-model="visible" title="物料绑定" size="480px" append-to-body destroy-on-close @closed="onClosed">
    <el-form label-width="100px">
      <el-form-item label="产品 SN">
        <el-input v-model="snCode" placeholder="扫码或输入产品 SN" clearable @keyup.enter="lookupSn">
          <template #append>
            <el-button @click="lookupSn">查询</el-button>
          </template>
        </el-input>
      </el-form-item>
      <el-form-item v-if="snInfo" label="产品信息">
        <span style="font-size:13px">{{ snInfo.productName || "—" }} · {{ snInfo.workOrderNo || "—" }}</span>
      </el-form-item>
      <el-form-item label="物料批次">
        <el-input v-model="batchNo" placeholder="扫码或输入批次号" clearable @keyup.enter="lookupBatch">
          <template #append>
            <el-button @click="lookupBatch">查询</el-button>
          </template>
        </el-input>
      </el-form-item>
      <el-form-item v-if="batchInfo" label="物料信息">
        <span style="font-size:13px">{{ batchInfo.material_name || batchInfo.materialName || "—" }}</span>
      </el-form-item>
      <el-form-item>
        <el-button type="primary" :loading="submitting" :disabled="!canBind" @click="submit">确认绑定</el-button>
      </el-form-item>
    </el-form>
    <el-divider content-position="left">已绑定记录</el-divider>
    <el-table v-loading="listLoading" class="yunshu-data-table" stripe border :data="bindings" size="small" max-height="240">
      <el-table-column label="批次" prop="batch_no" min-width="120" show-overflow-tooltip />
      <el-table-column label="物料" min-width="120" show-overflow-tooltip>
        <template #default="s">{{ s.row.material_name || s.row.material_code || "—" }}</template>
      </el-table-column>
      <el-table-column label="绑定时间" min-width="130">
        <template #default="s">{{ formatTime(s.row.bind_time) }}</template>
      </el-table-column>
    </el-table>
  </el-drawer>
</template>

<script>
import { getAuthUser } from "@/utils/auth-context";
import { bindMaterial, listMaterialBindings, traceMaterialBatch, traceProductSn } from "@/yunshu-ui/api/mes/pro/operator";

export default {
  name: "OperatorBindMaterialDrawer",
  emits: ["bound"],
  data() {
    return {
      visible: false,
      submitting: false,
      listLoading: false,
      task: null,
      authUser: {},
      snCode: "",
      batchNo: "",
      snInfo: null,
      snId: null,
      batchInfo: null,
      batchId: null,
      materialId: null,
      bindings: [],
    };
  },
  computed: {
    canBind() {
      return this.snId && this.batchId && this.materialId && this.task;
    },
  },
  methods: {
    open(task) {
      this.task = task;
      this.authUser = getAuthUser() || {};
      this.snCode = "";
      this.batchNo = "";
      this.snInfo = null;
      this.snId = null;
      this.batchInfo = null;
      this.batchId = null;
      this.materialId = null;
      this.bindings = [];
      this.visible = true;
    },
    onClosed() {
      this.task = null;
    },
    formatTime(v) {
      if (!v) return "—";
      return String(v).slice(0, 16).replace("T", " ");
    },
    async lookupSn() {
      const code = (this.snCode || "").trim();
      if (!code) return;
      try {
        const res = await traceProductSn(code);
        const data = res?.data;
        if (!data?.snId) {
          this.$modal.msgWarning("未找到该产品 SN");
          return;
        }
        this.snInfo = data;
        this.snId = data.snId;
        await this.loadBindings();
      } catch {
        this.$modal.msgError("SN 查询失败");
      }
    },
    async lookupBatch() {
      const code = (this.batchNo || "").trim();
      if (!code) return;
      try {
        const res = await traceMaterialBatch(code);
        const batch = res?.data?.batch || res?.data;
        if (!batch?.batch_id && !batch?.batchId) {
          this.$modal.msgWarning("未找到该批次");
          return;
        }
        this.batchInfo = batch;
        this.batchId = batch.batch_id || batch.batchId;
        this.materialId = batch.material_id || batch.materialId;
      } catch {
        this.$modal.msgError("批次查询失败");
      }
    },
    async loadBindings() {
      if (!this.snId) return;
      this.listLoading = true;
      try {
        const res = await listMaterialBindings(this.snId);
        this.bindings = res?.data || [];
      } catch {
        this.bindings = [];
      } finally {
        this.listLoading = false;
      }
    },
    submit() {
      if (!this.canBind) return;
      this.submitting = true;
      bindMaterial({
        snId: this.snId,
        batchId: this.batchId,
        materialId: this.materialId,
        stepId: this.task.stepId,
        bindUserId: this.authUser.userId,
      })
        .then(() => {
          this.$modal.msgSuccess("物料绑定成功");
          this.batchNo = "";
          this.batchInfo = null;
          this.batchId = null;
          this.materialId = null;
          this.loadBindings();
          this.$emit("bound");
        })
        .finally(() => {
          this.submitting = false;
        });
    },
  },
};
</script>
