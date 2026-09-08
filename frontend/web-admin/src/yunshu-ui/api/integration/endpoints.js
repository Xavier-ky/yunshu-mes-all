import request from '@/yunshu-ui/api/request'

export function listEndpoints(query) {
  return request({ url: '/integration/endpoints/list', method: 'get', params: query })
}
