import request from '@/yunshu-ui/api/request'

// 查询过程检验单列表
export function listIpqc(query) {
  return request({
    url: '/mes/qc/ipqc/list',
    method: 'get',
    params: query
  })
}

// 查询过程检验单详细
export function getIpqc(ipqcId) {
  return request({
    url: '/mes/qc/ipqc/' + ipqcId,
    method: 'get'
  })
}

// 新增过程检验单
export function addIpqc(data) {
  return request({
    url: '/mes/qc/ipqc',
    method: 'post',
    data: data
  })
}

// 修改过程检验单
export function updateIpqc(data) {
  return request({
    url: '/mes/qc/ipqc',
    method: 'put',
    data: data
  })
}

// 删除过程检验单
export function delIpqc(ipqcId) {
  return request({
    url: '/mes/qc/ipqc/' + ipqcId,
    method: 'delete'
  })
}
