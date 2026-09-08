import request from '@/yunshu-ui/api/request'

// 查询生产退料单头列表
export function listRtissue(query) {
  return request({
    url: '/mes/wm/rtissue/list',
    method: 'get',
    params: query
  })
}

// 查询生产退料单头详细
export function getRtissue(rtId) {
  return request({
    url: '/mes/wm/rtissue/' + rtId,
    method: 'get'
  })
}

// 新增生产退料单头
export function addRtissue(data) {
  return request({
    url: '/mes/wm/rtissue',
    method: 'post',
    data: data
  })
}

// 修改生产退料单头
export function updateRtissue(data) {
  return request({
    url: '/mes/wm/rtissue',
    method: 'put',
    data: data
  })
}

export function execute(rtId){
  return request({
    url: '/mes/wm/rtissue/'+rtId,
    method: 'put'
  })
}

// 删除生产退料单头
export function delRtissue(rtId) {
  return request({
    url: '/mes/wm/rtissue/' + rtId,
    method: 'delete'
  })
}
