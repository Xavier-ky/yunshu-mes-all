import { getTrendData } from '../../services/dashboard'

Page({
  data: {
    daily: [],

    todayOutput: 0,
    avgOutput: 0,
    peakOutput: 0,
    peakDate: '',
    trend: 'up',
    trendDiff: 0,
    yLabels: [],
    yLabelsReversed: [],
    cumulativeOutput: 0,

    avgQualifiedRate: 0,
    bestQualifiedRate: 0,
    bestQrDate: ''
  },

  onLoad() {
    this.loadData()
  },

  loadData() {
    getTrendData().then(res => {
      if (res.code === 0 && res.data) {
        const daily = res.data.daily || []
        if (daily.length === 0) return

        const maxOutput = Math.max(...daily.map(d => d.output), 1)
        const todayOutput = daily[daily.length - 1].output
        const avgOutput = Math.round(daily.reduce((s, d) => s + d.output, 0) / daily.length)

        const peak = daily.reduce((max, d) => d.output > max.output ? d : max, daily[0])

        const mid = Math.floor(daily.length / 2)
        const firstHalf = daily.slice(0, mid).reduce((s, d) => s + d.output, 0)
        const secondHalf = daily.slice(mid).reduce((s, d) => s + d.output, 0)
        const trend = secondHalf >= firstHalf ? 'up' : 'down'
        const trendDiff = avgOutput > 0
          ? Math.round(Math.abs(secondHalf - firstHalf) / (daily.length / 2) / avgOutput * 100)
          : 0

        const chartDaily = daily.map(d => ({
          ...d,
          barHeight: Math.max(d.output / maxOutput * 100, 5)
        }))

        const step = Math.ceil(maxOutput / 4 / 50) * 50 || 50
        const yLabels = []
        for (let i = 0; i <= 4; i++) {
          yLabels.push(step * i)
        }

        const avgQualifiedRate = (daily.reduce((s, d) => s + d.qualifiedRate, 0) / daily.length).toFixed(1)
        const bestQr = daily.reduce((max, d) => d.qualifiedRate > max.qualifiedRate ? d : max, daily[0])

        const cumulativeOutput = daily.reduce((s, d) => s + d.output, 0)

        const yLabelsReversed = [...yLabels].reverse()

        this.setData({
          daily: chartDaily,
          todayOutput,
          avgOutput,
          peakOutput: peak.output,
          peakDate: peak.date,
          trend,
          trendDiff,
          yLabels,
          yLabelsReversed,
          avgQualifiedRate,
          bestQualifiedRate: bestQr.qualifiedRate,
          bestQrDate: bestQr.date,
          cumulativeOutput
        })
      }
    })
  }
})
