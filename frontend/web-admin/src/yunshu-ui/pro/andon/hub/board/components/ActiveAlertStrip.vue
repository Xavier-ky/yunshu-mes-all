<template>
  <div class="andon-alert-strip" :class="{ 'is-all-clear': !loading && !displayAlerts.length }">
    <div class="andon-alert-strip__head">
      <strong>活动告警</strong>
      <span v-if="!loading && !displayAlerts.length" class="andon-alert-strip__all-clear">
        <span class="andon-alert-strip__ok-dot" /> 全线正常
      </span>
      <span v-else-if="displayAlerts.length" class="andon-alert-strip__meta">
        共 {{ displayAlerts.length }} 条待处置
      </span>
    </div>

    <div v-if="!loading && !displayAlerts.length" class="andon-alert-strip__empty-bar">
      当前无待处置安灯，产线运行正常
    </div>

    <div
      v-else
      ref="scrollViewport"
      v-loading="loading"
      class="andon-alert-strip__scroll is-marquee"
      @mouseenter="onPause"
      @mouseleave="onResume"
    >
      <div
        ref="track"
        class="andon-alert-strip__track"
        :style="trackStyle"
      >
        <div
          v-for="item in scrollItems"
          :key="`${item.recordId}-${item._dupKey}`"
          class="andon-alert-card"
          :class="urgencyClass(item)"
          @click="$emit('select', item)"
        >
          <div class="andon-alert-card__top">
            <span class="andon-alert-card__chip" :class="reasonToneClass(item.andonReason)">
              {{ categoryLabel(item.andonReason) }}
            </span>
            <el-tag size="small" :type="levelTag(item.andonLevel)">{{ item.andonLevel || "—" }}</el-tag>
            <span class="andon-alert-card__elapsed">{{ liveElapsed(item) }} 分钟</span>
          </div>
          <strong class="andon-alert-card__reason">{{ item.andonReason }}</strong>
          <div class="andon-alert-card__sla">
            <div class="andon-alert-card__sla-bar" :style="slaBarStyle(item)" />
          </div>
          <div class="andon-alert-card__meta">
            {{ item.workstationName || item.workstationCode }} · {{ item.workorderCode || "—" }}
          </div>
          <div class="andon-alert-card__meta">发起人 {{ item.nickName || "—" }}</div>
        </div>
      </div>
    </div>
  </div>
</template>

<script>
import { categorizeReason, CATEGORIES, reasonTone } from "../../../utils/category";

const SLA_MINUTES = 30;
/** 轮播速度 px/s，略快于原先 CSS 动画 */
const SCROLL_SPEED = 62;

