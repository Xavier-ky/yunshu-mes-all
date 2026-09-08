import request from '@/yunshu-ui/api/request'

// 查询检测结果明细记录列表
export function listQcresultdetail(query) {
  return request({
    url: '/mes/qc/qcresultdetail/list',
    method: 'get',
    params: query
  })
}

export function listDetails(query) {
  return request({
    url: '/mes/qc/qcresultdetail/listDetails',
    method: 'get',
    params: query
  })
}

// 查询检测结果明细记录详细
export function getQcresultdetail(detailId) {
  return request({
    url: '/mes/qc/qcresultdetail/' + detailId,
    method: 'get'
  })
}

// 新增检测结果明细记录
export function addQcresultdetail(data) {
  return request({
    url: '/mes/qc/qcresultdetail',
    method: 'post',
    data: data
  })
}

// 修改检测结果明细记录
export function updateQcresultdetail(data) {
  return request({
    url: '/mes/qc/qcresultdetail',
    method: 'put',
    data: data
  })
}

// 删除检测结果明细记录
export function delQcresultdetail(detailId) {
  return request({
    url: '/mes/qc/qcresultdetail/' + detailId,
    method: 'delete'
  })
}
