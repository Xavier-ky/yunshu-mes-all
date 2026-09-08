

export function getProductTrace(sn) {
  return new Promise((resolve) => {
    wx.showLoading({ title: '查询中...', mask: true })

    wx.cloud.callFunction({
      name: 'mes-traceability',
      data: { action: 'query', sn }
    }).then(res => {
      wx.hideLoading()
      const result = (res && res.result) ? res.result : res
      if (result && result.code === 0) {
        resolve(result)
      } else {
        resolve({ code: -1, msg: '追溯功能暂不可用', data: null })
      }
    }).catch(() => {
      wx.hideLoading()
      wx.showToast({ title: '追溯功能暂不可用', icon: 'none', duration: 3000 })
      resolve({ code: -1, msg: '服务暂不可用', data: null })
    })
  })
}
