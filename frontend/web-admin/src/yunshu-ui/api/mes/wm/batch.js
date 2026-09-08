import request from '@/yunshu-ui/api/request'

export function listBatch(query) {
  return request({
    url: '/mes/wm/batch/list',
    method: 'get',
    params: query
  })
}

export function getBatch(batchId) {
  return request({
    url: '/mes/wm/batch/' + batchId,
    method: 'get'
  }).catch(() => ({ data: null }))
}

export function addBatch(data) {
  return request({ url: '/mes/wm/batch', method: 'post', data })
}

export function updateBatch(data) {
  return request({ url: '/mes/wm/batch', method: 'put', data })
}

export function delBatch(batchId) {
  return request({ url: '/mes/wm/batch/' + batchId, method: 'delete' })
}

export function listForward(query) {
  return request({
    url: '/mes/wm/batch/listForward',
    method: 'get',
    params: query
  }).catch(() => ({ rows: [], total: 0, data: [] }))
}

export function listBackward(query) {
  return request({
    url: '/mes/wm/batch/listBackward',
    method: 'get',
    params: query
  }).catch(() => ({ rows: [], total: 0, data: [] }))
}
