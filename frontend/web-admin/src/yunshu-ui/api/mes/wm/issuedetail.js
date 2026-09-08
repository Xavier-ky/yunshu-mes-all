import request from '@/yunshu-ui/api/request'

// 查询生产领料单明细列表
export function listIssuedetail(query) {
  return request({
    url: '/mes/wm/issuedetail/list',
    method: 'get',
    params: query
  })
}

// 查询生产领料单明细详细
export function getIssuedetail(detailId) {
  return request({
    url: '/mes/wm/issuedetail/' + detailId,
    method: 'get'
  })
}

// 新增生产领料单明细
export function addIssuedetail(data) {
  return request({
    url: '/mes/wm/issuedetail',
    method: 'post',
    data: data
  })
}

// 修改生产领料单明细
export function updateIssuedetail(data) {
  return request({
    url: '/mes/wm/issuedetail',
    method: 'put',
    data: data
  })
}

// 删除生产领料单明细
export function delIssuedetail(detailId) {
  return request({
    url: '/mes/wm/issuedetail/' + detailId,
    method: 'delete'
  })
}
