import { getInventoryTaskDetail } from '../../../services/inventory'
import { formatDateTime } from '../../../utils/format'

Page({
  data: {
    taskId: '',
    task: null,
    loading: true
  },

  onLoad(options) {
    const taskId = options.taskId || ''
    if (taskId) {
      this.setData({ taskId })
      this.loadDetail(taskId)
    } else {
      this.setData({ loading: false })
    }
  },

  loadDetail(taskId) {
    this.setData({ loading: true })
    getInventoryTaskDetail(taskId).then(res => {
      if (res && res.code === 0 && res.data) {
        this.setData({ task: res.data, loading: false })
      } else {
        this.setData({ loading: false })
        wx.showToast({ title: '退料单不存在', icon: 'none' })
      }
    }).catch(() => {
      this.setData({ loading: false })
      wx.showToast({ title: '加载失败', icon: 'none' })
    })
  }
})
