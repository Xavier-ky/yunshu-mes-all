<template>
  <div class="qc-defect-level-panel">
    <div class="qc-defect-level-panel__head">
      <strong>缺陷等级分布</strong>
      <span class="qc-defect-level-panel__sub">近 7 日</span>
      <span v-if="total > 0" class="qc-defect-level-panel__total">累计 <b>{{ total }}</b> 件</span>
    </div>
    <div v-if="total <= 0" class="qc-defect-level-panel__body qc-defect-level-panel__body--empty">近 7 日无缺陷记录</div>
    <div v-else class="qc-defect-level-panel__body">
      <div class="qc-defect-level-panel__bar">
        <div
          v-for="seg in segments"
          :key="seg.level"
          class="qc-defect-level-panel__seg"
          :class="`qc-defect-level-panel__seg--${seg.level.toLowerCase()}`"
          :style="{ flex: seg.cnt || 0.001 }"
          :title="`${seg.label}: ${seg.cnt}`"
        />
      </div>
      <div class="qc-defect-level-panel__legend">
        <span v-for="seg in segments" :key="seg.level" class="qc-defect-level-panel__item">
          <i :class="`qc-defect-level-panel__dot--${seg.level.toLowerCase()}`" />
          {{ seg.label }}
          <b>{{ seg.cnt }}</b>
          <em>{{ seg.pct }}%</em>
        </span>
      </div>
      <div v-if="topDefects.length" class="qc-defect-level-panel__footer">
        <span class="qc-defect-level-panel__footer-label">主要缺陷</span>
        <span class="qc-defect-level-panel__tags">
          <span
            v-for="d in topDefects"
            :key="d.defectName"
            class="qc-defect-level-panel__tag"
            :class="`qc-defect-level-panel__tag--${(d.defectLevel || 'min').toLowerCase()}`"
          >
            {{ d.defectName }}<em>{{ d.cnt }}</em>
          </span>
        </span>
      </div>
    </div>
  </div>
</template>

<script>
const LEVELS = [
  { level: "CR", label: "致命 CR" },
  { level: "MAJ", label: "严重 MAJ" },
  { level: "MIN", label: "轻微 MIN" },
];

export default {
  name: "QcDefectLevelPanel",
  props: {
    levels: { type: Array, default: () => [] },
    defectTop: { type: Array, default: () => [] },
  },
  computed: {
    mapByLevel() {
      const m = {};
      (this.levels || []).forEach((r) => {
        const k = String(r.level || r.defectLevel || "").toUpperCase();
        if (k) m[k] = Number(r.cnt) || 0;
      });
      return m;
    },
    total() {
      return LEVELS.reduce((s, l) => s + (this.mapByLevel[l.level] || 0), 0);
    },
    segments() {
      const t = this.total || 1;
      return LEVELS.map((l) => {
        const cnt = this.mapByLevel[l.level] || 0;
        return {
          ...l,
          cnt,
          pct: Math.round((cnt / t) * 1000) / 10,
        };
      });
    },
    topDefects() {
      return (this.defectTop || []).slice(0, 3);
    },
  },
};
</script>
