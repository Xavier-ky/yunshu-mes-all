

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

export function getTasks(params = {}) {
  return callCloudFunction('mes-production', { action: 'listTasks', ...params }, { loading: false })
}

export function getTaskDetail(id) {
  return callCloudFunction('mes-production', { action: 'taskDetail', taskId: id })
}

export function submitReport(data) {
  return callCloudFunction('mes-production', { action: 'submitReport', ...data }, { loadingText: '报工中...' })
}

export function bindMaterial(data) {
  return callCloudFunction('mes-production', { action: 'bindMaterial', ...data }, { loadingText: '绑定中...' })
}
