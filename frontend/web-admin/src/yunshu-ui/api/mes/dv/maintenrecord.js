import request from '@/yunshu-ui/api/request'

// 查询设备保养记录列表
export function listMaintenrecord(query) {
  return request({
    url: '/mes/dv/maintenrecord/list',
    method: 'get',
    params: query
  })
}

// 查询设备保养记录详细
export function getMaintenrecord(recordId) {
  return request({
    url: '/mes/dv/maintenrecord/' + recordId,
    method: 'get'
  })
}

// 新增设备保养记录
export function addMaintenrecord(data) {
  return request({
    url: '/mes/dv/maintenrecord',
    method: 'post',
    data: data
  })
}

// 修改设备保养记录
export function updateMaintenrecord(data) {
  return request({
    url: '/mes/dv/maintenrecord',
    method: 'put',
    data: data
  })
}

// 删除设备保养记录
export function delMaintenrecord(recordId) {
  return request({
    url: '/mes/dv/maintenrecord/' + recordId,
    method: 'delete'
  })
}
