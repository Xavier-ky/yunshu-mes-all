<template>
  <div class="cal-workbench-subpanel cal-plan-team-panel">
    <div class="cal-workbench-split cal-plan-team-split">
      <aside class="cal-workbench-list cal-plan-team-list">
        <div class="cal-workbench-list__toolbar">
          <strong>关联班组</strong>
          <span>
            <el-button v-if="!readonly" link type="primary" @click="openTeamSelect">添加</el-button>
            <el-button
              v-if="!readonly && activeTeamId"
              link
              type="danger"
              @click="removeActiveTeam"
            >
              移除
            </el-button>
          </span>
        </div>
        <div class="cal-workbench-list__body" v-loading="loading">
          <button
            v-for="team in teamRows"
            :key="team.recordId || team.teamId"
            type="button"
            class="cal-workbench-list__item"
            :class="{ 'is-active': team.teamId === activeTeamId }"
            @click="activeTeamId = team.teamId; loadMembers(team.teamId)"
          >
            <span class="cal-workbench-list__item-head">
              <b>{{ team.teamName }}</b>
              <small>{{ team.teamCode }}</small>
            </span>
          </button>
          <div v-if="!loading && !teamRows.length" class="cal-workbench-list__empty">暂无关联班组</div>
        </div>
        <div v-if="teamRows.length" class="cal-workbench-list__footer">
          <span class="cal-workbench-list__footer-total">共 {{ teamRows.length }} 条</span>
        </div>
      </aside>
      <section class="cal-workbench-detail cal-plan-team-members">
        <div class="cal-workbench-detail__head">
          <div><h3>班组成员</h3></div>
        </div>
        <div class="cal-workbench-detail__body cal-plan-team-members__body">
          <div class="cal-workbench-subpanel__table-wrap" v-loading="memberLoading">
            <el-table class="yunshu-data-table" stripe border :data="memberRows">
              <el-table-column label="用户名" prop="userName" min-width="100" show-overflow-tooltip />
              <el-table-column label="用户昵称" prop="nickName" min-width="100" show-overflow-tooltip />
              <el-table-column label="电话" prop="tel" min-width="110" show-overflow-tooltip />
            </el-table>
          </div>
          <div v-if="activeTeamId" class="cal-workbench-subpanel__footer">
            <span class="cal-workbench-subpanel__footer-total">共 {{ memberRows.length }} 条</span>
          </div>
        </div>
      </section>
    </div>
    <TeamSelect ref="teamSelectRef" @on-selected="onTeamsSelected" />
  </div>
</template>

<script setup>
import { ref, watch } from "vue";
import { ElMessage, ElMessageBox } from "element-plus";
import { listPlanteam, addPlanteam, delPlanteam } from "@/yunshu-ui/api/mes/cal/planteam";
import { getListByTeamId } from "@/yunshu-ui/api/mes/cal/teammember";
import TeamSelect from "@/yunshu-ui/components/calTeamSelect/multi.vue";

const props = defineProps({
  planId: { type: [Number, String], default: null },
  calendarType: { type: String, default: "ZZ" },
  readonly: Boolean,
});

const loading = ref(false);
const memberLoading = ref(false);
const teamRows = ref([]);
const memberRows = ref([]);
const activeTeamId = ref(null);
const teamSelectRef = ref(null);

async function loadTeams() {
  if (!props.planId) {
    teamRows.value = [];
    memberRows.value = [];
    activeTeamId.value = null;
    return;
  }
  loading.value = true;
  try {
    const response = await listPlanteam({
      pageNum: 1,
      pageSize: 200,
      planId: props.planId,
      calendarType: props.calendarType,
    });
    teamRows.value = response?.rows || [];
    if (teamRows.value.length) {
      const firstId = teamRows.value[0].teamId;
      activeTeamId.value = firstId;
      await loadMembers(firstId);
    } else {
      memberRows.value = [];
      activeTeamId.value = null;
    }
  } catch (cause) {
    ElMessage.error(cause?.message || "计划班组加载失败");
  } finally {
    loading.value = false;
  }
}

async function loadMembers(teamId) {
  if (!teamId) {
    memberRows.value = [];
    return;
  }
  memberLoading.value = true;
  try {
    const response = await getListByTeamId(String(teamId));
    memberRows.value = response?.data || [];
  } catch {
    memberRows.value = [];
  } finally {
    memberLoading.value = false;
  }
}

function openTeamSelect() {
  if (props.readonly) return;
  teamSelectRef.value.showFlag = true;
}

async function onTeamsSelected(teams) {
  if (!props.planId || !Array.isArray(teams)) return;
  for (const team of teams) {
    await addPlanteam({
      planId: props.planId,
      teamId: team.teamId,
      teamCode: team.teamCode,
      teamName: team.teamName,
      calendarType: props.calendarType,
    });
  }
  ElMessage.success("班组已关联");
  await loadTeams();
}

async function removeActiveTeam() {
  const row = teamRows.value.find((team) => team.teamId === activeTeamId.value);
  if (!row?.recordId) return;
  await removeTeam(row);
}

async function removeTeam(row) {
  await ElMessageBox.confirm("是否确认移除该班组？", "提示", { type: "warning" });
  await delPlanteam(row.recordId);
  ElMessage.success("已移除");
  await loadTeams();
}

watch(
  () => [props.planId, props.calendarType],
  () => loadTeams(),
  { immediate: true },
);
</script>
