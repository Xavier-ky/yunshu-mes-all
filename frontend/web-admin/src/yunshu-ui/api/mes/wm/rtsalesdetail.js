import request from '@/yunshu-ui/api/request'

// 查询销售退货记录明细列表
export function listRtsalesdetail(query) {
  return request({
    url: '/mes/wm/rtsalesdetail/list',
    method: 'get',
    params: query
  })
}

// 查询销售退货记录明细详细
export function getRtsalesdetail(detailId) {
  return request({
    url: '/mes/wm/rtsalesdetail/' + detailId,
    method: 'get'
  })
}

// 新增销售退货记录明细
export function addRtsalesdetail(data) {
  return request({
    url: '/mes/wm/rtsalesdetail',
    method: 'post',
    data: data
  })
}

// 修改销售退货记录明细
export function updateRtsalesdetail(data) {
  return request({
    url: '/mes/wm/rtsalesdetail',
    method: 'put',
    data: data
  })
}

// 删除销售退货记录明细
export function delRtsalesdetail(detailId) {
  return request({
    url: '/mes/wm/rtsalesdetail/' + detailId,
    method: 'delete'
  })
}
