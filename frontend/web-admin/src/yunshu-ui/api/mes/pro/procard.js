import request from '@/yunshu-ui/api/request'

export function listProcard(query) {
  return request({ url: '/mes/pro/procard/list', method: 'get', params: query })
}

export function getProcard(cardId) {
  return request({ url: '/mes/pro/procard/' + cardId, method: 'get' })
}

export function addProcard(data) {
  return request({ url: '/mes/pro/procard', method: 'post', data })
}

export function updateProcard(data) {
  return request({ url: '/mes/pro/procard', method: 'put', data })
}

export function delProcard(cardId) {
  return request({ url: '/mes/pro/procard/' + cardId, method: 'delete' })
}
