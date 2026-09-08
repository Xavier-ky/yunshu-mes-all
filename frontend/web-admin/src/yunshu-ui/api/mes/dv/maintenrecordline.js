import request from '@/yunshu-ui/api/request'

// 查询设备保养记录行列表
export function listMaintenrecordline(query) {
  return request({
    url: '/mes/dv/maintenrecordline/list',
    method: 'get',
    params: query
  })
}

// 查询设备保养记录行详细
export function getMaintenrecordline(lineId) {
  return request({
    url: '/mes/dv/maintenrecordline/' + lineId,
    method: 'get'
  })
}

// 新增设备保养记录行
export function addMaintenrecordline(data) {
  return request({
    url: '/mes/dv/maintenrecordline',
    method: 'post',
    data: data
  })
}

// 修改设备保养记录行
export function updateMaintenrecordline(data) {
  return request({
    url: '/mes/dv/maintenrecordline',
    method: 'put',
    data: data
  })
}

// 删除设备保养记录行
export function delMaintenrecordline(lineId) {
  return request({
    url: '/mes/dv/maintenrecordline/' + lineId,
    method: 'delete'
  })
}
