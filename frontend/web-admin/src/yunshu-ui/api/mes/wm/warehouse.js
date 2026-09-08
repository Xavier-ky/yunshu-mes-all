import request from '@/yunshu-ui/api/request'

export function listWarehouse(query) {
  return request({
    url: '/mes/wm/warehouse/list',
    method: 'get',
    params: query
  })
}

export function getTreeList() {
  return request({
    url: '/mes/wm/warehouse/getTreeList',
    method: 'get'
  })
}

export function getWarehouse(warehouseId) {
  return request({
    url: '/mes/wm/warehouse/' + warehouseId,
    method: 'get'
  })
}

export function addWarehouse(data) {
  return request({
    url: '/mes/wm/warehouse',
    method: 'post',
    data: data
  })
}

export function updateWarehouse(data) {
  return request({
    url: '/mes/wm/warehouse',
    method: 'put',
    data: data
  })
}

export function delWarehouse(warehouseId) {
  return request({
    url: '/mes/wm/warehouse/' + warehouseId,
    method: 'delete'
  })
}

export function changeFrozenState(warehouseId, status) {
  const data = {
    warehouseId: warehouseId,
    frozenFlag: status
  }
  return request({
    url: '/mes/wm/warehouse',
    method: 'put',
    data: data
  })
}
