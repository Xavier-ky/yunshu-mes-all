import request from '@/yunshu-ui/api/request'

// 查询生产退料单行列表
export function listRtissueline(query) {
  return request({
    url: '/mes/wm/rtissueline/list',
    method: 'get',
    params: query
  })
}


// 查询生产退料单行列表(含明细)
export function listWithDetail(query) {
  return request({
    url: '/mes/wm/rtissueline/listWithDetail',
    method: 'get',
    params: query
  })
}


// 查询生产退料单行详细
export function getRtissueline(lineId) {
  return request({
    url: '/mes/wm/rtissueline/' + lineId,
    method: 'get'
  })
}

// 新增生产退料单行
export function addRtissueline(data) {
  return request({
    url: '/mes/wm/rtissueline',
    method: 'post',
    data: data
  })
}

// 修改生产退料单行
export function updateRtissueline(data) {
  return request({
    url: '/mes/wm/rtissueline',
    method: 'put',
    data: data
  })
}

// 删除生产退料单行
export function delRtissueline(lineId) {
  return request({
    url: '/mes/wm/rtissueline/' + lineId,
    method: 'delete'
  })
}
