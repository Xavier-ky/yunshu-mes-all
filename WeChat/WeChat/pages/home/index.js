import { getMobileSummary, getSupervisorSummary, getLineProgress } from '../../services/dashboard'
import { getTasks } from '../../services/production'
import { getCurrentUser, getRoleCode } from '../../utils/auth'
import { PAGE_PATH_MAP } from '../../utils/constants'
import { formatNumber } from '../../utils/format'
import { getVisibleEntries, ROLE_CODES } from '../../utils/permission'
import { getEvents } from '../../services/andon'
import { getDevices } from '../../services/equipment'
import { getChatGroups } from '../../services/chat'
import { getUnreadCount } from '../../services/message'

const app = getApp()

const TAB_PAGES = new Set([
  PAGE_PATH_MAP.home, PAGE_PATH_MAP.message, PAGE_PATH_MAP.aiAssistant, PAGE_PATH_MAP.mine
])

function getPulseClass(status) {
  if (status === 'INITIATED' || status === 'FAULT' || status === 'PENDING' || status === 'ABNORMAL') return 'pulse-alert'
  if (status === 'PROCESSING' || status === 'RESPONDED' || status === 'REPAIRING' || status === 'WARNING') return 'pulse-caution'
  if (status === 'COMPLETED' || status === 'RESOLVED' || status === 'CLOSED') return 'pulse-calm'
  return ''
}

const ROLE_ANDON_TYPES = {
  [ROLE_CODES.QUALITY_INSPECTOR]:    ['QUALITY'],
  [ROLE_CODES.WAREHOUSE_KEEPER]:     ['MATERIAL'],
  [ROLE_CODES.EQUIPMENT_MAINTAINER]: ['EQUIPMENT'],

}

async function buildPulseCards(roleCode, operatorName) {
  const cards = []

  try {

    const andonRes = await getEvents()
    const andonList = (andonRes && andonRes.data && andonRes.data.list) || []
    const allowedTypes = ROLE_ANDON_TYPES[roleCode]

    andonList
      .filter(e => {
        if (e.status === 'RESOLVED' || e.status === 'CLOSED' || e.status === 'COMPLETED') return false

        if (allowedTypes) return allowedTypes.includes(e.eventType)
        if (roleCode === ROLE_CODES.LINE_OPERATOR) return operatorName ? e.createdByName === operatorName : true
        return true
      })
      .slice(0, 3)
      .forEach(e => {
        cards.push({
          id: e.eventId,
          icon: '⚠️',
          title: `${e.eventTypeName || '安灯'} · ${e.lineName || e.workstationName || ''}`,
          subtitle: (e.description || '').substring(0, 40),
          status: e.status, statusLabel: e.statusLabel,
          time: e.createTime ? new Date(e.createTime).toLocaleTimeString('zh-CN', { hour: '2-digit', minute: '2-digit' }) : '',
          pulseClass: getPulseClass(e.status),
          visible: true,
          jumpUrl: `${PAGE_PATH_MAP.andonDetail}?eventId=${e.eventId}`
        })
      })
  } catch (_) {  }

  try {

    if ([ROLE_CODES.EQUIPMENT_MAINTAINER, ROLE_CODES.PRODUCTION_SUPERVISOR, ROLE_CODES.MANAGER].includes(roleCode)) {
      const deviceRes = await getDevices({ status: '' })
      const devices = (deviceRes && deviceRes.data && deviceRes.data.list) || []
      devices
        .filter(d => d.status === 'FAULT' || d.status === 'REPAIRING')
        .slice(0, 2)
        .forEach(d => {
          cards.push({
            id: d.deviceCode,
            icon: '🔧',
            title: `${d.deviceName}维修`,
            subtitle: (d.faultDescription || '设备维修中').substring(0, 30),
            status: d.status, statusLabel: d.statusLabel,
            time: '',
            pulseClass: 'pulse-caution', visible: true,
            jumpUrl: `${PAGE_PATH_MAP.equipmentDetail}?deviceCode=${d.deviceCode}`
          })
        })
    }
  } catch (_) {  }

  cards.sort((a, b) => {
    const order = { 'pulse-alert': 0, 'pulse-caution': 1, 'pulse-calm': 2 }
    return (order[a.pulseClass] || 1) - (order[b.pulseClass] || 1)
  })
  return cards
}

