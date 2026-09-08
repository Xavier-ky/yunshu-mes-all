<template>
  <aside class="andon-pending-aside">
    <div class="andon-pending-aside__head">
      <strong>待处置</strong>
      <el-select v-model="levelFilter" size="small" clearable placeholder="级别" style="width:100px">
        <el-option label="LEVEL1" value="LEVEL1" />
        <el-option label="LEVEL2" value="LEVEL2" />
        <el-option label="LEVEL3" value="LEVEL3" />
      </el-select>
    </div>
    <el-input v-model="keyword" size="small" clearable placeholder="工位/工单/原因" class="andon-pending-aside__search" />
    <div v-loading="loading" class="andon-pending-aside__list">
      <div
        v-for="row in filteredRows"
        :key="row.recordId"
        class="andon-pending-item"
        :class="{ 'is-selected': selectedId === row.recordId }"
        @click="$emit('select', row)"
      >
        <div class="andon-pending-item__top">
          <el-tag size="small" :type="levelTag(row.andonLevel)">{{ row.andonLevel }}</el-tag>
          <span>{{ fmtTime(row.createTime) }}</span>
        </div>
        <strong>{{ row.andonReason }}</strong>
        <span>{{ row.workstationName }} · {{ row.workorderCode }}</span>
      </div>
      <el-empty v-if="!loading && !filteredRows.length" description="暂无待处置" />
    </div>
    <div class="andon-pending-aside__foot">
      <el-button link type="primary" @click="$emit('history')">查看已处置历史</el-button>
    </div>
  </aside>
</template>

<script>
export default {
  name: "AndonPendingAside",
  props: {
    rows: { type: Array, default: () => [] },
    selectedId: { type: [Number, String], default: null },
    loading: { type: Boolean, default: false },
  },
  emits: ["select", "history"],
  data() {
    return {
      keyword: "",
      levelFilter: "",
    };
  },
  computed: {
    filteredRows() {
      const kw = this.keyword.trim().toLowerCase();
      return this.rows.filter((r) => {
        if (this.levelFilter && r.andonLevel !== this.levelFilter) return false;
        if (!kw) return true;
        const blob = [r.workstationName, r.workstationCode, r.workorderCode, r.andonReason, r.nickName].join(" ").toLowerCase();
        return blob.includes(kw);
      });
    },
  },
  methods: {
    levelTag(level) {
      if (level === "LEVEL1") return "danger";
      if (level === "LEVEL2") return "warning";
      return "info";
    },
    fmtTime(v) {
      return v ? String(v).substring(5, 16) : "—";
    },
  },
};
</script>
