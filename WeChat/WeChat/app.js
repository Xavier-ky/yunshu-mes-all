import { isLoggedIn, getCurrentUser } from './utils/auth'
import { getToken } from './utils/storage'
import { PAGE_PATH_MAP } from './utils/constants'
import { initStore, forcePersist } from './store/index'

App({
  globalData: {
    userInfo: null,
    token: '',
    currentTab: 0
  },

  onLaunch() {
    wx.cloud.init({ traceUser: true })

    const systemInfo = wx.getSystemInfoSync()
    this.globalData.statusBarHeight = systemInfo.statusBarHeight
    this.globalData.navBarHeight = 44

    initStore()

    wx.onAppHide(() => { forcePersist() })

    this.checkLogin()
  },

  checkLogin() {
    const token = getToken()
    if (token) {
      this.globalData.token = token
      this.globalData.userInfo = getCurrentUser()
    }
  },

  isLoggedIn() {
    return isLoggedIn()
  },

  getRoleCode() {
    const user = this.globalData.userInfo
    return user ? user.roleCode : ''
  },

  setUserInfo(userInfo) {
    this.globalData.userInfo = userInfo
  },

  setToken(token) {
    this.globalData.token = token
  },

  navigateToLogin() {
    wx.reLaunch({ url: PAGE_PATH_MAP.login })
  }
})