

export function login(username, password) {
  return new Promise((resolve, reject) => {
    wx.showLoading({ title: '登录中...', mask: true })
    wx.cloud.callFunction({
      name: 'mes-auth',
      data: { username, password },
      success: res => {
        wx.hideLoading()
        const result = (res && res.result) ? res.result : res
        if (result && result.code === 0) {
          resolve(result)
        } else {

          reject(new Error((result && result.msg) || '登录失败'))
        }
      },
      fail: err => {
        wx.hideLoading()

        wx.showToast({ title: '网络异常，请重试', icon: 'none' })
        reject(new Error('网络异常，请重试'))
      }
    })
  })
}

export function logout() {
  return Promise.resolve({ code: 0, msg: '已退出' })
}