export default {
  name: "ActiveAlertStrip",
  props: {
    alerts: { type: Array, default: () => [] },
    loading: { type: Boolean, default: false },
    categoryFilter: { type: String, default: "" },
  },
  emits: ["select"],
  data() {
    return {
      tick: 0,
      tickTimer: null,
      pauseScroll: false,
      scrollOffset: 0,
      halfCycleWidth: 0,
      copySets: 2,
      rafId: null,
      lastFrameTime: 0,
      resizeObserver: null,
    };
  },
  computed: {
    displayAlerts() {
      let list = this.alerts || [];
      if (this.categoryFilter) {
        list = list.filter((a) => categorizeReason(a.andonReason) === this.categoryFilter);
      }
      return list;
    },
    scrollItems() {
      const base = this.displayAlerts;
      if (!base.length) return [];
      const items = [];
      for (let set = 0; set < this.copySets; set += 1) {
        base.forEach((a, idx) => {
          items.push({ ...a, _dupKey: `${set}-${idx}` });
        });
      }
      return items;
    },
    trackStyle() {
      return {
        transform: `translate3d(${this.scrollOffset}px, 0, 0)`,
      };
    },
  },
  watch: {
    displayAlerts: {
      handler() {
        this.scrollOffset = 0;
        this.$nextTick(() => this.setupMarquee());
      },
      deep: true,
    },
    loading(v) {
      if (!v) this.$nextTick(() => this.setupMarquee());
    },
  },
  mounted() {
    this.tickTimer = setInterval(() => { this.tick += 1; }, 30000);
    this.$nextTick(() => {
      this.setupMarquee();
      this.startScroll();
      if (typeof ResizeObserver !== "undefined" && this.$refs.scrollViewport) {
        this.resizeObserver = new ResizeObserver(() => this.setupMarquee());
        this.resizeObserver.observe(this.$refs.scrollViewport);
      }
    });
  },
  beforeUnmount() {
    if (this.tickTimer) clearInterval(this.tickTimer);
    this.stopScroll();
    this.resizeObserver?.disconnect();
  },
  methods: {
    onPause() {
      this.pauseScroll = true;
    },
    onResume() {
      this.pauseScroll = false;
      this.lastFrameTime = 0;
    },
    setupMarquee() {
      const viewport = this.$refs.scrollViewport;
      const track = this.$refs.track;
      const count = this.displayAlerts.length;
      if (!viewport || !track || !count) {
        this.halfCycleWidth = 0;
        return;
      }

      const viewportWidth = viewport.clientWidth || 0;
      const approxCardStep = 230;
      let sets = 2;
      while (sets * count * approxCardStep < viewportWidth * 2 && sets < 8) {
        sets += 2;
      }
      if (sets !== this.copySets) {
        this.copySets = sets;
        this.scrollOffset = 0;
        this.$nextTick(() => this.measureCycle());
        return;
      }
      this.measureCycle();
    },
    measureCycle() {
      const track = this.$refs.track;
      if (!track || !this.scrollItems.length) {
        this.halfCycleWidth = 0;
        return;
      }
      this.halfCycleWidth = track.scrollWidth / 2;
      if (this.halfCycleWidth > 0) {
        while (this.scrollOffset <= -this.halfCycleWidth) {
          this.scrollOffset += this.halfCycleWidth;
        }
      }
    },
    startScroll() {
      this.stopScroll();
      const step = (timestamp) => {
        if (!this.lastFrameTime) this.lastFrameTime = timestamp;
        const delta = Math.min(0.05, (timestamp - this.lastFrameTime) / 1000);
        this.lastFrameTime = timestamp;

        if (!this.pauseScroll && this.halfCycleWidth > 0) {
          this.scrollOffset -= SCROLL_SPEED * delta;
          while (this.scrollOffset <= -this.halfCycleWidth) {
            this.scrollOffset += this.halfCycleWidth;
          }
        }

        this.rafId = requestAnimationFrame(step);
      };
      this.rafId = requestAnimationFrame(step);
    },
    stopScroll() {
      if (this.rafId) {
        cancelAnimationFrame(this.rafId);
        this.rafId = null;
      }
    },
    liveElapsed(item) {
      void this.tick;
      if (!item.createTime) return item.elapsedMinutes ?? 0;
      const start = new Date(String(item.createTime).replace(" ", "T")).getTime();
      if (Number.isNaN(start)) return item.elapsedMinutes ?? 0;
      return Math.max(0, Math.floor((Date.now() - start) / 60000));
    },
    urgencyClass(item) {
      const m = this.liveElapsed(item);
      if (m >= SLA_MINUTES) return "is-critical";
      if (m >= 15) return "is-warn";
      return "";
    },
    slaBarStyle(item) {
      const m = this.liveElapsed(item);
      const pct = Math.min(100, (m / SLA_MINUTES) * 100);
      let color = "#22c55e";
      if (m >= SLA_MINUTES) color = "#fca5a5";
      else if (m >= 15) color = "#fbbf24";
      return { width: `${pct}%`, background: color };
    },
    reasonToneClass(reason) {
      return reasonTone(reason);
    },
    categoryLabel(reason) {
      const key = categorizeReason(reason);
      return CATEGORIES.find((c) => c.key === key)?.label || "其他";
    },
    levelTag(level) {
      if (level === "LEVEL1") return "danger";
      if (level === "LEVEL2") return "warning";
      return "info";
    },
  },
};
</script>
