<template>
  <div class="dv-analytics-action-row">
    <el-button type="primary" plain size="default" @click="$emit('workbench')">进入工作台</el-button>
    <el-button plain size="default" @click="$emit('machinery')">设备台账</el-button>
    <el-button plain size="default" @click="$emit('check-maint')">点检保养</el-button>
    <el-button plain size="default" @click="$emit('repairs')">维修中心</el-button>
  </div>
</template>

<script>
export default {
  name: "DvAnalyticsActionRow",
  emits: ["workbench", "machinery", "check-maint", "repairs"],
};
</script>
