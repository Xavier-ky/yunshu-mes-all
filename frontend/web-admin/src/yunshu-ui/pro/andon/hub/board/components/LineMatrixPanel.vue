<template>
  <div class="andon-line-matrix">
    <div class="andon-line-matrix__head">
      <strong>产线工位矩阵</strong>
      <span v-if="categoryFilter" class="andon-line-matrix__filter">
        筛选：{{ categoryLabel }}
        <el-button link type="primary" size="small" @click="$emit('clear-filter')">清除</el-button>
      </span>
    </div>
    <div v-loading="loading" class="andon-line-matrix__body">
      <div v-for="line in displayLines" :key="line.lineId || line.lineName" class="andon-line-block">
        <div class="andon-line-block__head" :class="lineHeadClass(line)">
          <span class="andon-line-block__lamp" />
          <strong>{{ line.lineName }}</strong>
          <el-tag size="small" :type="line.alertCount > 0 ? 'danger' : 'success'">
            {{ line.alertCount > 0 ? `${line.alertCount} 告警` : "正常" }}
          </el-tag>
        </div>
        <div class="andon-line-block__tiles">
          <el-tooltip
            v-for="s in line.stations"
            :key="s.stationId"
            :content="tooltipText(s)"
            placement="top"
            :disabled="s.andonStatus !== 'ALERT'"
          >
            <div
              class="andon-station-tile"
              :class="[s.andonStatus === 'ALERT' ? 'is-alert' : 'is-ok', { 'is-dimmed': !matchesFilter(s) }]"
              @click="onTileClick(s)"
            >
              <span class="andon-station-tile__led" />
              <strong>{{ s.stationName || s.stationCode }}</strong>
              <span class="andon-station-tile__code">{{ s.stationCode }}</span>
            </div>
          </el-tooltip>
        </div>
      </div>
      <el-empty v-if="!loading && !displayLines.length" description="暂无产线工位数据" />
    </div>
  </div>
</template>

<script>
import { categorizeReason } from "../../../utils/category";

export default {
  name: "LineMatrixPanel",
  props: {
    stations: { type: Array, default: () => [] },
    lines: { type: Array, default: () => [] },
    loading: { type: Boolean, default: false },
    categoryFilter: { type: String, default: "" },
  },
  emits: ["select-station", "select-alert", "clear-filter"],
  computed: {
    categoryLabel() {
      const map = { material: "物料", quality: "质量", equipment: "设备", process: "工艺", other: "其他" };
      return map[this.categoryFilter] || this.categoryFilter;
    },
    groupedLines() {
      const byLine = {};
      (this.stations || []).forEach((s) => {
        const key = String(s.lineId || s.lineName || "unknown");
        if (!byLine[key]) {
          byLine[key] = {
            lineId: s.lineId,
            lineName: s.lineName || "未命名产线",
            stations: [],
            alertCount: 0,
          };
        }
        byLine[key].stations.push(s);
        if (s.andonStatus === "ALERT") byLine[key].alertCount += 1;
      });
      return Object.values(byLine);
    },
    displayLines() {
      const meta = this.lines || [];
      const merged = this.groupedLines.map((g) => {
        const m = meta.find((l) => String(l.lineId) === String(g.lineId) || l.lineName === g.lineName);
        return { ...g, alertCount: m?.alertCount ?? g.alertCount, worstElapsed: m?.worstElapsed ?? 0 };
      });
      if (!this.categoryFilter) return merged;
      return merged
        .map((line) => ({
          ...line,
          stations: line.stations.filter((s) => this.matchesFilter(s)),
        }))
        .filter((line) => line.stations.length);
    },
  },
  methods: {
    matchesFilter(station) {
      if (!this.categoryFilter) return true;
      if (station.andonStatus !== "ALERT") return false;
      return categorizeReason(station.andonReason) === this.categoryFilter;
    },
    lineHeadClass(line) {
      if (line.alertCount > 0) return "is-alert";
      return "is-ok";
    },
    tooltipText(s) {
      if (s.andonStatus !== "ALERT") return "";
      return `${s.andonReason || "安灯中"} · 已等待 ${s.elapsedMinutes ?? 0} 分钟`;
    },
    onTileClick(s) {
      if (s.andonStatus === "ALERT" && s.activeRecordId) {
        this.$emit("select-alert", s);
      } else {
        this.$emit("select-station", s);
      }
    },
  },
};
</script>
