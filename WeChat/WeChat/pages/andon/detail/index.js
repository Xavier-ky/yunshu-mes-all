import { getEventDetail } from '../../../services/andon'
import { ANDON_STATUS_MAP, PAGE_PATH_MAP } from '../../../utils/constants'
import { formatDateTime } from '../../../utils/format'
import { getRoleCode } from '../../../utils/auth'

Page({
  data: {
    eventId: '',
    event: null,
    loading: true,
    showHandleBtn: false
  },

  onLoad(options) {
    const eventId = options.eventId || ''
    if (eventId) {
      this.setData({ eventId })
      this.loadDetail(eventId)
    } else {
      this.setData({ loading: false })
      wx.showToast({ title: '缺少事件ID', icon: 'none' })
    }
  },

  loadDetail(eventId) {
    this.setData({ loading: true })

    getEventDetail(eventId).then(res => {
      if (res && res.code === 0 && res.data) {
        const event = res.data
        const handleableStatuses = ['INITIATED', 'RESPONDED', 'PROCESSING']
        const roleCode = getRoleCode()
        this.setData({
          event,
          loading: false,
          showHandleBtn: handleableStatuses.includes(event.status) && roleCode !== 'LINE_OPERATOR'
        })
      } else {
        this.setData({ loading: false })
        wx.showToast({ title: '未找到安灯事件', icon: 'none' })
      }
    }).catch(() => {
      this.setData({ loading: false })
      wx.showToast({ title: '加载失败，请重试', icon: 'none' })
    })
  },

  goToHandle() {
    const { eventId } = this.data
    wx.navigateTo({ url: `${PAGE_PATH_MAP.andonHandle}?eventId=${eventId}` })
  },

  onGoChat() {
    const { event } = this.data
    if (event && event.groupId) {
      wx.navigateTo({ url: `${PAGE_PATH_MAP.chatDetail}?groupId=${event.groupId}` })
    }
  },

  onPreviewDescImage(e) {
    const { index } = e.currentTarget.dataset
    const { event } = this.data
    if (event && event.images && event.images.length > 0) {
      wx.previewImage({
        urls: event.images,
        current: event.images[index]
      })
    }
  },

  formatTime(dateStr) {
    return formatDateTime(dateStr)
  }
})