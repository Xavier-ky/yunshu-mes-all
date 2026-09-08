import request from '@/yunshu-ui/api/request'

export function listChart(query) {
  return request({ url: '/mes/report/chart/list', method: 'get', params: query })
}

export function getChart(chartId) {
  return request({ url: '/mes/report/chart/' + chartId, method: 'get' })
}

export function addChart(data) {
  return request({ url: '/mes/report/chart', method: 'post', data })
}

export function updateChart(data) {
  return request({ url: '/mes/report/chart', method: 'put', data })
}

export function delChart(chartId) {
  return request({ url: '/mes/report/chart/' + chartId, method: 'delete' })
}

export function getMyCharts() {
  return request({ url: '/mes/report/chart/getMyCharts', method: 'get' })
}
