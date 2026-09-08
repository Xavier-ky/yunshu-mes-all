<template>
  <div class="bs-root" :class="{ 'bs-root--embedded': embedded }">
    <!-- 背景网格层 -->
    <div class="bs-3d-bg"></div>

    <!-- HUD 层 -->
    <div class="bs-hud">
      <!-- 顶部标题栏 -->
      <header class="bs-header">
        <div class="bs-header-deco bs-header-deco--left"></div>
        <div class="bs-header-meta">
          <span class="bs-header-date">{{ headerDate }}</span>
          <span class="bs-header-weekday">{{ headerWeekday }}</span>
          <span class="bs-header-divider" aria-hidden="true"></span>
          <span class="bs-header-time">{{ headerTime }}</span>
          <span class="bs-header-shift" v-if="shiftName">当前班次 · {{ shiftName }}</span>
        </div>
        <div class="bs-header-main">
          <span class="bs-header-title">数字车间监控大屏</span>
          <span class="bs-header-sub">Digital Workshop Monitoring Center</span>
        </div>
        <div class="bs-header-deco bs-header-deco--right"></div>
      </header>

      <div v-if="dataHint" class="bs-data-hint" :class="dataHintLevel">{{ dataHint }}</div>

      <!-- 顶部指标卡片区 -->
      <div class="bs-top-bar">
        <!-- 生产概况（monitor metrics） -->
        <div class="bs-top-card personnel">
          <div class="bs-card-title">
            <span class="bs-card-icon">
              <svg viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="1.5">
                <rect x="3" y="3" width="7" height="7"></rect>
                <rect x="14" y="3" width="7" height="7"></rect>
                <rect x="14" y="14" width="7" height="7"></rect>
                <rect x="3" y="14" width="7" height="7"></rect>
              </svg>
            </span>
            生产概况
          </div>
          <div class="bs-card-body">
            <div class="bs-metric-row">
              <div class="bs-metric" v-for="m in overviewMetrics" :key="m.label">
                <span class="bs-metric-label">{{ m.label }}</span>
                <span class="bs-metric-value" :class="m.tone === 'WARNING' || m.tone === 'ALERT' ? 'warn' : ''">
                  {{ m.value }}<small v-if="m.unit">{{ m.unit }}</small>
                </span>
              </div>
            </div>
          </div>
        </div>

        <!-- 今日指标达成（dashboard metrics） -->
        <div class="bs-top-card kpi-group">
          <div class="bs-kpi-header">
            <span class="bs-kpi-date">{{ now.slice(0, 11) }} 指标达成情况</span>
          </div>
          <div class="bs-kpi-grid">
            <div class="bs-kpi-item" v-for="k in kpiItems" :key="k.label">
              <div class="bs-kpi-icon" :style="{ '--c': k.color }">
                <svg viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="1.5" v-html="k.icon"></svg>
              </div>
              <div class="bs-kpi-info">
                <span class="bs-kpi-label">{{ k.label }}</span>
                <span class="bs-kpi-value">{{ k.value }}<small>{{ k.unit }}</small></span>
                <span class="bs-kpi-trend" :class="k.trendClass">{{ k.trendText }}</span>
              </div>
            </div>
          </div>
        </div>

        <!-- 产出情况（monitor metrics） -->
        <div class="bs-top-card output">
          <div class="bs-card-title">
            <span class="bs-card-icon">
              <svg viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="1.5">
                <rect x="2" y="7" width="20" height="14" rx="2" ry="2"></rect>
                <path d="M16 21V5a2 2 0 0 0-2-2h-4a2 2 0 0 0-2 2v16"></path>
              </svg>
            </span>
            产出情况
          </div>
          <div class="bs-card-body">
            <div class="bs-metric-row">
              <div class="bs-metric" v-for="m in outputMetrics" :key="m.label">
                <span class="bs-metric-label">{{ m.label }}</span>
                <span class="bs-metric-value">{{ m.value }}<small v-if="m.unit">{{ m.unit }}</small></span>
              </div>
            </div>
          </div>
        </div>
      </div>

      <!-- 主体内容区 -->
      <div class="bs-body">
        <!-- 左侧栏：产品销售市场 -->
        <aside class="bs-side bs-side--left">
          <div class="bs-panel">
            <div class="bs-panel-head">
              <span class="bs-panel-title">产品销售市场</span>
              <span class="bs-panel-sub">全国销售分布</span>
            </div>
            <div class="bs-panel-body bs-sales-body">
              <div ref="mapRef" class="bs-sales-map"></div>
              <div ref="cityBarRef" class="bs-sales-bar"></div>
            </div>
          </div>
        </aside>

        <!-- 中间区域 -->
        <main class="bs-center bs-panel">
          <div class="bs-panel-head bs-center-head">
            <span class="bs-panel-title">车间 3D 总览</span>
            <div class="bs-center-tabs">
              <button
                v-for="tab in centerTabs"
                :key="tab"
                class="bs-tab"
                :class="{ active: activeTab === tab }"
                @click="activeTab = tab"
              >
                {{ tab }}
              </button>
            </div>
          </div>
          <div class="bs-panel-body bs-center-body">
            <!-- 3D 模型容器 -->
            <div class="bs-3d-wrap" :class="{ 'bs-3d-hidden': activeTab !== '车间概览' }">
              <div class="bs-3d-frame">
                <div class="bs-3d-corner bs-3d-corner--tl"></div>
                <div class="bs-3d-corner bs-3d-corner--tr"></div>
                <div class="bs-3d-corner bs-3d-corner--bl"></div>
                <div class="bs-3d-corner bs-3d-corner--br"></div>
                <div class="bs-3d-inner">
                  <LineOverview3D :lines="lines" :initial-line-id="null" @select-line="goDetail" />
                </div>
              </div>
            </div>

            <!-- 单产线详情视图 -->
            <div v-if="activeTab !== '车间概览'" class="bs-line-detail">
              <template v-if="activeLine">
                <div class="bs-line-detail-head">
                  <div class="bs-line-detail-title">
                    <span class="bs-line-detail-name">{{ activeLine.lineName }}</span>
                    <span class="bs-status-badge" :class="statusClass(activeLine.lineStatus)">
                      {{ statusText(activeLine.lineStatus) }}
                    </span>
                  </div>
                  <button class="bs-back-btn" @click="activeTab = '车间概览'">
                    <svg viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2">
                      <path d="M9 14l-5-5 5-5"></path>
                      <path d="M4 9h10.5a4.5 4.5 0 0 1 0 9H11"></path>
                    </svg>
                    返回车间概览
                  </button>
                </div>
                <div class="bs-line-detail-grid">
                  <div class="bs-ld-item"><span>当前工单</span><b>{{ activeLine.currentWorkOrderNo || "—" }}</b></div>
                  <div class="bs-ld-item"><span>生产产品</span><b>{{ activeLine.productName || "—" }}</b></div>
                  <div class="bs-ld-item"><span>计划数量</span><b>{{ activeLine.planQty ?? "—" }}</b></div>
                  <div class="bs-ld-item"><span>完工数量</span><b>{{ activeLine.completedQty ?? "—" }}</b></div>
                  <div class="bs-ld-item"><span>OEE</span><b>{{ fmtNum(activeLine.oee) }}%</b></div>
                  <div class="bs-ld-item"><span>待处理安灯</span><b>{{ activeLine.openAndonCount ?? 0 }} 件</b></div>
                </div>
                <div class="bs-ld-progress">
                  <div class="bs-progress-label">
                    <span>生产进度</span>
                    <span class="bs-progress-pct">{{ fmtNum(activeLine.progressPct) }}%</span>
                  </div>
                  <div class="bs-progress-track">
                    <div class="bs-progress-fill" :style="{ '--pct': fmtNum(activeLine.progressPct) + '%', '--c': '#06b6d4' }"></div>
                  </div>
                </div>
              </template>
              <div v-else class="bs-empty">未找到该产线数据</div>
            </div>

          </div>
        </main>

        <!-- 右侧栏 -->
        <aside class="bs-side bs-side--right">
          <!-- 设备运行状态 -->
          <div class="bs-panel">
            <div class="bs-panel-head">
              <span class="bs-panel-title">设备运行状态</span>
              <span class="bs-panel-sub">共 {{ deviceTotal }} 台</span>
            </div>
            <div class="bs-panel-body bs-equip-body">
              <template v-if="equipmentSegments.length">
                <div class="bs-equip-top">
                  <div class="bs-ring bs-equip-ring" :style="{ '--pct': equipmentRunningRate, '--c': '#10b981' }">
                    <span class="bs-ring-value">{{ fmtNum(equipmentRunningRate) }}%</span>
                    <span class="bs-ring-label">设备运行率</span>
                  </div>
                  <div class="bs-equip-chips">
                    <div v-for="chip in equipmentKpis" :key="chip.label" class="bs-equip-chip">
                      <span class="bs-equip-chip-value" :style="{ color: chip.color }">{{ chip.value }}</span>
                      <span class="bs-equip-chip-label">{{ chip.label }}</span>
                    </div>
                  </div>
                </div>
                <div class="bs-equip-stack-wrap">
                  <div class="bs-equip-stack-label">
                    <span>运行 {{ fmtNum(equipmentRunningRate) }}%</span>
                    <span>异常 {{ equipmentFaultCount }} 台</span>
                  </div>
                  <div class="bs-equip-stack">
                    <div
                      v-for="seg in equipmentSegments"
                      :key="seg.key"
                      class="bs-equip-stack-seg"
                      :style="{
                        flexBasis: seg.pct + '%',
                        background: seg.color,
                        boxShadow: '0 0 10px color-mix(in srgb, ' + seg.color + ' 45%, transparent)',
                      }"
                      :title="seg.label + ' ' + seg.count + '台'"
                    ></div>
                  </div>
                </div>
                <div class="bs-equip-list">
                  <div v-for="seg in equipmentSegments" :key="'row-' + seg.key" class="bs-equip-row">
                    <span class="bs-equip-dot" :style="{ background: seg.color, boxShadow: '0 0 6px ' + seg.color }"></span>
                    <span class="bs-equip-name">{{ seg.label }}</span>
                    <span class="bs-equip-count">{{ seg.count }}</span>
                    <span class="bs-equip-pct">{{ seg.pct }}%</span>
                    <div class="bs-equip-mini-bar">
                      <div class="bs-equip-mini-fill" :style="{ width: seg.pct + '%', background: seg.color }"></div>
                    </div>
                  </div>
                </div>
              </template>
              <div v-else class="bs-empty">暂无设备数据</div>
            </div>
          </div>

          <!-- 告警信息 -->
          <div class="bs-panel">
            <div class="bs-panel-head">
              <span class="bs-panel-title">告警信息</span>
              <span class="bs-alert-count">{{ alertTotal }}</span>
            </div>
            <div class="bs-panel-body bs-alert-body-wrap">
              <template v-if="alerts.length">
                <div class="bs-alert-top">
                  <div class="bs-ring bs-alert-ring" :style="{ '--pct': alertRingPct, '--c': '#f87171' }">
                    <span class="bs-ring-value">{{ alertTotal }}</span>
                    <span class="bs-ring-label">待处理</span>
                  </div>
                  <div class="bs-alert-chips">
                    <div v-for="chip in alertKpis" :key="chip.label" class="bs-alert-chip">
                      <span class="bs-alert-chip-value" :style="{ color: chip.color }">{{ chip.value }}</span>
                      <span class="bs-alert-chip-label">{{ chip.label }}</span>
                    </div>
                  </div>
                </div>
                <div class="bs-alert-stack-wrap" v-if="alertTypeSegments.length">
                  <div class="bs-alert-stack-label">
                    <span>{{ alertStackSummary }}</span>
                    <span>最长 {{ formatDuration(alertMaxDuration) }}</span>
                  </div>
                  <div class="bs-alert-stack">
                    <div
                      v-for="seg in alertTypeSegments"
                      :key="seg.key"
                      class="bs-alert-stack-seg"
                      :style="{
                        flexBasis: seg.pct + '%',
                        background: seg.color,
                        boxShadow: '0 0 10px color-mix(in srgb, ' + seg.color + ' 45%, transparent)',
                      }"
                      :title="seg.label + ' ' + seg.count"
                    ></div>
                  </div>
                </div>
                <div class="bs-alert-cards">
                  <div
                    v-for="(a, idx) in alertCards"
                    :key="a.andonId || a.andonNo"
                    class="bs-alert-card"
                    :class="{ 'bs-alert-card--pulse': idx === 0 }"
                    :style="{ '--accent': a.color }"
                    @click="goAndon"
                  >
                    <div class="bs-alert-card-head">
                      <span class="bs-alert-badge" :style="{ background: a.color + '22', color: a.color, borderColor: a.color + '55' }">{{ a.typeLabel }}</span>
                      <span class="bs-alert-code">{{ a.andonNo }}</span>
                      <span v-if="a.priority === 'HIGH'" class="bs-alert-priority">高</span>
                    </div>
                    <div class="bs-alert-desc">{{ a.exceptionDesc }}</div>
                    <div class="bs-alert-meta">
                      <span>{{ a.lineName }}</span>
                      <span>{{ a.occurTime }}<template v-if="a.durationMinutes != null"> · {{ formatDuration(a.durationMinutes) }}</template></span>
                    </div>
                  </div>
                </div>
              </template>
              <div v-else class="bs-alert-safe">
                <svg class="bs-alert-safe-icon" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="1.5">
                  <path d="M12 22s8-4 8-10V5l-8-3-8 3v7c0 6 8 10 8 10z"></path>
                  <path d="M9 12l2 2 4-4"></path>
                </svg>
                <span class="bs-alert-safe-title">产线运行正常</span>
                <span class="bs-alert-safe-sub">暂无待处理安灯</span>
              </div>
            </div>
          </div>
        </aside>
      </div>


      <!-- 中间进度条行（OEE + 产线进度） -->
      <div class="bs-mid-progress">
        <div class="bs-progress-card" v-for="p in bottomProgress" :key="p.label">
          <div class="bs-progress-label">
            <span class="bs-progress-title" :title="p.label">{{ p.label }}</span>
            <span class="bs-progress-pct" :style="{ '--c': p.color }">{{ p.pct }}%</span>
          </div>
          <div class="bs-progress-track">
            <div class="bs-progress-fill" :style="{ '--pct': p.pct + '%', '--c': p.color }"></div>
          </div>
          <div class="bs-progress-detail">
            <span>{{ p.left }}</span>
            <span>{{ p.right }}</span>
          </div>
        </div>
      </div>
      <!-- 底部在制工单 + 生产协同 -->
      <div class="bs-footer">
        <div class="bs-footer-split">
          <div class="bs-panel bs-material">
            <div class="bs-panel-head">
              <span class="bs-panel-title">在制工单</span>
              <span class="bs-panel-sub">进行中 {{ workOrderRows.length }} 单</span>
            </div>
            <div class="bs-panel-body bs-material-body">
              <div class="bs-material-main">
                <div class="bs-material-side">
                  <div class="bs-ring" :style="{ '--pct': avgProgress, '--c': '#06b6d4' }">
                    <span class="bs-ring-value">{{ avgProgress }}%</span>
                    <span class="bs-ring-label">平均达成</span>
                  </div>
                </div>
                <div class="bs-material-list">
                  <template v-if="workOrderRows.length">
                    <div v-for="w in workOrderRows" :key="w.workOrderNo" class="bs-wo-item">
                      <div class="bs-wo-head">
                        <span class="bs-wo-no" :title="w.lineName ? `${w.workOrderNo} · ${w.lineName}` : w.workOrderNo">{{ w.workOrderNo }}</span>
                        <span class="bs-wo-name" :title="w.productName">{{ w.productName || "—" }}</span>
                        <span class="bs-wo-qty">{{ w.completedQty }}/{{ w.planQty }}</span>
                      </div>
                      <div class="bs-wo-bar">
                        <div class="bs-wo-bar-fill" :style="{ width: w.pct + '%' }"></div>
                      </div>
                    </div>
                  </template>
                  <div v-else class="bs-empty">暂无在制工单</div>
                </div>
              </div>
            </div>
          </div>

          <div class="bs-panel bs-footer-coord">
            <div class="bs-panel-head">
              <span class="bs-panel-title">生产协同</span>
              <span class="bs-panel-sub">预警 {{ coordAlertTotal }} 条</span>
            </div>
            <div class="bs-panel-body bs-coord-body">
              <div class="bs-coord-lines">
                <div class="bs-coord-section-title">产线工单分布</div>
                <template v-if="lineWoDistribution.length">
                  <div v-for="line in lineWoDistribution" :key="line.lineName" class="bs-coord-line-item">
                    <div class="bs-coord-line-head">
                      <span class="bs-coord-line-name" :title="line.lineName">{{ line.lineName }}</span>
                      <span class="bs-coord-line-meta">{{ line.count }}单 · {{ line.pct }}%</span>
                    </div>
                    <div class="bs-wo-bar">
                      <div class="bs-wo-bar-fill" :style="{ width: line.pct + '%' }"></div>
                    </div>
                  </div>
                </template>
                <div v-else class="bs-empty">暂无产线分布</div>
              </div>
              <div class="bs-coord-alerts">
                <div class="bs-coord-section-title">协同预警</div>
                <template v-if="coordAlertRows.length">
                  <div
                    v-for="(a, idx) in coordAlertRows"
                    :key="idx"
                    class="bs-coord-alert-item"
                  >
                    <span
                      class="bs-coord-alert-badge"
                      :style="{ color: a.color, borderColor: a.color + '55', background: a.color + '18' }"
                    >{{ a.typeLabel }}</span>
                    <span class="bs-coord-alert-msg" :title="a.message">{{ a.message }}</span>
                  </div>
                </template>
                <div v-else class="bs-empty">暂无协同预警</div>
              </div>
            </div>
          </div>
        </div>
      </div>
    </div>
  </div>
