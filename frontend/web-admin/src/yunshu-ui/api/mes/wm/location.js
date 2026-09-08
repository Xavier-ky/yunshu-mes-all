import request from '@/yunshu-ui/api/request'

export function listLocation(query) {
  return request({
    url: '/mes/wm/location/list',
    method: 'get',
    params: query
  })
}

export function getLocation(locationId) {
  return request({
    url: '/mes/wm/location/' + locationId,
    method: 'get'
  })
}

export function addLocation(data) {
  return request({
    url: '/mes/wm/location',
    method: 'post',
    data: data
  })
}

export function updateLocation(data) {
  return request({
    url: '/mes/wm/location',
    method: 'put',
    data: data
  })
}

export function delLocation(locationId) {
  return request({
    url: '/mes/wm/location/' + locationId,
    method: 'delete'
  })
}

export function setProductMixing(locationId, flag) {
  return request({
    url: '/mes/wm/location/setProductMixing',
    method: 'post',
    params: { locationId, flag }
  })
}

export function setBatchMixing(locationId, flag) {
  return request({
    url: '/mes/wm/location/setBatchMixing',
    method: 'post',
    params: { locationId, flag }
  })
}

export function changeFrozenState(locationId, status) {
  const data = { locationId, frozenFlag: status }
  return request({
    url: '/mes/wm/location',
    method: 'put',
    data: data
  })
}
