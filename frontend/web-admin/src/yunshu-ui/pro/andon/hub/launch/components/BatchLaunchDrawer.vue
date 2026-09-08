<template>
  <el-drawer
    :model-value="modelValue"
    title="批量发起安灯"
    size="72%"
    append-to-body
    class="andon-launch-batch-drawer"
    @update:model-value="$emit('update:modelValue', $event)"
  >
    <BatchLaunchPanel
      :loading="loading"
      :tasks="tasks"
      :reason-options="reasonOptions"
      :active-by-station="activeByStation"
      :auth-user="authUser"
      :draft="draft"
      @reload="$emit('reload')"
      @submitted="$emit('submitted')"
    />
  </el-drawer>
</template>

<script>
import BatchLaunchPanel from "./BatchLaunchPanel.vue";

export default {
  name: "BatchLaunchDrawer",
  components: { BatchLaunchPanel },
  props: {
    modelValue: { type: Boolean, default: false },
    loading: { type: Boolean, default: false },
    tasks: { type: Array, default: () => [] },
    reasonOptions: { type: Array, default: () => [] },
    activeByStation: { type: Object, default: () => ({}) },
    authUser: { type: Object, default: () => ({}) },
    draft: { type: Object, default: () => ({}) },
  },
  emits: ["update:modelValue", "reload", "submitted"],
};
</script>
