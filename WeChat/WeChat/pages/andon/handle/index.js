import { getEventDetail, handleEvent, getEvents, acceptEvent } from '../../../services/andon'
import { triggerAutoDisbandGroup } from '../../../services/chat'
import { formatDateTime } from '../../../utils/format'
import { getRoleCode } from '../../../utils/auth'
import { ANDON_TARGET } from '../../../utils/constants'

const ROLE_VISIBLE_TYPES = {
  WAREHOUSE_KEEPER:      ['MATERIAL'],
  EQUIPMENT_MAINTAINER:   ['EQUIPMENT'],
  QUALITY_INSPECTOR:      ['QUALITY'],
  PRODUCTION_SUPERVISOR:   ['PROCESS'],
  PRODUCTION_SUPERVISOR:  ['PROCESS'],
  MANAGER:                ['PROCESS', 'EQUIPMENT', 'QUALITY', 'MATERIAL']
}

const HANDLE_STATUS_OPTIONS = [
  { value: 'RESOLVED', label: '已解决' },
  { value: 'CLOSED', label: '已关闭' }
]

Page({
  data: {

    eventId: '',

    eventList: [],
    loading: false,
    activeStatus: '',
    statusList: [
      { label: '全部', value: '' },
      { label: '已发起', value: 'INITIATED' },
      { label: '已响应', value: 'RESPONDED' },
      { label: '处理中', value: 'PROCESSING' },
      { label: '已解决', value: 'RESOLVED' }
    ],

    event: null,
    handleResult: '',
    statusIndex: 0,
    status: 'RESOLVED',
    statusOptions: HANDLE_STATUS_OPTIONS,
    submitting: false,
    accepting: false
  },

  onLoad(options) {
    const eventId = options.eventId || ''
    if (eventId) {
      this.setData({ eventId })
      this.loadDetail(eventId)
    } else {
      this.loadEventList()
    }
  },

  onShow() {

    if (!this.data.eventId) {
      this.loadEventList()
    }
  },

  loadEventList() {
    this.setData({ loading: true })
    const params = {}
    if (this.data.activeStatus) params.status = this.data.activeStatus

    const roleCode = getRoleCode()
    const visibleTypes = ROLE_VISIBLE_TYPES[roleCode] || []

    getEvents(params).then(res => {
      if (res.code === 0 && res.data) {
        let list = res.data.list || []

        if (visibleTypes.length > 0) {
          list = list.filter(e => visibleTypes.includes(e.eventType))
        }
        this.setData({ eventList: list, loading: false })
      } else {
        this.setData({ loading: false })
      }
    }).catch(() => {
      this.setData({ loading: false })
      wx.showToast({ title: '加载失败', icon: 'none' })
    })
  },

  onStatusFilter(e) {
    const value = e.currentTarget.dataset.value
    this.setData({ activeStatus: value })
    this.loadEventList()
  },

  onEventTap(e) {
    const eventId = e.currentTarget.dataset.eventId
    if (eventId) {

      wx.redirectTo({
        url: `/pages/andon/handle/index?eventId=${eventId}`
      })
    }
  },

  loadDetail(eventId) {
    this.setData({ loading: true })

    getEventDetail(eventId).then(res => {
      this.setData({ loading: false })
      if (res.code === 0 && res.data) {
        const event = res.data
        const needsAccept = event.status === 'INITIATED' || event.status === 'RESPONDED'
        this.setData({ event, needsAccept })
      } else {
        wx.showToast({ title: '未找到安灯事件', icon: 'none' })
      }
    }).catch(() => {
      this.setData({ loading: false })
      wx.showToast({ title: '加载失败', icon: 'none' })
    })
  },

  onStatusChange(e) {
    const index = e.detail.value
    this.setData({
      statusIndex: index,
      status: HANDLE_STATUS_OPTIONS[index].value
    })
  },

  onHandleResultInput(e) {
    this.setData({ handleResult: e.detail.value })
  },

  onPreviewImage(e) {
    const { index } = e.currentTarget.dataset
    const { event } = this.data
    if (event && event.images && event.images.length > 0) {
      wx.previewImage({
        urls: event.images,
        current: event.images[index]
      })
    }
  },

  onAccept() {
    const { eventId, accepting } = this.data
    if (accepting) return

    this.setData({ accepting: true })

    acceptEvent(eventId).then(res => {
      this.setData({ accepting: false })
      if (res.code === 0) {
        wx.showToast({ title: '已接收任务', icon: 'success' })

        this.loadDetail(eventId)
      } else {
        wx.showToast({ title: res.message || '接收失败', icon: 'none' })
      }
    }).catch(() => {
      this.setData({ accepting: false })
      wx.showToast({ title: '网络错误，请重试', icon: 'none' })
    })
  },

  onSubmit() {
    const { eventId, handleResult, status, submitting } = this.data

    if (submitting) return

    if (!handleResult.trim()) {
      wx.showToast({ title: '请输入处理结果', icon: 'none' })
      return
    }

    this.setData({ submitting: true })

    handleEvent({
      eventId,
      handleResult: handleResult.trim(),
      status
    }).then(res => {
      this.setData({ submitting: false })
      if (res.code === 0) {

        if (status === 'RESOLVED' || status === 'CLOSED') {
          triggerAutoDisbandGroup(eventId, '安灯已' + (status === 'RESOLVED' ? '解决' : '关闭') + '，系统自动解散群聊')
        }

        wx.showToast({ title: '处理成功', icon: 'success' })
        setTimeout(() => {
          wx.navigateBack({ delta: 1 })
        }, 1500)
      } else {
        wx.showToast({ title: res.message || '处理失败', icon: 'none' })
      }
    }).catch(() => {
      this.setData({ submitting: false })
      wx.showToast({ title: '网络错误，请重试', icon: 'none' })
    })
  },

  formatTime(dateStr) {
    return formatDateTime(dateStr)
  }
})
