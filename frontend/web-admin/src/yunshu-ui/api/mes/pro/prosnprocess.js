import request from '@/yunshu-ui/api/request'

// 查询SN流转-工序信息列表
export function listProsnprocess(query) {
  return request({
    url: '/mes/pro/prosnprocess/list',
    method: 'get',
    params: query
  })
}

// 查询SN流转-工序信息详细
export function getProsnprocess(recordId) {
  return request({
    url: '/mes/pro/prosnprocess/' + recordId,
    method: 'get'
  })
}

// 新增SN流转-工序信息
export function addProsnprocess(data) {
  return request({
    url: '/mes/pro/prosnprocess',
    method: 'post',
    data: data
  })
}

// 修改SN流转-工序信息
export function updateProsnprocess(data) {
  return request({
    url: '/mes/pro/prosnprocess',
    method: 'put',
    data: data
  })
}

// 删除SN流转-工序信息
export function delProsnprocess(recordId) {
  return request({
    url: '/mes/pro/prosnprocess/' + recordId,
    method: 'delete'
  })
}