</template>

<script setup>
import { computed, onBeforeUnmount, onMounted, ref } from "vue"
import { useRoute, useRouter } from "vue-router"
import * as echarts from "echarts"
import { fetchLineMonitorSummary } from "@/api/lineMonitor"
import { fetchDashboardSummary } from "@/api/dashboard"
import { unwrapPayload } from "@/utils/unwrapPayload"
import LineOverview3D from "@/components/three/LineOverview3D.vue"

const router = useRouter()
const route = useRoute()
const embedded = computed(() => route.path.startsWith("/app"))
const now = ref("")
const headerDate = ref("")
const headerTime = ref("")
const headerWeekday = ref("")
const summary = ref(null)   // 产线监控聚合
const dash = ref(null)      // 仪表盘聚合
const mapRef = ref(null)
const cityBarRef = ref(null)
let mapChart, cityBarChart
const error = ref("")
const lineError = ref("")
const dashError = ref("")
const activeTab = ref("车间概览")
let clock, pollTimer

/* ---------- 基础数据 ---------- */
const lines = computed(() => summary.value?.lines || [])
const alerts = computed(() => summary.value?.alerts || [])
const shiftName = computed(() => summary.value?.currentShiftName || "")

const dataHint = computed(() => {
  const parts = []
  if (lineError.value) parts.push(`产线：${lineError.value}`)
  if (dashError.value) parts.push(`指标：${dashError.value}`)
  return parts.length ? parts.join(" · ") : ""
})

const dataHintLevel = computed(() =>
  lineError.value && dashError.value ? "bs-data-hint--error" : "bs-data-hint--warn",
)

// monitor metrics 按 code 建 map
const monitorMetrics = computed(() => {
  const map = {}
  for (const m of summary.value?.metrics || []) map[m.code] = m
  return map
})
// dashboard metrics 按 name 建 map
const dashMetrics = computed(() => {
  const map = {}
  for (const m of dash.value?.metrics || []) map[m.name] = m
  return map
})

const fmtNum = (v) => {
  const n = Number(v)
  if (Number.isNaN(n)) return "0"
  return Number.isInteger(n) ? String(n) : n.toFixed(1)
}

