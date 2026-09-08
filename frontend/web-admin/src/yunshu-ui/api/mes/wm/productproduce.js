import request from '@/yunshu-ui/api/request'

// 查询产品产出记录列表
export function listProductproduce(query) {
  return request({
    url: '/mes/wm/productproduce/list',
    method: 'get',
    params: query
  })
}

// 查询产品产出记录详细
export function getProductproduce(recordId) {
  return request({
    url: '/mes/wm/productproduce/' + recordId,
    method: 'get'
  })
}

// 新增产品产出记录
export function addProductproduce(data) {
  return request({
    url: '/mes/wm/productproduce',
    method: 'post',
    data: data
  })
}

// 修改产品产出记录
export function updateProductproduce(data) {
  return request({
    url: '/mes/wm/productproduce',
    method: 'put',
    data: data
  })
}

// 删除产品产出记录
export function delProductproduce(recordId) {
  return request({
    url: '/mes/wm/productproduce/' + recordId,
    method: 'delete'
  })
}
