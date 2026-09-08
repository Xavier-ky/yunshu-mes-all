import request from '@/yunshu-ui/api/request'

export function listSystems(query) {
  return request({ url: '/integration/systems/list', method: 'get', params: query })
}

export function getSystem(id) {
  return request({ url: '/integration/systems/' + id, method: 'get' })
}

export function addSystem(data) {
  return request({ url: '/integration/systems', method: 'post', data })
}

export function updateSystem(id, data) {
  return request({ url: '/integration/systems/' + id, method: 'put', data })
}

export function delSystem(id) {
  return request({ url: '/integration/systems/' + id, method: 'delete' })
}
