import request from '@/yunshu-ui/api/request'

// 查询退料检验单列表
export function listRqc(query) {
  return request({
    url: '/mes/qc/rqc/list',
    method: 'get',
    params: query
  })
}

// 查询退料检验单详细
export function getRqc(rqcId) {
  return request({
    url: '/mes/qc/rqc/' + rqcId,
    method: 'get'
  })
}

// 新增退料检验单
export function addRqc(data) {
  return request({
    url: '/mes/qc/rqc',
    method: 'post',
    data: data
  })
}

// 修改退料检验单
export function updateRqc(data) {
  return request({
    url: '/mes/qc/rqc',
    method: 'put',
    data: data
  })
}

// 删除退料检验单
export function delRqc(rqcId) {
  return request({
    url: '/mes/qc/rqc/' + rqcId,
    method: 'delete'
  })
}
