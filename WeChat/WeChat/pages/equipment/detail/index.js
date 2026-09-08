import { getDeviceDetail } from '../../../services/equipment'
import { PAGE_PATH_MAP } from '../../../utils/constants'
import { guardPageAccess } from '../../../utils/permission'

Page({
  data: {
    device: null,
    deviceCode: '',
    loading: true
  },

  onLoad(options) {
    guardPageAccess(PAGE_PATH_MAP.equipmentDetail)
    const deviceCode = options.deviceCode || ''
    this.setData({ deviceCode })
    if (deviceCode) {
      this.loadDetail()
    } else {
      this.setData({ loading: false })
    }
  },

  async loadDetail() {
    this.setData({ loading: true })
    try {
      const res = await getDeviceDetail(this.data.deviceCode)
      if (res && res.code === 0 && res.data) {
        this.setData({ device: res.data, loading: false })
      } else {
        this.setData({ device: null, loading: false })
        wx.showToast({ title: '设备不存在', icon: 'none' })
      }
    } catch (e) {
      this.setData({ device: null, loading: false })
      wx.showToast({ title: '加载失败，请重试', icon: 'none' })
    }
  },

  onRepair() {
    wx.navigateTo({
      url: `${PAGE_PATH_MAP.equipmentRepair}?deviceCode=${this.data.deviceCode}`
    })
  },

  onTrace() {
    wx.navigateTo({
      url: PAGE_PATH_MAP.traceabilityProduct
    })
  }
})