/* ---------- 顶部：生产概况 ---------- */
const overviewMetrics = computed(() => {
  const mm = monitorMetrics.value
  return [
    { label: "运行产线", value: mm.running_lines?.value ?? "0", unit: "条", tone: "NORMAL" },
    { label: "待机产线", value: mm.idle_lines?.value ?? "0", unit: "条", tone: "NORMAL" },
    { label: "故障设备", value: mm.fault_devices?.value ?? "0", unit: "台", tone: (Number(mm.fault_devices?.value) > 0 ? "ALERT" : "NORMAL") },
  ]
})

/* ---------- 顶部：今日指标达成 ---------- */
const kpiItems = computed(() => {
  const dm = dashMetrics.value
  const build = (name, color, icon) => {
    const m = dm[name]
    const rawTrend = m?.trend ?? "—"
    const trendText = fmtTrendText(rawTrend)
    const trendClass = fmtTrendClass(rawTrend)
    return {
      label: name,
      value: m?.value ?? "0",
      unit: m?.unit ?? "",
      color,
      icon,
      trendText,
      trendClass,
    }
  }
  return [
    build("今日计划产量", "#3b82f6", '<path d="M9 11l3 3L22 4"></path><path d="M21 12v7a2 2 0 0 1-2 2H5a2 2 0 0 1-2-2V5a2 2 0 0 1 2-2h11"></path>'),
    build("今日完工数量", "#06b6d4", '<rect x="2" y="7" width="20" height="14" rx="2"></rect><path d="M16 21V5a2 2 0 0 0-2-2h-4a2 2 0 0 0-2 2v16"></path>'),
    build("工单达成率", "#8b5cf6", '<path d="M22 12h-4l-3 9L9 3l-3 9H2"></path>'),
  ]
})
function fmtTrendText(raw) {
  if (!raw || raw === "—") return "较昨日 —"
  const t = String(raw).trim()
  const sign = t.startsWith("+") ? "提升" : t.startsWith("-") ? "降低" : ""
  if (!sign) return `较昨日 ${t}`
  const num = t.replace(/^[+-]/, "").replace(/%/g, "")
  return `较昨日${sign} ${num}%`
}
function fmtTrendClass(raw) {
  if (!raw || raw === "—") return ""
  const t = String(raw).trim()
  if (t.startsWith("+")) return "up"   // 提升 → 红色（中国习惯）
  if (t.startsWith("-")) return "down" // 降低 → 绿色
  return ""
}

/* ---------- 顶部：产出情况 ---------- */
const outputMetrics = computed(() => {
  const mm = monitorMetrics.value
  return [
    { label: "今日产出", value: mm.today_output?.value ?? "0", unit: mm.today_output?.unit ?? "台" },
    { label: "平均OEE", value: mm.avg_oee?.value ?? "0", unit: mm.avg_oee?.unit ?? "%" },
    { label: "在制工单", value: mm.active_wo?.value ?? "0", unit: mm.active_wo?.unit ?? "单" },
  ]
})

/* ---------- 左侧：产线生产进度表 ---------- */
const lineProgressRows = computed(() =>
  lines.value.map((l) => ({
    lineId: l.lineId,
    name: l.lineName,
    status: l.lineStatus,
    rate: Math.round(Number(l.progressPct) || 0),
  }))
)

/* ---------- 中间：动态标签 + 单产线详情 ---------- */
const centerTabs = computed(() => ["车间概览", ...lines.value.map((l) => l.lineName)])
const activeLine = computed(() => lines.value.find((l) => l.lineName === activeTab.value) || null)

/* ---------- 中间底部：平均OEE + 前两条产线进度 ---------- */
const bottomProgress = computed(() => {
  const mm = monitorMetrics.value
  const cards = []
  cards.push({
    label: "平均OEE",
    pct: Math.round(Number(mm.avg_oee?.value) || 0),
    color: "#10b981",
    left: "运行产线：" + (mm.running_lines?.value ?? 0),
    right: "故障产线：" + (mm.fault_lines?.value ?? 0),
  })
  const palette = ["#06b6d4", "#3b82f6"]
  lines.value.slice(0, 2).forEach((l, i) => {
    cards.push({
      label: l.lineName + " 生产进度",
      pct: Math.round(Number(l.progressPct) || 0),
      color: palette[i] || "#06b6d4",
      left: "计划：" + (l.planQty ?? 0),
      right: "完工：" + (l.completedQty ?? 0),
    })
  })
  return cards.slice(0, 3)
})

/* ---------- 右侧：设备运行状态（圆环 + 堆叠条 + 明细） ---------- */
const EQUIPMENT_STATUS = {
  WORKING: { label: "运行", color: "#10b981" },
  NORMAL: { label: "正常", color: "#10b981" },
  RUNNING: { label: "运行", color: "#06b6d4" },
  IDLE: { label: "闲置", color: "#3b82f6" },
  STANDBY: { label: "待机", color: "#3b82f6" },
  STOP: { label: "停机", color: "#94a3b8" },
  STOPPED: { label: "停机", color: "#94a3b8" },
  REPAIR: { label: "维修", color: "#fbbf24" },
  FAULT: { label: "故障", color: "#f87171" },
  MAINTENANCE: { label: "保养", color: "#f59e0b" },
  OFFLINE: { label: "离线", color: "#64748b" },
  DISABLED: { label: "停用", color: "#64748b" },
}
const equipmentStatusRows = computed(() => summary.value?.equipmentStatus || [])
const equipmentSummary = computed(() => summary.value?.equipmentSummary || null)
const deviceTotal = computed(() => {
  if (equipmentSummary.value?.total != null) return Number(equipmentSummary.value.total) || 0
  return equipmentStatusRows.value.reduce((s, r) => s + (Number(r.count) || 0), 0)
})
const equipmentSegments = computed(() => {
  const rows = equipmentStatusRows.value
  if (!rows.length) return []
  const total = deviceTotal.value || rows.reduce((s, r) => s + (Number(r.count) || 0), 0)
  return rows.map((r) => {
    const status = (r.status || "UNKNOWN").toUpperCase()
    const meta = EQUIPMENT_STATUS[status] || { label: status, color: "#64748b" }
    const count = Number(r.count) || 0
    const pct = total > 0 ? Math.round((count / total) * 1000) / 10 : 0
    return { key: status, label: meta.label, color: meta.color, count, pct }
  })
})
const equipmentRunningRate = computed(() => {
  if (equipmentSummary.value?.runningRate != null) return Number(equipmentSummary.value.runningRate) || 0
  const total = deviceTotal.value
  if (!total) return 0
  const running = equipmentSegments.value
    .filter((s) => ["WORKING", "NORMAL", "RUNNING"].includes(s.key))
    .reduce((sum, s) => sum + s.count, 0)
  return Math.round((running / total) * 1000) / 10
})
const equipmentFaultCount = computed(() => {
  if (equipmentSummary.value?.faultCount != null) return Number(equipmentSummary.value.faultCount) || 0
  return equipmentSegments.value
    .filter((s) => ["REPAIR", "STOP", "STOPPED", "FAULT", "OFFLINE"].includes(s.key))
    .reduce((sum, s) => sum + s.count, 0)
})
const equipmentKpis = computed(() => {
  const seg = equipmentSegments.value
  const countOf = (...keys) => seg.filter((s) => keys.includes(s.key)).reduce((sum, s) => sum + s.count, 0)
  const running = equipmentSummary.value?.runningCount ?? countOf("WORKING", "NORMAL", "RUNNING")
  const avgOee = equipmentSummary.value?.avgOee ?? monitorMetrics.value.avg_oee?.value ?? 0
  return [
    { label: "运行中", value: running, color: "#10b981" },
    { label: "停机", value: countOf("STOP", "STOPPED"), color: "#94a3b8" },
    { label: "维修", value: countOf("REPAIR"), color: "#fbbf24" },
    { label: "平均OEE", value: `${fmtNum(avgOee)}%`, color: "#06b6d4" },
  ]
})

