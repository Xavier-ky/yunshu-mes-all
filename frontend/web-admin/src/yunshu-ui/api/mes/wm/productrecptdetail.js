import request from '@/yunshu-ui/api/request'

// 查询产品入库记录明细列表
export function listProductrecptdetail(query) {
  return request({
    url: '/mes/wm/productrecptdetail/list',
    method: 'get',
    params: query
  })
}

// 查询产品入库记录明细详细
export function getProductrecptdetail(detailId) {
  return request({
    url: '/mes/wm/productrecptdetail/' + detailId,
    method: 'get'
  })
}

// 新增产品入库记录明细
export function addProductrecptdetail(data) {
  return request({
    url: '/mes/wm/productrecptdetail',
    method: 'post',
    data: data
  })
}

// 修改产品入库记录明细
export function updateProductrecptdetail(data) {
  return request({
    url: '/mes/wm/productrecptdetail',
    method: 'put',
    data: data
  })
}

// 删除产品入库记录明细
export function delProductrecptdetail(detailId) {
  return request({
    url: '/mes/wm/productrecptdetail/' + detailId,
    method: 'delete'
  })
}
