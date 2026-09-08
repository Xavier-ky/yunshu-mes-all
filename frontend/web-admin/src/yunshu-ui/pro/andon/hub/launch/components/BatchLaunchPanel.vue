<template>
  <div class="andon-launch-batch">
    <el-form :inline="true" size="small" class="andon-launch-filter">
      <el-form-item label="产线"><el-input v-model="localFilters.lineName" placeholder="产线" clearable style="width:120px" /></el-form-item>
      <el-form-item label="工位"><el-input v-model="localFilters.stationName" placeholder="工位" clearable style="width:120px" /></el-form-item>
      <el-form-item label="工单"><el-input v-model="localFilters.workOrderNo" placeholder="工单号" clearable style="width:140px" /></el-form-item>
      <el-form-item label="仅未完成"><el-switch v-model="localFilters.incompleteOnly" /></el-form-item>
      <el-form-item>
        <el-button type="primary" icon="Search" @click="applyFilter">搜索</el-button>
        <el-button icon="Refresh" @click="$emit('reload')">刷新</el-button>
      </el-form-item>
    </el-form>

    <el-table
      v-loading="loading"
      class="yunshu-data-table"
      stripe
      border
      :data="displayRows"
      @selection-change="onSelect"
      max-height="calc(100vh - 340px)"
    >
      <el-table-column type="selection" width="48" align="center" :selectable="rowSelectable" />
      <el-table-column label="派工编号" prop="dispatchNo" min-width="120" show-overflow-tooltip align="center" />
      <el-table-column label="工位" min-width="110" show-overflow-tooltip align="center">
        <template #default="s">{{ s.row.stationName || s.row.workstationName || "—" }}</template>
      </el-table-column>
      <el-table-column label="产线" prop="lineName" min-width="100" show-overflow-tooltip align="center" />
      <el-table-column label="工单" min-width="120" show-overflow-tooltip align="center">
        <template #default="s">{{ s.row.workOrderNo || s.row.workorderCode || "—" }}</template>
      </el-table-column>
      <el-table-column label="工序" min-width="100" show-overflow-tooltip align="center">
        <template #default="s">{{ s.row.stepName || s.row.processName || "—" }}</template>
      </el-table-column>
      <el-table-column label="呼叫原因" min-width="160" align="center">
        <template #default="s">
          <el-select
            v-model="draft[s.row._key].configId"
            placeholder="选择原因"
            style="width:100%"
            clearable
            :disabled="!rowSelectable(s.row)"
            @change="onReasonChange(s.row)"
          >
            <el-option v-for="c in reasonOptions" :key="c.configId" :label="c.andonReason" :value="c.configId" />
          </el-select>
        </template>
      </el-table-column>
      <el-table-column label="级别" width="90" align="center">
        <template #default="s">
          <el-tag v-if="draft[s.row._key].andonLevel" size="small" :type="levelTagType(draft[s.row._key].andonLevel)">{{ draft[s.row._key].andonLevel }}</el-tag>
        </template>
      </el-table-column>
      <el-table-column label="操作" width="90" align="center" class-name="col-actions" fixed="right">
        <template #default="s">
          <el-button type="warning" link :disabled="!canSubmitRow(s.row)" @click="submitOne(s.row)">发起</el-button>
        </template>
      </el-table-column>
    </el-table>

    <div class="andon-launch-footer">
      <span>已选 {{ selectedRows.length }} 条</span>
      <el-button type="warning" :disabled="!selectedRows.length" @click="submitBatch">批量发起</el-button>
    </div>
  </div>
</template>

<script>
import { addAndonrecord } from "@/yunshu-ui/api/mes/pro/andonrecord";

