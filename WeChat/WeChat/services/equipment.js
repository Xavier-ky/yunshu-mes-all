

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
          const msg = (result && result.msg) || '云函数调用失败'
          if (showError) wx.showToast({ title: msg, icon: 'none', duration: 2500 })
          reject(new Error(msg))
        }
      },
      fail: err => {
        if (loading) wx.hideLoading()
        const msg = err.errMsg || '网络异常，请重试'
        if (showError) wx.showToast({ title: msg, icon: 'none', duration: 2500 })
        reject(err)
      }
    })
  })
}

export function getDevices(params = {}) {
  return callCloudFunction('mes-equipment', { action: 'list', ...params }, { loading: false })
}

export function getDeviceDetail(deviceCode) {
  return callCloudFunction('mes-equipment', { action: 'detail', deviceCode })
}

export function createRepairOrder(data) {
  return callCloudFunction('mes-equipment', { action: 'submitRepair', ...data }, { loadingText: '提交报修...' })
}

export function completeRepair(data) {
  return callCloudFunction('mes-equipment', { action: 'completeRepair', ...data }, { loadingText: '处理中...' })
}

export function completeInspection(deviceCode, inspector, result) {
  return callCloudFunction('mes-equipment', { action: 'completeInspection', deviceCode, inspector, result }, { loadingText: '提交点检...' })
}

export function getTpmAlerts() {
  return callCloudFunction('mes-equipment', { action: 'getTpmAlerts' }, { loading: false })
}
