

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

export function getTasks(params) {
  return callCloudFunction('mes-quality', { action: 'listTasks', ...params }, { loading: false })
}

export function getTaskDetail(id) {
  return callCloudFunction('mes-quality', { action: 'taskDetail', taskId: id })
}

export function submitRecord(data) {

  return callCloudFunction('mes-quality', { ...data, action: 'submitDefectRecord' }, { loadingText: '提交中...' })
}

export function submitInspection(data) {
  return callCloudFunction('mes-quality', { ...data, action: 'submitInspection' }, { loadingText: '提交中...' })
}

export function freezeWorkOrder(taskId, reason) {
  return callCloudFunction('mes-quality', { action: 'freezeWorkOrder', taskId, reason }, { loadingText: '冻结中...' })
}

export function unfreezeWorkOrder(taskId) {
  return callCloudFunction('mes-quality', { action: 'unfreezeWorkOrder', taskId }, { loadingText: '解冻中...' })
}

export function getInspectionResults(params) {
  return callCloudFunction('mes-quality', { action: 'listRecords', ...params }, { loading: false })
}

export function getInspectionResult(recordId) {
  return callCloudFunction('mes-quality', { action: 'recordDetail', recordId })
}
