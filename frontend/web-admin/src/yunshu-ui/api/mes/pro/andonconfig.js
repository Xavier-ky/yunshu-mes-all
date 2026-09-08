import request from '@/yunshu-ui/api/request'

// 查询安灯呼叫配置列表
export function listAndonconfig(query) {
  return request({
    url: '/mes/pro/andonconfig/list',
    method: 'get',
    params: query
  })
}

// 查询安灯呼叫配置详细
export function getAndonconfig(configId) {
  return request({
    url: '/mes/pro/andonconfig/' + configId,
    method: 'get'
  })
}

// 新增安灯呼叫配置
export function addAndonconfig(data) {
  return request({
    url: '/mes/pro/andonconfig',
    method: 'post',
    data: data
  })
}

// 修改安灯呼叫配置
export function updateAndonconfig(data) {
  return request({
    url: '/mes/pro/andonconfig',
    method: 'put',
    data: data
  })
}

// 删除安灯呼叫配置
export function delAndonconfig(configId) {
  return request({
    url: '/mes/pro/andonconfig/' + configId,
    method: 'delete'
  })
}