/* ---------- 右侧：告警信息（KPI + 类型堆叠条 + 卡片） ---------- */
const ALERT_TYPE_COLORS = {
  MATERIAL_SHORTAGE: { label: "缺料", color: "#f59e0b" },
  QUALITY_ABNORMAL: { label: "质量", color: "#f87171" },
  DEVICE_FAULT: { label: "设备", color: "#fbbf24" },
  PROCESS_HELP: { label: "工艺", color: "#06b6d4" },
}
function resolveAlertType(typeCode, typeName) {
  const code = (typeCode || "").toUpperCase()
  if (ALERT_TYPE_COLORS[code]) return ALERT_TYPE_COLORS[code]
  const name = typeName || ""
  if (name.includes("缺料")) return ALERT_TYPE_COLORS.MATERIAL_SHORTAGE
  if (name.includes("质量")) return ALERT_TYPE_COLORS.QUALITY_ABNORMAL
  if (name.includes("设备")) return ALERT_TYPE_COLORS.DEVICE_FAULT
  if (name.includes("工艺")) return ALERT_TYPE_COLORS.PROCESS_HELP
  return { label: name.slice(0, 2) || "安灯", color: "#94a3b8" }
}
function formatDuration(minutes) {
  const m = Number(minutes) || 0
  if (m < 60) return `${m}min`
  const h = Math.floor(m / 60)
  const r = m % 60
  return r > 0 ? `${h}h${r}m` : `${h}h`
}
const alertSummary = computed(() => summary.value?.alertSummary || null)
const alertTotal = computed(() => alertSummary.value?.total ?? alerts.value.length)
const alertMaxDuration = computed(() => alertSummary.value?.maxDurationMinutes ?? 0)
const alertRingPct = computed(() => Math.min(100, Math.max(8, alertTotal.value * 12)))
const alertTypeSegments = computed(() => {
  const slices = alertSummary.value?.typeSlices
  const rows = slices?.length
    ? slices
    : alerts.value.reduce((acc, a) => {
        const key = a.typeCode || a.typeName || "UNKNOWN"
        const found = acc.find((r) => r.typeCode === key)
        if (found) found.count += 1
        else acc.push({ typeCode: key, typeName: a.typeName || "安灯", count: 1 })
        return acc
      }, [])
  if (!rows.length) return []
  const total = rows.reduce((s, r) => s + (Number(r.count) || 0), 0)
  return rows.map((r) => {
    const meta = resolveAlertType(r.typeCode, r.typeName)
    const count = Number(r.count) || 0
    return {
      key: r.typeCode || r.typeName,
      label: meta.label,
      color: meta.color,
      count,
      pct: total > 0 ? Math.round((count / total) * 1000) / 10 : 0,
    }
  })
})
const alertStackSummary = computed(() =>
  alertTypeSegments.value.map((s) => `${s.label} ${s.count}`).join(" · "),
)
const alertKpis = computed(() => {
  const s = alertSummary.value
  const high = s?.highPriorityCount ?? alerts.value.filter((a) => a.priority === "HIGH").length
  const lines = s?.lineCount ?? new Set(alerts.value.map((a) => a.lineId || a.lineName)).size
  const types = s?.typeSlices?.length ?? alertTypeSegments.value.length
  const duration = s?.maxDurationMinutes ?? alertMaxDuration.value
  return [
    { label: "高优先级", value: high, color: "#f87171" },
    { label: "涉及产线", value: lines, color: "#fbbf24" },
    { label: "类型种类", value: types, color: "#06b6d4" },
    { label: "最长持续", value: formatDuration(duration), color: "#94a3b8" },
  ]
})
const alertCards = computed(() =>
  alerts.value.map((a) => {
    const meta = resolveAlertType(a.typeCode, a.typeName)
    let desc = a.exceptionDesc
    if (!desc && a.message) {
      const parts = String(a.message).split("：")
      desc = parts.length > 1 ? parts.slice(1).join("：") : a.message
    }
    return {
      ...a,
      typeLabel: meta.label,
      color: meta.color,
      exceptionDesc: desc || "—",
    }
  }),
)
function goAndon() {
  router.push("/app/andon")
}

/* ---------- 底部：在制工单 + 生产协同 ---------- */
const ALERT_TYPE_LABEL = {
  MATERIAL: "物料",
  QUALITY: "质量",
  EQUIPMENT: "设备",
}
const ALERT_LEVEL_COLOR = {
  HIGH: "#f87171",
  MEDIUM: "#fbbf24",
  LOW: "#06b6d4",
}

const workOrderRows = computed(() => {
  const wos = dash.value?.workOrders || []
  return wos.map((w) => {
    const plan = Number(w.planQty) || 0
    const done = Number(w.completedQty) || 0
    return {
      workOrderNo: w.workOrderNo,
      productName: w.productName,
      lineName: w.lineName || "",
      status: w.status || "",
      planQty: plan,
      completedQty: done,
      pct: plan > 0 ? Math.min(100, Math.round((done / plan) * 100)) : 0,
    }
  })
})
const avgProgress = computed(() => {
  const rows = workOrderRows.value
  if (!rows.length) {
    const ls = lines.value
    if (!ls.length) return 0
    return Math.round(ls.reduce((s, l) => s + (Number(l.progressPct) || 0), 0) / ls.length)
  }
  return Math.round(rows.reduce((s, w) => s + w.pct, 0) / rows.length)
})

const lineWoDistribution = computed(() => {
  const map = new Map()
  for (const w of workOrderRows.value) {
    const key = w.lineName || "未分配产线"
    const cur = map.get(key) || { lineName: key, count: 0, planSum: 0, doneSum: 0 }
    cur.count += 1
    cur.planSum += w.planQty
    cur.doneSum += w.completedQty
    map.set(key, cur)
  }
  return [...map.values()]
    .map((line) => ({
      ...line,
      pct: line.planSum > 0 ? Math.min(100, Math.round((line.doneSum / line.planSum) * 100)) : 0,
    }))
    .sort((a, b) => b.count - a.count)
})

const coordAlertRows = computed(() => {
  const alerts = dash.value?.alerts || []
  return alerts.slice(0, 4).map((a) => {
    const type = (a.alertType || "").toUpperCase()
    const level = (a.level || "LOW").toUpperCase()
    return {
      typeLabel: ALERT_TYPE_LABEL[type] || type || "预警",
      message: a.alertTitle || "—",
      color: ALERT_LEVEL_COLOR[level] || "#06b6d4",
    }
  })
})
const coordAlertTotal = computed(() => (dash.value?.alerts || []).length)

/* ---------- 工具函数 ---------- */
function rateClass(rate) {
  if (rate >= 80) return "high"
  if (rate >= 50) return "mid"
  return "low"
}
function statusClass(status) {
  const s = (status || "").toUpperCase()
  if (s === "RUNNING") return "st-run"
  if (s === "WARNING") return "st-warn"
  if (s === "FAULT") return "st-fault"
  return "st-idle"
}
function statusText(status) {
  const map = { RUNNING: "运行", WARNING: "预警", FAULT: "故障", IDLE: "待机", CHANGEOVER: "换型" }
  return map[(status || "").toUpperCase()] || "待机"
}
function goDetail(id) {
  router.push(`/app/line-monitor?lineId=${id}`)
}
function tick() {
  const d = new Date()
  headerDate.value = d.toLocaleDateString("zh-CN", {
    year: "numeric", month: "2-digit", day: "2-digit",
  })
  headerTime.value = d.toLocaleTimeString("zh-CN", {
    hour: "2-digit", minute: "2-digit", second: "2-digit", hour12: false,
  })
  headerWeekday.value = d.toLocaleDateString("zh-CN", { weekday: "short" })
  now.value = d.toLocaleString("zh-CN", {
    year: "numeric", month: "2-digit", day: "2-digit",
    hour: "2-digit", minute: "2-digit", second: "2-digit", weekday: "short",
  })
}

async function load() {
  lineError.value = ""
  dashError.value = ""

  const [monRes, dashRes] = await Promise.allSettled([
    fetchLineMonitorSummary(),
    fetchDashboardSummary(),
  ])

  if (monRes.status === "fulfilled") {
    summary.value = unwrapPayload(monRes.value)
    if (!summary.value) lineError.value = "数据为空"
  } else {
    summary.value = null
    lineError.value = monRes.reason?.message || "加载失败"
  }

  if (dashRes.status === "fulfilled") {
    dash.value = unwrapPayload(dashRes.value)
    if (!dash.value) dashError.value = "数据为空"
  } else {
    dash.value = null
    dashError.value = dashRes.reason?.message || "加载失败"
  }

  error.value = lineError.value || dashError.value
}

onMounted(() => {
  tick()
  clock = setInterval(tick, 1000)
  load()
  pollTimer = setInterval(load, 30000)
  initSalesCharts()
})

onBeforeUnmount(() => {
  clearInterval(clock)
  clearInterval(pollTimer)
  disposeSalesCharts()
})

/* ---------- 销售市场地图 ---------- */
const SALES_CITIES = [
  { name: "广州市", value: 9300 },
  { name: "青岛市", value: 11200 },
  { name: "太原市", value: 8900 },
  { name: "天津市", value: 8100 },
  { name: "兰州市", value: 7400 },
  { name: "杭州市", value: 5600 },
  { name: "沈阳市", value: 4300 },
  { name: "重庆市", value: 4000 },
  { name: "拉萨", value: 3900 },
  { name: "乌鲁木齐", value: 3600 },
  { name: "长沙市", value: 1800 },
  { name: "厦门市", value: 2400 },
  { name: "福州市", value: 1900 },
  { name: "上海市", value: 1700 },
  { name: "深圳市", value: 1400 },
  { name: "贵阳市", value: 800 },
  { name: "北京市", value: 400 },
  { name: "香港", value: 1500 },
]

async function initSalesCharts() {
  try {
    const res = await fetch("/map/china.json")
    if (!res.ok) throw new Error("map load failed")
    const chinaJson = await res.json()
    echarts.registerMap("china", chinaJson)
    renderMap()
    renderCityBar()
  } catch (e) {
    console.warn("中国地图加载失败，使用轮廓备用", e)
    renderCityBar()
  }
}

function renderMap() {
  if (!mapRef.value) return
  mapChart = echarts.init(mapRef.value)
  mapChart.setOption({
    backgroundColor: "transparent",
    geo: {
      map: "china",
      roam: false,
      zoom: 1.35,
      layoutCenter: ["50%", "35%"],
      layoutSize: "105%",
      itemStyle: {
        areaColor: "rgba(6, 182, 212, 0.08)",
        borderColor: "rgba(6, 182, 212, 0.45)",
        borderWidth: 1,
      },
      emphasis: {
        itemStyle: { areaColor: "rgba(6, 182, 212, 0.22)" },
        label: { color: "#fff" },
      },
      label: { show: true, color: "rgba(230, 247, 255, 0.55)", fontSize: 10 },
    },
    series: [
      {
        type: "effectScatter",
        coordinateSystem: "geo",
        data: SALES_CITIES.slice(0, 12).map((c) => {
          const coord = cityCoords(c.name)
          return { name: c.name, value: [...coord, c.value] }
        }).filter((c) => c.value[0] && c.value[1]),
        symbolSize: (val) => Math.max(6, Math.min(18, val[2] / 700)),
        itemStyle: { color: "#06b6d4" },
        rippleEffect: { brushType: "stroke", scale: 2.5, period: 4 },
      },
    ],
  })
}

function cityCoords(name) {
  const COORDS = {
    "北京市": [116.4074, 39.9042], "上海市": [121.4737, 31.2304], "广州市": [113.2644, 23.1291],
    "深圳市": [114.0579, 22.5431], "杭州市": [120.1551, 30.2741], "南京市": [118.7969, 32.0603],
    "成都市": [104.0668, 30.5728], "重庆市": [106.5516, 29.5630], "武汉市": [114.3054, 30.5928],
    "西安市": [108.9398, 34.3416], "天津市": [117.2009, 39.0842], "苏州市": [120.5853, 31.2989],
    "青岛市": [120.3826, 36.0671], "沈阳市": [123.4315, 41.8057], "长沙市": [112.9388, 28.2282],
    "大连市": [121.6147, 38.9140], "厦门市": [118.0894, 24.4798], "福州市": [119.2965, 26.0745],
    "太原市": [112.5489, 37.8706], "兰州市": [103.8343, 36.0611], "乌鲁木齐市": [87.6168, 43.8256],
    "拉萨": [91.1409, 29.6456], "香港": [114.1710, 22.3196], "贵阳市": [106.6302, 26.6477],
  }
  return COORDS[name] || [116, 39]
}

