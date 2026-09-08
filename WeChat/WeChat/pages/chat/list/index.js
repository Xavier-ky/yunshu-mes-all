import { getChatGroups } from '../../../services/chat'
import { getCurrentUser, getRoleCode } from '../../../utils/auth'
import { formatRelativeTime } from '../../../utils/format'

const BASE_GRADIENTS = [
  '135deg, #005A75, #0098C8',
  '135deg, #006080, #00A8D0',
  '135deg, #004D6B, #0088B8',
  '135deg, #006888, #00B0D8',
  '135deg, #005070, #0090C0',
  '135deg, #006890, #00A0D0',
]

function avatarGradient(str) {
  if (!str) return BASE_GRADIENTS[0]
  let hash = 0
  for (let i = 0; i < String(str).length; i++) {
    hash = ((hash << 5) - hash) + String(str).charCodeAt(i)
    hash |= 0
  }
  return BASE_GRADIENTS[Math.abs(hash) % BASE_GRADIENTS.length]
}

function initial(name) {
  if (!name) return '?'
  return String(name).charAt(0).toUpperCase()
}

function groupAvatar(members) {
  if (!members || members.length === 0) return { type: 'emoji', emoji: '💬' }
  const top4 = members.slice(0, 4)
  return {
    type: 'composite',
    cells: top4.map(m => ({
      initial: initial(m.userName),
      gradient: avatarGradient(m.userId)
    }))
  }
}

Page({
  data: {
    groupList: [],
    loading: false,
    userInfo: null,
    roleCode: ''
  },

  onLoad() {
    const userInfo = getCurrentUser()
    const roleCode = getRoleCode()
    this.setData({ userInfo, roleCode })
  },

  onShow() {
    if (typeof this.getTabBar === 'function' && this.getTabBar()) {
      this.getTabBar().setData({ selected: 2 })
    }
    this.loadGroups()
  },

  loadGroups() {
    const { userInfo, roleCode } = this.data
    if (!userInfo) return

    this.setData({ loading: true })
    const userId = String(userInfo.userId || userInfo.realName)
    getChatGroups(userId, roleCode).then(res => {
      if (res.code === 0 && res.data) {
        const all = res.data.list || []

        const filtered = all.filter(g => {
          if (g.status !== 'ACTIVE') return false
          if (!g.members || !g.members.some(m =>
            String(m.userId) === userId || m.userName === userInfo.realName
          )) return false
          return true
        })
        this.setData({
          groupList: filtered.map(g => ({
            ...g,
            lastTimeStr: formatRelativeTime(g.lastMessageTime),
            _avatar: groupAvatar(g.members)
          })),
          loading: false
        })
      } else {
        this.setData({ loading: false })
      }
    }).catch(() => {
      this.setData({ loading: false })
    })
  },

  onGroupTap(e) {
    const groupId = e.currentTarget.dataset.groupId
    if (groupId) {
      wx.navigateTo({ url: `/pages/chat/detail/index?groupId=${groupId}` })
    }
  }
})
