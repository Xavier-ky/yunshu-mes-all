import request from '@/yunshu-ui/api/request'

export function listCalholiday(query) {
  return request({
    url: '/mes/cal/calholiday/list',
    method: 'get',
    params: query
  })
}

export function getCalholiday(holidayId) {
  return request({
    url: '/mes/cal/calholiday/' + holidayId,
    method: 'get'
  })
}

export function addCalholiday(data) {
  return request({
    url: '/mes/cal/calholiday',
    method: 'post',
    data
  })
}

export function updateCalholiday(data) {
  return request({
    url: '/mes/cal/calholiday',
    method: 'put',
    data
  })
}

export function delCalholiday(holidayId) {
  return request({
    url: '/mes/cal/calholiday/' + holidayId,
    method: 'delete'
  })
}
