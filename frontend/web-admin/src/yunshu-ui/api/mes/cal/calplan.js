import request from '@/yunshu-ui/api/request'

export function listCalplan(query) {
  return request({
    url: '/mes/cal/calplan/list',
    method: 'get',
    params: query
  })
}

export function getCalplan(planId) {
  return request({
    url: '/mes/cal/calplan/' + planId,
    method: 'get'
  })
}

export function addCalplan(data) {
  return request({
    url: '/mes/cal/calplan',
    method: 'post',
    data
  })
}

export function updateCalplan(data) {
  return request({
    url: '/mes/cal/calplan',
    method: 'put',
    data
  })
}

export function delCalplan(planId) {
  return request({
    url: '/mes/cal/calplan/' + planId,
    method: 'delete'
  })
}
