<template>
  <el-drawer v-model="visible" title="生产报工" size="420px" append-to-body destroy-on-close @closed="onClosed">
    <el-form ref="formRef" :model="form" label-width="96px">
      <el-form-item label="派工编号">
        <el-input :model-value="task?.dispatchNo || '—'" disabled />
      </el-form-item>
      <el-form-item label="工序">
        <el-input :model-value="task?.stepName || '—'" disabled />
      </el-form-item>
      <el-form-item label="良品数量">
        <el-input-number v-model="form.quantityQualified" :min="0" :max="99999" style="width:100%" />
      </el-form-item>
      <el-form-item label="不良数量">
        <el-input-number v-model="form.quantityUnquanlified" :min="0" :max="99999" style="width:100%" />
      </el-form-item>
      <el-form-item label="备注">
        <el-input v-model="form.remark" type="textarea" :rows="3" placeholder="可选" />
      </el-form-item>
    </el-form>
    <template #footer>
      <el-button @click="visible = false">取消</el-button>
      <el-button type="primary" :loading="submitting" @click="submit">提交报工</el-button>
    </template>
  </el-drawer>
</template>

<script>
import { getAuthUser } from "@/utils/auth-context";
import { addFeedback, execute as executeFeedback } from "@/yunshu-ui/api/mes/pro/feedback";

export default {
  name: "OperatorReportDrawer",
  emits: ["submitted"],
  data() {
    return {
      visible: false,
      submitting: false,
      task: null,
      authUser: {},
      form: { quantityQualified: 1, quantityUnquanlified: 0, remark: "" },
    };
  },
  methods: {
    open(task) {
      this.task = task;
      this.authUser = getAuthUser() || {};
      this.form = { quantityQualified: 1, quantityUnquanlified: 0, remark: "" };
      this.visible = true;
    },
    onClosed() {
      this.task = null;
    },
    submit() {
      if (!this.task) return;
      const row = this.task;
      const u = this.authUser;
      const payload = {
        feedbackType: "SELF",
        feedbackCode: "FB-" + Date.now(),
        workstationId: row.stationId || row.workstationId || 1,
        workstationCode: row.stationCode,
        workstationName: row.stationName || row.workstationName,
        workorderId: row.workOrderId,
        workorderCode: row.workOrderNo || row.workorderCode,
        routeId: row.routeId || 0,
        processId: row.stepId || row.processId || 0,
        processCode: row.stepCode || row.processCode,
        processName: row.stepName || row.processName,
        taskId: row.dispatchId || row.taskId,
        taskCode: row.dispatchNo || row.taskCode,
        itemId: row.productId || row.itemId || 1,
        itemCode: row.productCode || row.itemCode || "ITEM",
        itemName: row.productName || row.itemName || "产品",
        quantityFeedback: Number(this.form.quantityQualified) + Number(this.form.quantityUnquanlified),
        quantityQualified: this.form.quantityQualified,
        quantityUnquanlified: this.form.quantityUnquanlified,
        userName: u.username,
        nickName: u.realName || u.displayName || u.username,
        feedbackChannel: "PC",
        status: "PREPARE",
        remark: this.form.remark,
      };
      this.submitting = true;
      addFeedback(payload)
        .then((res) => {
          const recordId = res?.data ?? res;
          if (recordId) {
            return executeFeedback(recordId);
          }
        })
        .then(() => {
          this.$modal.msgSuccess("报工成功");
          this.visible = false;
          this.$emit("submitted");
        })
        .catch((err) => {
          const msg = err?.response?.data?.msg || err?.message || "报工失败";
          this.$modal.msgError(msg);
        })
        .finally(() => {
          this.submitting = false;
        });
    },
  },
};
</script>
