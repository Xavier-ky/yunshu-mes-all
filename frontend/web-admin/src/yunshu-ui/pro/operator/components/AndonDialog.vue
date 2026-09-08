<template>
  <el-dialog v-model="visible" title="安灯呼叫" width="520px" append-to-body destroy-on-close @closed="onClosed">
    <el-form label-width="100px">
      <el-form-item label="派工">
        <el-input :model-value="task?.dispatchNo || '—'" disabled />
      </el-form-item>
      <el-form-item label="呼叫原因">
        <el-select v-model="form.configId" placeholder="请选择原因" style="width:100%" @change="onReasonPick">
          <el-option v-for="c in andonReasons" :key="c.configId" :label="c.andonReason" :value="c.configId" />
        </el-select>
      </el-form-item>
      <el-form-item label="备注">
        <el-input v-model="form.remark" type="textarea" :rows="3" />
      </el-form-item>
    </el-form>
    <template #footer>
      <el-button @click="visible = false">取消</el-button>
      <el-button type="warning" :loading="submitting" @click="submit">发起</el-button>
    </template>
  </el-dialog>
</template>

<script>
import { getAuthUser } from "@/utils/auth-context";
import { addAndonrecord } from "@/yunshu-ui/api/mes/pro/andonrecord";
import request from "@/yunshu-ui/api/request";

export default {
  name: "OperatorAndonDialog",
  emits: ["submitted"],
  data() {
    return {
      visible: false,
      submitting: false,
      task: null,
      authUser: {},
      andonReasons: [],
      form: { configId: null, andonReason: "", andonLevel: "LEVEL3", remark: "" },
    };
  },
  methods: {
    open(task) {
      this.task = task;
      this.authUser = getAuthUser() || {};
      this.form = { configId: null, andonReason: "", andonLevel: "LEVEL3", remark: "" };
      this.loadReasons().finally(() => {
        this.visible = true;
      });
    },
    onClosed() {
      this.task = null;
    },
    loadReasons() {
      return request
        .get("/mobile/pro/andonrecord/listReasons")
        .then((res) => {
          this.andonReasons = res.rows || res.data || [];
        })
        .catch(() =>
          request.get("/mes/pro/andonconfig/list", { params: { pageNum: 1, pageSize: 100 } }).then((res) => {
            this.andonReasons = res.rows || [];
          })
        );
    },
    onReasonPick(configId) {
      const cfg = this.andonReasons.find((c) => c.configId === configId);
      if (cfg) {
        this.form.andonReason = cfg.andonReason;
        this.form.andonLevel = cfg.andonLevel || "LEVEL3";
      }
    },
    submit() {
      if (!this.form.configId && !this.form.andonReason) {
        this.$modal.msgWarning("请选择呼叫原因");
        return;
      }
      const row = this.task || {};
      const u = this.authUser;
      const cfg = this.andonReasons.find((c) => c.configId === this.form.configId) || {};
      const payload = {
        workstationId: row.stationId || row.workstationId,
        workstationCode: row.stationCode,
        workstationName: row.stationName || row.workstationName,
        workorderId: row.workOrderId,
        workorderCode: row.workOrderNo || row.workorderCode,
        processId: row.stepId || row.processId,
        processCode: row.stepCode || row.processCode,
        processName: row.stepName || row.processName,
        userId: u.userId,
        userName: u.username,
        nickName: u.realName || u.displayName || u.username,
        andonReason: this.form.andonReason || cfg.andonReason,
        andonLevel: this.form.andonLevel || cfg.andonLevel || "LEVEL3",
        handlerRoleId: cfg.handlerRoleId,
        handlerRoleName: cfg.handlerRoleName,
        handlerUserId: cfg.handlerUserId,
        handlerUserName: cfg.handlerUserName,
        handlerNickName: cfg.handlerNickName,
        status: "ACTIVE",
        remark: this.form.remark,
      };
      this.submitting = true;
      addAndonrecord(payload)
        .then(() => {
          this.$modal.msgSuccess("安灯呼叫已发起");
          this.visible = false;
          this.$emit("submitted");
        })
        .finally(() => {
          this.submitting = false;
        });
    },
  },
};
</script>
