<template>
  <div class="cal-workbench-toolbar">
    <div class="cal-workbench-toolbar__period">
      <el-button-group>
        <el-button :icon="ArrowLeft" title="上一周期" @click="$emit('navigate', -1)" />
        <el-button @click="$emit('navigate', 0)">今天</el-button>
        <el-button :icon="ArrowRight" title="下一周期" @click="$emit('navigate', 1)" />
      </el-button-group>
      <strong>{{ periodLabel }}</strong>
      <el-radio-group :model-value="displayMode" @change="$emit('display-mode', $event)">
        <el-radio-button value="month">月</el-radio-button>
        <el-radio-button value="week">周</el-radio-button>
      </el-radio-group>
    </div>

    <div class="cal-workbench-toolbar__filters">
      <el-radio-group :model-value="queryType" @change="$emit('query-type', $event)">
        <el-radio-button value="TYPE">分类</el-radio-button>
        <el-radio-button value="TEAM">班组</el-radio-button>
        <el-radio-button value="USER">人员</el-radio-button>
      </el-radio-group>

      <el-select
        v-if="queryType === 'TYPE'"
        :model-value="selectedType"
        class="cal-workbench-toolbar__select"
        placeholder="班组类型"
        @change="$emit('type-change', $event)"
      >
        <el-option
          v-for="item in typeOptions"
          :key="item.value"
          :label="item.label"
          :value="item.value"
        />
      </el-select>
      <el-select
        v-else-if="queryType === 'TEAM'"
        :model-value="selectedTeamId"
        class="cal-workbench-toolbar__select"
        placeholder="选择班组"
        @change="$emit('team-change', $event)"
      >
        <el-option
          v-for="team in enabledTeams"
          :key="team.teamId"
          :label="team.teamName"
          :value="team.teamId"
        />
      </el-select>
      <el-button
        v-else
        class="cal-workbench-toolbar__user"
        :type="selectedUser ? 'primary' : 'default'"
        plain
        @click="$emit('choose-user')"
      >
        {{ selectedUser?.nickName || selectedUser?.userName || "选择人员" }}
      </el-button>

      <el-button :icon="Refresh" :loading="loading" title="刷新" @click="$emit('refresh')" />
      <el-button link type="primary" @click="$emit('navigate-tab', 'holiday')">节假日</el-button>
      <el-button link type="primary" @click="$emit('navigate-tab', 'plan')">排班计划</el-button>
    </div>
  </div>
</template>

<script setup>
import { computed } from "vue";
import { ArrowLeft, ArrowRight, Refresh } from "@element-plus/icons-vue";
import { formatDate, isTeamEnabled, parseLocalDate } from "./calScheduleModel";

const props = defineProps({
  anchorDate: { type: [Date, String], required: true },
  displayMode: { type: String, required: true },
  queryType: { type: String, required: true },
  calendarTypes: { type: Array, default: () => [] },
  teams: { type: Array, default: () => [] },
  selectedType: { type: String, default: "" },
  selectedTeamId: { type: [Number, String], default: null },
  selectedUser: { type: Object, default: null },
  weekStart: { type: String, default: "" },
  weekEnd: { type: String, default: "" },
  loading: Boolean,
});

defineEmits([
  "navigate",
  "display-mode",
  "query-type",
  "type-change",
  "team-change",
  "choose-user",
  "refresh",
  "navigate-tab",
]);

const typeOptions = computed(() => {
  if (props.calendarTypes.length) return props.calendarTypes;
  return [{ label: "制造班组", value: "ZZ" }];
});

const enabledTeams = computed(() => props.teams.filter(isTeamEnabled));

const periodLabel = computed(() => {
  if (props.displayMode === "week" && props.weekStart) {
    return `${props.weekStart.slice(5)} — ${props.weekEnd.slice(5)}`;
  }
  const date = parseLocalDate(formatDate(props.anchorDate));
  return `${date.getFullYear()}年${date.getMonth() + 1}月`;
});
</script>
