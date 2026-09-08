import request from '@/yunshu-ui/api/request'

export function listCalendars(query) {
  return request({
    url: '/mes/cal/calendar/list',
    method: 'get',
    params: query
  })
}

export function getCalendarSummary(query) {
  return request({
    url: '/mes/cal/calendar/summary',
    method: 'get',
    params: query
  })
}

export function getCalendarDay(query) {
  return request({
    url: '/mes/cal/calendar/day',
    method: 'get',
    params: query
  })
}

export function getCalendarWeek(query) {
  return request({
    url: '/mes/cal/calendar/week',
    method: 'get',
    params: query
  })
}
