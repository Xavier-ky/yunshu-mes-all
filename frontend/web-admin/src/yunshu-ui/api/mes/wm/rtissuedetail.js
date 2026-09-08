import request from '@/yunshu-ui/api/request'

// 查询生产退料单明细列表
export function listRtissuedetail(query) {
  return request({
    url: '/mes/wm/rtissuedetail/list',
    method: 'get',
    params: query
  })
}

// 查询生产退料单明细详细
export function getRtissuedetail(detailId) {
  return request({
    url: '/mes/wm/rtissuedetail/' + detailId,
    method: 'get'
  })
}

// 新增生产退料单明细
export function addRtissuedetail(data) {
  return request({
    url: '/mes/wm/rtissuedetail',
    method: 'post',
    data: data
  })
}

// 修改生产退料单明细
export function updateRtissuedetail(data) {
  return request({
    url: '/mes/wm/rtissuedetail',
    method: 'put',
    data: data
  })
}

// 删除生产退料单明细
export function delRtissuedetail(detailId) {
  return request({
    url: '/mes/wm/rtissuedetail/' + detailId,
    method: 'delete'
  })
}
