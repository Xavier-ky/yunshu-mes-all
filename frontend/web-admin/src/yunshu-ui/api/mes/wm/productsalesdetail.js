import request from '@/yunshu-ui/api/request'

// 查询产品销售出库记录明细列表
export function listProductsalesdetail(query) {
  return request({
    url: '/mes/wm/productsalesdetail/list',
    method: 'get',
    params: query
  })
}

// 查询产品销售出库记录明细详细
export function getProductsalesdetail(detailId) {
  return request({
    url: '/mes/wm/productsalesdetail/' + detailId,
    method: 'get'
  })
}

// 新增产品销售出库记录明细
export function addProductsalesdetail(data) {
  return request({
    url: '/mes/wm/productsalesdetail',
    method: 'post',
    data: data
  })
}

// 修改产品销售出库记录明细
export function updateProductsalesdetail(data) {
  return request({
    url: '/mes/wm/productsalesdetail',
    method: 'put',
    data: data
  })
}

// 删除产品销售出库记录明细
export function delProductsalesdetail(detailId) {
  return request({
    url: '/mes/wm/productsalesdetail/' + detailId,
    method: 'delete'
  })
}
