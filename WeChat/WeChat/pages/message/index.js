import { getMessages, markAsRead } from '../../services/message'
import { formatRelativeTime } from '../../utils/format'
import { getRoleCode, getCurrentUser } from '../../utils/auth'
import { PAGE_PATH_MAP } from '../../utils/constants'

const REF_TYPE_PATH = {
  WORK_ORDER: PAGE_PATH_MAP.productionTaskDetail,
  EQUIPMENT: PAGE_PATH_MAP.equipmentDetail,
  ANDON: PAGE_PATH_MAP.andonDetail,
  QUALITY: PAGE_PATH_MAP.qualityDetail,
  MATERIAL: PAGE_PATH_MAP.inventoryTaskList,
  TASK: PAGE_PATH_MAP.tasks
}

const REF_PARAM_MAP = {
  WORK_ORDER: (refId) => `?taskId=${refId}`,
  EQUIPMENT: (refId) => `?deviceCode=${refId}`,
  ANDON: (refId) => `?eventId=${refId}`,
  QUALITY: (refId) => `?recordId=${refId}`,
  MATERIAL: (refId) => `?taskId=${refId}`,
  TASK: (refId) => refId ? `?taskId=${refId}` : ''
}

Page({
  data: {
    activeType: '',
    messageList: [],
    unreadCount: 0,
    typeList: [
      { label: '全部', value: '' },
      { label: '任务', value: 'TASK' },
      { label: '设备', value: 'EQUIPMENT' },
      { label: '安灯', value: 'ANDON' },
      { label: '质量', value: 'QUALITY' },
      { label: '物料', value: 'MATERIAL' }
    ],
    allMessages: [],
    roleCode: ''
  },

  onShow() {
    if (typeof this.getTabBar === 'function' && this.getTabBar()) {
      this.getTabBar().setData({ selected: 1 })
    }
    this.loadMessages()
  },

  loadMessages() {
    const roleCode = getRoleCode()
    const userInfo = getCurrentUser()
    const operatorName = (userInfo && userInfo.realName) || ''
    this.setData({ roleCode })

    getMessages({ roleCode, operatorName }).then(res => {
      if (res.code === 0 && res.data) {
        const list = (res.data.list || []).map(m => ({
          ...m,
          timeStr: formatRelativeTime(m.time),

          jumpUrl: this.buildJumpUrl(m)
        }))
        const unreadCount = list.filter(m => !m.isRead).length

        this.setData({
          allMessages: list,
          unreadCount,
          roleCode
        })
        this.filterMessages()

        this.updateTabBadge(unreadCount)
      }
    }).catch(() => {
      wx.showToast({ title: '加载失败', icon: 'none' })
    })
  },

  buildJumpUrl(msg) {
    const base = REF_TYPE_PATH[msg.refType]
    if (!base) return PAGE_PATH_MAP.tasks

    const paramFn = REF_PARAM_MAP[msg.refType]
    const param = paramFn ? paramFn(msg.refId) : ''
    return base + param
  },

  filterMessages() {
    const { activeType, allMessages } = this.data
    let list = [...allMessages]
    if (activeType) {
      list = list.filter(m => m.type === activeType)
    }
    this.setData({ messageList: list })
  },

  onTypeChange(e) {
    this.setData({ activeType: e.currentTarget.dataset.value })
    this.filterMessages()
  },

  onMessageTap(e) {
    const { id } = e.currentTarget.dataset
    const msg = this.data.allMessages.find(m => m.msgId === id)
    if (!msg) return

    if (!msg.isRead) {
      msg.isRead = true
      markAsRead(id).then(() => {

        const newCount = this.data.allMessages.filter(m => !m.isRead).length
        this.setData({ unreadCount: newCount })
        this.updateTabBadge(newCount)
        this.filterMessages()
      })
    }

    const url = msg.jumpUrl
    if (url) {
      wx.navigateTo({ url })
    }
  },

  onMarkAllRead() {
    const unread = this.data.allMessages.filter(m => !m.isRead)
    if (unread.length === 0) {
      wx.showToast({ title: '没有未读消息', icon: 'none' })
      return
    }

    wx.showModal({
      title: '标记全部已读',
      content: `确认将 ${unread.length} 条未读消息标为已读？`,
      success: (res) => {
        if (res.confirm) {
          unread.forEach(m => { m.isRead = true })
          Promise.all(unread.map(m => markAsRead(m.msgId))).then(() => {
            this.setData({ unreadCount: 0 })
            this.updateTabBadge(0)
            this.filterMessages()
            wx.showToast({ title: '已全部标为已读', icon: 'success' })
          })
        }
      }
    })
  },

  updateTabBadge(count) {
    const app = getApp()
    if (app && app.globalData) {
      app.globalData.messageUnread = count
    }

    if (typeof this.getTabBar === 'function' && this.getTabBar()) {
      this.getTabBar().setData({
        messageBadge: count > 0 ? String(count > 99 ? '99+' : count) : ''
      })
    }
  }
})
