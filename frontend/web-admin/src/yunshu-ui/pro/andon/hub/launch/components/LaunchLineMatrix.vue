<template>
  <div class="andon-launch-matrix">
    <div class="andon-launch-matrix__head">
      <strong class="andon-launch-matrix__title">选择工位</strong>
      <div class="andon-matrix-legend">
        <span class="andon-matrix-legend__item"><i class="andon-matrix-legend__sq is-ok" />可拉绳</span>
        <span class="andon-matrix-legend__item"><i class="andon-matrix-legend__sq is-filter" />已选中</span>
        <span class="andon-matrix-legend__item"><i class="andon-matrix-legend__sq is-alert" />处置中</span>
      </div>
    </div>

    <div v-loading="loading" class="andon-launch-matrix__body">
      <div
        v-for="line in displayLines"
        :key="line.lineId || line.lineName"
        class="andon-launch-matrix__block"
      >
        <div class="andon-launch-matrix__bar">
          <span class="andon-matrix-block__code">{{ line.lineCode || "—" }}</span>
          <strong class="andon-matrix-block__name">{{ line.lineName }}</strong>
          <span class="andon-launch-matrix__meta">可发起 {{ line.eligibleCount }}</span>
        </div>
        <div
          class="andon-launch-matrix__cells"
          :style="{ '--andon-matrix-cols': line.gridCols }"
        >
          <div
            v-for="cell in line.stations"
            :key="cell.stationId"
            class="andon-launch-cell"
            :class="cellClass(cell)"
            @click="onCellClick(cell)"
          >
            <div class="andon-launch-cell__top">
              <span class="andon-station-cell__led" />
              <span class="andon-station-cell__type">{{ stationTypeLabel(cell.stationType) }}</span>
            </div>
            <strong class="andon-launch-cell__name">{{ cell.stationName || cell.stationCode }}</strong>
            <span class="andon-launch-cell__code">{{ cell.stationCode }}</span>
            <span class="andon-launch-cell__hint">{{ cellHint(cell) }}</span>
          </div>
        </div>
      </div>
      <el-empty v-if="!loading && !displayLines.length" description="暂无产线工位数据" />
    </div>
  </div>
</template>

<script>
const MATRIX_COLS = 8;

const STATION_TYPE_LABEL = {
  ASSEMBLY: "装配",
  TEST: "测试",
  PACKAGE: "包装",
};

export default {
  name: "LaunchLineMatrix",
  props: {
    stations: { type: Array, default: () => [] },
    lines: { type: Array, default: () => [] },
    tasks: { type: Array, default: () => [] },
    activeByStation: { type: Object, default: () => ({}) },
    loading: { type: Boolean, default: false },
    selectedKey: { type: String, default: "" },
  },
  emits: ["select"],
  computed: {
    taskByStation() {
      const map = {};
      (this.tasks || []).forEach((t) => {
        const id = String(t.stationId || t.workstationId || "");
        if (id) map[id] = t;
      });
      return map;
    },
    matrixStations() {
      const list = this.stations || [];
      if (!list.length) return [];
      const fanByCode = list.filter((s) => String(s.lineCode || "").startsWith("LINE-FAN"));
      if (fanByCode.length) return fanByCode;
      const fanLineIds = new Set(
        (this.lines || [])
          .filter((l) => String(l.lineCode || "").startsWith("LINE-FAN"))
          .map((l) => String(l.lineId)),
      );
      if (fanLineIds.size) return list.filter((s) => fanLineIds.has(String(s.lineId)));
      return list;
    },
    displayLines() {
      const byLine = {};
      this.matrixStations.forEach((s) => {
        const key = String(s.lineId || s.lineName || "unknown");
        if (!byLine[key]) {
          byLine[key] = {
            lineId: s.lineId,
            lineCode: s.lineCode,
            lineName: s.lineName || "未命名产线",
            stations: [],
            eligibleCount: 0,
            gridCols: MATRIX_COLS,
          };
        }
        const task = this.taskByStation[String(s.stationId)] || null;
        const cell = { ...s, task };
        byLine[key].stations.push(cell);
        if (this.isSelectable(cell)) byLine[key].eligibleCount += 1;
      });
      return Object.values(byLine)
        .map((line) => ({
          ...line,
          stations: [...line.stations].sort((a, b) =>
            String(a.stationCode || "").localeCompare(String(b.stationCode || ""), undefined, { numeric: true }),
          ),
        }))
        .sort((a, b) =>
          String(a.lineCode || a.lineName).localeCompare(String(b.lineCode || b.lineName), undefined, { numeric: true }),
        );
    },
  },
  methods: {
    stationTypeLabel(type) {
      return STATION_TYPE_LABEL[type] || "工位";
    },
    hasActive(station) {
      return !!this.activeByStation[String(station.stationId)];
    },
    isSelectable(cell) {
      const task = cell.task;
      if (!task || task.status === "COMPLETED") return false;
      if (this.hasActive(cell)) return false;
      return true;
    },
    cellClass(cell) {
      const classes = [];
      if (this.hasActive(cell)) classes.push("is-active-andon");
      else if (!cell.task || cell.task.status === "COMPLETED") classes.push("is-no-task");
      else classes.push("is-eligible");
      if (cell.task && cell.task._key === this.selectedKey) classes.push("is-selected");
      return classes;
    },
    cellHint(cell) {
      if (this.hasActive(cell)) return "处置中";
      if (!cell.task) return "无派工";
      if (cell.task.status === "COMPLETED") return "已完成";
      if (cell.task._key === this.selectedKey) return "已选中";
      return cell.task.workOrderNo || cell.task.workorderCode || "可拉绳";
    },
    onCellClick(cell) {
      if (!this.isSelectable(cell) || !cell.task) return;
      this.$emit("select", cell.task);
    },
  },
};
</script>
