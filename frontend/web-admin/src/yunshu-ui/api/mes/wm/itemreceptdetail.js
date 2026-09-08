import request from '@/yunshu-ui/api/request'

// 查询物料入库单明细列表
export function listItemreceptdetail(query) {
  return request({
    url: '/mes/wm/itemreceptdetail/list',
    method: 'get',
    params: query
  })
}

// 查询物料入库单明细详细
export function getItemreceptdetail(detailId) {
  return request({
    url: '/mes/wm/itemreceptdetail/' + detailId,
    method: 'get'
  })
}

// 新增物料入库单明细
export function addItemreceptdetail(data) {
  return request({
    url: '/mes/wm/itemreceptdetail',
    method: 'post',
    data: data
  })
}

// 修改物料入库单明细
export function updateItemreceptdetail(data) {
  return request({
    url: '/mes/wm/itemreceptdetail',
    method: 'put',
    data: data
  })
}

// 删除物料入库单明细
export function delItemreceptdetail(detailId) {
  return request({
    url: '/mes/wm/itemreceptdetail/' + detailId,
    method: 'delete'
  })
}
