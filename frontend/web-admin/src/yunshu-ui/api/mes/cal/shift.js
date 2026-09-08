import request from '@/yunshu-ui/api/request'

export function listShift(query) {
  return request({
    url: '/mes/cal/shift/list',
    method: 'get',
    params: query
  })
}

export function getShift(shiftId) {
  return request({
    url: '/mes/cal/shift/' + shiftId,
    method: 'get'
  })
}

export function addShift(data) {
  return request({
    url: '/mes/cal/shift',
    method: 'post',
    data
  })
}

export function updateShift(data) {
  return request({
    url: '/mes/cal/shift',
    method: 'put',
    data
  })
}

export function delShift(shiftId) {
  return request({
    url: '/mes/cal/shift/' + shiftId,
    method: 'delete'
  })
}
