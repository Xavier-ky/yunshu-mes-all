<template>
  <div class="app-container qc-wb-inspect-form dv-wb-exec">
    <div class="qc-wb-inspect-form__scroll dv-wb-exec__body">
      <el-row :gutter="12" class="dv-wb-exec__header-row">
        <el-col :span="8"><el-form-item label="维修单号"><el-input :model-value="repairCode" readonly /></el-form-item></el-col>
        <el-col :span="8"><el-form-item label="设备编码"><el-input :model-value="machineryCode" readonly /></el-form-item></el-col>
        <el-col :span="8"><el-form-item label="设备名称"><el-input :model-value="machineryName" readonly /></el-form-item></el-col>
      </el-row>
      <RepairLine
        v-if="repairId"
        :key="`repair-line-${repairId}`"
        :repairId="repairId"
        optType="edit"
        workbenchMode
        @line-total="$emit('line-total', $event)"
      />
    </div>
  </div>
</template>

<script>
import RepairLine from "../../repair/line.vue";

export default {
  name: "DvWbExecRepair",
  components: { RepairLine },
  emits: ["line-total"],
  computed: {
    repairId() {
      return this.$route.query.taskId || null;
    },
    repairCode() {
      return this.$route.query.planCode || this.$route.query.repairCode || "";
    },
    machineryCode() {
      return this.$route.query.machineryCode || "";
    },
    machineryName() {
      return this.$route.query.machineryName || "";
    },
  },
};
</script>
