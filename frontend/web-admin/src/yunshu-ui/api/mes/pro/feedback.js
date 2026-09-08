import request from '@/yunshu-ui/api/request'

export function listFeedback(query) {
  return request({ url: '/mes/pro/feedback/list', method: 'get', params: query })
}

export function getFeedback(recordId) {
  return request({ url: '/mes/pro/feedback/' + recordId, method: 'get' })
}

export function addFeedback(data) {
  return request({ url: '/mes/pro/feedback', method: 'post', data })
}

export function updateFeedback(data) {
  return request({ url: '/mes/pro/feedback', method: 'put', data })
}

export function delFeedback(recordId) {
  return request({ url: '/mes/pro/feedback/' + recordId, method: 'delete' })
}

export function execute(recordId) {
  return request({ url: '/mes/pro/feedback/execute/' + recordId, method: 'put' })
}
