<template>
  <div class="qc-exec-workbench dv-exec-workbench">
    <DvExecContextBar
      v-if="selectedRow"
      :row="selectedRow"
      @inspect="$emit('inspect', selectedRow)"
      @machinery="$emit('machinery')"
      @records="$emit('records')"
      @andon="$emit('andon')"
    />
    <div class="qc-workbench-exec-scroll">
      <router-view
        v-if="isExecRoute"
        @line-total="lineTotal = $event"
      />
      <DvWorkbenchEmpty v-else-if="!selectedRow" :pending-rows="pendingRows" />
      <div v-else class="qc-workbench-prompt">
        <p>已选中 <strong>{{ selectedRow.machineryCode }}</strong></p>
        <p>点击上方「开始处理」打开执行表单</p>
      </div>
    </div>
    <DvWorkbenchFooter
      v-if="isExecRoute"
      :task-type="selectedRow?.taskType"
      :line-total="lineTotal"
      :recent-rows="recentFinished"
      :recent-loading="recentLoading"
      :today-expanded="todayExpanded"
      @back="onBack"
      @close="onClose"
      @toggle-today="todayExpanded = !todayExpanded"
      @open-records="$emit('open-records')"
    />
  </div>
</template>

<script>
import DvExecContextBar from "./DvExecContextBar.vue";
import DvWorkbenchEmpty from "./DvWorkbenchEmpty.vue";
import DvWorkbenchFooter from "./DvWorkbenchFooter.vue";

export default {
  name: "DvExecWorkbench",
  components: { DvExecContextBar, DvWorkbenchEmpty, DvWorkbenchFooter },
  props: {
    selectedRow: { type: Object, default: null },
    pendingRows: { type: Array, default: () => [] },
    recentFinished: { type: Array, default: () => [] },
    recentLoading: { type: Boolean, default: false },
    isExecRoute: { type: Boolean, default: false },
  },
  emits: ["inspect", "machinery", "records", "andon", "open-records"],
  data() {
    return {
      lineTotal: 0,
      todayExpanded: false,
    };
  },
  watch: {
    "$route.query.taskId"() {
      this.lineTotal = 0;
    },
  },
  methods: {
    onClose() {
      this.$router.push({ name: "dv-workbench" });
    },
    onBack() {
      const t = this.selectedRow?.taskType;
      if (t === "REPAIR") {
        this.$router.push("/app/equipment/repairs");
        return;
      }
      const type = t === "MAINTEN" ? "MAINTEN" : "CHECK";
      this.$router.push({ path: "/app/equipment/check-maint", query: { tab: "records", type } });
    },
  },
};
</script>