function renderCityBar() {
  if (!cityBarRef.value) return
  const sorted = [...SALES_CITIES].sort((a, b) => a.value - b.value)
  cityBarChart = echarts.init(cityBarRef.value)
  cityBarChart.setOption({
    backgroundColor: "transparent",
    grid: { left: 64, right: 16, top: 8, bottom: 10 },
    xAxis: {
      type: "value",
      max: 12000,
      axisLabel: { color: "rgba(230, 247, 255, 0.55)", fontSize: 10 },
      splitLine: { lineStyle: { color: "rgba(6, 182, 212, 0.08)" } },
    },
    yAxis: {
      type: "category",
      data: sorted.map((c) => c.name),
      axisLabel: { color: "rgba(230, 247, 255, 0.75)", fontSize: 11 },
      axisLine: { lineStyle: { color: "rgba(6, 182, 212, 0.2)" } },
    },
    series: [{
      type: "bar",
      data: sorted.map((c) => c.value),
      barWidth: 10,
      itemStyle: {
        color: new echarts.graphic.LinearGradient(0, 0, 1, 0, [
          { offset: 0, color: "rgba(6, 182, 212, 0.4)" },
          { offset: 1, color: "#06b6d4" },
        ]),
        borderRadius: [0, 5, 5, 0],
      },
      label: { show: false },
    }],
  })
}

function disposeSalesCharts() {
  mapChart?.dispose?.()
  cityBarChart?.dispose?.()
  mapChart = null
  cityBarChart = null
}

window.addEventListener("resize", () => {
  mapChart?.resize?.()
  cityBarChart?.resize?.()
})
</script>

<style scoped>
/* ============================================================
   数字车间监控大屏
   ============================================================ */
.bs-root {
  position: fixed;
  inset: 0;
  overflow: hidden;
  background: #00050a;
  color: #e6f7ff;
  font-family: "PingFang SC", "Microsoft YaHei", "Noto Sans SC", sans-serif;
}

.bs-root--embedded {
  z-index: 1000;
}

/* 背景网格 */
.bs-3d-bg {
  position: absolute;
  inset: 0;
  z-index: 0;
  background:
    linear-gradient(rgba(6, 182, 212, 0.02) 1px, transparent 1px),
    linear-gradient(90deg, rgba(6, 182, 212, 0.02) 1px, transparent 1px);
  background-size: 40px 40px;
  pointer-events: none;
}

/* HUD 层 */
.bs-hud {
  position: relative;
  z-index: 2;
  display: flex;
  flex-direction: column;
  height: 100%;
  padding: 0 18px 16px;
  background:
    radial-gradient(ellipse at 50% 0%, rgba(6, 182, 212, 0.04) 0%, transparent 50%),
    radial-gradient(ellipse at 0% 100%, rgba(59, 130, 246, 0.03) 0%, transparent 40%),
    radial-gradient(ellipse at 100% 100%, rgba(6, 182, 212, 0.03) 0%, transparent 40%);
  pointer-events: none;
}
.bs-hud > * {
  pointer-events: auto;
}

/* ============================================================ Header ============================================================ */
.bs-header {
  position: relative;
  flex-shrink: 0;
  height: 64px;
  display: flex;
  align-items: center;
  justify-content: center;
  margin-bottom: 8px;
}

.bs-data-hint {
  flex-shrink: 0;
  margin: 0 12px 8px;
  padding: 6px 14px;
  border-radius: 6px;
  font-size: 12px;
  line-height: 1.4;
  text-align: center;
  border: 1px solid rgba(251, 191, 36, 0.35);
  background: rgba(251, 191, 36, 0.1);
  color: #fde68a;
}

.bs-data-hint--error {
  border-color: rgba(248, 113, 113, 0.45);
  background: rgba(248, 113, 113, 0.12);
  color: #fecaca;
}

.bs-data-hint--warn {
  border-color: rgba(251, 191, 36, 0.35);
  background: rgba(251, 191, 36, 0.1);
  color: #fde68a;
}

.bs-header-main {
  display: flex;
  flex-direction: column;
  align-items: center;
  justify-content: center;
  padding: 0 40px;
  background: linear-gradient(90deg, transparent, rgba(6, 182, 212, 0.08) 20%, rgba(6, 182, 212, 0.15) 50%, rgba(6, 182, 212, 0.08) 80%, transparent);
  border-bottom: 1px solid rgba(6, 182, 212, 0.25);
  clip-path: polygon(0 0, 100% 0, 92% 100%, 8% 100%);
}
.bs-header-title {
  font-size: clamp(22px, 2.4vw, 32px);
  font-weight: 700;
  letter-spacing: 4px;
  color: #fff;
  text-shadow: 0 0 20px rgba(6, 182, 212, 0.6), 0 0 40px rgba(6, 182, 212, 0.3);
}
.bs-header-sub {
  font-size: 10px;
  letter-spacing: 2px;
  color: rgba(6, 182, 212, 0.7);
  text-transform: uppercase;
  margin-top: 2px;
}
.bs-header-deco {
  position: absolute;
  top: 50%;
  width: 28%;
  height: 1px;
  background: linear-gradient(90deg, rgba(6, 182, 212, 0.4), transparent);
}
.bs-header-deco--left { left: 0; }
.bs-header-deco--right { right: 0; transform: scaleX(-1); }
.bs-header-deco::after {
  content: "";
  position: absolute;
  right: 0;
  top: -2px;
  width: 40px;
  height: 5px;
  background: linear-gradient(90deg, transparent, rgba(6, 182, 212, 0.6));
  clip-path: polygon(0 0, 100% 0, 80% 100%, 0 100%);
}
.bs-header-meta {
  position: absolute;
  left: 20px;
  top: 50%;
  transform: translateY(-50%);
  display: flex;
  flex-direction: row;
  align-items: center;
  flex-wrap: nowrap;
  gap: 10px 14px;
  padding: 6px 14px;
  border: 1px solid rgba(6, 182, 212, 0.22);
  border-radius: 4px;
  background: rgba(5, 16, 25, 0.55);
  font-family: "JetBrains Mono", "Fira Code", monospace;
  font-size: 14px;
  color: rgba(230, 247, 255, 0.75);
  white-space: nowrap;
}
.bs-header-date {
  font-size: 15px;
  font-weight: 600;
  color: rgba(230, 247, 255, 0.88);
  letter-spacing: 0.5px;
}
.bs-header-weekday {
  font-size: 14px;
  color: rgba(6, 182, 212, 0.85);
}
.bs-header-divider {
  width: 1px;
  height: 18px;
  background: linear-gradient(180deg, transparent, rgba(6, 182, 212, 0.55), transparent);
  flex-shrink: 0;
}
.bs-header-time {
  font-size: 17px;
  font-weight: 700;
  color: #fff;
  letter-spacing: 1.5px;
  text-shadow: 0 0 12px rgba(6, 182, 212, 0.45);
}
.bs-header-shift {
  margin-left: 4px;
  padding-left: 14px;
  border-left: 1px solid rgba(6, 182, 212, 0.28);
  color: rgba(6, 182, 212, 0.9);
  font-size: 14px;
}

