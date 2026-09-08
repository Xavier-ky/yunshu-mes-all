Component({
  properties: {
    primaryText: { type: String, value: '提交' },
    secondaryText: { type: String, value: '取消' },
    primaryDisabled: { type: Boolean, value: false },
    showSecondary: { type: Boolean, value: true },
    primaryLoading: { type: Boolean, value: false }
  },

  methods: {
    onPrimary() {
      if (this.properties.primaryDisabled) return
      this.triggerEvent('primary')
    },
    onSecondary() {
      this.triggerEvent('secondary')
    }
  }
})