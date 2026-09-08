

function callCloudFunction(name, data, opts = {}) {
  const { loading = true, loadingText = '处理中...', showError = true } = opts
  if (loading) wx.showLoading({ title: loadingText, mask: true })
  return new Promise((resolve, reject) => {
    wx.cloud.callFunction({
      name,
      data,
      success: res => {
        if (loading) wx.hideLoading()
        const result = (res && res.result) ? res.result : res
        if (result && result.code === 0) {
          resolve({ code: 0, data: result.data, msg: result.msg || 'ok' })
        } else {
          reject(new Error((result && result.msg) || '云函数调用失败'))
        }
      },
      fail: err => {
        if (loading) wx.hideLoading()
        reject(new Error(err.errMsg || '网络异常，请重试'))
      }
    })
  })
}

export function getEvents(params = {}) {
  return callCloudFunction('mes-andon', { action: 'list', ...params }, { loading: false })
}

export function getEventDetail(eventId) {
  return callCloudFunction('mes-andon', { action: 'detail', eventId })
}

export function createEvent(data) {
  return callCloudFunction('mes-andon', { action: 'create', ...data }, { loadingText: '发起安灯...' })
}

export function handleEvent(data) {
  const handler = data.handler || data.handleResult || undefined
  const handleAction = data.handleAction || data.handleResult || data.status || undefined
  return callCloudFunction('mes-andon', { action: 'resolve', eventId: data.eventId, handler, handleAction }, { loadingText: '处理中...' })
}

export function acceptEvent(eventId) {
  return callCloudFunction('mes-andon', { action: 'accept', eventId }, { loadingText: '接收中...' })
}
