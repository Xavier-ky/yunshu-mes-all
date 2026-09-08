import request from '@/yunshu-ui/api/request'

// 查询销售出库单列表
export function listProductsales(query) {
  return request({
    url: '/mes/wm/productsales/list',
    method: 'get',
    params: query
  })
}

// 查询销售出库单详细
export function getProductsales(salesId) {
  return request({
    url: '/mes/wm/productsales/' + salesId,
    method: 'get'
  })
}

/**
 * 检查出库数量与拣货数量是否一致
 * @param  salesId 
 * @returns 
 */
export function checkQuantity(salesId) {
  return request({
    url: '/mes/wm/productsales/checkQuantity/' + salesId,
    method: 'get'
  })
}


// 新增销售出库单
export function addProductsales(data) {
  return request({
    url: '/mes/wm/productsales',
    method: 'post',
    data: data
  })
}

// 修改销售出库单
export function updateProductsales(data) {
  return request({
    url: '/mes/wm/productsales',
    method: 'put',
    data: data
  })
}

// 删除销售出库单
export function delProductsales(salesId) {
  return request({
    url: '/mes/wm/productsales/' + salesId,
    method: 'delete'
  })
}

//执行出库
export function execute(salesId) {
  return request({
    url: '/mes/wm/productsales/' + salesId,
    method: 'put'
  })
}

// 根据客户 id 查询销售产品信息
export function getItem(clientId) {
  return request({
    url: '/mes/wm/productsales/getItem/' + clientId,
    method: 'get'
  })
}

// 根据客户 id 查询销售订单信息
export function getSaleRecord(clientId) {
  return request({
    url: '/mes/wm/productsales/getSaleRecord/' + clientId,
    method: 'get'
  })
}
