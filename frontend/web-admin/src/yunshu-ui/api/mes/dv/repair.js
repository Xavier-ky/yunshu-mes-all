import request from '@/yunshu-ui/api/request'

// 查询设备维修单列表
export function listRepair(query) {
  return request({
    url: '/mes/dv/repair/list',
    method: 'get',
    params: query
  })
}

// 查询设备维修单详细
export function getRepair(repairId) {
  return request({
    url: '/mes/dv/repair/' + repairId,
    method: 'get'
  })
}

// 新增设备维修单
export function addRepair(data) {
  return request({
    url: '/mes/dv/repair',
    method: 'post',
    data: data
  })
}

// 修改设备维修单
export function updateRepair(data) {
  return request({
    url: '/mes/dv/repair',
    method: 'put',
    data: data
  })
}

// 删除设备维修单
export function delRepair(repairId) {
  return request({
    url: '/mes/dv/repair/' + repairId,
    method: 'delete'
  })
}

// 根据设备编码查询设备维修单列表
export function getRepairList(query) {
  return request({
    url: '/mes/dv/repair/getRepairList',
    method: 'get',
    params: query
  })
}
