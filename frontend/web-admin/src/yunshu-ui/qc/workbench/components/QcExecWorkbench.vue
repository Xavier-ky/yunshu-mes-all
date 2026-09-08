<template>
  <div class="qc-exec-workbench">
    <QcExecContextBar
      v-if="selectedRow"
      :row="selectedRow"
      @inspect="$emit('inspect', selectedRow)"
      @open-blade-photos="bladePhotoVisible = true"
      @records="$emit('records')"
      @config="$emit('config')"
      @andon="$emit('andon')"
    />

    <BladeInstallPhotoDrawer :visible="bladePhotoVisible" @close="bladePhotoVisible = false" />

    <div class="qc-workbench-exec-scroll">
      <router-view v-if="isExecRoute" />
      <QcWorkbenchEmpty v-else-if="!selectedRow" :pending-rows="pendingRows" />
      <div v-else class="qc-workbench-prompt">
        <p>已选中任务 <strong>{{ selectedRow.sourceDocCode }}</strong></p>
        <p>点击上方「开始检验」打开检验表单</p>
      </div>
    </div>

    <QcTodayDoneStrip
      :rows="recentFinished"
      :loading="recentLoading"
      @open-records="$emit('open-records', $event)"
    />
  </div>
</template>

<script>
import QcExecContextBar from "./QcExecContextBar.vue";
import QcWorkbenchEmpty from "./QcWorkbenchEmpty.vue";
import QcTodayDoneStrip from "./QcTodayDoneStrip.vue";
import BladeInstallPhotoDrawer from "./BladeInstallPhotoDrawer.vue";
import { isBladeInstallQcTask } from "../blade-install-qc-photos";

export default {
  name: "QcExecWorkbench",
  components: { QcExecContextBar, QcWorkbenchEmpty, QcTodayDoneStrip, BladeInstallPhotoDrawer },
  props: {
    selectedRow: { type: Object, default: null },
    pendingRows: { type: Array, default: () => [] },
    recentFinished: { type: Array, default: () => [] },
    recentLoading: { type: Boolean, default: false },
    isExecRoute: { type: Boolean, default: false },
  },
  emits: ["inspect", "records", "config", "andon", "open-records"],
  data() {
    return {
      bladePhotoVisible: false,
    };
  },
  watch: {
    selectedRow(row) {
      if (!row || !this.isBladeInstallRow(row)) {
        this.bladePhotoVisible = false;
      }
    },
  },
  methods: {
    isBladeInstallRow(row) {
      return isBladeInstallQcTask(row);
    },
  },
};
</script>
