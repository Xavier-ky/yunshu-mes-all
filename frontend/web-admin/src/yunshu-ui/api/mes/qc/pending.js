import request from '@/yunshu-ui/api/request'

function parsePendingTime(value) {
  if (value == null || value === '') return 0
  const text = String(value).trim()
  const ms = Date.parse(text.includes('T') ? text : text.replace(' ', 'T'))
  return Number.isFinite(ms) ? ms : 0
}

/** 待检任务按最近报工/来源时间倒序（与操作工侧栏一致） */
export function sortQcPendingRecentFirst(rows) {
  return [...(rows || [])].sort((a, b) => {
    const timeDiff =
      parsePendingTime(b.createTime || b.feedbackTime || b.updateTime) -
      parsePendingTime(a.createTime || a.feedbackTime || a.updateTime)
    if (timeDiff !== 0) return timeDiff
    return (Number(b.sourceDocId) || 0) - (Number(a.sourceDocId) || 0)
  })
}

// 查询待检验任务清单
export function listPending(query) {
  return request({
    url: '/mes/qc/pending/list',
    method: 'get',
    params: query,
  }).then((res) => {
    const rows = sortQcPendingRecentFirst(res?.rows || res?.data || [])
    return { ...res, rows }
  })
}
