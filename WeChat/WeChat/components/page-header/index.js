Component({
  properties: {
    title: { type: String, value: '' },
    showBack: { type: Boolean, value: true },
    rightText: { type: String, value: '' }
  },

  data: {
    statusBarHeight: 0,
    headerHeight: 44
  },

  lifetimes: {
    attached() {
      const app = getApp()
      const statusBarHeight = app.globalData.statusBarHeight || 20
      const navBarHeight = app.globalData.navBarHeight || 44
      const headerHeight = statusBarHeight + navBarHeight
      this.setData({
        statusBarHeight,
        headerHeight
      })
    }
  },

  methods: {
    onBack() {
      this.triggerEvent('back')
      wx.navigateBack({
        delta: 1,
        fail() {
          wx.switchTab({ url: '/pages/home/index' })
        }
      })
    },
    onRightTap() {
      this.triggerEvent('righttap')
    }
  }
})