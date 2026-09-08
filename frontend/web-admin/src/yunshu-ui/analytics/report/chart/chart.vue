<template>
  <div ref="chart" :class="className" :style="{height:height,width:width}" />
</template>

<script>
import echarts from 'echarts'
require('echarts/theme/macarons') // echarts theme
import resize from './mixins/resize'
import axios from 'axios'

const animationDuration = 6000

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
      default: '300px'
    },
    chartOptions: {
        type: Object,
        required: true
    },
    api: {
        type: String,
        required: true
    }
  },
  data() {
    return {
      chart: null
    }
  },
  mounted() {
    this.$nextTick(() => {
      this.initChart();
      this.getData();
    })
    window.addEventListener('resize', () => {
      this.chart?.resize();
    })
  },
  beforeDestroy() {
    if (!this.chart) {
      return
    }
    this.chart.dispose()
    this.chart = null
  },
  methods: {
    initChart() {
      this.chart = echarts.init(this.$refs.chart, 'macarons');
      this.chart.setOption(this.chartOptions);
    },
    isValidUrl(url) {
      try {
        new URL(url);
        return true;
      } catch (error) {
        return false;
      }
    },
    getData() {
      if(this.isValidUrl(this.api)) {
        axios.get(this.api)
          .then(response => {
            const option = {
              series: [{
                data: response.data
              }]
            };
            this.chart.setOption(option);
          })
          .catch(error => {
            console.log(error);
          });
      }
    }
  }
}
</script>
