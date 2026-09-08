<template>

  <div class="lm-root">

    <div v-if="error" class="lm-error">{{ error }}</div>



    <div v-if="!lineId" class="lm-head">

      <div class="lm-head-row">

        <span class="lm-sub">全厂产线运行状态 · 工单进度 · 安灯 · OEE</span>

        <div class="lm-head-actions">

          <span v-if="currentShift" class="meta-tag">班次 {{ currentShift }}</span>

          <span class="meta-time"><span class="meta-time-label">当前</span><strong class="meta-live-time">{{ liveTime }}</strong></span>

          <button type="button" class="refresh-btn" :disabled="loading" @click="loadData">

            {{ loading ? "刷新中…" : "刷新" }}

          </button>

          <button type="button" class="bigscreen-btn" @click="openBigScreen">大屏模式</button>

        </div>

      </div>

      <LineKpiStrip v-if="summary?.metrics?.length" :metrics="summary.metrics" />

    </div>



    <template v-if="lineId">

      <div v-if="detail" class="lm-detail-top">

        <button type="button" class="lm-back-btn" @click="goOverview">← 返回总览</button>

        <div class="lm-line-info">

          <h2>{{ detail.line?.lineName }}</h2>

          <span class="mono">{{ detail.line?.lineCode }}</span>

          <span class="sep">·</span>

          <span>{{ detail.line?.workshopName }}</span>

          <span class="sep">·</span>

          <span>额定 {{ detail.line?.ratedCapacity }} {{ detail.line?.capacityUnit }}</span>

        </div>

      </div>

      <div v-else-if="loading" class="lm-loading">加载产线详情…</div>

    </template>



    <div v-if="!lineId" class="lm-overview-body">

      <div v-if="loading && !summary" class="lm-loading">

        <div v-for="i in 6" :key="i" class="skel"></div>

      </div>

      <LineOverview3D

        v-else-if="summary?.lines?.length"

        class="lm-3d-view"

        :lines="summary.lines"

        :initial-line-id="returnLineId"

        @select-line="goDetail"

        @create-line="handleCreateLine"

      />

      <div v-else class="lm-empty">暂无产线数据，请检查后端连接后刷新</div>

    </div>



    <template v-else-if="lineId && detail">

      <div class="lm-detail-main">

        <div class="lm-detail-3d">

          <LineDetail3D

            :line="detail.line"

            :stations="detail.stations"

            :work-orders="detail.workOrders"

            :dispatches="detail.dispatches"

            :andons="detail.andons"

            @navigate="handleNavigate"

            @switch-line="handleDetailSwitch"

          />

        </div>

        <div class="lm-detail-side">

          <section class="side-panel">

            <h3>在制工单</h3>

            <div v-if="detail.workOrders?.length" class="side-list">

              <div v-for="wo in detail.workOrders" :key="wo.workOrderId" class="side-card">

                <strong>{{ wo.workOrderNo }}</strong>

                <span>{{ wo.productName }}</span>

                <div class="side-meta">{{ wo.completedQty }} / {{ wo.planQty }} · {{ wo.status }}</div>

              </div>

            </div>

            <div v-else class="side-empty">暂无在制工单</div>

          </section>



          <section class="side-panel">

            <h3>派工进度</h3>

            <div v-if="detail.dispatches?.length" class="side-list compact">

              <div v-for="d in detail.dispatches" :key="d.dispatchId" class="side-row">

                <span>{{ d.stationName }}</span>

                <span>{{ d.completedQty }}/{{ d.plannedQty }}</span>

                <span class="mono">{{ d.operatorName }}</span>

              </div>

            </div>

            <div v-else class="side-empty">暂无派工</div>

          </section>



          <section class="side-panel">

            <div class="panel-head">

              <h3>开放安灯</h3>

              <router-link to="/app/andon" class="link">查看全部 →</router-link>

            </div>

            <div v-if="detail.andons?.length" class="side-list">

              <div v-for="a in detail.andons" :key="a.andonId" class="side-card warn">

                <strong>{{ a.andonNo }}</strong>

                <span>{{ a.typeName }} · {{ a.stationName }}</span>

                <div class="side-meta">{{ a.exceptionDesc || "—" }}</div>

              </div>

            </div>

            <div v-else class="side-empty">暂无开放安灯</div>

          </section>

        </div>

      </div>



      <LineMonitorCharts mode="detail" :hourly-output="detail.hourlyOutput" :oee="detail.oee" :events="detail.events" />

    </template>

  </div>

</template>



<script setup>

import { ref, computed, watch, onMounted, onBeforeUnmount } from "vue";

import { useRoute, useRouter } from "vue-router";

import { fetchLineMonitorSummary, fetchLineMonitorDetail } from "@/api/lineMonitor";



import LineOverview3D from "@/components/three/LineOverview3D.vue";

import LineDetail3D from "@/components/three/LineDetail3D.vue";



