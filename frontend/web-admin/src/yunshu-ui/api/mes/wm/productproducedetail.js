import request from '@/yunshu-ui/api/request'

// 查询产品产出记录明细列表
export function listProductproducedetail(query) {
  return request({
    url: '/mes/wm/productproducedetail/list',
    method: 'get',
    params: query
  })
}

// 查询产品产出记录明细详细
export function getProductproducedetail(detailId) {
  return request({
    url: '/mes/wm/productproducedetail/' + detailId,
    method: 'get'
  })
}

// 新增产品产出记录明细
export function addProductproducedetail(data) {
  return request({
    url: '/mes/wm/productproducedetail',
    method: 'post',
    data: data
  })
}

// 修改产品产出记录明细
export function updateProductproducedetail(data) {
  return request({
    url: '/mes/wm/productproducedetail',
    method: 'put',
    data: data
  })
}

// 删除产品产出记录明细
export function delProductproducedetail(detailId) {
  return request({
    url: '/mes/wm/productproducedetail/' + detailId,
    method: 'delete'
  })
}
