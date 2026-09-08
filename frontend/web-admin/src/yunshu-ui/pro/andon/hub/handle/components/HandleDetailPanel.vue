<template>
  <div class="andon-handle-detail">
    <el-empty v-if="!recordId" description="请从左侧选择待处置呼叫" />
    <template v-else>
      <el-tabs v-model="activeTab">
        <el-tab-pane label="处置详情" name="detail">
          <el-form ref="formRef" :model="form" :rules="rules" label-width="100px" v-loading="loading">
            <el-row :gutter="12">
              <el-col :span="8"><el-form-item label="工位"><el-input v-model="form.workstationName" readonly /></el-form-item></el-col>
              <el-col :span="8"><el-form-item label="工单"><el-input v-model="form.workorderCode" readonly /></el-form-item></el-col>
              <el-col :span="8"><el-form-item label="工序"><el-input v-model="form.processName" readonly /></el-form-item></el-col>
            </el-row>
            <el-row :gutter="12">
              <el-col :span="8"><el-form-item label="发起人"><el-input v-model="form.nickName" readonly /></el-form-item></el-col>
              <el-col :span="8"><el-form-item label="发起时间"><el-input :model-value="form.createTime" readonly /></el-form-item></el-col>
              <el-col :span="8"><el-form-item label="级别"><el-input v-model="form.andonLevel" readonly /></el-form-item></el-col>
            </el-row>
            <el-form-item label="呼叫原因"><el-input v-model="form.andonReason" type="textarea" readonly /></el-form-item>
            <el-alert
              v-if="linkedRepairId"
              type="success"
              :closable="false"
              show-icon
              class="andon-handle-detail__repair-link"
              :title="`已关联维修单 #${linkedRepairId}`"
            >
              <el-button link type="primary" @click="goRepairWorkbench">前往设备工作台</el-button>
            </el-alert>
            <el-row v-else-if="isEquipmentAndon" :gutter="12" class="andon-handle-detail__repair-create">
              <el-col :span="24">
                <el-button type="warning" :loading="creatingRepair" @click="createRepair">创建维修单并同步工作台</el-button>
              </el-col>
            </el-row>
            <el-row :gutter="12">
              <el-col :span="12">
                <el-form-item label="处置人" prop="handlerNickName">
                  <el-input v-model="form.handlerNickName" placeholder="请选择处置人" readonly>
                    <template #append><el-button icon="Search" @click="pickUser" /></template>
                  </el-input>
                </el-form-item>
              </el-col>
              <el-col :span="12">
                <el-form-item label="处置时间" prop="handleTime">
                  <el-date-picker v-model="form.handleTime" type="datetime" value-format="YYYY-MM-DD HH:mm:ss" style="width:100%" />
                </el-form-item>
              </el-col>
            </el-row>
            <el-form-item label="备注"><el-input v-model="form.remark" type="textarea" placeholder="处置说明" /></el-form-item>
            <div class="andon-handle-detail__actions">
              <el-button type="primary" @click="save('ACTIVE')">保存</el-button>
              <el-button type="success" @click="save('HANDLED')">已处置</el-button>
            </div>
          </el-form>
          <UserSingleSelect ref="userSelect" @onSelected="onUserSelected" />
        </el-tab-pane>
        <el-tab-pane label="处理任务" name="tasks">
          <TaskPanel v-if="activeTab === 'tasks'" ref="taskPanel" />
        </el-tab-pane>
      </el-tabs>
    </template>
  </div>
</template>

<script>
import { getAndonrecord, updateAndonrecord } from "@/yunshu-ui/api/mes/pro/andonrecord";
import { createRepairFromAndon } from "@/yunshu-ui/api/mes/dv/workbench";
import { categorizeReason } from "../../../utils/category";
import UserSingleSelect from "@/yunshu-ui/components/userSelect/single.vue";
import TaskPanel from "../../../panels/TaskPanel.vue";
import { parseTime } from "@/yunshu-ui/utils/yunshu-utils";

export default {
  name: "HandleDetailPanel",
  components: { UserSingleSelect, TaskPanel },
  props: {
    recordId: { type: [Number, String], default: null },
    initialTab: { type: String, default: "detail" },
  },
  emits: ["saved"],
  data() {
    return {
      activeTab: "detail",
      loading: false,
      creatingRepair: false,
      form: {},
      rules: {
        andonReason: [{ required: true, message: "呼叫原因不能为空", trigger: "blur" }],
      },
    };
  },
  computed: {
    linkedRepairId() {
      const v = this.form?.attr2;
      return v && String(v).trim() ? String(v) : "";
    },
    isEquipmentAndon() {
      return categorizeReason(this.form?.andonReason) === "equipment";
    },
  },
  watch: {
    initialTab: {
      immediate: true,
      handler(v) {
        if (v === "tasks" || v === "detail") this.activeTab = v;
      },
    },
    recordId: {
      immediate: true,
      handler(id) {
        if (id) this.load(id);
        else this.form = {};
      },
    },
  },
  methods: {
    async load(id) {
      this.loading = true;
      try {
        const res = await getAndonrecord(id);
        this.form = res.data || {};
        if (!this.form.handleTime && this.form.status === "ACTIVE") {
          this.form.handleTime = parseTime(new Date(), "{y}-{m}-{d} {h}:{i}:{s}");
        }
      } finally {
        this.loading = false;
      }
    },
    pickUser() {
      this.$refs.userSelect.showFlag = true;
    },
    onUserSelected(row) {
      this.form.handlerUserId = row.userId;
      this.form.handlerUserName = row.userName;
      this.form.handlerNickName = row.nickName;
    },
    save(status) {
      this.form.status = status;
      this.$refs.formRef.validate((valid) => {
        if (!valid) return;
        updateAndonrecord(this.form).then(() => {
          this.$modal.msgSuccess(status === "HANDLED" ? "已处置" : "保存成功");
          this.$emit("saved");
        });
      });
    },
    refreshTasks() {
      this.$refs.taskPanel?.load?.();
    },
    async createRepair() {
      if (!this.recordId) return;
      this.creatingRepair = true;
      try {
        const res = await createRepairFromAndon(this.recordId);
        const repairId = res?.data;
        if (repairId) {
          this.form.attr2 = String(repairId);
          this.$modal.msgSuccess(`维修单已创建（#${repairId}）`);
        }
      } finally {
        this.creatingRepair = false;
      }
    },
    goRepairWorkbench() {
      const q = {};
      if (this.linkedRepairId) {
        q.taskType = "REPAIR";
        q.taskId = this.linkedRepairId;
      }
      if (this.form.workstationId) q.stationId = String(this.form.workstationId);
      this.$router.push({ path: "/app/equipment/workbench/exec/repair", query: q });
    },
  },
};
</script>
