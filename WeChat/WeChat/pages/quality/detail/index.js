import { getInspectionResult } from '../../../services/quality'
import { formatDateTime } from '../../../utils/format'

Page({
  data: {
    recordId: '',
    record: null,
    loading: true
  },

  onLoad(options) {
    const recordId = options.recordId || ''
    if (recordId) {
      this.setData({ recordId })
      this.loadDetail(recordId)
    } else {
      this.setData({ loading: false })
      wx.showToast({ title: '缺少记录ID', icon: 'none' })
    }
  },

  loadDetail(recordId) {
    this.setData({ loading: true })
    getInspectionResult(recordId).then(res => {
      if (res.code === 0 && res.data) {
        this.setData({ record: res.data, loading: false })
      } else {
        this.setData({ loading: false })
        wx.showToast({ title: '质检记录不存在', icon: 'none' })
      }
    }).catch(() => {
      this.setData({ loading: false })
      wx.showToast({ title: '加载失败', icon: 'none' })
    })
  },

  onPreviewImage(e) {
    const { urls, current } = e.currentTarget.dataset
    if (urls && urls.length > 0) {
      wx.previewImage({ urls, current: current || urls[0] })
    }
  },

  formatTime(str) {
    return formatDateTime(str)
  }
})
