<template>
  <div class="andon-line-grid">
    <div class="andon-line-grid__head">
      <strong>工位状态</strong>
      <span class="andon-line-grid__legend">
        <i class="dot dot--ok"></i>正常
        <i class="dot dot--alert"></i>安灯中
      </span>
    </div>
    <div v-loading="loading" class="andon-line-grid__tiles">
      <div
        v-for="s in stations"
        :key="s.stationId"
        class="andon-station-tile"
        :class="s.andonStatus === 'ALERT' ? 'is-alert' : 'is-ok'"
        @click="$emit('select', s)"
      >
        <span class="andon-station-tile__line">{{ s.lineName }}</span>
        <strong>{{ s.stationName || s.stationCode }}</strong>
        <span class="andon-station-tile__code">{{ s.stationCode }}</span>
      </div>
      <el-empty v-if="!loading && !stations.length" description="暂无工位数据" />
    </div>
  </div>
</template>

<script>
export default {
  name: "LineStatusGrid",
  props: {
    stations: { type: Array, default: () => [] },
    loading: { type: Boolean, default: false },
  },
  emits: ["select"],
};
</script>
