<template>
  <div class="andon-reason-top-panel">
    <div class="andon-reason-top-panel__head">
      <strong>原因 TOP</strong>
      <span class="andon-reason-top-panel__sub">本班统计</span>
    </div>
    <div class="andon-reason-top-panel__body">
      <div class="inbound-table-frame andon-reason-table-frame">
        <span class="frame-corner frame-corner--tl" aria-hidden="true" />
        <span class="frame-corner frame-corner--tr" aria-hidden="true" />
        <span class="frame-corner frame-corner--bl" aria-hidden="true" />
        <span class="frame-corner frame-corner--br" aria-hidden="true" />
        <el-table
          class="yunshu-data-table inbound-table andon-reason-table"
          stripe
          border
          height="100%"
          :data="rows"
        >
          <el-table-column label="#" width="48" align="center" header-align="center">
            <template #default="{ $index }">{{ $index + 1 }}</template>
          </el-table-column>
          <el-table-column label="呼叫原因" prop="reason" min-width="128" align="left" header-align="center" show-overflow-tooltip />
          <el-table-column label="次数" prop="cnt" width="72" align="center" header-align="center">
            <template #default="{ row }">{{ formatCount(row) }}</template>
          </el-table-column>
        </el-table>
      </div>
    </div>
  </div>
</template>

<script>
export default {
  name: "AndonReasonTopPanel",
  props: {
    stats: { type: Object, default: () => ({}) },
  },
  computed: {
    rows() {
      return this.stats.reasonTop || [];
    },
  },
  methods: {
    formatCount(row) {
      const v = row?.cnt ?? row?.count;
      return v == null ? "—" : v;
    },
  },
};
</script>