import LineKpiStrip from "./line-monitor/LineKpiStrip.vue";

import LineMonitorCharts from "./line-monitor/LineMonitorCharts.vue";



const POLL_MS = 30000;



const route = useRoute();

const router = useRouter();



const summary = ref(null);

const detail = ref(null);

const loading = ref(false);

const error = ref("");

const refreshedAt = ref("");

const currentShift = ref("");

const returnLineId = ref(null);

const liveTime = ref("");

let pollTimer = null;

let clockTimer = null;



function pad2(n) {

  return String(n).padStart(2, "0");

}



function tickLiveTime() {

  const d = new Date();

  liveTime.value = `${pad2(d.getHours())}:${pad2(d.getMinutes())}:${pad2(d.getSeconds())}`;

}



function startClock() {

  tickLiveTime();

  if (clockTimer) clearInterval(clockTimer);

  clockTimer = setInterval(tickLiveTime, 1000);

}



function stopClock() {

  if (clockTimer) {

    clearInterval(clockTimer);

    clockTimer = null;

  }

}



function unwrapPayload(res) {

  if (!res) return null;

  if (res.data !== undefined && res.data !== null) return res.data;

  if (Array.isArray(res.metrics) || Array.isArray(res.lines) || res.line) return res;

  return null;

}



const lineId = computed(() => {

  const id = route.query.lineId;

  return id ? Number(id) : null;

});



async function loadData() {

  loading.value = true;

  error.value = "";

  try {

    if (lineId.value) {

      const res = await fetchLineMonitorDetail(lineId.value);

      detail.value = unwrapPayload(res);

      refreshedAt.value = detail.value?.refreshedAt || "";

      currentShift.value = detail.value?.currentShiftName || "";

    } else {

      const res = await fetchLineMonitorSummary();

      summary.value = unwrapPayload(res);

      detail.value = null;

      refreshedAt.value = summary.value?.refreshedAt || "";

      currentShift.value = summary.value?.currentShiftName || "";

    }

  } catch (e) {

    error.value = e?.message || "加载失败，请稍后重试";

  } finally {

    loading.value = false;

  }

}



function goDetail(id) { router.push({ query: { lineId: id } }); }



function goOverview() {

  returnLineId.value = lineId.value;

  router.push({ query: {} });

}



function handleCreateLine({ lineCode, lineName, modelPosX, modelPosY, modelPosZ }) {

  if (summary.value) {

    const newLine = {

      lineId: Date.now(), lineCode, lineName,

      workshopName: "电风扇总装车间",

      lineStatus: "IDLE",

      currentWorkOrderNo: "", productName: "",

      planQty: 0, completedQty: 0, progressPct: 0,

      oee: 0, oeeEstimated: true,

      stationCount: 0,

      stationRatio: { running: 0, warning: 0, fault: 0, changeover: 0, idle: 0 },

      openAndonCount: 0,

      modelPosX, modelPosY, modelPosZ,

    };

    summary.value.lines.push(newLine);

  }

}



function handleNavigate(path) { router.push(path); }



function openBigScreen() { router.push("/app"); }



async function handleDetailSwitch(dir) {

  if (!summary.value) return;

  const lines = summary.value.lines;

  const curIdx = lines.findIndex((l) => l.lineId === lineId.value);

  if (curIdx < 0) return;

  const newIdx = dir > 0 ? (curIdx + 1) % lines.length : (curIdx - 1 + lines.length) % lines.length;

  const newId = lines[newIdx].lineId;

  router.replace({ query: { lineId: newId } });

  const res = await fetchLineMonitorDetail(newId);

  detail.value = unwrapPayload(res);

  refreshedAt.value = detail.value?.refreshedAt || "";

  currentShift.value = detail.value?.currentShiftName || "";

}



function startPoll() { stopPoll(); pollTimer = setInterval(() => { if (document.visibilityState === "visible") loadData(); }, POLL_MS); }

function stopPoll() { if (pollTimer) { clearInterval(pollTimer); pollTimer = null; } }



watch(lineId, () => loadData());



onMounted(() => {

  startClock();

  loadData();

  startPoll();

  document.addEventListener("visibilitychange", onVisibility);

});

onBeforeUnmount(() => {

  stopClock();

  stopPoll();

  document.removeEventListener("visibilitychange", onVisibility);

});



function onVisibility() { if (document.visibilityState === "visible") loadData(); }

</script>



<style scoped>

.lm-root {

  display: flex;

  flex-direction: column;

  gap: 8px;

  flex: 1;

  min-height: 0;

  height: 100%;

  overflow: hidden;

  padding-left: 3px;

}

.lm-head {

  flex-shrink: 0;

  padding: 10px 18px;

  background: #fff;

  border: 1px solid #e7e7e7;

}

.lm-head-row {

  display: flex;

  align-items: center;

  justify-content: space-between;

  gap: 12px;

  flex-wrap: wrap;

}

