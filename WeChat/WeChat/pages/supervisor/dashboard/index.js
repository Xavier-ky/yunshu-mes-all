import { getLineProgress, getTrendData, getAbnormalOverview, getDefectDistribution } from '../../../services/dashboard'
import { guardPageAccess } from '../../../utils/permission'
import { PAGE_PATH_MAP } from '../../../utils/constants'

const LINE_COLORS = ['#1A3A5C', '#2E8B8B', '#D4AF37', '#5B7FA5']

Page({
  data: {

    kpiOutput: 0,
    kpiPlan: 0,
    kpiCompletionRate: 0,
    kpiQualifiedRate: 0,
    kpiActiveLines: 0,
    kpiTotalLines: 0,
    kpiWip: 0,
    loading: true,

    lines: [],

    trendDays: [],
    trendValues: [],
    trendMax: 0,
    trendQualifiedRates: [],

    distribution: [],
    distTotal: 0,

    totalOutput: 0,
    totalPlan: 0,
    avgQualifiedRate: 0,

    abnormal: {},

    defectDistribution: [],
    defectTotal: 0
  },

  onLoad() {
    guardPageAccess(PAGE_PATH_MAP.supervisorDashboard)
    this.loadData()
  },

  onShow() {
    if (!this.data.loading) {
      this.loadData()
    }
  },

  loadData() {
    this.setData({ loading: true })

    Promise.all([
      getLineProgress(),
      getTrendData(),
      getAbnormalOverview(),
      getDefectDistribution()
    ]).then(([lineRes, trendRes, abnormalRes, defectRes]) => {

      const lines = (lineRes.data?.lines || []).map((l, i) => ({
        ...l,
        progress: l.planQty > 0 ? Math.round(l.actualQty / l.planQty * 100) : 0,
        color: LINE_COLORS[i % LINE_COLORS.length],
        statusLabel: l.planQty > 0 && l.actualQty >= l.planQty ? '已完成'
          : l.progress >= 80 ? '正常' : l.progress >= 50 ? '注意' : '预警'
      }))

      const totalOutput = lines.reduce((s, l) => s + l.actualQty, 0)
      const totalPlan = lines.reduce((s, l) => s + l.planQty, 0)
      const completionRate = totalPlan > 0 ? Math.round(totalOutput / totalPlan * 100) : 0
      const avgQr = lines.length > 0
        ? (lines.reduce((s, l) => s + l.qualifiedRate, 0) / lines.length).toFixed(1)
        : 0
      const activeLines = lines.filter(l => l.actualQty > 0).length
      const totalWip = lines.reduce((s, l) => s + (l.taskCount || 0), 0)

      const distribution = lines.map((l, i) => ({
        name: l.lineName,
        output: l.actualQty,
        percent: totalOutput > 0 ? Math.round(l.actualQty / totalOutput * 100) : 0,
        color: LINE_COLORS[i % LINE_COLORS.length]
      }))

      const trendDaysList = (trendRes.data?.daily || []).map(d => d.date)
      const trendValuesList = (trendRes.data?.daily || []).map(d => d.output)
      const trendMax = Math.max(...trendValuesList, 1)
      const trendQualifiedRates = (trendRes.data?.daily || []).map(d => d.qualifiedRate)

      this.setData({
        lines,
        totalOutput,
        totalPlan,
        avgQualifiedRate: avgQr,
        kpiOutput: totalOutput,
        kpiPlan: totalPlan,
        kpiCompletionRate: completionRate,
        kpiQualifiedRate: avgQr,
        kpiActiveLines: activeLines,
        kpiTotalLines: lines.length,
        kpiWip: totalWip,
        distribution,
        distTotal: totalOutput,
        trendDays: trendDaysList,
        trendValues: trendValuesList,
        trendMax,
        trendQualifiedRates,
        abnormal: abnormalRes.data || {},
        defectDistribution: (defectRes.data?.items || []),
        defectTotal: defectRes.data?.total || 0,
        loading: false
      })
    }).catch(() => {
      this.setData({ loading: false })
      wx.showToast({ title: '加载失败', icon: 'none' })
    })
  },

  onLineTap(e) {
    const index = e.currentTarget.dataset.index
    const line = this.data.lines[index]
    if (line) {
      wx.showToast({ title: `${line.lineName}\n计划:${line.planQty} 实际:${line.actualQty} 合格率:${line.qualifiedRate}%`, icon: 'none', duration: 2500 })
    }
  }
})
