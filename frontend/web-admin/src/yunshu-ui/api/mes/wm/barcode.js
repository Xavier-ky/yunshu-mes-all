import request from '@/yunshu-ui/api/request'

// 查询条码清单列表
export function listBarcode(query) {
  return request({
    url: '/mes/wm/barcode/list',
    method: 'get',
    params: query
  })
}

// 查询条码清单详细
export function getBarcode(barcodeId) {
  return request({
    url: '/mes/wm/barcode/' + barcodeId,
    method: 'get'
  })
}

// 新增条码清单
export function addBarcode(data) {
  return request({
    url: '/mes/wm/barcode',
    method: 'post',
    data: data
  })
}

// 修改条码清单
export function updateBarcode(data) {
  return request({
    url: '/mes/wm/barcode',
    method: 'put',
    data: data
  })
}

// 删除条码清单
export function delBarcode(barcodeId) {
  return request({
    url: '/mes/wm/barcode/' + barcodeId,
    method: 'delete'
  })
}

//获取某个对象的二维码信息
export function getBarcodeUrl(query){
  return request({
    url: '/mes/wm/barcode/getBarcodeUrl',
    method: 'get',
    params: query
  })
}

