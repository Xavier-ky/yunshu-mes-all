

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

export function getIssueTasks(params = {}) {
  return callCloudFunction('mes-inventory', { action: 'listTasks', taskType: 'ISSUE', ...params }, { loading: false })
}

export function getReturnTasks(params = {}) {
  return callCloudFunction('mes-inventory', { action: 'listTasks', taskType: 'RETURN', ...params }, { loading: false })
}

export function getInboundTasks(params = {}) {
  return callCloudFunction('mes-inventory', { action: 'listTasks', taskType: 'INBOUND', ...params }, { loading: false })
}

export function getOutboundTasks(params = {}) {
  return callCloudFunction('mes-inventory', { action: 'listTasks', taskType: 'OUTBOUND', ...params }, { loading: false })
}

export function getInventoryTaskDetail(taskId) {
  return callCloudFunction('mes-inventory', { action: 'taskDetail', taskId })
}

export function getFifoBatch(materialCode) {
  return callCloudFunction('mes-inventory', { action: 'fifoQuery', materialCode }, { loading: false })
}

export function lookupMaterialByBatch(batchNo) {
  return callCloudFunction('mes-inventory', { action: 'batchLookup', batchNo }, { loading: false })
}

export function submitMaterialIssue(data) {
  return callCloudFunction('mes-inventory', { action: 'materialIssue', ...data }, { loadingText: '发料中...' })
}

export function submitMaterialReturn(data) {
  return callCloudFunction('mes-inventory', { action: 'materialReturn', ...data }, { loadingText: '退料中...' })
}

export function submitInbound(data) {
  return callCloudFunction('mes-inventory', { action: 'submitInbound', ...data }, { loadingText: '入库中...' })
}

export function submitOutbound(data) {
  return callCloudFunction('mes-inventory', { action: 'submitOutbound', ...data }, { loadingText: '出库中...' })
}

export function getMaterialCalls(params = {}) {
  return callCloudFunction('mes-inventory', { action: 'listMaterialCalls', ...params }, { loading: false })
}

export function getMaterialCallDetail(id) {
  if (!id) return Promise.resolve({ code: 404, data: null })
  return callCloudFunction('mes-inventory', { action: 'listMaterialCalls' }, { loading: false }).then(res => {

    const list = (res && res.data && res.data.list) || []
    const call = list.find(c => c.callId === id || c.callNo === id)
    if (call) {
      return { code: 0, data: call }
    }

    return callCloudFunction('mes-inventory', { action: 'taskDetail', taskId: id }, { loading: false }).then(taskRes => {
      if (taskRes && taskRes.code === 0 && taskRes.data) {
        return { code: 0, data: taskRes.data }
      }
      return { code: 404, data: null }
    }).catch(() => {
      return { code: 404, data: null }
    })
  }).catch(() => {
    return { code: 404, data: null }
  })
}

export function createMaterialCall(data) {
  return callCloudFunction('mes-inventory', { action: 'createMaterialCall', ...data }, { loadingText: '提交中...' })
}

export function processMaterialCall(id, data) {
  return callCloudFunction('mes-inventory', { action: 'processMaterialCall', callId: id, ...data }, { loadingText: '处理中...' })
}

export function completeMaterialCall(id) {
  return callCloudFunction('mes-inventory', { action: 'completeMaterialCall', callId: id }, { loadingText: '完成中...' })
}
