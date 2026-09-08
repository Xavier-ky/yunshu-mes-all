

function callCloudFunction(name, data, opts = {}) {
  const { loading = true, showError = true } = opts
  if (loading) wx.showLoading({ title: '加载中...', mask: true })
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

export function getSupervisorSummary() {
  return callCloudFunction('mes-dashboard', { action: 'getSupervisorStats' }, { loading: false })
}

export function getManagementSummary() {
  return callCloudFunction('mes-dashboard', { action: 'getManagementStats' }, { loading: false })
}

export function getLineProgress() {
  return callCloudFunction('mes-dashboard', { action: 'getLineProgress' }, { loading: false })
}

export function getTrendData() {
  return callCloudFunction('mes-dashboard', { action: 'getTrendData' }, { loading: false })
}

export function getAbnormalOverview() {
  return callCloudFunction('mes-dashboard', { action: 'getAbnormalOverview' }, { loading: false })
}

export function getDefectDistribution() {
  return callCloudFunction('mes-dashboard', { action: 'getDefectDistribution' }, { loading: false })
}

export function getMobileSummary() {
  return getSupervisorSummary()
}
