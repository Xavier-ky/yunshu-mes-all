import { PAGE_PATH_MAP } from '../../../utils/constants'

Page({
  data: {},

  onShow() {
    wx.showToast({ title: '退料已迁移至领料确认页', icon: 'none', duration: 2000 })
  },

  onGoToIssue() {
    wx.navigateTo({ url: PAGE_PATH_MAP.materialIssue })
  }
})
