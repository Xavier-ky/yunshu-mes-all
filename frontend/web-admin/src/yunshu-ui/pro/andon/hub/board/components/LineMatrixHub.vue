<template>
  <div class="andon-matrix-main">
    <div class="andon-matrix-head">
      <strong class="andon-matrix-head__title">产线工位矩阵</strong>
      <div class="andon-matrix-legend">
        <span class="andon-matrix-legend__item"><i class="andon-matrix-legend__sq is-ok" />正常</span>
        <span class="andon-matrix-legend__item"><i class="andon-matrix-legend__sq is-warn" />15分+</span>
        <span class="andon-matrix-legend__item"><i class="andon-matrix-legend__sq is-alert" />告警/30分+</span>
        <span v-if="categoryFilter" class="andon-matrix-legend__item"><i class="andon-matrix-legend__sq is-filter" />筛选中</span>
      </div>
      <div class="andon-matrix-head__actions">
        <span v-if="categoryFilter" class="andon-matrix-head__filter">筛选：{{ categoryLabel }}</span>
        <el-button v-if="categoryFilter" class="operator-ui-btn" size="small" :auto-insert-space="false" @click="$emit('clear-filter')">
          <span class="operator-ui-btn__label">清除筛选</span>
        </el-button>
      </div>
    </div>

    <div class="andon-matrix-kpi">
      <div v-for="(item, idx) in kpiCards" :key="item.key" class="andon-matrix-kpi__item" :class="{ 'is-last': idx === kpiCards.length - 1 }">
        <span class="andon-matrix-kpi__label">{{ item.label }}</span>
        <strong class="andon-matrix-kpi__value" :class="item.tone">{{ item.value }}</strong>
      </div>
    </div>

    <div v-loading="loading" class="andon-matrix-body">
      <div
        v-for="line in displayLines"
        :key="line.lineId || line.lineName"
        class="andon-matrix-block"
        :class="lineHeadClass(line)"
      >
        <div class="andon-matrix-block__bar" :class="lineHeadClass(line)">
          <span class="andon-matrix-block__code">{{ line.lineCode || "—" }}</span>
          <strong class="andon-matrix-block__name">{{ line.lineName }}</strong>
          <span class="andon-matrix-bar-status" :class="lineHeadClass(line)">
            <span class="andon-matrix-bar-status__lamp" />
            <span class="andon-matrix-stamp" :class="lineStampClass(line)">
              {{ lineStatusText(line) }}
            </span>
          </span>
          <span class="andon-matrix-block__meta">{{ line.stations.length }} 工位</span>
          <span class="andon-matrix-block__meta">正常 {{ line.okCount }}</span>
          <span v-if="line.alertCount > 0" class="andon-matrix-block__meta" :class="alertMetaClass(line)">{{ line.alertCount }} 告警</span>
          <span v-if="lineWorstMinutes(line) > 0" class="andon-matrix-block__meta is-wait" :class="waitMetaClass(line)">最长 {{ lineWorstMinutes(line) }} 分</span>
        </div>
        <div class="andon-matrix-row">
          <div
            class="andon-matrix-row__cells"
            :style="{ '--andon-matrix-cols': line.gridCols }"
          >
            <el-tooltip
              v-for="s in line.stations"
              :key="s.stationId"
              :content="tooltipText(s)"
              placement="top"
              :disabled="s.andonStatus !== 'ALERT'"
            >
              <div
                class="andon-station-cell"
                :class="cellClass(s)"
                @click="onCellClick(s)"
              >
                <div class="andon-station-cell__top">
                  <span class="andon-station-cell__led" />
                  <span class="andon-station-cell__type">{{ stationTypeLabel(s.stationType) }}</span>
                </div>
                <strong class="andon-station-cell__name">{{ s.stationName || s.stationCode }}</strong>
                <span class="andon-station-cell__code">{{ s.stationCode }}</span>
                <span v-if="s.andonStatus === 'ALERT'" class="andon-station-cell__alert">
                  {{ s.elapsedMinutes ?? 0 }}分 · {{ shortReason(s.andonReason) }}
                </span>
                <span v-else class="andon-station-cell__status">正常</span>
              </div>
            </el-tooltip>
          </div>
        </div>
      </div>
      <el-empty v-if="!loading && !displayLines.length" description="暂无产线工位数据" />
    </div>
  </div>
</template>

<script>
import { categorizeReason } from "../../../utils/category";

const SLA_MINUTES = 30;
const WARN_MINUTES = 15;
const MATRIX_COLS = 8;

const STATION_TYPE_LABEL = {
  ASSEMBLY: "装配",
  TEST: "测试",
  PACKAGE: "包装",
};