Page({
  data: {

    userInfo: { realName: '', roleName: '', roleCode: '', departmentName: '', lineName: '', shiftName: '' },
    userNameFirst: '', currentDate: '', shiftHours: '',

    kpiPrimary: { label: '排产达成率', value: '75', unit: '%', target: 100, color: '#0088AA' },
    kpiSubtitle: '',
    kpiSecondary: [
      { label: '产量', value: '--', unit: '台' },
      { label: '良率', value: '--', unit: '%' },
      { label: '异常', value: '--', unit: '起' }
    ],

    pulseCards: [],

    myTasks: [],

    visibleEntries: [],

    isOperator: false, isInspector: false, isKeeper: false,
    isMaintainer: false, isSupervisor: false, isManager: false,

    lineProgress: [],

    unreadMsgCount: 0
  },

  onShow() {
    if (typeof this.getTabBar === 'function' && this.getTabBar()) {
      this.getTabBar().setData({ selected: 0 })
    }
    if (!app.isLoggedIn()) { app.navigateToLogin(); return }
    this.setShiftInfo()
    this.loadUserInfo()
    this.loadDashboard()
    this.loadPulseCards()
    this.loadEntries()
    this.checkUnread()
  },

  setShiftInfo() {
    const now = new Date()
    const y = now.getFullYear(); const m = String(now.getMonth()+1).padStart(2,'0'); const d = String(now.getDate()).padStart(2,'0')
    const w = ['日','一','二','三','四','五','六'][now.getDay()]
    const hour = now.getHours(); const min = now.getMinutes()
    const worked = Math.max(0, (hour - 8) + min / 60)
    const shiftStr = worked >= 1 ? `在岗 ${Math.floor(worked)}h${Math.floor((worked%1)*60)||0}min` : '在岗 <1h'
    this.setData({ currentDate: `${y}/${m}/${d} 周${w}`, shiftHours: shiftStr })
  },

  loadUserInfo() {
    const userInfo = getCurrentUser() || app.globalData.userInfo
    if (userInfo) {
      const rc = userInfo.roleCode || ''
      this.setData({
        userInfo, userNameFirst: (userInfo.realName||'?').charAt(0),
        isOperator: rc===ROLE_CODES.LINE_OPERATOR,
        isInspector: rc===ROLE_CODES.QUALITY_INSPECTOR,
        isKeeper: rc===ROLE_CODES.WAREHOUSE_KEEPER,
        isMaintainer: rc===ROLE_CODES.EQUIPMENT_MAINTAINER,
        isSupervisor: rc===ROLE_CODES.PRODUCTION_SUPERVISOR,
        isManager: rc===ROLE_CODES.MANAGER
      })

      this.initRoleKpi(rc)
    }
  },

  initRoleKpi(rc) {
    const kpiMap = {
      [ROLE_CODES.LINE_OPERATOR]:         { label: '排产达成率', value: '75', target: 100, color: '#F59E0B', subtitle: '今日已完成 150/200 台' },
      [ROLE_CODES.QUALITY_INSPECTOR]:     { label: '批次合格率', value: '96', target: 100, color: '#10B981', subtitle: '48/50 批合格 · 目标 100%' },
      [ROLE_CODES.WAREHOUSE_KEEPER]:      { label: '拣货完成率', value: '67', target: 100, color: '#F59E0B', subtitle: '80/120 单已完成 · 待拣 40 单' },
      [ROLE_CODES.EQUIPMENT_MAINTAINER]:  { label: '设备 OEE',   value: '86', target: 90,  color: '#3B82F6', subtitle: '综合效率 = 可用率×性能×质量' },
      [ROLE_CODES.PRODUCTION_SUPERVISOR]: { label: '全厂达成率', value: '92', target: 100, color: '#06B6D4', subtitle: '今日产出 2,860 台 · 环比 +2.3%' },
      [ROLE_CODES.MANAGER]:               { label: '全厂达成率', value: '92', target: 100, color: '#06B6D4', subtitle: '4 条产线运行中 · 综合良率 97.1%' }
    }
    const kpi = kpiMap[rc] || kpiMap[ROLE_CODES.LINE_OPERATOR]
    this.setData({
      'kpiPrimary.label': kpi.label,
      'kpiPrimary.value': kpi.value,
      'kpiPrimary.target': kpi.target,
      'kpiPrimary.color': kpi.color,
      kpiSubtitle: kpi.subtitle
    })
  },

  async loadPulseCards() {
    const rc = getRoleCode()
    const userInfo = getCurrentUser()
    const operatorName = (userInfo && userInfo.realName) || ''
    try {
      const cards = await buildPulseCards(rc, operatorName)
      this.setData({ pulseCards: Array.isArray(cards) ? cards : [] })
    } catch (_) {
      this.setData({ pulseCards: [] })
    }
  },

  loadEntries() {
    this.setData({ visibleEntries: getVisibleEntries() })

    if (getRoleCode() === ROLE_CODES.LINE_OPERATOR) this.loadMyTasks()

    const rc = getRoleCode()
    if (rc===ROLE_CODES.PRODUCTION_SUPERVISOR) this.loadLineProgress()
  },

  loadMyTasks() {
    getTasks({ status: 'PENDING' }).then(res => {
      if (res.code === 0 && res.data) {
        const tasks = (res.data.list||[]).slice(0, 4).map(t => ({
          ...t,
          progress: t.planQty > 0 ? Math.round((t.completedQty||0) / t.planQty * 100) : 0
        }))
        this.setData({ myTasks: tasks })
      }
    }).catch(() => {})
  },

  loadLineProgress() {
    getLineProgress().then(res => {
      if (res.code === 0 && res.data) {
        const lines = (res.data.lines || []).map(l => ({
          ...l,
          progress: l.planQty > 0 ? Math.round(l.actualQty / l.planQty * 100) : 0
        }))
        this.setData({ lineProgress: lines })
      }
    }).catch(() => {})
  },

  loadDashboard() {
    const rc = getRoleCode()
    const kpiSecondary = [...this.data.kpiSecondary]

    if (rc===ROLE_CODES.PRODUCTION_SUPERVISOR||rc===ROLE_CODES.MANAGER) {
      getSupervisorSummary().then(res => {
        if (res.code===0&&res.data) {
          const s = res.data
          kpiSecondary[0].value = formatNumber(s.todayOutput)
          kpiSecondary[1].value = s.todayQualifiedRate
          kpiSecondary[2].value = s.todayAbnormalCount
          const primaryVal = 86
          const primaryTarget = rc===ROLE_CODES.MANAGER ? 100 : 90

          const ratio = primaryTarget > 0 ? primaryVal / primaryTarget : 0
          const ringColor = ratio >= 1 ? '#10B981' : ratio >= 0.85 ? '#06B6D4' : '#F59E0B'

          this.setData({
            kpiSecondary,
            'kpiPrimary.value': String(primaryVal),
            'kpiPrimary.label': rc===ROLE_CODES.MANAGER ? '全厂达成率' : '设备 OEE',
            'kpiPrimary.unit': '%',
            'kpiPrimary.target': primaryTarget,
            'kpiPrimary.color': ringColor
          })
        }
      })
    } else {
      getMobileSummary().then(res => {
        if (res.code===0&&res.data) {
          const s = res.data
          kpiSecondary[0].value = formatNumber(s.todayOutput)
          kpiSecondary[1].value = s.todayQualifiedRate
          kpiSecondary[2].value = s.todayAbnormalCount || '--'
          this.setData({ kpiSecondary })
        }
      })
    }
  },

  checkUnread() {
    const rc = getRoleCode()
    const userInfo = getCurrentUser()
    const operatorName = (userInfo && userInfo.realName) || ''
    getUnreadCount(rc, operatorName).then(res => {
      if (res.code === 0 && res.data) {
        const count = res.data.count || 0
        this.setData({ unreadMsgCount: count })

        if (app?.globalData) app.globalData.messageUnread = count
        if (typeof this.getTabBar === 'function' && this.getTabBar()) {
          this.getTabBar().setData({
            messageBadge: count > 0 ? String(count > 99 ? '99+' : count) : ''
          })
        }
      }
    }).catch(() => {})
  },

  onPulseCardTap(e) {
    const { url } = e.currentTarget.dataset
    if (url) wx.navigateTo({ url })
  },

  onTaskTap(e) {
    const taskId = e.currentTarget.dataset.taskid
    if (taskId) wx.navigateTo({ url: PAGE_PATH_MAP.productionTaskDetail+'?taskId='+taskId })
  },

  onEntryTap(e) {
    const { page, istab } = e.currentTarget.dataset
    if (!page) return
    istab ? wx.switchTab({ url: page }) : wx.navigateTo({ url: page })
  },

  onKpiTap() {
    const rc = getRoleCode()
    if (rc===ROLE_CODES.EQUIPMENT_MAINTAINER) {
      wx.navigateTo({ url: PAGE_PATH_MAP.equipmentStatus })
    } else if (rc===ROLE_CODES.PRODUCTION_SUPERVISOR||rc===ROLE_CODES.MANAGER) {
      wx.navigateTo({ url: PAGE_PATH_MAP.supervisorDashboard })
    } else {
      wx.navigateTo({ url: PAGE_PATH_MAP.productionReport })
    }
  },

  goToProfile() { wx.switchTab({ url: PAGE_PATH_MAP.mine }) },
  goToMessages() { wx.switchTab({ url: PAGE_PATH_MAP.message }) },
  goToDashboard() { wx.navigateTo({ url: PAGE_PATH_MAP.supervisorDashboard }) }
})
