Component({
  data: {
    selected: 0,
    list: [
      { pagePath: '/pages/home/index', text: '首页', icon: '🏠' },
      { pagePath: '/pages/ai/chat/index', text: '智能助手', icon: '🤖', iconImage: '/images/AI助手.svg' },
      { pagePath: '/pages/mine/index', text: '我的', icon: '👤' }
    ]
  },

  methods: {
    switchTab(e) {
      const { path, index } = e.currentTarget.dataset
      if (this.data.selected === index) return
      wx.switchTab({ url: path })
    }
  }
})