export default {
  name: "BatchLaunchPanel",
  props: {
    loading: { type: Boolean, default: false },
    tasks: { type: Array, default: () => [] },
    reasonOptions: { type: Array, default: () => [] },
    activeByStation: { type: Object, default: () => ({}) },
    authUser: { type: Object, default: () => ({}) },
    draft: { type: Object, required: true },
  },
  emits: ["reload", "submitted", "update:draft"],
  data() {
    return {
      displayRows: [],
      selectedRows: [],
      localFilters: { lineName: "", stationName: "", workOrderNo: "", incompleteOnly: true },
    };
  },
  watch: {
    tasks: {
      immediate: true,
      handler() {
        this.applyFilter();
      },
    },
  },
  methods: {
    applyFilter() {
      const f = this.localFilters;
      this.displayRows = this.tasks.filter((t) => {
        if (f.incompleteOnly && t.status === "COMPLETED") return false;
        const line = (t.lineName || "").toLowerCase();
        const station = (t.stationName || t.workstationName || "").toLowerCase();
        const wo = (t.workOrderNo || t.workorderCode || "").toLowerCase();
        if (f.lineName && !line.includes(f.lineName.toLowerCase())) return false;
        if (f.stationName && !station.includes(f.stationName.toLowerCase())) return false;
        if (f.workOrderNo && !wo.includes(f.workOrderNo.toLowerCase())) return false;
        return true;
      });
    },
    rowSelectable(row) {
      const wsId = row.stationId || row.workstationId;
      if (wsId && this.activeByStation[String(wsId)]) return false;
      return row.status !== "COMPLETED" && !this.draft[row._key]?.submitted;
    },
    levelTagType(level) {
      if (level === "LEVEL1") return "danger";
      if (level === "LEVEL2") return "warning";
      return "info";
    },
    onReasonChange(row) {
      const d = this.draft[row._key];
      const cfg = this.reasonOptions.find((c) => c.configId === d.configId);
      if (!cfg) return;
      d.andonReason = cfg.andonReason;
      d.andonLevel = cfg.andonLevel || "LEVEL3";
      d.handlerRoleId = cfg.handlerRoleId;
      d.handlerRoleName = cfg.handlerRoleName;
      d.handlerUserId = cfg.handlerUserId;
      d.handlerUserName = cfg.handlerUserName;
      d.handlerNickName = cfg.handlerNickName;
    },
    canSubmitRow(row) {
      const d = this.draft[row._key];
      return this.rowSelectable(row) && d && (d.configId || d.andonReason);
    },
    buildPayload(row) {
      const d = this.draft[row._key];
      const u = this.authUser;
      return {
        workstationId: row.stationId || row.workstationId,
        workstationCode: row.stationCode || row.workstationCode,
        workstationName: row.stationName || row.workstationName,
        workorderId: row.workOrderId || row.workorderId,
        workorderCode: row.workOrderNo || row.workorderCode,
        workorderName: row.workOrderName || row.productName,
        processId: row.stepId || row.processId,
        processCode: row.stepCode || row.processCode,
        processName: row.stepName || row.processName,
        userId: u.userId,
        userName: u.username,
        nickName: u.realName || u.displayName || u.username,
        andonReason: d.andonReason,
        andonLevel: d.andonLevel || "LEVEL3",
        handlerRoleId: d.handlerRoleId,
        handlerRoleName: d.handlerRoleName,
        handlerUserId: d.handlerUserId,
        handlerUserName: d.handlerUserName,
        handlerNickName: d.handlerNickName,
        status: "ACTIVE",
        remark: d.remark,
      };
    },
    async submitOne(row) {
      if (!this.canSubmitRow(row)) {
        this.$modal.msgWarning("请选择呼叫原因");
        return;
      }
      await addAndonrecord(this.buildPayload(row));
      this.draft[row._key].submitted = true;
      this.$modal.msgSuccess("安灯已发起");
      this.$emit("submitted");
    },
    async submitBatch() {
      const rows = this.selectedRows.filter((r) => this.canSubmitRow(r));
      if (!rows.length) {
        this.$modal.msgWarning("请勾选已填原因的派工行");
        return;
      }
      for (const row of rows) {
        await addAndonrecord(this.buildPayload(row));
        this.draft[row._key].submitted = true;
      }
      this.$modal.msgSuccess(`已成功发起 ${rows.length} 条安灯`);
      this.$emit("submitted");
    },
    onSelect(rows) {
      this.selectedRows = rows;
    },
  },
};
</script>
