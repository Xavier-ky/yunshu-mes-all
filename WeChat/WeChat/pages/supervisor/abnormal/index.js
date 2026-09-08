

import { guardPageAccess } from '../../../utils/permission'
import { PAGE_PATH_MAP } from '../../../utils/constants'
import { getEvents } from '../../../services/andon'
import { getAbnormalOverview } from '../../../services/dashboard'
import { getDevices } from '../../../services/equipment'
import { getTasks } from '../../../services/production'

const TABS = [
  { key: 'andon', label: '安灯异常', icon: '⚠' },
  { key: 'quality', label: '质量异常', icon: '✗' },
  { key: 'equipment', label: '设备异常', icon: '🔧' }
]

function andonTagClass(status) {
  if (status === 'INITIATED') return 'tag-red'
  if (status === 'PROCESSING' || status === 'RESPONDED') return 'tag-orange'
  return 'tag-green'
}

function equipTagClass(status) {
  return status === 'FAULT' ? 'tag-red' : 'tag-orange'
}

Page({
  data: {
    tabs: TABS,
    activeTab: 'andon',
    loading: true,
    refreshing: false,

    stats: { andon: 0, quality: 0, equipment: 0 },

    statAndonClass: 'stat-card stat-andon',
    statQualityClass: 'stat-card stat-quality',
    statEquipmentClass: 'stat-card stat-equipment',

    andonList: [],
    qualityList: [],
    equipmentList: []
  },

  onLoad(options) {
    guardPageAccess(PAGE_PATH_MAP.supervisorAbnormal)
    if (options && options.tab) {
      this.setData({ activeTab: options.tab })
      this.updateStatClasses(options.tab)
    }
  },

  onShow() {
    this.loadAll()
  },

  updateStatClasses(activeKey) {
    const act = activeKey || this.data.activeTab
    this.setData({
      statAndonClass: 'stat-card stat-andon' + (act === 'andon' ? ' stat-active' : ''),
      statQualityClass: 'stat-card stat-quality' + (act === 'quality' ? ' stat-active' : ''),
      statEquipmentClass: 'stat-card stat-equipment' + (act === 'equipment' ? ' stat-active' : '')
    })
  },

  async loadAll() {
    this.setData({ loading: true })
    await Promise.allSettled([
      this.loadAndonEvents(),
      this.loadQualityAbnormals(),
      this.loadEquipmentAbnormals()
    ])
    this.setData({ loading: false })
  },

  async onRefresh() {
    this.setData({ refreshing: true })
    await this.loadAll()
    this.setData({ refreshing: false })
  },

  async loadAndonEvents() {
    try {
      const res = await getEvents()
      const raw = (res && res.data && res.data.list || [])
        .filter(e => e.status !== 'CLOSED' && e.status !== 'RESOLVED')
      const list = raw.map(item => ({
        ...item,
        _tagClass: andonTagClass(item.status)
      }))
      this.setData({ andonList: list, 'stats.andon': list.length })
    } catch (_) {
      this.setData({ andonList: [], 'stats.andon': 0 })
    }
  },

  async loadQualityAbnormals() {
    try {

      const overviewRes = await getAbnormalOverview()
      const overviewData = overviewRes && overviewRes.data
      const frozenCount = overviewData ? (overviewData.frozenWorkOrders || 0) : 0

      let list = []
      if (frozenCount > 0) {
        const tasksRes = await getTasks({ frozen: true })
        const raw = (tasksRes && tasksRes.data && tasksRes.data.list) || []
        list = raw.map(t => ({
          workOrderNo: t.workOrderNo || t.taskNo || '',
          productName: t.productName || '未知产品',
          reason: t.freezeReason || t.description || '质量缺陷',
          frozenAt: t.freezeTime || t.createTime,
          statusLabel: '已冻结'
        }))
      }
      this.setData({ qualityList: list, 'stats.quality': frozenCount })
    } catch (_) {
      this.setData({ qualityList: [], 'stats.quality': 0 })
    }
  },

  async loadEquipmentAbnormals() {
    try {
      const res = await getDevices()
      const raw = (res && res.data && res.data.list || [])
        .filter(d => d.status === 'FAULT' || d.status === 'REPAIRING')
      const list = raw.map(item => ({
        ...item,
        _tagClass: equipTagClass(item.status)
      }))
      this.setData({ equipmentList: list, 'stats.equipment': list.length })
    } catch (_) {
      this.setData({ equipmentList: [], 'stats.equipment': 0 })
    }
  },

  onTabChange(e) {
    const key = e.currentTarget.dataset.key
    if (key !== this.data.activeTab) {
      this.setData({ activeTab: key })
      this.updateStatClasses(key)
    }
  },

  onStatTap(e) {
    const key = e.currentTarget.dataset.key
    if (key !== this.data.activeTab) {
      this.setData({ activeTab: key })
      this.updateStatClasses(key)
    }
  },

  onAndonTap(e) {
    const id = e.currentTarget.dataset.id
    if (id) {
      wx.navigateTo({ url: `${PAGE_PATH_MAP.andonDetail}?eventId=${id}` })
    }
  },

  onEquipmentTap(e) {
    const code = e.currentTarget.dataset.code
    if (code) {
      wx.navigateTo({ url: `${PAGE_PATH_MAP.equipmentDetail}?deviceCode=${code}` })
    }
  }
})
