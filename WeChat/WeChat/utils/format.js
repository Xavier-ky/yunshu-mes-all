

export function formatDateTime(dateStr) {
  if (!dateStr) return ''

  let d

  if (dateStr instanceof Date) {
    d = dateStr
  } else if (typeof dateStr === 'string') {

    d = new Date(dateStr)

    if (isNaN(d.getTime())) {
      d = new Date(dateStr.replace(' ', 'T') + '+08:00')
    }

    if (isNaN(d.getTime())) return dateStr
  } else if (typeof dateStr === 'number') {

    d = new Date(dateStr)
  } else {
    return String(dateStr)
  }

  const year = d.getFullYear()
  const month = String(d.getMonth() + 1).padStart(2, '0')
  const day = String(d.getDate()).padStart(2, '0')
  const hour = String(d.getHours()).padStart(2, '0')
  const minute = String(d.getMinutes()).padStart(2, '0')
  return `${year}-${month}-${day} ${hour}:${minute}`
}

export function formatDate(dateStr) {
  if (!dateStr) return ''
  const full = formatDateTime(dateStr)
  return full ? full.split(' ')[0] : ''
}

export function formatTime(dateStr) {
  if (!dateStr) return ''
  const full = formatDateTime(dateStr)
  return full ? full.split(' ')[1] || '' : ''
}

export function formatNumber(num) {
  if (num === null || num === undefined || isNaN(num)) return '--'
  return Number(num).toLocaleString()
}

export function formatRelativeTime(dateStr) {
  if (!dateStr) return ''
  const input = formatDateTime(dateStr)
  if (!input) return ''
  const d = new Date(dateStr)
  if (isNaN(d.getTime())) return ''
  const now = Date.now()
  const diff = now - d.getTime()
  if (diff < 0) return input
  if (diff < 60000) return '刚刚'
  if (diff < 3600000) return `${Math.floor(diff / 60000)}分钟前`
  if (diff < 86400000) return `${Math.floor(diff / 3600000)}小时前`
  if (diff < 604800000) return `${Math.floor(diff / 86400000)}天前`
  return input.split(' ')[0]
}
