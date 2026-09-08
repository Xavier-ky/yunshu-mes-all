import { BASE_URL } from '../utils/constants'
import { getToken, removeToken } from '../utils/storage'
import { signRequest, encryptPayload, decryptPayload, generateNonce } from '../utils/crypto'

const request = (config) => {
  return new Promise((resolve, reject) => {
    const url = (config.baseURL || BASE_URL) + config.url
    const header = {
      'Content-Type': 'application/json',
      ...config.header
    }

    const token = getToken()
    if (token) {
      header['Authorization'] = `Bearer ${token}`
    }

    if (config.needSign) {
      header['X-Signature'] = signRequest(config.data || {})
      header['X-Nonce'] = generateNonce()
      header['X-Timestamp'] = String(Date.now())
    }

    let data = config.data
    if (config.needEncrypt && data) {
      data = encryptPayload(data)
      header['X-Encrypted'] = '1'
    }

    if (config.showLoading !== false) {
      wx.showLoading({ title: config.loadingText || '加载中...', mask: true })
    }

    wx.request({
      url,
      method: config.method || 'GET',
      data,
      header,
      timeout: config.timeout || 10000,
      success(res) {
        wx.hideLoading()

        if (res.statusCode === 401) {
          removeToken()
          wx.reLaunch({ url: '/pages/login/index' })
          reject(res)
          return
        }

        if (res.statusCode >= 200 && res.statusCode < 300) {
          if (res.data && res.data.code !== undefined && res.data.code !== 0) {
            wx.showToast({ title: res.data.message || '请求失败', icon: 'none', duration: 2000 })
            reject(res.data)
            return
          }
          const result = config.needEncrypt ? decryptPayload(res.data) : res.data
          resolve(result)
        } else {
          wx.showToast({ title: '服务器异常', icon: 'none', duration: 2000 })
          reject(res)
        }
      },
      fail(err) {
        wx.hideLoading()
        const msg = err.errMsg || '网络异常'
        if (msg.indexOf('timeout') > -1) {
          wx.showToast({ title: '请求超时', icon: 'none', duration: 2000 })
        } else if (msg.indexOf('fail') > -1) {
          wx.showToast({ title: '网络异常，请检查网络', icon: 'none', duration: 2000 })
        } else {
          wx.showToast({ title: msg, icon: 'none', duration: 2000 })
        }
        reject(err)
      }
    })
  })
}

export default request