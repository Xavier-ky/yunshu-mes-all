<template>
  <div class="app-container qc-wb-inspect-form dv-wb-exec">
    <div class="qc-wb-inspect-form__scroll dv-wb-exec__body">
      <el-row :gutter="12" class="dv-wb-exec__header-row">
        <el-col :span="8"><el-form-item label="设备编码"><el-input :model-value="machineryCode" readonly /></el-form-item></el-col>
        <el-col :span="8"><el-form-item label="设备名称"><el-input :model-value="machineryName" readonly /></el-form-item></el-col>
        <el-col :span="8"><el-form-item label="计划"><el-input :model-value="planLabel" readonly /></el-form-item></el-col>
      </el-row>
      <MaintenRecordLine
        v-if="recordId"
        :key="`mainten-line-${recordId}`"
        :recordId="recordId"
        optType="edit"
        workbenchMode
        @line-total="$emit('line-total', $event)"
      />
    </div>
  </div>
</template>

<script>
import MaintenRecordLine from "../../maintenrecord/line.vue";

export default {
  name: "DvWbExecMainten",
  components: { MaintenRecordLine },
  emits: ["line-total"],
  computed: {
    recordId() {
      return this.$route.query.taskId || null;
    },
    machineryCode() {
      return this.$route.query.machineryCode || "";
    },
    machineryName() {
      return this.$route.query.machineryName || "";
    },
    planLabel() {
      const c = this.$route.query.planCode || "";
      const n = this.$route.query.planName || "";
      return c && n ? `${c} / ${n}` : c || n || "—";
    },
  },
};
</script>
