import request from '@/yunshu-ui/api/request'

// 查询打印机配置列表
export function listPrinterconfig(query) {
  return request({
    url: '/print/printerconfig/list',
    method: 'get',
    params: query
  })
}

// 查询打印机配置详细
export function getPrinterconfig(printerId) {
  return request({
    url: '/print/printerconfig/' + printerId,
    method: 'get'
  })
}

// 新增打印机配置
export function addPrinterconfig(data) {
  return request({
    url: '/print/printerconfig',
    method: 'post',
    data: data
  })
}

// 修改打印机配置
export function updatePrinterconfig(data) {
  return request({
    url: '/print/printerconfig',
    method: 'put',
    data: data
  })
}

// 删除打印机配置
export function delPrinterconfig(printerId) {
  return request({
    url: '/print/printerconfig/' + printerId,
    method: 'delete'
  })
}

// 设置默认打印机
export function setDefaultPrinter(data) {
  return request({
    url: '/print/printerconfig/editDefaultPrint/',
    method: 'post',
    data: data
  })
}

// 查询默认打印机
export function getDefaultPrinter(clientId) {
  return request({
    url: '/print/printerconfig/getDefaultPrint/' + clientId,
    method: 'get'
  })
}
