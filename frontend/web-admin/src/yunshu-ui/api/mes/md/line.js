import request from '@/yunshu-ui/api/request'

export function listLine(query) {
  return request({ url: '/mes/md/line/list', method: 'get', params: query })
}

export function getLine(lineId) {
  return request({ url: '/mes/md/line/' + lineId, method: 'get' })
}

export function addLine(data) {
  return request({ url: '/mes/md/line', method: 'post', data })
}

export function updateLine(data) {
  return request({ url: '/mes/md/line', method: 'put', data })
}

export function delLine(lineId) {
  return request({ url: '/mes/md/line/' + lineId, method: 'delete' })
}
