import request from '@/yunshu-ui/api/request'

export function listSyncLogs(query) {
  return request({ url: '/integration/sync-logs/list', method: 'get', params: query })
}
