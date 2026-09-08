import request from '@/yunshu-ui/api/request'

// 查询设备点检记录行列表
export function listCheckrecordline(query) {
  return request({
    url: '/mes/dv/checkrecordline/list',
    method: 'get',
    params: query
  })
}

// 查询设备点检记录行详细
export function getCheckrecordline(lineId) {
  return request({
    url: '/mes/dv/checkrecordline/' + lineId,
    method: 'get'
  })
}

// 新增设备点检记录行
export function addCheckrecordline(data) {
  return request({
    url: '/mes/dv/checkrecordline',
    method: 'post',
    data: data
  })
}

// 修改设备点检记录行
export function updateCheckrecordline(data) {
  return request({
    url: '/mes/dv/checkrecordline',
    method: 'put',
    data: data
  })
}

// 删除设备点检记录行
export function delCheckrecordline(lineId) {
  return request({
    url: '/mes/dv/checkrecordline/' + lineId,
    method: 'delete'
  })
}
