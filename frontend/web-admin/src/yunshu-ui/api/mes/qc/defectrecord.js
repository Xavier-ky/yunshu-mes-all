import request from '@/yunshu-ui/api/request'

// 查询检验单缺陷记录列表
export function listDefectrecord(query) {
  return request({
    url: '/mes/qc/defectrecord/list',
    method: 'get',
    params: query
  })
}

// 查询检验单缺陷记录详细
export function getDefectrecord(recordId) {
  return request({
    url: '/mes/qc/defectrecord/' + recordId,
    method: 'get'
  })
}

// 新增检验单缺陷记录
export function addDefectrecord(data) {
  return request({
    url: '/mes/qc/defectrecord',
    method: 'post',
    data: data
  })
}

// 修改检验单缺陷记录
export function updateDefectrecord(data) {
  return request({
    url: '/mes/qc/defectrecord',
    method: 'put',
    data: data
  })
}

// 删除检验单缺陷记录
export function delDefectrecord(recordId) {
  return request({
    url: '/mes/qc/defectrecord/' + recordId,
    method: 'delete'
  })
}
