import request from '@/yunshu-ui/api/request'

// 查询产品入库记录行列表
export function listProductrecptline(query) {
  return request({
    url: '/mes/wm/productrecptline/list',
    method: 'get',
    params: query
  })
}

export  function listWithDetail(query){
  return request({
    url: '/mes/wm/productrecptline/listWithDetail',
    method: 'get',
    params: query
  })
}

// 查询产品入库记录行详细
export function getProductrecptline(lineId) {
  return request({
    url: '/mes/wm/productrecptline/' + lineId,
    method: 'get'
  })
}

// 新增产品入库记录行
export function addProductrecptline(data) {
  return request({
    url: '/mes/wm/productrecptline',
    method: 'post',
    data: data
  })
}

// 修改产品入库记录行
export function updateProductrecptline(data) {
  return request({
    url: '/mes/wm/productrecptline',
    method: 'put',
    data: data
  })
}

// 删除产品入库记录行
export function delProductrecptline(lineId) {
  return request({
    url: '/mes/wm/productrecptline/' + lineId,
    method: 'delete'
  })
}