.lm-sub {

  font-family: "Bahnschrift", "DIN Alternate", "Arial Narrow", "PingFang SC", "Microsoft YaHei", sans-serif;

  font-size: 18px;

  font-weight: 700;

  color: #1a1a1a;

  letter-spacing: 0.04em;

  white-space: nowrap;

}

.lm-head-actions {

  display: flex;

  align-items: center;

  gap: 12px;

  font-size: 14px;

  color: #555;

  flex-shrink: 0;

}

.meta-tag {

  padding: 3px 10px;

  background: #f5f5f5;

  border: 1px solid #ececec;

  font-size: 13px;

  white-space: nowrap;

}

.meta-time {

  display: inline-flex;

  align-items: baseline;

  gap: 6px;

  font-size: 13px;

  color: #888;

  white-space: nowrap;

}

.meta-time-label {

  color: #888;

  font-weight: 400;

}

.meta-live-time {

  font-family: "JetBrains Mono", "Consolas", monospace;

  font-size: 15px;

  font-weight: 700;

  color: #1a1a1a;

  letter-spacing: 0.06em;

}

.lm-overview-body {

  flex: 1;

  display: flex;

  flex-direction: column;

  min-height: 0;

  overflow: hidden;

}

.lm-3d-view {

  flex: 1;

  min-height: 0;

  height: 100%;

}

.lm-empty {

  flex: 1;

  display: grid;

  place-items: center;

  background: #fff;

  border: 1px solid #e7e7e7;

  color: #999;

  font-size: 14px;

}

.refresh-btn { background: #fff; color: #1a1a1a; border: 1px solid #e7e7e7; padding: 5px 14px; font-size: 13px; cursor: pointer; }

.refresh-btn:disabled { opacity: 0.6; cursor: not-allowed; }

.bigscreen-btn { background: #2563eb; color: #fff; border: 1px solid #2563eb; padding: 5px 16px; font-size: 13px; cursor: pointer; white-space: nowrap; }

.bigscreen-btn:hover { background: #1d4ed8; }

.lm-error { background: #fef2f2; border: 1px solid #fecaca; color: #dc2626; padding: 10px 14px; font-size: 13px; flex-shrink: 0; }

.lm-loading { display: grid; grid-template-columns: repeat(auto-fit, minmax(120px, 1fr)); gap: 10px; flex: 1; align-content: start; padding: 12px; background: #fff; border: 1px solid #e7e7e7; color: #888; font-size: 14px; }

.skel { height: 72px; background: linear-gradient(90deg, #f0f0f0 25%, #e8e8e8 50%, #f0f0f0 75%); background-size: 200% 100%; animation: shimmer 1.2s infinite; }

@keyframes shimmer { 0% { background-position: 200% 0; } 100% { background-position: -200% 0; } }

.lm-detail-top { display: flex; align-items: center; gap: 16px; flex-shrink: 0; }

.lm-back-btn { background: #fff; border: 1px solid #e7e7e7; padding: 6px 12px; font-size: 13px; cursor: pointer; color: #2563eb; white-space: nowrap; }

.lm-line-info { display: flex; align-items: baseline; flex-wrap: wrap; gap: 8px; }

.lm-line-info h2 { margin: 0; font-size: 18px; font-weight: 600; }

.mono { font-family: "JetBrains Mono", monospace; font-size: 12px; color: #999; }

.sep { color: #ccc; }

.lm-detail-main { display: grid; grid-template-columns: 1fr 340px; gap: 12px; margin-bottom: 12px; flex: 1; min-height: 0; overflow: auto; }

.lm-detail-3d { min-height: 0; }

.lm-detail-side { display: grid; gap: 12px; align-content: start; }

.side-panel { background: #fff; border: 1px solid #e7e7e7; padding: 14px 16px; }

.side-panel h3 { margin: 0 0 12px; font-size: 13px; font-weight: 600; }

.panel-head { display: flex; justify-content: space-between; align-items: center; margin-bottom: 12px; }

.panel-head h3 { margin: 0; }

.link { font-size: 12px; color: #2563eb; text-decoration: none; }

.side-list { display: grid; gap: 8px; }

.side-card { padding: 8px 10px; background: #fafafa; border: 1px solid #f0f0f0; font-size: 12px; display: grid; gap: 2px; }

.side-card.warn { border-color: #fecaca; background: #fef2f2; }

.side-card strong { font-size: 13px; }

.side-meta { color: #888; font-size: 11px; }

.side-row { display: grid; grid-template-columns: 1fr auto auto; gap: 8px; font-size: 12px; padding: 4px 0; border-bottom: 1px solid #f5f5f5; }

.side-empty { color: #ccc; font-size: 12px; text-align: center; padding: 16px; }

@media (max-width: 960px) { .lm-detail-main { grid-template-columns: 1fr; } }

</style>


