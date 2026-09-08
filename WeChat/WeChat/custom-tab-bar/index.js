const app = getApp()

Component({
  data: {
    selected: 0,
    messageBadge: '',
    list: [
      { pagePath: '/pages/home/index', text: '首页', icon: '🏠', iconImage: '/images/首页.svg' },
      { pagePath: '/pages/message/index', text: '系统信息', icon: '✉', iconImage: '/images/消息.svg', hasBadge: true },
      { pagePath: '/pages/chat/list/index', text: '群聊', icon: '💬', iconImage: '/images/群聊.svg' },
      { pagePath: '/pages/ai/chat/index', text: 'AI助手', icon: '🤖', iconImage: '/images/AI助手.svg' },
      { pagePath: '/pages/mine/index', text: '我的', icon: '👤', iconImage: '/images/_我的.svg' }
    ]
  },

  lifetimes: {
    attached() {
      if (app && app.globalData) {
        const count = app.globalData.messageUnread || 0
        this.setData({
          messageBadge: count > 0 ? String(count > 99 ? '99+' : count) : ''
        })
      }
    }
  },

  pageLifetimes: {
    show() {
      if (app && app.globalData) {
        const count = app.globalData.messageUnread || 0
        this.setData({
          messageBadge: count > 0 ? String(count > 99 ? '99+' : count) : ''
        })
      }
    }
  },

  methods: {
    switchTab(e) {
      const { path, index } = e.currentTarget.dataset
      if (this.data.selected === index) return
      wx.switchTab({ url: path })
    }
  }
})
