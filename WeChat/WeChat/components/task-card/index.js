const PAGE_MAP = {
  PRODUCTION: '/pages/production/task-detail/index',
  QUALITY: '/pages/quality/task-list/index',
  ANDON: '/pages/andon/detail/index',
  REPAIR: '/pages/equipment/repair/index',
  MATERIAL: '/pages/inventory/task-detail/index'
}

Component({
  properties: {
    task: { type: Object, value: {} }
  },

  methods: {
    onTap() {
      const task = this.properties.task
      if (!task || !task.taskType) return
      const baseUrl = PAGE_MAP[task.taskType] || PAGE_MAP.PRODUCTION
      const deviceCode = task.productModel && task.productModel.startsWith('EQ-') ? task.productModel : ''
      const url = `${baseUrl}?taskId=${task.taskId}${deviceCode ? `&deviceCode=${deviceCode}` : ''}`
      wx.navigateTo({ url })
    }
  }
})
