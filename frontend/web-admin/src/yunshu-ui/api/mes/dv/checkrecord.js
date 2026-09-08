import request from '@/yunshu-ui/api/request'

// 查询设备点检记录列表
export function listCheckrecord(query) {
  return request({
    url: '/mes/dv/checkrecord/list',
    method: 'get',
    params: query
  })
}

// 查询设备点检记录详细
export function getCheckrecord(recordId) {
  return request({
    url: '/mes/dv/checkrecord/' + recordId,
    method: 'get'
  })
}

// 新增设备点检记录
export function addCheckrecord(data) {
  return request({
    url: '/mes/dv/checkrecord',
    method: 'post',
    data: data
  })
}

// 修改设备点检记录
export function updateCheckrecord(data) {
  return request({
    url: '/mes/dv/checkrecord',
    method: 'put',
    data: data
  })
}

// 删除设备点检记录
export function delCheckrecord(recordId) {
  return request({
    url: '/mes/dv/checkrecord/' + recordId,
    method: 'delete'
  })
}
