import request from '@/yunshu-ui/api/request'

// 查询检测结果记录列表
export function listQcresult(query) {
  return request({
    url: '/mes/qc/qcresult/list',
    method: 'get',
    params: query
  })
}

// 查询检测结果记录详细
export function getQcresult(resultId) {
  return request({
    url: '/mes/qc/qcresult/' + resultId,
    method: 'get'
  })
}

// 新增检测结果记录
export function addQcresult(data) {
  return request({
    url: '/mes/qc/qcresult',
    method: 'post',
    data: data
  })
}

// 修改检测结果记录
export function updateQcresult(data) {
  return request({
    url: '/mes/qc/qcresult',
    method: 'put',
    data: data
  })
}

// 删除检测结果记录
export function delQcresult(resultId) {
  return request({
    url: '/mes/qc/qcresult/' + resultId,
    method: 'delete'
  })
}
