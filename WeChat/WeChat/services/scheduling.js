

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
        wx.showToast({ title: '网络异常，请重试', icon: 'none' })
        reject(new Error('网络异常，请重试'))
      }
    })
  })
}

export function getSchedules(params = {}) {
  return callCloudFunction('mes-schedule', { action: 'list', ...params }, { loading: false })
}

export function getScheduleDetail(id) {
  return callCloudFunction('mes-schedule', { action: 'detail', scheduleNo: id })
}

export function doCreateSchedule(data) {
  return callCloudFunction('mes-schedule', { action: 'create', ...data }, { loadingText: '排产布置中...' })
}

export function doDeleteSchedule(id) {
  return callCloudFunction('mes-schedule', { action: 'delete', scheduleNo: id })
}

export function getOperators(lineName) {
  return callCloudFunction('mes-schedule', { action: 'getOperators', lineName }, { loading: false })
}
