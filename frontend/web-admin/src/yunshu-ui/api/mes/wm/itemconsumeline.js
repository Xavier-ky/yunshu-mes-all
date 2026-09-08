import request from '@/yunshu-ui/api/request'

// 查询物料消耗记录行列表
export function listItemconsumeline(query) {
  return request({
    url: '/mes/wm/itemconsumeline/list',
    method: 'get',
    params: query
  })
}

// 查询物料消耗记录行详细
export function getItemconsumeline(lineId) {
  return request({
    url: '/mes/wm/itemconsumeline/' + lineId,
    method: 'get'
  })
}

// 新增物料消耗记录行
export function addItemconsumeline(data) {
  return request({
    url: '/mes/wm/itemconsumeline',
    method: 'post',
    data: data
  })
}

// 修改物料消耗记录行
export function updateItemconsumeline(data) {
  return request({
    url: '/mes/wm/itemconsumeline',
    method: 'put',
    data: data
  })
}

// 删除物料消耗记录行
export function delItemconsumeline(lineId) {
  return request({
    url: '/mes/wm/itemconsumeline/' + lineId,
    method: 'delete'
  })
}
