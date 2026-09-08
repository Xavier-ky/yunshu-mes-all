import { getProductTrace } from '../../../services/traceability'
import { formatDateTime } from '../../../utils/format'

Page({
  data: {
    scanValue: '',
    productInfo: null,
    processHistory: [],
    materialBindings: [],
    qualityRecords: [],
    andonRecords: [],
    loading: false,
    hasSearched: false
  },

  onLoad(options) {
    const code = options.code || ''
    if (code) {
      this.setData({ scanValue: code })
      this.searchProduct(code)
    }
  },

  onScan(e) {
    const { code } = e.detail
    this.setData({ scanValue: code })
    this.searchProduct(code)
  },

  onInput(e) {
    this.setData({ scanValue: e.detail.value })
  },

  onSearch() {
    const sn = this.data.scanValue.trim()
    if (!sn) {
      wx.showToast({ title: '请输入产品SN', icon: 'none' })
      return
    }
    this.searchProduct(sn)
  },

  searchProduct(sn) {
    this.setData({ loading: true, hasSearched: true })
    getProductTrace(sn).then(res => {
      if (res.code === 0 && res.data) {
        const data = res.data
        this.setData({
          productInfo: {
            productSn: data.productSn,
            productName: data.productName,
            productModel: data.productModel,
            workOrderNo: data.workOrderNo,
            lineName: data.lineName || '',
            currentStatus: data.currentStatus,
            currentStatusLabel: data.currentStatusLabel || data.currentStatus,
            productionTime: formatDateTime(data.productionTime)
          },
          processHistory: (data.processHistory || []).map(item => ({
            ...item,
            startTimeStr: formatDateTime(item.startTime),
            endTimeStr: formatDateTime(item.endTime)
          })),
          materialBindings: data.materialBindings || [],
          qualityRecords: (data.qualityRecords || []).map(item => ({
            ...item,
            inspectTimeStr: formatDateTime(item.inspectTime)
          })),
          andonRecords: data.andonRecords || []
        })
      } else {
        wx.showToast({ title: res.message || '未找到产品信息', icon: 'none' })
        this.setData({
          productInfo: null,
          processHistory: [],
          materialBindings: [],
          qualityRecords: [],
          andonRecords: []
        })
      }
    }).catch(() => {
      wx.showToast({ title: '查询失败', icon: 'none' })
    }).finally(() => {
      this.setData({ loading: false })
    })
  }
})