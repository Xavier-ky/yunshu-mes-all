import request from '@/yunshu-ui/api/request'

// 查询产品产出记录行列表
export function listProductproduceline(query) {
  return request({
    url: '/mes/wm/productproduceline/list',
    method: 'get',
    params: query
  })
}


// 查询产品产出记录行详细
export function getProductproduceline(lineId) {
  return request({
    url: '/mes/wm/productproduceline/' + lineId,
    method: 'get'
  })
}

// 新增产品产出记录行
export function addProductproduceline(data) {
  return request({
    url: '/mes/wm/productproduceline',
    method: 'post',
    data: data
  })
}

// 修改产品产出记录行
export function updateProductproduceline(data) {
  return request({
    url: '/mes/wm/productproduceline',
    method: 'put',
    data: data
  })
}

// 删除产品产出记录行
export function delProductproduceline(lineId) {
  return request({
    url: '/mes/wm/productproduceline/' + lineId,
    method: 'delete'
  })
}
