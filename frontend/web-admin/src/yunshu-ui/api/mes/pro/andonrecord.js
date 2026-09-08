import request from '@/yunshu-ui/api/request'

// 查询安灯呼叫记录列表
export function listAndonrecord(query) {
  return request({
    url: '/mes/pro/andonrecord/list',
    method: 'get',
    params: query
  })
}

// 查询安灯呼叫记录详细
export function getAndonrecord(recordId) {
  return request({
    url: '/mes/pro/andonrecord/' + recordId,
    method: 'get'
  })
}

// 新增安灯呼叫记录
export function addAndonrecord(data) {
  return request({
    url: '/mes/pro/andonrecord',
    method: 'post',
    data: data
  })
}

// 修改安灯呼叫记录
export function updateAndonrecord(data) {
  return request({
    url: '/mes/pro/andonrecord',
    method: 'put',
    data: data
  })
}

// 删除安灯呼叫记录
export function delAndonrecord(recordId) {
  return request({
    url: '/mes/pro/andonrecord/' + recordId,
    method: 'delete'
  })
}
