const TOKEN_KEY = 'mes_token'
const USER_INFO_KEY = 'mes_user_info'

export function setToken(token) {
  wx.setStorageSync(TOKEN_KEY, token)
}

export function getToken() {
  return wx.getStorageSync(TOKEN_KEY) || ''
}

export function removeToken() {
  wx.removeStorageSync(TOKEN_KEY)
}

export function setUserInfo(userInfo) {
  wx.setStorageSync(USER_INFO_KEY, userInfo)
}

export function getUserInfo() {
  return wx.getStorageSync(USER_INFO_KEY) || null
}

export function removeUserInfo() {
  wx.removeStorageSync(USER_INFO_KEY)
}

export function clearAll() {
  wx.clearStorageSync()
}