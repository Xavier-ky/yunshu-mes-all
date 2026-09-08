import request from '@/yunshu-ui/api/request'

// 查询打印机客户端配置列表
export function getClientList(query) {
  return request({
    url: '/print/client/list',
    method: 'get',
    params: query
  })
}

// 查询打印机客户端配置列表
export function getClientPage(query) {
  return request({
    url: '/print/client/page',
    method: 'get',
    params: query
  })
}

// 新增客户端配置
export function addClient(data) {
  return request({
    url: '/print/client/add',
    method: 'post',
    data: data
  })
}

// 查询客户端配置详细
export function getClient(clientId) {
  return request({
    url: '/print/client/' + clientId,
    method: 'get',
  })
}

// 修改客户端配置
export function updateClient(data) {
  return request({
    url: '/print/client/edit',
    method: 'post',
    data: data
  })
}

// 删除客户端配置
export function delClient(clientIds) {
  return request({
    url: '/print/client/remove/' + clientIds,
    method: 'get'
  })
}

// 获取所属车间和所属工作站数据
export function getWorkshopAndWorkstation() {
  return request({
    url: '/print/client/getWorkshopAndWorkstation',
    method: 'get'
  })
}

// 查询所有客户端配置列表
export function getAll() {
  return request({
    url: '/print/client/getAll',
    method: 'get'
  })
}
