import { getDevices, getTpmAlerts } from '../../../services/equipment'
import { EQUIPMENT_STATUS_MAP, PAGE_PATH_MAP } from '../../../utils/constants'
import { guardPageAccess } from '../../../utils/permission'

Page({
  data: {
    activeStatus: '',
    deviceList: [],
    loading: false,
    statusList: [
      { label: '全部', value: '' },
      { label: '运行', value: 'RUNNING' },
      { label: '停机', value: 'STOPPED' },
      { label: '故障', value: 'FAULT' },
      { label: '维修中', value: 'REPAIRING' },
      { label: '保养中', value: 'MAINTAINING' },
      { label: '超期未检', value: 'OVERDUE_MAINT' },
      { label: '已锁定', value: 'LOCKED' }
    ]
  },

  onLoad() {
    guardPageAccess(PAGE_PATH_MAP.equipmentStatus)
  },

  async onShow() {

    this.loadTpmAlerts()
    this.loadDevices()
  },

  async loadTpmAlerts() {
    try {
      const res = await getTpmAlerts()
      const data = (res && res.data) || {}
      if (data.lockedCount > 0) {
        const names = (data.devices || []).map(d => d.deviceName || d.deviceCode).join('、')
        wx.showToast({ title: `${data.lockedCount} 台设备超期未点检：${names}`, icon: 'none', duration: 3000 })
      }
    } catch (_) {  }
  },

  async loadDevices() {
    this.setData({ loading: true })
    const params = {}
    if (this.data.activeStatus) {
      params.status = this.data.activeStatus
    }
    try {
      const res = await getDevices(params)
      if (res.code === 0) {
        const list = res.data.list || []
        const deviceList = list.map(item => ({
          ...item,
          statusLabel: EQUIPMENT_STATUS_MAP[item.status] || item.status
        }))
        this.setData({ deviceList, loading: false })
      } else {
        this.setData({ loading: false })
        wx.showToast({ title: res.message || '加载失败', icon: 'none' })
      }
    } catch (err) {
      this.setData({ loading: false })
      wx.showToast({ title: '网络异常', icon: 'none' })
    }
  },

  onStatusChange(e) {
    const value = e.currentTarget.dataset.value
    this.setData({ activeStatus: value })
    this.loadDevices()
  },

  onDeviceTap(e) {
    const deviceCode = e.currentTarget.dataset.deviceCode
    wx.navigateTo({
      url: `${PAGE_PATH_MAP.equipmentDetail}?deviceCode=${deviceCode}`
    })
  }
})