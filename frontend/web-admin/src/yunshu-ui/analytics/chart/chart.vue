<template>
  <div ref="chartRef" :class="className" :style="{ height, width }" />
</template>

<script>
import * as echarts from 'echarts'
import resize from './mixins/resize.js'
import request from '@/yunshu-ui/api/request'

export default {
  mixins: [resize],
  props: {
    className: {
      type: String,
      default: 'chart'
    },
    width: {
      type: String,
      default: '100%'
    },
    height: {
      type: String,
      default: '280px'
    },
    chartOptions: {
      type: Object,
      default: () => ({})
    },
    api: {
      type: String,
      default: ''
    }
  },
  data() {
    return {
      chart: null
    }
  },
  watch: {
    chartOptions: {
      deep: true,
      handler() {
        this.renderChart()
      }
    }
  },
  mounted() {
    this.$nextTick(() => {
      this.renderChart()
      this.loadRemoteData()
    })
  },
  beforeUnmount() {
    this.disposeChart()
  },
  methods: {
    disposeChart() {
      if (this.chart) {
        this.chart.dispose()
        this.chart = null
      }
    },
    renderChart() {
      if (!this.$refs.chartRef) {
        return
      }
      if (!this.chart) {
        this.chart = echarts.init(this.$refs.chartRef)
      }
      const options = this.chartOptions && typeof this.chartOptions === 'object'
        ? this.chartOptions
        : {}
      this.chart.setOption(options, true)
      this.chart.resize()
    },
    async loadRemoteData() {
      const api = (this.api || '').trim()
      if (!api) {
        return
      }
      try {
        let payload
        if (/^https?:\/\//i.test(api)) {
          const res = await fetch(api)
          payload = await res.json()
        } else {
          const path = api.startsWith('/api/') ? api.slice(4) : api.replace(/^\//, '')
          payload = await request({ url: path.startsWith('/') ? path : `/${path}`, method: 'get' })
        }
        const data = payload?.data ?? payload
        if (!this.chart || !data) {
          return
        }
        if (Array.isArray(data)) {
          this.chart.setOption({ series: [{ data }] })
          return
        }
        if (Array.isArray(data.metrics)) {
          const names = data.metrics.map((m) => m.label || m.name || m.code)
          const values = data.metrics.map((m) => Number(m.value ?? m.count ?? 0))
          this.chart.setOption({
            xAxis: { type: 'category', data: names },
            yAxis: { type: 'value' },
            series: [{ type: 'bar', data: values }]
          })
        }
      } catch (e) {
        console.warn('[chart] loadRemoteData failed', e)
      }
    }
  }
}
</script>
