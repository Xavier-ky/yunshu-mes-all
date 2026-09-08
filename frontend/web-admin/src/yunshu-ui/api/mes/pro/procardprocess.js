import request from '@/yunshu-ui/api/request'

// 查询工序流转卡-工序信息列表
export function listProcardprocess(query) {
  return request({
    url: '/mes/pro/procardprocess/list',
    method: 'get',
    params: query
  })
}

// 查询工序流转卡-工序信息详细
export function getProcardprocess(recordId) {
  return request({
    url: '/mes/pro/procardprocess/' + recordId,
    method: 'get'
  })
}

// 新增工序流转卡-工序信息
export function addProcardprocess(data) {
  return request({
    url: '/mes/pro/procardprocess',
    method: 'post',
    data: data
  })
}

// 修改工序流转卡-工序信息
export function updateProcardprocess(data) {
  return request({
    url: '/mes/pro/procardprocess',
    method: 'put',
    data: data
  })
}

// 删除工序流转卡-工序信息
export function delProcardprocess(recordId) {
  return request({
    url: '/mes/pro/procardprocess/' + recordId,
    method: 'delete'
  })
}
