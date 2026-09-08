Component({
  properties: {
    visible: { type: Boolean, value: true }
  },

  data: {
    expanded: false
  },

  methods: {
    onTap() {

      if (this.data.expanded) {
        this.navigateToChat()
        return
      }
      this.triggerEvent('tap')
      this.setData({ expanded: true })

      clearTimeout(this._navTimer)
      this._navTimer = setTimeout(() => {
        this.navigateToChat()
      }, 600)
    },

    navigateToChat() {
      wx.switchTab({ url: '/pages/ai/chat/index' })
    },

    onPageShow() {
      this.setData({ expanded: false })
    }
  },

  lifetimes: {
    detached() {
      clearTimeout(this._navTimer)
    }
  }
})
