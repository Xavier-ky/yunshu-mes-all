import request from '@/yunshu-ui/api/request'

// 查询物料消耗记录列表
export function listItemconsume(query) {
  return request({
    url: '/mes/wm/itemconsume/list',
    method: 'get',
    params: query
  })
}

// 查询物料消耗记录详细
export function getItemconsume(recordId) {
  return request({
    url: '/mes/wm/itemconsume/' + recordId,
    method: 'get'
  })
}

// 新增物料消耗记录
export function addItemconsume(data) {
  return request({
    url: '/mes/wm/itemconsume',
    method: 'post',
    data: data
  })
}

// 修改物料消耗记录
export function updateItemconsume(data) {
  return request({
    url: '/mes/wm/itemconsume',
    method: 'put',
    data: data
  })
}

// 删除物料消耗记录
export function delItemconsume(recordId) {
  return request({
    url: '/mes/wm/itemconsume/' + recordId,
    method: 'delete'
  })
}
