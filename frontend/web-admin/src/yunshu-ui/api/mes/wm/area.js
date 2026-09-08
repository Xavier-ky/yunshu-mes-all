import request from '@/yunshu-ui/api/request'

export function listArea(query) {
  return request({
    url: '/mes/wm/area/list',
    method: 'get',
    params: query
  })
}

export function getArea(areaId) {
  return request({
    url: '/mes/wm/area/' + areaId,
    method: 'get'
  })
}

export function addArea(data) {
  return request({
    url: '/mes/wm/area',
    method: 'post',
    data: data
  })
}

export function updateArea(data) {
  return request({
    url: '/mes/wm/area',
    method: 'put',
    data: data
  })
}

export function delArea(areaId) {
  return request({
    url: '/mes/wm/area/' + areaId,
    method: 'delete'
  })
}

export function changeFrozenState(areaId, status) {
  const data = { areaId, frozenFlag: status }
  return request({
    url: '/mes/wm/area',
    method: 'put',
    data: data
  })
}

export function getLocationName(query) {
  return request({
    url: '/mes/wm/area/getLocationName',
    method: 'get',
    params: query
  })
}
