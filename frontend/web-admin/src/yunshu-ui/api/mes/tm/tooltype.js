import request from '@/yunshu-ui/api/request'

export function listTooltype(query) {
  return request({
    url: '/mes/tm/tooltype/list',
    method: 'get',
    params: query
  })
}

export function listAllTooltype() {
  return request({
    url: '/mes/tm/tooltype/listAll',
    method: 'get'
  })
}

export function getTooltype(toolTypeId) {
  return request({
    url: '/mes/tm/tooltype/' + toolTypeId,
    method: 'get'
  })
}

export function addTooltype(data) {
  return request({
    url: '/mes/tm/tooltype',
    method: 'post',
    data
  })
}

export function updateTooltype(data) {
  return request({
    url: '/mes/tm/tooltype',
    method: 'put',
    data
  })
}

export function delTooltype(toolTypeId) {
  return request({
    url: '/mes/tm/tooltype/' + toolTypeId,
    method: 'delete'
  })
}
