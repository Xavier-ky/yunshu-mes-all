import request from '@/yunshu-ui/api/request'

// 查询物料批次属性配置列表
export function listBatchconfig(query) {
  return request({
    url: '/mes/md/batchconfig/list',
    method: 'get',
    params: query
  })
}

// 查询物料批次属性配置详细
export function getBatchconfig(itemId) {
  return request({
    url: '/mes/md/batchconfig/' + itemId,
    method: 'get'
  })
}

// 新增物料批次属性配置
export function addBatchconfig(data) {
  return request({
    url: '/mes/md/batchconfig',
    method: 'post',
    data: data
  })
}

// 修改物料批次属性配置
export function updateBatchconfig(data) {
  return request({
    url: '/mes/md/batchconfig',
    method: 'put',
    data: data
  })
}

// 删除物料批次属性配置
export function delBatchconfig(configId) {
  return request({
    url: '/mes/md/batchconfig/' + configId,
    method: 'delete'
  })
}