/* ============================================================ Top Bar Cards ============================================================ */
.bs-top-bar {
  flex-shrink: 0;
  display: grid;
  grid-template-columns: 1.1fr 1.8fr 1fr;
  gap: 14px;
  height: 110px;
  margin-bottom: 12px;
}
.bs-top-card {
  position: relative;
  background: rgba(5, 16, 25, 0.72);
  border: 1px solid rgba(6, 182, 212, 0.22);
  padding: 12px 16px;
  backdrop-filter: blur(6px);
  overflow: hidden;
  box-shadow: inset 0 0 24px rgba(6, 182, 212, 0.04), 0 4px 20px rgba(0, 0, 0, 0.35);
}
.bs-top-card::before {
  content: "";
  position: absolute;
  top: 0; left: 0; right: 0; bottom: 0;
  border-radius: inherit;
  padding: 1px;
  background: linear-gradient(135deg, rgba(6, 182, 212, 0.45) 0%, rgba(6, 182, 212, 0.05) 30%, rgba(6, 182, 212, 0.05) 70%, rgba(6, 182, 212, 0.35) 100%);
  -webkit-mask: linear-gradient(#fff 0 0) content-box, linear-gradient(#fff 0 0);
  -webkit-mask-composite: xor;
  mask-composite: exclude;
  pointer-events: none;
}
.bs-top-card::after {
  content: "";
  position: absolute;
  top: 0; right: 0;
  width: 34px; height: 34px;
  border-top: 1px solid rgba(6, 182, 212, 0.6);
  border-right: 1px solid rgba(6, 182, 212, 0.6);
}
.bs-card-title {
  position: relative;
  display: flex;
  align-items: center;
  gap: 8px;
  font-size: 14px;
  font-weight: 600;
  color: #fff;
  margin-bottom: 12px;
  padding-bottom: 4px;
}
.bs-card-title::before {
  content: "";
  position: absolute;
  top: -2px;
  left: 28px;
  width: 50%;
  height: 1px;
  background: linear-gradient(90deg, rgba(6, 182, 212, 0.85), rgba(6, 182, 212, 0.1) 70%, transparent);
}
.bs-card-title::after {
  content: "";
  position: absolute;
  bottom: 2px;
  left: 28px;
  width: 35%;
  height: 1px;
  background: linear-gradient(90deg, rgba(6, 182, 212, 0.5), rgba(6, 182, 212, 0.05) 70%, transparent);
}
.bs-card-icon { width: 20px; height: 20px; color: rgba(6, 182, 212, 0.9); }
.bs-card-icon svg { width: 100%; height: 100%; }
.bs-card-body { display: flex; align-items: center; height: calc(100% - 36px); }
.bs-metric-row { display: flex; gap: 18px; width: 100%; justify-content: space-around; }
.bs-metric { display: flex; flex-direction: column; gap: 2px; align-items: center; }
.bs-metric-label { font-size: 11px; color: rgba(230, 247, 255, 0.5); }
.bs-metric-value {
  font-family: "JetBrains Mono", "Fira Code", monospace;
  font-size: 22px;
  font-weight: 700;
  color: #fff;
  text-shadow: 0 0 10px rgba(6, 182, 212, 0.4);
}
.bs-metric-value.warn { color: #fbbf24; text-shadow: 0 0 10px rgba(251, 191, 36, 0.4); }
.bs-metric-value small { font-size: 11px; color: rgba(230, 247, 255, 0.5); margin-left: 2px; font-weight: 400; }

.bs-kpi-header { position: absolute; top: 8px; right: 12px; font-size: 11px; color: rgba(6, 182, 212, 0.7); }
.bs-kpi-grid { display: grid; grid-template-columns: repeat(3, 1fr); gap: 12px; height: 100%; padding-top: 18px; }
.bs-kpi-item {
  display: flex;
  align-items: center;
  gap: 10px;
  padding: 8px 12px;
  background: rgba(6, 182, 212, 0.06);
  border: 1px solid rgba(6, 182, 212, 0.12);
}
.bs-kpi-icon {
  width: 34px; height: 34px;
  display: grid; place-items: center;
  border-radius: 50%;
  background: rgba(255, 255, 255, 0.05);
  color: var(--c);
  border: 1px solid var(--c);
  box-shadow: 0 0 10px color-mix(in srgb, var(--c) 30%, transparent);
  flex-shrink: 0;
}
.bs-kpi-icon svg { width: 18px; height: 18px; }
.bs-kpi-info { display: flex; flex-direction: column; gap: 2px; }
.bs-kpi-label { font-size: 11px; color: rgba(230, 247, 255, 0.55); }
.bs-kpi-value { font-family: "JetBrains Mono", "Fira Code", monospace; font-size: 22px; font-weight: 700; color: #fff; }
.bs-kpi-value small { font-size: 11px; color: rgba(230, 247, 255, 0.5); margin-left: 3px; font-weight: 400; }
.bs-kpi-trend { font-size: 11px; color: rgba(230, 247, 255, 0.45); margin-top: 2px; }
.bs-kpi-trend.up { color: #f87171; }   /* 提升 → 红色（中国习惯） */
.bs-kpi-trend.down { color: #34d399; } /* 降低 → 绿色 */

/* ============================================================ Body Grid ============================================================ */
.bs-body {
  flex: 1;
  min-height: 0;
  display: grid;
  grid-template-columns: 270px 1fr 270px;
  gap: 14px;
  margin-bottom: 8px;
}
.bs-side { display: flex; flex-direction: column; gap: 12px; min-height: 0; overflow: hidden; }
.bs-panel {
  flex: 1;
  display: flex;
  flex-direction: column;
  min-height: 0;
  background: rgba(5, 16, 25, 0.72);
  border: 1px solid rgba(6, 182, 212, 0.18);
  backdrop-filter: blur(6px);
  position: relative;
  box-shadow: inset 0 0 24px rgba(6, 182, 212, 0.04), 0 4px 20px rgba(0, 0, 0, 0.35);
}
.bs-panel::before {
  content: "";
  position: absolute;
  top: 0; left: 0; right: 0; bottom: 0;
  border-radius: inherit;
  padding: 1px;
  background: linear-gradient(135deg, rgba(6, 182, 212, 0.45) 0%, rgba(6, 182, 212, 0.05) 25%, rgba(6, 182, 212, 0.05) 75%, rgba(6, 182, 212, 0.35) 100%);
  -webkit-mask: linear-gradient(#fff 0 0) content-box, linear-gradient(#fff 0 0);
  -webkit-mask-composite: xor;
  mask-composite: exclude;
  pointer-events: none;
}
.bs-panel::after {
  content: "";
  position: absolute;
  bottom: 0; right: 0;
  width: 24px; height: 24px;
  border-bottom: 1px solid rgba(6, 182, 212, 0.55);
  border-right: 1px solid rgba(6, 182, 212, 0.55);
}
.bs-panel-head {
  flex-shrink: 0;
  display: flex;
  align-items: center;
  justify-content: space-between;
  padding: 10px 14px;
  border-bottom: 1px solid rgba(6, 182, 212, 0.12);
}
.bs-panel-title { position: relative; font-size: 14px; font-weight: 600; color: #fff; padding-left: 12px; margin-bottom: 2px; }
.bs-panel-title::before {
  content: "";
  position: absolute;
  left: 0; top: 50%;
  transform: translateY(-50%);
  width: 4px; height: 14px;
  background: linear-gradient(180deg, #06b6d4, #3b82f6);
  border-radius: 2px;
  box-shadow: 0 0 6px rgba(6, 182, 212, 0.4);
}
.bs-panel-title::after {
  content: "";
  position: absolute;
  bottom: -4px;
  left: 12px;
  width: 40%;
  height: 1px;
  background: linear-gradient(90deg, rgba(6, 182, 212, 0.7), rgba(6, 182, 212, 0.05) 80%, transparent);
}
.bs-panel-sub { font-size: 11px; color: rgba(6, 182, 212, 0.7); }
.bs-panel-body { flex: 1; min-height: 0; overflow-y: auto; padding: 10px 14px; }
.bs-panel-body::-webkit-scrollbar { width: 3px; }
.bs-panel-body::-webkit-scrollbar-thumb { background: rgba(6, 182, 212, 0.2); }
.bs-panel-body::-webkit-scrollbar-track { background: transparent; }
.bs-empty { text-align: center; color: rgba(230, 247, 255, 0.3); font-size: 12px; padding: 24px 0; }

/* 产线进度表格 */
.bs-table { width: 100%; border-collapse: collapse; font-size: 12px; }
.bs-table th, .bs-table td { padding: 8px 6px; text-align: left; border-bottom: 1px solid rgba(6, 182, 212, 0.08); }
.bs-table th { color: rgba(6, 182, 212, 0.85); font-weight: 600; background: rgba(6, 182, 212, 0.06); }
.bs-table td { color: rgba(230, 247, 255, 0.75); }
.bs-table tbody tr:nth-child(even) { background: rgba(255, 255, 255, 0.02); }
.bs-td-name { max-width: 110px; overflow: hidden; text-overflow: ellipsis; white-space: nowrap; }
.bs-rate { font-family: "JetBrains Mono", monospace; font-weight: 600; }
.bs-rate.high { color: #34d399; }
.bs-rate.mid { color: #fbbf24; }
.bs-rate.low { color: #f87171; }
.bs-status-dot { display: inline-block; width: 7px; height: 7px; border-radius: 50%; margin-right: 4px; vertical-align: middle; }
.st-run { background: #34d399; box-shadow: 0 0 6px #34d399; }
.st-warn { background: #fbbf24; box-shadow: 0 0 6px #fbbf24; }
.st-fault { background: #f87171; box-shadow: 0 0 6px #f87171; }
.st-idle { background: #64748b; }

/* ============================================================ Center Area ============================================================ */
.bs-center { display: flex; flex-direction: column; min-height: 0; }
.bs-center-head {
  gap: 10px;
  padding: 8px 14px;
}
.bs-center-head .bs-panel-title {
  margin-bottom: 0;
  flex-shrink: 0;
  white-space: nowrap;
}
.bs-center-body { flex: 1; min-height: 0; display: flex; flex-direction: column; gap: 0; padding: 8px 14px 10px; }
.bs-center-tabs {
  position: relative;
  display: flex;
  gap: 6px;
  justify-content: flex-end;
  flex: 1;
  flex-wrap: wrap;
  margin: 0;
  min-width: 0;
  pointer-events: none;
}
.bs-center-tabs .bs-tab { pointer-events: auto; }
.bs-tab {
  padding: 4px 12px;
  font-size: 11px;
  color: rgba(230, 247, 255, 0.55);
  background: rgba(5, 16, 25, 0.85);
  border: 1px solid rgba(6, 182, 212, 0.15);
  cursor: pointer;
  transition: all 0.2s;
}
.bs-tab:hover, .bs-tab.active {
  color: #fff;
  background: rgba(6, 182, 212, 0.2);
  border-color: rgba(6, 182, 212, 0.5);
  box-shadow: 0 0 12px rgba(6, 182, 212, 0.2);
}
.bs-tab.active { background: linear-gradient(180deg, rgba(6, 182, 212, 0.3), rgba(6, 182, 212, 0.05)); }

.bs-3d-wrap { flex: 1; min-height: 0; position: relative; }
.bs-3d-wrap.bs-3d-hidden { display: none; }
.bs-3d-frame {
  position: absolute;
  inset: 0;
  border: none;
  background: rgba(0, 5, 10, 0.25);
}
.bs-3d-corner { position: absolute; width: 16px; height: 16px; border-color: rgba(6, 182, 212, 0.7); border-style: solid; border-width: 0; }
.bs-3d-corner--tl { top: 0; left: 0; border-top-width: 2px; border-left-width: 2px; }
.bs-3d-corner--tr { top: 0; right: 0; border-top-width: 2px; border-right-width: 2px; }
.bs-3d-corner--bl { bottom: 0; left: 0; border-bottom-width: 2px; border-left-width: 2px; }
.bs-3d-corner--br { bottom: 0; right: 0; border-bottom-width: 2px; border-right-width: 2px; }
.bs-3d-inner { position: absolute; inset: 0; overflow: hidden; }
.bs-3d-inner :deep(.lo3d-root) { height: 100% !important; min-height: 100% !important; background: transparent !important; }
.bs-3d-inner :deep(.lo3d-loading),
.bs-3d-inner :deep(.lo3d-arrow),
.bs-3d-inner :deep(.lo3d-bar),
.bs-3d-inner :deep(.lo3d-tooltip),
.bs-3d-inner :deep(.lo3d-ctx) { display: none !important; }

/* 单产线详情 */
.bs-line-detail {
  flex: 1;
  min-height: 0;
  border: 1px solid rgba(6, 182, 212, 0.2);
  background: rgba(5, 16, 25, 0.65);
  padding: 16px 20px;
  display: flex;
  flex-direction: column;
  gap: 14px;
  overflow-y: auto;
}
.bs-line-detail-head { display: flex; align-items: center; justify-content: space-between; gap: 12px; }
.bs-line-detail-title { display: flex; align-items: center; gap: 12px; }
.bs-line-detail-name { font-size: 18px; font-weight: 700; color: #fff; }
.bs-back-btn {
  display: flex;
  align-items: center;
  gap: 6px;
  padding: 5px 12px;
  font-size: 12px;
  color: rgba(230, 247, 255, 0.8);
  background: rgba(6, 182, 212, 0.1);
  border: 1px solid rgba(6, 182, 212, 0.25);
  cursor: pointer;
  transition: all 0.2s;
}
.bs-back-btn:hover {
  color: #fff;
  background: rgba(6, 182, 212, 0.2);
  border-color: rgba(6, 182, 212, 0.5);
  box-shadow: 0 0 10px rgba(6, 182, 212, 0.2);
}
.bs-back-btn svg { width: 14px; height: 14px; }
.bs-status-badge { font-size: 12px; padding: 2px 10px; border-radius: 3px; color: #fff; }
.bs-status-badge.st-run { background: rgba(52, 211, 153, 0.2); border: 1px solid #34d399; color: #34d399; }
.bs-status-badge.st-warn { background: rgba(251, 191, 36, 0.2); border: 1px solid #fbbf24; color: #fbbf24; }
.bs-status-badge.st-fault { background: rgba(248, 113, 113, 0.2); border: 1px solid #f87171; color: #f87171; }
.bs-status-badge.st-idle { background: rgba(100, 116, 139, 0.2); border: 1px solid #64748b; color: #94a3b8; }
.bs-line-detail-grid { display: grid; grid-template-columns: repeat(3, 1fr); gap: 12px; }
.bs-ld-item { display: flex; flex-direction: column; gap: 4px; padding: 10px 12px; background: rgba(6, 182, 212, 0.06); border: 1px solid rgba(6, 182, 212, 0.12); }
.bs-ld-item span { font-size: 11px; color: rgba(230, 247, 255, 0.5); }
.bs-ld-item b { font-family: "JetBrains Mono", monospace; font-size: 17px; color: #fff; font-weight: 700; }
.bs-ld-progress { margin-top: auto; }

/* 底部进度区 */
.bs-mid-progress {
  flex-shrink: 0;
  display: grid;
  grid-template-columns: repeat(3, 1fr);
  gap: 10px;
  min-height: 76px;
  margin-bottom: 8px;
}
.bs-progress-card {
  background: rgba(5, 16, 25, 0.7);
  border: 1px solid rgba(6, 182, 212, 0.15);
  padding: 8px 12px;
  display: flex;
  flex-direction: column;
  justify-content: space-between;
  gap: 6px;
  min-width: 0;
}
.bs-progress-label {
  display: flex;
  justify-content: space-between;
  align-items: flex-start;
  gap: 8px;
  min-height: 30px;
}
.bs-progress-title {
  flex: 1;
  min-width: 0;
  font-size: 12px;
  line-height: 1.35;
  color: #fff;
  overflow: hidden;
  display: -webkit-box;
  -webkit-line-clamp: 2;
  -webkit-box-orient: vertical;
}
.bs-progress-pct {
  flex-shrink: 0;
  font-family: "JetBrains Mono", monospace;
  font-size: 17px;
  font-weight: 700;
  color: var(--c, #06b6d4);
  text-shadow: 0 0 10px color-mix(in srgb, var(--c, #06b6d4) 40%, transparent);
  line-height: 1;
  padding-top: 1px;
}
.bs-progress-track { height: 7px; background: rgba(255, 255, 255, 0.06); border-radius: 4px; overflow: hidden; }
.bs-progress-fill {
  height: 100%;
  width: var(--pct);
  background: linear-gradient(90deg, color-mix(in srgb, var(--c, #06b6d4) 60%, transparent), var(--c, #06b6d4));
  box-shadow: 0 0 10px color-mix(in srgb, var(--c, #06b6d4) 50%, transparent);
  transition: width 0.8s ease;
}
.bs-progress-detail {
  display: flex;
  justify-content: space-between;
  gap: 8px;
  font-size: 11px;
  color: rgba(230, 247, 255, 0.55);
  white-space: nowrap;
}
.bs-progress-detail span {
  overflow: hidden;
  text-overflow: ellipsis;
}

/* 销售市场地图与条形图（层叠：地图底层，条形图叠在上层底部） */
.bs-sales-body {
  position: relative;
  height: 100%;
  min-height: 0;
  padding: 8px 10px 6px;
  overflow: hidden;
}
.bs-sales-map {
  position: absolute;
  inset: 8px 10px 6px;
  z-index: 1;
  overflow: hidden;
}
.bs-sales-bar {
  position: absolute;
  left: 10px;
  right: 10px;
  bottom: 6px;
  height: 40%;
  z-index: 2;
}

/* ============================================================ Right Side — Equipment ============================================================ */
.bs-equip-body {
  display: flex;
  flex-direction: column;
  gap: 12px;
  height: 100%;
  padding: 8px 12px !important;
  overflow: hidden;
}
.bs-equip-top {
  display: flex;
  align-items: center;
  gap: 14px;
  flex-shrink: 0;
}
.bs-equip-ring {
  width: 72px;
  height: 72px;
  flex-shrink: 0;
}
.bs-equip-ring .bs-ring-value { font-size: 15px; }
.bs-equip-ring .bs-ring-label { bottom: 13px; font-size: 9px; white-space: nowrap; }
.bs-equip-chips {
  display: grid;
  grid-template-columns: 1fr 1fr;
  gap: 10px 12px;
  flex: 1;
  min-width: 0;
  align-items: center;
}
.bs-equip-chip {
  display: flex;
  flex-direction: column;
  align-items: center;
  justify-content: center;
  gap: 3px;
  text-align: center;
}
.bs-equip-chip-value {
  font-family: "JetBrains Mono", monospace;
  font-size: 19px;
  font-weight: 700;
  line-height: 1.15;
}
.bs-equip-chip-label { font-size: 11px; color: rgba(230, 247, 255, 0.5); }
.bs-equip-stack-wrap { flex-shrink: 0; display: flex; flex-direction: column; gap: 6px; }
.bs-equip-stack-label {
  display: flex;
  justify-content: space-between;
  font-size: 10px;
  color: rgba(230, 247, 255, 0.55);
}
.bs-equip-stack {
  display: flex;
  align-items: stretch;
  height: 11px;
  gap: 2px;
  border-radius: 6px;
  overflow: hidden;
  background: rgba(255, 255, 255, 0.06);
}
.bs-equip-stack-seg {
  min-width: 4px;
  border-radius: 3px;
  transition: flex-basis 0.6s ease;
}
.bs-equip-list {
  flex: 1;
  min-height: 0;
  display: flex;
  flex-direction: column;
  gap: 3px;
  overflow-y: auto;
}
.bs-equip-list::-webkit-scrollbar { width: 3px; }
.bs-equip-list::-webkit-scrollbar-thumb { background: rgba(6, 182, 212, 0.2); }
.bs-equip-row {
  display: grid;
  grid-template-columns: 8px 1fr 32px 38px 36%;
  gap: 5px;
  align-items: center;
  padding: 2px 6px;
  background: rgba(255, 255, 255, 0.03);
  border-radius: 4px;
  font-size: 11px;
}
.bs-equip-dot { width: 8px; height: 8px; border-radius: 50%; flex-shrink: 0; }
.bs-equip-name { color: rgba(230, 247, 255, 0.85); overflow: hidden; text-overflow: ellipsis; white-space: nowrap; }
.bs-equip-count {
  font-family: "JetBrains Mono", monospace;
  font-weight: 700;
  font-size: 14px;
  color: #fff;
  text-align: center;
  justify-self: center;
  min-width: 32px;
}
.bs-equip-pct {
  font-family: "JetBrains Mono", monospace;
  color: rgba(230, 247, 255, 0.45);
  text-align: center;
  justify-self: center;
  min-width: 38px;
  font-size: 11px;
}
.bs-equip-mini-bar {
  height: 8px;
  background: rgba(255, 255, 255, 0.08);
  border-radius: 3px;
  overflow: hidden;
  align-self: center;
}
.bs-equip-mini-fill { height: 100%; border-radius: 3px; transition: width 0.6s ease; }

.bs-alert-count {
  font-family: "JetBrains Mono", monospace;
  font-size: 12px;
  font-weight: 600;
  color: #f87171;
  background: rgba(239, 68, 68, 0.12);
  border: 1px solid rgba(239, 68, 68, 0.3);
  padding: 1px 8px;
}
.bs-alert-body-wrap {
  display: flex;
  flex-direction: column;
  gap: 12px;
  height: 100%;
  padding: 8px 12px !important;
  overflow: hidden;
}
.bs-alert-top {
  display: flex;
  align-items: center;
  gap: 14px;
  flex-shrink: 0;
}
.bs-alert-ring {
  width: 72px;
  height: 72px;
  flex-shrink: 0;
}
.bs-alert-ring .bs-ring-value { font-size: 19px; color: #f87171; }
.bs-alert-ring .bs-ring-label { bottom: 13px; font-size: 9px; white-space: nowrap; }
.bs-alert-chips {
  display: grid;
  grid-template-columns: 1fr 1fr;
  gap: 10px 12px;
  flex: 1;
  min-width: 0;
  align-items: center;
}
.bs-alert-chip {
  display: flex;
  flex-direction: column;
  align-items: center;
  justify-content: center;
  gap: 3px;
  text-align: center;
}
.bs-alert-chip-value {
  font-family: "JetBrains Mono", monospace;
  font-size: 17px;
  font-weight: 700;
  line-height: 1.15;
}
.bs-alert-chip-label { font-size: 11px; color: rgba(230, 247, 255, 0.5); }
.bs-alert-stack-wrap { flex-shrink: 0; display: flex; flex-direction: column; gap: 6px; }
.bs-alert-stack-label {
  display: flex;
  justify-content: space-between;
  font-size: 10px;
  color: rgba(230, 247, 255, 0.55);
}
.bs-alert-stack {
  display: flex;
  align-items: stretch;
  height: 11px;
  gap: 2px;
  border-radius: 6px;
  overflow: hidden;
  background: rgba(255, 255, 255, 0.06);
}
.bs-alert-stack-seg {
  min-width: 4px;
  border-radius: 3px;
  transition: flex-basis 0.6s ease;
}
.bs-alert-cards {
  flex: 1;
  min-height: 0;
  display: flex;
  flex-direction: column;
  gap: 3px;
  overflow-y: auto;
}
.bs-alert-cards::-webkit-scrollbar { width: 3px; }
.bs-alert-cards::-webkit-scrollbar-thumb { background: rgba(239, 68, 68, 0.2); }
.bs-alert-card {
  padding: 4px 10px 4px 10px;
  background: rgba(239, 68, 68, 0.04);
  border-left: 3px solid var(--accent, #f87171);
  border-radius: 4px;
  cursor: pointer;
  transition: background 0.2s ease, transform 0.2s ease;
}
.bs-alert-card:hover { background: rgba(239, 68, 68, 0.08); }
.bs-alert-card--pulse {
  animation: bs-alert-pulse 2.4s ease-in-out infinite;
}
@keyframes bs-alert-pulse {
  0%, 100% { box-shadow: 0 0 0 rgba(248, 113, 113, 0); }
  50% { box-shadow: 0 0 12px rgba(248, 113, 113, 0.18); }
}
.bs-alert-card-head {
  display: flex;
  align-items: center;
  gap: 6px;
  margin-bottom: 1px;
}
.bs-alert-badge {
  font-size: 9px;
  font-weight: 600;
  padding: 0 5px;
  border-radius: 3px;
  border: 1px solid;
  flex-shrink: 0;
  line-height: 1.5;
}
.bs-alert-code {
  font-family: "JetBrains Mono", monospace;
  color: #f87171;
  font-weight: 600;
  font-size: 11px;
}
.bs-alert-priority {
  margin-left: auto;
  font-size: 10px;
  font-weight: 700;
  color: #f87171;
  background: rgba(239, 68, 68, 0.15);
  padding: 0 5px;
  border-radius: 3px;
}
.bs-alert-desc {
  color: rgba(255, 255, 255, 0.92);
  font-size: 11px;
  line-height: 1.25;
  overflow: hidden;
  text-overflow: ellipsis;
  white-space: nowrap;
  margin-bottom: 1px;
}
.bs-alert-meta {
  display: flex;
  justify-content: space-between;
  gap: 6px;
  font-size: 9px;
  color: rgba(230, 247, 255, 0.45);
  overflow: hidden;
  line-height: 1.2;
}
.bs-alert-meta span { overflow: hidden; text-overflow: ellipsis; white-space: nowrap; }
.bs-alert-safe {
  flex: 1;
  display: flex;
  flex-direction: column;
  align-items: center;
  justify-content: center;
  gap: 8px;
  min-height: 120px;
  color: rgba(16, 185, 129, 0.85);
}
.bs-alert-safe-icon { width: 40px; height: 40px; opacity: 0.7; }
.bs-alert-safe-title { font-size: 14px; font-weight: 600; color: rgba(230, 247, 255, 0.75); }
.bs-alert-safe-sub { font-size: 11px; color: rgba(230, 247, 255, 0.35); }

/* ============================================================ Footer ============================================================ */
.bs-footer { flex-shrink: 0; height: 148px; }
.bs-footer-split {
  display: grid;
  grid-template-columns: 1fr 1fr;
  gap: 10px;
  height: 100%;
  min-height: 0;
}
.bs-material,
.bs-footer-coord {
  height: 100%;
  display: flex;
  flex-direction: column;
  min-height: 0;
  min-width: 0;
}
.bs-material-body { flex: 1; min-height: 0; padding: 6px 12px 8px !important; overflow: hidden; }
.bs-coord-body {
  flex: 1;
  min-height: 0;
  display: flex;
  flex-direction: row;
  gap: 10px;
  padding: 6px 12px 8px !important;
  overflow: hidden;
}
.bs-coord-lines {
  flex: 1;
  min-width: 0;
  min-height: 0;
  display: flex;
  flex-direction: column;
  gap: 4px;
  overflow-y: auto;
  padding-right: 2px;
}
.bs-coord-alerts {
  flex: 1;
  min-width: 0;
  min-height: 0;
  display: flex;
  flex-direction: column;
  gap: 4px;
  overflow-y: auto;
  padding-right: 2px;
  border-left: 1px solid rgba(6, 182, 212, 0.1);
  padding-left: 10px;
}
.bs-coord-lines::-webkit-scrollbar,
.bs-coord-alerts::-webkit-scrollbar { width: 3px; }
.bs-coord-lines::-webkit-scrollbar-thumb,
.bs-coord-alerts::-webkit-scrollbar-thumb { background: rgba(6, 182, 212, 0.25); border-radius: 2px; }
.bs-coord-section-title {
  flex-shrink: 0;
  font-size: 10px;
  color: rgba(6, 182, 212, 0.65);
  letter-spacing: 0.5px;
  margin-bottom: 2px;
}
.bs-coord-line-item {
  display: flex;
  flex-direction: column;
  gap: 3px;
  padding: 3px 0;
}
.bs-coord-line-head {
  display: flex;
  justify-content: space-between;
  align-items: center;
  gap: 8px;
  font-size: 11px;
}
.bs-coord-line-name {
  flex: 1;
  min-width: 0;
  color: rgba(230, 247, 255, 0.85);
  overflow: hidden;
  text-overflow: ellipsis;
  white-space: nowrap;
}
.bs-coord-line-meta {
  flex-shrink: 0;
  font-family: "JetBrains Mono", monospace;
  font-size: 10px;
  color: rgba(6, 182, 212, 0.9);
  white-space: nowrap;
}
.bs-coord-alert-item {
  display: flex;
  align-items: center;
  gap: 6px;
  font-size: 11px;
  line-height: 1.3;
  padding: 2px 0;
}
.bs-coord-alert-badge {
  flex-shrink: 0;
  font-size: 10px;
  padding: 1px 6px;
  border-radius: 3px;
  border: 1px solid;
  font-weight: 600;
}
.bs-coord-alert-msg {
  flex: 1;
  min-width: 0;
  color: rgba(230, 247, 255, 0.75);
  overflow: hidden;
  text-overflow: ellipsis;
  white-space: nowrap;
}
.bs-material-main {
  display: flex;
  align-items: stretch;
  gap: 12px;
  height: 100%;
  min-height: 0;
}
.bs-material-side {
  flex-shrink: 0;
  display: flex;
  align-items: center;
  justify-content: center;
  width: 84px;
  padding-right: 12px;
  border-right: 1px solid rgba(6, 182, 212, 0.12);
}
.bs-ring {
  position: relative;
  width: 76px;
  height: 76px;
  flex-shrink: 0;
  border-radius: 50%;
  background: conic-gradient(var(--c, #06b6d4) calc(var(--pct, 0) * 1%), rgba(255, 255, 255, 0.08) 0);
  display: grid;
  place-items: center;
  box-shadow: 0 0 20px color-mix(in srgb, var(--c, #06b6d4) 20%, transparent);
}
.bs-ring::before { content: ""; position: absolute; inset: 7px; border-radius: 50%; background: #00050a; }
.bs-ring-value, .bs-ring-label { position: relative; z-index: 1; }
.bs-ring-value { font-family: "JetBrains Mono", monospace; font-size: 16px; font-weight: 700; color: var(--c, #06b6d4); }
.bs-ring-label { position: absolute; bottom: 14px; font-size: 10px; color: rgba(230, 247, 255, 0.6); white-space: nowrap; }
.bs-material-list {
  flex: 1;
  min-width: 0;
  display: flex;
  flex-direction: column;
  gap: 0;
  overflow-y: auto;
  padding-right: 2px;
}
.bs-material-list::-webkit-scrollbar { width: 3px; }
.bs-material-list::-webkit-scrollbar-thumb { background: rgba(6, 182, 212, 0.25); border-radius: 2px; }
.bs-wo-item {
  display: flex;
  flex-direction: column;
  gap: 4px;
  padding: 5px 0;
  border-bottom: 1px solid rgba(6, 182, 212, 0.08);
}
.bs-wo-item:last-child { border-bottom: none; }
.bs-wo-head {
  display: grid;
  grid-template-columns: minmax(96px, 1.1fr) minmax(0, 2fr) 64px;
  gap: 10px;
  align-items: center;
  font-size: 11px;
  line-height: 1.3;
}
.bs-wo-no {
  font-family: "JetBrains Mono", monospace;
  color: rgba(6, 182, 212, 0.9);
  overflow: hidden;
  text-overflow: ellipsis;
  white-space: nowrap;
}
.bs-wo-name {
  color: rgba(230, 247, 255, 0.75);
  overflow: hidden;
  text-overflow: ellipsis;
  white-space: nowrap;
}
.bs-wo-qty {
  font-family: "JetBrains Mono", monospace;
  color: #fff;
  text-align: right;
  white-space: nowrap;
}
.bs-wo-bar { height: 5px; background: rgba(255, 255, 255, 0.06); border-radius: 3px; overflow: hidden; }
.bs-wo-bar-fill { height: 100%; background: linear-gradient(90deg, rgba(6, 182, 212, 0.5), #06b6d4); transition: width 0.6s ease; border-radius: 3px; }
</style>
