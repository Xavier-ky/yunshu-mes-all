import request from '@/yunshu-ui/api/request'

export function listTeam(query) {
  return request({
    url: '/mes/cal/team/list',
    method: 'get',
    params: query
  })
}

export function listAllTeam() {
  return request({
    url: '/mes/cal/team/listAll',
    method: 'get'
  })
}

export function getTeam(teamId) {
  return request({
    url: '/mes/cal/team/' + teamId,
    method: 'get'
  })
}

export function addTeam(data) {
  return request({
    url: '/mes/cal/team',
    method: 'post',
    data
  })
}

export function updateTeam(data) {
  return request({
    url: '/mes/cal/team',
    method: 'put',
    data
  })
}

export function delTeam(teamId) {
  return request({
    url: '/mes/cal/team/' + teamId,
    method: 'delete'
  })
}
