import request from '@/yunshu-ui/api/request'

export function listReport(query) {
  return request({ url: '/ureportM/list', method: 'get', params: query })
}

export function getReport(id) {
  return request({ url: '/ureportM/' + id, method: 'get' })
}

export function addReport(data) {
  return request({ url: '/ureportM', method: 'post', data })
}

export function updateReport(data) {
  return request({ url: '/ureportM', method: 'put', data })
}

export function delReport(id) {
  return request({ url: '/ureportM/' + id, method: 'delete' })
}
