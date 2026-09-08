import request from '@/yunshu-ui/api/request'

// 查询出货检验单列表
export function listOqc(query) {
  return request({
    url: '/mes/qc/oqc/list',
    method: 'get',
    params: query
  })
}

// 查询出货检验单详细
export function getOqc(oqcId) {
  return request({
    url: '/mes/qc/oqc/' + oqcId,
    method: 'get'
  })
}

// 新增出货检验单
export function addOqc(data) {
  return request({
    url: '/mes/qc/oqc',
    method: 'post',
    data: data
  })
}

// 修改出货检验单
export function updateOqc(data) {
  return request({
    url: '/mes/qc/oqc',
    method: 'put',
    data: data
  })
}

// 删除出货检验单
export function delOqc(oqcId) {
  return request({
    url: '/mes/qc/oqc/' + oqcId,
    method: 'delete'
  })
}
