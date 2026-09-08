import request from '@/yunshu-ui/api/request'

// 查询生产工单列表
export function listWorkorder(query) {
  return request({
    url: '/mes/pro/workorder/list',
    method: 'get',
    params: query
  })
}

// 查询生产工单详细
export function getWorkorder(workorderId) {
  return request({
    url: '/mes/pro/workorder/' + workorderId,
    method: 'get'
  })
}

//查询生产工单，并携带每个生产工单下生产任务的情况
export function listWithTaskJson(query) {
  return request({
    url: '/mes/pro/workorder/listWithTaskJson',
    method: 'get',
    params: query
  })
}

// 新增生产工单
export function addWorkorder(data) {
  return request({
    url: '/mes/pro/workorder',
    method: 'post',
    data: data
  })
}

// 修改生产工单
export function updateWorkorder(data) {
  return request({
    url: '/mes/pro/workorder',
    method: 'put',
    data: data
  })
}

// 删除生产工单
export function delWorkorder(workorderId) {
  return request({
    url: '/mes/pro/workorder/' + workorderId,
    method: 'delete'
  })
}

// 查询物料依赖列表
export function listItems(query) {
  return request({
    url: '/mes/pro/workorder/listItems',
    method: 'get',
    params: query
  })
}

export function dofinish(workorderId){
  return request({
    url: '/mes/pro/workorder/finish/'+workorderId,
    method: 'put'
  })
}

export function doCancel(workorderId){
  return request({
    url: '/mes/pro/workorder/cancel/'+workorderId,
    method: 'put'
  })
}


// 首页查询生产工单列表
export function getHomeList(query) {
  return request({
    url: '/mes/pro/workorder/getHomeList',
    method: 'get',
    params: query
  })
}
