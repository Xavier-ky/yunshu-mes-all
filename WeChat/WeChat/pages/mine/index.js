import { getCurrentUser } from '../../utils/auth'
import { removeToken, removeUserInfo, clearAll } from '../../utils/storage'
import { PAGE_PATH_MAP } from '../../utils/constants'

const app = getApp()

Page({
  data: {
    userInfo: {},
    userNameFirst: ''
  },

  onShow() {
    if (typeof this.getTabBar === 'function' && this.getTabBar()) {
      this.getTabBar().setData({ selected: 4 })
    }
    this.loadUserInfo()
  },

  loadUserInfo() {
    const userInfo = getCurrentUser() || app.globalData.userInfo || {}
    this.setData({
      userInfo,
      userNameFirst: (userInfo.realName || '?').charAt(0)
    })
  },

  onClearCache() {
    wx.showModal({
      title: '提示',
      content: '确定清除缓存数据？',
      success: (res) => {
        if (res.confirm) {
          clearAll()
          wx.showToast({ title: '缓存已清除', icon: 'success' })
        }
      }
    })
  },

  onLogout() {
    wx.showModal({
      title: '提示',
      content: '确定退出登录？',
      success: (res) => {
        if (res.confirm) {
          removeToken()
          removeUserInfo()
          app.globalData.userInfo = null
          app.globalData.token = ''
          wx.reLaunch({ url: PAGE_PATH_MAP.login })
        }
      }
    })
  },

  onNavToTasks() { wx.navigateTo({ url: PAGE_PATH_MAP.tasks }) },
  onNavToScan() { wx.navigateTo({ url: PAGE_PATH_MAP.scan }) },
  onNavToMessages() { wx.navigateTo({ url: PAGE_PATH_MAP.message }) }
})
