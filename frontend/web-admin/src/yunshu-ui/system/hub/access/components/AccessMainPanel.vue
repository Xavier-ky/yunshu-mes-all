<template>
  <div class="access-hub-panel-host">
    <UserPanel
      v-if="module === 'users'"
      ref="panel"
      embed-mode
      :scope-dept-id="deptId"
      :scope-status="status"
      @changed="$emit('changed')"
    />
    <RolePanel
      v-else-if="module === 'roles'"
      ref="panel"
      embed-mode
      :scope-status="status"
      :scope-role-id="roleId"
      @changed="$emit('changed')"
    />
    <DeptPanel
      v-else-if="module === 'departments'"
      ref="panel"
      embed-mode
      :scope-dept-id="deptId"
      :scope-status="status"
    />
    <PostPanel
      v-else-if="module === 'posts'"
      ref="panel"
      embed-mode
      :scope-status="status"
    />
  </div>
</template>

<script>
import UserPanel from "../../../user/index.vue";
import RolePanel from "../../../role/index.vue";
import DeptPanel from "../../../dept/index.vue";
import PostPanel from "../../../post/index.vue";

export default {
  name: "AccessMainPanel",
  components: { UserPanel, RolePanel, DeptPanel, PostPanel },
  emits: ["changed"],
  props: {
    module: { type: String, default: "users" },
    deptId: { type: [Number, String], default: null },
    status: { type: String, default: null },
    roleId: { type: [Number, String], default: null },
  },
  methods: {
    getPanel() {
      return this.$refs.panel;
    },
  },
};
</script>
