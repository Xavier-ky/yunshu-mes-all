import request from '@/yunshu-ui/api/request'

export function traceWorkOrder(workOrderNo) {
  return request({
    url: '/traceability/work-order/' + encodeURIComponent(workOrderNo),
    method: 'get'
  })
}

export function listRecentTraceWorkOrders(limit = 20) {
  return request({
    url: '/traceability/recent-work-orders',
    method: 'get',
    params: { limit }
  })
}
