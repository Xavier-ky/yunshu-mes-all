import { getChatDetail, sendMessage, disbandChatGroup } from '../../../services/chat'
import { getCurrentUser, getRoleCode } from '../../../utils/auth'
import { formatRelativeTime, formatTime } from '../../../utils/format'

const BIZ_NAV_MAP = {
  ANDON: '/pages/andon/detail/index',
  QUALITY: '/pages/quality/inspect/index',
  MATERIAL: '/pages/inventory/task-detail/index',
  EQUIPMENT: '/pages/equipment/detail/index'
}

const BASE_GRADIENTS = [
  '135deg, #005A75, #0098C8',
  '135deg, #006080, #00A8D0',
  '135deg, #004D6B, #0088B8',
  '135deg, #006888, #00B0D8',
  '135deg, #005070, #0090C0',
  '135deg, #006890, #00A0D0',
]

function avatarGradient(uid) {
  if (!uid) return BASE_GRADIENTS[0]
  let hash = 0
  for (let i = 0; i < String(uid).length; i++) {
    hash = ((hash << 5) - hash) + String(uid).charCodeAt(i)
    hash |= 0
  }
  return BASE_GRADIENTS[Math.abs(hash) % BASE_GRADIENTS.length]
}

function initial(name) {
  if (!name) return '?'
  return String(name).charAt(0).toUpperCase()
}

Page({
  data: {
    groupId: '',
    group: null,
    messages: [],
    loading: true,
    sending: false,

    userInfo: null,
    roleCode: '',
    userId: '',

    inputValue: '',
    scrollToView: '',

    showMenu: false,
    isCreator: false
  },

  onLoad(options) {
    const groupId = options.groupId || ''
    if (!groupId) {
      wx.showToast({ title: '缺少群聊ID', icon: 'none' })
      setTimeout(() => wx.navigateBack(), 1500)
      return
    }

    const userInfo = getCurrentUser()
    const roleCode = getRoleCode()
    const userId = String(userInfo?.userId || userInfo?.realName || '')

    this.setData({ groupId, userInfo, roleCode, userId })
    this.loadDetail()
  },

  onShow() {
    if (this.data.groupId && this.data.group) {
      this.loadDetail()
    }
  },

  loadDetail() {
    this.setData({ loading: true })
    getChatDetail(this.data.groupId).then(res => {
      if (res.code === 0 && res.data) {
        const { group, messages } = res.data
        const isCreator = String(group.creatorId) === String(this.data.userId) ||
          group.creatorName === this.data.userInfo?.realName

        const enrichedMessages = (messages || []).map(m => this._enrichMessage(m))

        this.setData({
          group,
          messages: enrichedMessages,
          loading: false,
          isCreator,
          scrollToView: messages && messages.length > 0
            ? 'msg-' + messages[messages.length - 1].msgId
            : ''
        })
      } else {
        this.setData({ loading: false })

        if (!res.data) {
          wx.showToast({ title: '群聊不存在', icon: 'none' })
          setTimeout(() => wx.navigateBack(), 1500)
        }
      }
    }).catch(() => {
      this.setData({ loading: false })
      wx.showToast({ title: '加载失败', icon: 'none' })
    })
  },

  _enrichMessage(m) {
    const uid = m.senderId || ''
    return {
      ...m,
      _avatarGradient: m.type === 'SYSTEM' ? '' : avatarGradient(uid),
      _avatarInitial: m.type === 'SYSTEM' ? '' : initial(m.senderName || ''),
      _timeStr: formatRelativeTime(m.time)
    }
  },

  onInput(e) {
    this.setData({ inputValue: e.detail.value })
  },

  onSend() {
    const { groupId, inputValue, sending, userId, userInfo, roleCode } = this.data
    const content = inputValue.trim()
    if (!content || sending) return

    this.setData({ sending: true, inputValue: '' })

    const senderName = userInfo?.realName || '我'
    sendMessage(groupId, userId, senderName, roleCode, content).then(res => {
      this.setData({ sending: false })
      if (res.code === 0) {
        const rawMsg = (res.data && res.data.message) ? res.data.message : res.data
        const newMsg = this._enrichMessage(rawMsg)
        const messages = [...this.data.messages, newMsg]
        this.setData({
          messages,
          scrollToView: 'msg-' + newMsg.msgId
        })
      } else {
        wx.showToast({ title: '发送失败', icon: 'none' })
        this.setData({ inputValue: content })
      }
    }).catch(() => {
      this.setData({ sending: false, inputValue: content })
      wx.showToast({ title: '发送失败', icon: 'none' })
    })
  },

  onBizTap() {
    const { group } = this.data
    if (!group || !group.bizRefId) return

    const base = BIZ_NAV_MAP[group.bizType]
    if (!base) return

    let param = ''
    if (group.bizType === 'ANDON') param = '?eventId=' + group.bizRefId
    else if (group.bizType === 'QUALITY') param = '?taskId=' + group.bizRefId
    else if (group.bizType === 'MATERIAL') param = '?taskId=' + group.bizRefId
    else if (group.bizType === 'EQUIPMENT') param = '?deviceCode=' + group.bizRefId

    wx.navigateTo({ url: base + param })
  },

  onToggleMenu() {
    this.setData({ showMenu: !this.data.showMenu })
  },

  onDisband() {
    wx.showModal({
      title: '解散群聊',
      content: '解散后所有成员将无法查看聊天记录。确认解散？',
      success: (res) => {
        if (res.confirm) {
          this.doDisband()
        }
      }
    })
  },

  doDisband() {
    disbandChatGroup(this.data.groupId, '群主手动解散').then(res => {
      if (res.code === 0) {
        wx.showToast({ title: '群聊已解散', icon: 'success' })
        const sysMsg = this._enrichMessage({
          msgId: 'CM-sys-disband',
          groupId: this.data.groupId,
          senderId: 'system',
          senderName: '系统',
          content: '群聊已被群主解散',
          type: 'SYSTEM',
          time: new Date().toISOString()
        })
        const messages = [...this.data.messages, sysMsg]
        this.setData({
          messages,
          'group.status': 'DISBANDED',
          showMenu: false,
          scrollToView: 'msg-CM-sys-disband'
        })
      }
    }).catch(() => {
      wx.showToast({ title: '操作失败', icon: 'none' })
    })
  },

  onViewMembers() {
    const members = this.data.group?.members || []
    const names = members.map(m => {
      const color = avatarGradient(m.userId)

      return `● ${m.userName}  ${m.roleName || ''}`
    }).join('\n')
    wx.showModal({
      title: `群成员（${members.length}人）`,
      content: names || '暂无成员',
      showCancel: false,
      confirmText: '关闭'
    })
  },

  memberGradient(uid) {
    return avatarGradient(uid)
  },

  memberInitial(name) {
    return initial(name)
  },

  formatTime(str) {
    return formatRelativeTime(str)
  }
})
