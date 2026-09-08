import request from '@/yunshu-ui/api/request'

export function listWmstock(query) {
  return request({
    url: '/mes/wm/wmstock/list',
    method: 'get',
    params: query
  })
}

export function getWmstockOverview() {
  return request({
    url: '/mes/wm/wmstock/overview',
    method: 'get'
  })
}

export function getBinMap(query) {
  return request({
    url: '/mes/wm/wmstock/bin-map',
    method: 'get',
    params: query
  })
}

export function getWmstock(materialStockId) {
  return request({
    url: '/mes/wm/wmstock/' + materialStockId,
    method: 'get'
  })
}

export function addWmstock(data) {
  return request({
    url: '/mes/wm/wmstock',
    method: 'post',
    data: data
  })
}

export function updateWmstock(data) {
  return request({
    url: '/mes/wm/wmstock',
    method: 'put',
    data: data
  })
}

export function delWmstock(materialStockId) {
  return request({
    url: '/mes/wm/wmstock/' + materialStockId,
    method: 'delete'
  })
}

export function changeFrozenState(materialStockId, status) {
  const data = { materialStockId, frozenFlag: status }
  return request({
    url: '/mes/wm/wmstock',
    method: 'put',
    data: data
  })
}
