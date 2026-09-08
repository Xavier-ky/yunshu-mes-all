import request from '@/yunshu-ui/api/request'

export function listPlanteam(query) {
  return request({
    url: '/mes/cal/planteam/list',
    method: 'get',
    params: query
  })
}

export function addPlanteam(data) {
  return request({
    url: '/mes/cal/planteam',
    method: 'post',
    data
  })
}

export function delPlanteam(recordId) {
  return request({
    url: '/mes/cal/planteam/' + recordId,
    method: 'delete'
  })
}
