import request from '@/yunshu-ui/api/request'

// 查询产品Sip列表
export function listSip(query) {
  return request({
    url: '/mes/md/sip/list',
    method: 'get',
    params: query
  })
}

// 查询产品Sip详细
export function getSip(sipId) {
  return request({
    url: '/mes/md/sip/' + sipId,
    method: 'get'
  })
}

// 新增产品Sip
export function addSip(data) {
  return request({
    url: '/mes/md/sip',
    method: 'post',
    data: data
  })
}

// 修改产品Sip
export function updateSip(data) {
  return request({
    url: '/mes/md/sip',
    method: 'put',
    data: data
  })
}

// 删除产品Sip
export function delSip(sipId) {
  return request({
    url: '/mes/md/sip/' + sipId,
    method: 'delete'
  })
}
