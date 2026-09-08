import request from '@/yunshu-ui/api/request'

// 查询采购退货单明细列表
export function listRtvendordetail(query) {
  return request({
    url: '/mes/wm/rtvendordetail/list',
    method: 'get',
    params: query
  })
}

// 查询采购退货单明细详细
export function getRtvendordetail(detailId) {
  return request({
    url: '/mes/wm/rtvendordetail/' + detailId,
    method: 'get'
  })
}

// 新增采购退货单明细
export function addRtvendordetail(data) {
  return request({
    url: '/mes/wm/rtvendordetail',
    method: 'post',
    data: data
  })
}

// 修改采购退货单明细
export function updateRtvendordetail(data) {
  return request({
    url: '/mes/wm/rtvendordetail',
    method: 'put',
    data: data
  })
}

// 删除采购退货单明细
export function delRtvendordetail(detailId) {
  return request({
    url: '/mes/wm/rtvendordetail/' + detailId,
    method: 'delete'
  })
}
