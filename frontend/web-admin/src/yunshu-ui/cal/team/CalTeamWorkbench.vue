<template>
  <div class="cal-workbench cal-team-workbench">
    <CalTeamToolbar
      :query-params="queryParams"
      :calendar-types="calendarTypes"
      :loading="loading"
      @update:calendar-type="setQueryField('calendarType', $event)"
      @update:team-code="setQueryField('teamCode', $event)"
      @update:team-name="setQueryField('teamName', $event)"
      @search="search"
      @reset="resetQuery"
      @create="startCreate"
      @export="exportTeams"
      @refresh="refresh"
      @navigate-tab="$emit('navigate-tab', $event)"
    />

    <div class="cal-workbench-kpis cal-team-kpis" aria-label="班组关键指标">
      <div class="is-accent"><span>班组总数</span><b>{{ kpis.totalCount }}</b><small>个</small></div>
      <div><span>启用班组</span><b>{{ kpis.enabledCount }}</b><small>个</small></div>
      <div><span>制造班组</span><b>{{ kpis.manufacturingCount }}</b><small>个</small></div>
      <div><span>当前筛选</span><b>{{ kpis.filteredCount }}</b><small>条</small></div>
      <div><span>成员合计</span><b>{{ kpis.memberTotal }}</b><small>人</small></div>
      <div><span>停用班组</span><b>{{ kpis.disabledCount }}</b><small>个</small></div>
    </div>

    <div class="cal-workbench-split">
      <CalTeamListPanel
        :teams="teams"
        :calendar-types="calendarTypes"
        :selected-team-id="selectedTeamId"
        :loading="loading"
        :total="total"
        :page="queryParams.pageNum"
        :page-size="queryParams.pageSize"
        @select="selectTeam"
        @update:page="setPage"
        @update:page-size="setPageSize"
      />
      <CalTeamDetailPanel
        :form="form"
        :editing="editing"
        :is-new="isNew"
        :saving="saving"
        :auto-gen-flag="autoGenFlag"
        :calendar-types="calendarTypes"
        @edit="startEdit"
        @save="saveTeam"
        @cancel-edit="cancelEdit"
        @delete="removeTeam()"
        @create="startCreate"
        @toggle-auto-gen="toggleAutoGen"
      />
    </div>
  </div>
</template>

<script setup>
import { computed, getCurrentInstance } from "vue";
import CalTeamToolbar from "./CalTeamToolbar.vue";
import CalTeamListPanel from "./CalTeamListPanel.vue";
import CalTeamDetailPanel from "./CalTeamDetailPanel.vue";
import { useCalTeam } from "./useCalTeam";

defineEmits(["navigate-tab"]);

const instance = getCurrentInstance();
const dict = computed(() => instance?.proxy?.dict?.type?.mes_calendar_type || []);
const calendarTypes = computed(() => dict.value);

const {
  teams,
  total,
  kpis,
  loading,
  saving,
  selectedTeamId,
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
  selectTeam,
  startCreate,
  startEdit,
  cancelEdit,
  saveTeam,
  removeTeam,
  toggleAutoGen,
  exportTeams,
} = useCalTeam();
</script>

<script>
export default {
  dicts: ["mes_calendar_type"],
};
</script>
