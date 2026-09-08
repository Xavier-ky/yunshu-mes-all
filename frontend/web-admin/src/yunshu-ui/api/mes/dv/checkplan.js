import request from '@/yunshu-ui/api/request'

// 查询设备点检计划头列表
export function listCheckplan(query) {
  return request({
    url: '/mes/dv/checkplan/list',
    method: 'get',
    params: query
  })
}

// 查询设备点检计划头详细
export function getCheckplan(planId) {
  return request({
    url: '/mes/dv/checkplan/' + planId,
    method: 'get'
  })
}

// 新增设备点检计划头
export function addCheckplan(data) {
  return request({
    url: '/mes/dv/checkplan',
    method: 'post',
    data: data
  })
}

// 修改设备点检计划头
export function updateCheckplan(data) {
  return request({
    url: '/mes/dv/checkplan',
    method: 'put',
    data: data
  })
}

// 删除设备点检计划头
export function delCheckplan(planId) {
  return request({
    url: '/mes/dv/checkplan/' + planId,
    method: 'delete'
  })
}

// 根据设备编码和计划类型查询设备点检计划头列表
export function getCheckPlan(query) {
  return request({
    url: '/mes/dv/checkplan/getCheckPlan',
    method: 'get',
    params: query
  })
}
