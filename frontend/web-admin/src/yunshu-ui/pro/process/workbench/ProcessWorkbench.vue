<template>
  <div class="cal-workbench process-workbench cal-team-workbench">
    <ProcessToolbar
      :query-params="queryParams"
      :yes-no-options="yesNoOptions"
      :loading="loading"
      @update:processCode="setQueryField('processCode', $event)"
      @update:processName="setQueryField('processName', $event)"
      @update:enableFlag="setQueryField('enableFlag', $event)"
      @search="search"
      @reset="resetQuery"
      @create="startCreate"
      @export="exportProcesses"
      @refresh="refresh"
    />

    <div class="cal-workbench-split process-workbench-split">
      <ProcessListPanel
        :processes="processes"
        :selected-process-id="selectedProcessId"
        :loading="loading"
        :total="total"
        :page="queryParams.pageNum"
        :page-size="queryParams.pageSize"
        @select="selectProcess"
        @update:page="setPage"
        @update:page-size="setPageSize"
      />
      <ProcessDetailPanel
        :form="form"
        :editing="editing"
        :is-new="isNew"
        :saving="saving"
        :auto-gen-flag="autoGenFlag"
        @edit="startEdit"
        @save="saveProcess"
        @cancel-edit="cancelEdit"
        @delete="removeProcess()"
        @create="startCreate"
        @toggle-auto-gen="toggleAutoGen"
        @update:cover="form.attr2 = $event"
      />
    </div>
  </div>
</template>

<script setup>
import { computed, getCurrentInstance } from "vue";
import ProcessToolbar from "./ProcessToolbar.vue";
import ProcessListPanel from "./ProcessListPanel.vue";
import ProcessDetailPanel from "./ProcessDetailPanel.vue";
import { useProcessWorkbench } from "./useProcessWorkbench.js";

const instance = getCurrentInstance();
const yesNoOptions = computed(() => instance?.proxy?.dict?.type?.sys_yes_no || []);

const {
  processes,
  total,
  kpis,
  loading,
  saving,
  selectedProcessId,
  editing,
  isNew,
  form,
  autoGenFlag,
  queryParams,
  refresh,
  search,
  resetQuery,
  setQueryField,
  setPage,
  setPageSize,
  selectProcess,
  startCreate,
  startEdit,
  cancelEdit,
  saveProcess,
  removeProcess,
  exportProcesses,
  toggleAutoGen,
} = useProcessWorkbench();
</script>

<script>
export default {
  dicts: ["sys_yes_no"],
};
</script>