export default {
  name: "LineMatrixHub",
  props: {
    stations: { type: Array, default: () => [] },
    lines: { type: Array, default: () => [] },
    loading: { type: Boolean, default: false },
    categoryFilter: { type: String, default: "" },
  },
  emits: ["select-station", "select-alert", "clear-filter"],
  computed: {
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
      if (fanLineIds.size) {
        return list.filter((s) => fanLineIds.has(String(s.lineId)));
      }
      const excludedLineIds = new Set(
        (this.lines || [])
          .filter((l) => /^Assembly Line/i.test(String(l.lineName || "")))
          .map((l) => String(l.lineId)),
      );
      if (excludedLineIds.size) {
        const filtered = list.filter((s) => !excludedLineIds.has(String(s.lineId)));
        if (filtered.length) return filtered;
      }
      return list;
    },
    categoryLabel() {
      const map = { material: "物料", quality: "质量", equipment: "设备", process: "工艺", other: "其他" };
      return map[this.categoryFilter] || this.categoryFilter;
    },
    kpiCards() {
      const stations = this.matrixStations;
      const alertStations = stations.filter((s) => s.andonStatus === "ALERT");
      const alertCount = alertStations.length;
      const warnCount = alertStations.filter((s) => {
        const m = Number(s.elapsedMinutes) || 0;
        return m >= WARN_MINUTES && m < SLA_MINUTES;
      }).length;
      const lineIds = new Set(stations.map((s) => s.lineId));
      const worst = alertStations.length
        ? Math.max(...alertStations.map((s) => Number(s.elapsedMinutes) || 0))
        : 0;
      const waitTone = worst >= SLA_MINUTES ? "tone-red" : worst >= WARN_MINUTES ? "tone-warn" : "";
      const alertTone = alertCount === 0 ? "tone-green" : warnCount > 0 && worst < SLA_MINUTES ? "tone-warn" : "tone-red";
      return [
        { key: "lines", label: "产线数", value: lineIds.size, tone: "" },
        { key: "stations", label: "工位总数", value: stations.length, tone: "" },
        { key: "alerts", label: "告警工位", value: alertCount, tone: alertTone },
        { key: "warn", label: "15分+预警", value: warnCount, tone: warnCount > 0 ? "tone-warn" : "" },
      ];
    },
    groupedLines() {
      const byLine = {};
      this.matrixStations.forEach((s) => {
        const key = String(s.lineId || s.lineName || "unknown");
        if (!byLine[key]) {
          byLine[key] = {
            lineId: s.lineId,
            lineCode: s.lineCode,
            lineName: s.lineName || "未命名产线",
            stations: [],
            alertCount: 0,
            okCount: 0,
          };
        }
        byLine[key].stations.push(s);
        if (s.andonStatus === "ALERT") byLine[key].alertCount += 1;
        else byLine[key].okCount += 1;
      });
      return Object.values(byLine)
        .map((line) => ({
        ...line,
        stations: [...line.stations].sort((a, b) =>
          String(a.stationCode || "").localeCompare(String(b.stationCode || ""), undefined, { numeric: true }),
        ),
      }));
    },
    displayLines() {
      const meta = this.lines || [];
      const merged = this.groupedLines.map((g) => {
        const m = meta.find((l) => String(l.lineId) === String(g.lineId) || l.lineName === g.lineName);
        return {
          ...g,
          lineCode: m?.lineCode ?? g.lineCode,
          alertCount: m?.alertCount ?? g.alertCount,
          worstElapsed: m?.worstElapsed ?? 0,
          stationCount: m?.stationCount ?? g.stations.length,
          gridCols: MATRIX_COLS,
        };
      });
      if (!this.categoryFilter) {
        return merged.sort((a, b) =>
          String(a.lineCode || a.lineName).localeCompare(String(b.lineCode || b.lineName), undefined, { numeric: true }),
        );
      }
      return merged
        .map((line) => ({
          ...line,
          stations: line.stations.filter((s) => this.matchesFilter(s)),
        }))
        .filter((line) => line.stations.length);
    },
  },
  methods: {
    stationTypeLabel(type) {
      return STATION_TYPE_LABEL[type] || "工位";
    },
    shortReason(reason) {
      const t = String(reason || "");
      return t.length > 8 ? `${t.slice(0, 8)}…` : t || "安灯";
    },
    matchesFilter(station) {
      if (!this.categoryFilter) return true;
      if (station.andonStatus !== "ALERT") return false;
      return categorizeReason(station.andonReason) === this.categoryFilter;
    },
    lineWorstMinutes(line) {
      const alerts = (line.stations || []).filter((s) => s.andonStatus === "ALERT");
      if (!alerts.length) return 0;
      return Math.max(...alerts.map((s) => Number(s.elapsedMinutes) || 0));
    },
    lineStatusText(line) {
      if (line.alertCount === 0) return "运行中";
      const w = this.lineWorstMinutes(line);
      if (w >= SLA_MINUTES) return "严重异常";
      if (w >= WARN_MINUTES) return "15分+异常";
      return "异常";
    },
    lineHeadClass(line) {
      if (line.alertCount === 0) return "is-ok";
      const w = this.lineWorstMinutes(line);
      if (w >= SLA_MINUTES) return "is-alert";
      if (w >= WARN_MINUTES) return "is-warn";
      return "is-early";
    },
    lineStampClass(line) {
      return this.lineHeadClass(line);
    },
    waitMetaClass(line) {
      const w = this.lineWorstMinutes(line);
      if (w >= SLA_MINUTES) return "is-critical";
      if (w >= WARN_MINUTES) return "is-warn";
      return "";
    },
    alertMetaClass(line) {
      const w = this.lineWorstMinutes(line);
      return w >= SLA_MINUTES ? "is-alert" : "is-warn-meta";
    },
    cellClass(station) {
      const classes = [];
      if (station.andonStatus === "ALERT") {
        const m = Number(station.elapsedMinutes) || 0;
        if (m >= SLA_MINUTES) classes.push("is-critical");
        else if (m >= WARN_MINUTES) classes.push("is-warn");
        else classes.push("is-early");
      } else {
        classes.push("is-ok");
      }
      if (!this.matchesFilter(station) && this.categoryFilter) classes.push("is-dimmed");
      return classes;
    },
    tooltipText(s) {
      if (s.andonStatus !== "ALERT") return `${s.stationName || s.stationCode} · 点击发起安灯`;
      return `${s.andonReason || "安灯中"} · 已等待 ${s.elapsedMinutes ?? 0} 分钟`;
    },
    onCellClick(s) {
      if (s.andonStatus === "ALERT" && s.activeRecordId) {
        this.$emit("select-alert", s);
      } else {
        this.$emit("select-station", s);
      }
    },
  },
};
</script>
