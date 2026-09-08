import request from '@/yunshu-ui/api/request'

// 查询设备维修单行列表
export function listRepairline(query) {
  return request({
    url: '/mes/dv/repairline/list',
    method: 'get',
    params: query
  })
}

// 查询设备维修单行详细
export function getRepairline(lineId) {
  return request({
    url: '/mes/dv/repairline/' + lineId,
    method: 'get'
  })
}

// 新增设备维修单行
export function addRepairline(data) {
  return request({
    url: '/mes/dv/repairline',
    method: 'post',
    data: data
  })
}

// 修改设备维修单行
export function updateRepairline(data) {
  return request({
    url: '/mes/dv/repairline',
    method: 'put',
    data: data
  })
}

// 删除设备维修单行
export function delRepairline(lineId) {
  return request({
    url: '/mes/dv/repairline/' + lineId,
    method: 'delete'
  })
}
