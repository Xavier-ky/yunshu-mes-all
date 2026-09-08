import request from '@/yunshu-ui/api/request'

// 查询装箱明细列表
export function listPackageline(query) {
  return request({
    url: '/mes/wm/packageline/list',
    method: 'get',
    params: query
  })
}

// 查询装箱明细详细
export function getPackageline(lineId) {
  return request({
    url: '/mes/wm/packageline/' + lineId,
    method: 'get'
  })
}

// 新增装箱明细
export function addPackageline(data) {
  return request({
    url: '/mes/wm/packageline',
    method: 'post',
    data: data
  })
}

// 修改装箱明细
export function updatePackageline(data) {
  return request({
    url: '/mes/wm/packageline',
    method: 'put',
    data: data
  })
}

// 删除装箱明细
export function delPackageline(lineId) {
  return request({
    url: '/mes/wm/packageline/' + lineId,
    method: 'delete'
  })
}
