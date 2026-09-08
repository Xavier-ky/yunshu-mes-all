import request from '@/yunshu-ui/api/request'

// 查询条码配置列表
export function listBarcodeconfig(query) {
  return request({
    url: '/mes/wm/barcodeconfig/list',
    method: 'get',
    params: query
  })
}

// 查询条码配置详细
export function getBarcodeconfig(configId) {
  return request({
    url: '/mes/wm/barcodeconfig/' + configId,
    method: 'get'
  })
}

// 新增条码配置
export function addBarcodeconfig(data) {
  return request({
    url: '/mes/wm/barcodeconfig',
    method: 'post',
    data: data
  })
}

// 修改条码配置
export function updateBarcodeconfig(data) {
  return request({
    url: '/mes/wm/barcodeconfig',
    method: 'put',
    data: data
  })
}

// 删除条码配置
export function delBarcodeconfig(configId) {
  return request({
    url: '/mes/wm/barcodeconfig/' + configId,
    method: 'delete'
  })
}
