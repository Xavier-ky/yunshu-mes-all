import request from '@/yunshu-ui/api/request'

export function listTeammember(query) {
  return request({
    url: '/mes/cal/teammember/list',
    method: 'get',
    params: query
  })
}

export function getTeammember(memberId) {
  return request({
    url: '/mes/cal/teammember/' + memberId,
    method: 'get'
  })
}

export function addTeammember(data) {
  return request({
    url: '/mes/cal/teammember',
    method: 'post',
    data
  })
}

export function delTeammember(memberId) {
  return request({
    url: '/mes/cal/teammember/' + memberId,
    method: 'delete'
  })
}

export function getListByTeamId(ids) {
  return request({
    url: '/mes/cal/teammember/getListByTeamId',
    method: 'get',
    params: { ids }
  })
}
