Component({
  properties: {
    text: { type: String, value: '暂无数据' },
    showAction: { type: Boolean, value: false },
    actionText: { type: String, value: '' }
  },

  methods: {
    onAction() {
      this.triggerEvent('action')
    }
  }
})