import request from '@/yunshu-ui/api/request'

// 查询退料检验单行列表
export function listRqcline(query) {
  return request({
    url: '/mes/qc/rqcline/list',
    method: 'get',
    params: query
  })
}

// 查询退料检验单行详细
export function getRqcline(lineId) {
  return request({
    url: '/mes/qc/rqcline/' + lineId,
    method: 'get'
  })
}

// 新增退料检验单行
export function addRqcline(data) {
  return request({
    url: '/mes/qc/rqcline',
    method: 'post',
    data: data
  })
}

// 修改退料检验单行
export function updateRqcline(data) {
  return request({
    url: '/mes/qc/rqcline',
    method: 'put',
    data: data
  })
}

// 删除退料检验单行
export function delRqcline(lineId) {
  return request({
    url: '/mes/qc/rqcline/' + lineId,
    method: 'delete'
  })
}
