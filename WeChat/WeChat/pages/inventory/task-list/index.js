import { getInboundTasks, getOutboundTasks, submitOutbound, lookupMaterialByBatch } from '../../../services/inventory'
import { PAGE_PATH_MAP } from '../../../utils/constants'
import { guardPageAccess } from '../../../utils/permission'
import { formatDateTime } from '../../../utils/format'

const DIRECTION_TABS = [
  { label: '入库', value: 'INBOUND' },
  { label: '出库', value: 'OUTBOUND' }
]

const INBOUND_STATUS_LIST = [
  { label: '全部', value: '' },
  { label: '待入库', value: 'PENDING' },
  { label: '已入库', value: 'COMPLETED' }
]

const OUTBOUND_STATUS_LIST = [
  { label: '全部', value: '' },
  { label: '待出库', value: 'PENDING' },
  { label: '已出库', value: 'COMPLETED' }
]

const DESTINATION_OPTIONS = ['总装一线', '总装二线', '返修区']

Page({
  data: {
    activeDirection: 'INBOUND',
    activeStatus: '',
    taskList: [],
    loading: false,
    directionTabs: DIRECTION_TABS,
    inboundStatusList: INBOUND_STATUS_LIST,
    outboundStatusList: OUTBOUND_STATUS_LIST,

    showOutboundForm: false,
    outboundForm: {
      materialCode: '',
      materialName: '',
      spec: '',
      batchNo: '',
      outboundQty: 0,
      destination: '',
      recipient: '',
      notes: ''
    },
    destinationOptions: DESTINATION_OPTIONS,
    destinationIndex: -1,
    submittingOutbound: false
  },

  onLoad() {
    guardPageAccess(PAGE_PATH_MAP.inventoryTaskList)
  },

  onShow() {
    this.loadTasks()
  },

  loadTasks() {
    const { activeDirection, activeStatus } = this.data
    const params = activeStatus ? { status: activeStatus } : {}
    const fetcher = activeDirection === 'INBOUND' ? getInboundTasks : getOutboundTasks

    this.setData({ loading: true })
    fetcher(params).then(res => {
      if (res.code === 0 && res.data) {
        this.setData({
          taskList: (res.data.list || []).map(item => ({
            ...item,
            taskType: activeDirection,
            createTimeStr: formatDateTime(item.createTime)
          })),
          loading: false
        })
      } else {
        this.setData({ loading: false })
      }
    }).catch(() => {
      this.setData({ loading: false })
      wx.showToast({ title: '加载失败', icon: 'none' })
    })
  },

  onDirectionChange(e) {
    const val = e.currentTarget.dataset.value
    this.setData({ activeDirection: val, activeStatus: '' })
    this.loadTasks()
  },

  onStatusChange(e) {
    this.setData({ activeStatus: e.currentTarget.dataset.value })
    this.loadTasks()
  },

  onTaskTap(e) {
    const taskId = e.currentTarget.dataset.taskId
    if (taskId) {
      wx.navigateTo({ url: `${PAGE_PATH_MAP.inventoryTaskDetail}?taskId=${taskId}` })
    }
  },

  onToggleOutboundForm() {
    const show = !this.data.showOutboundForm
    this.setData({ showOutboundForm: show })
    if (!show) {

      this.setData({
        outboundForm: { materialCode: '', materialName: '', spec: '', batchNo: '', outboundQty: 0, destination: '', recipient: '', notes: '' },
        destinationIndex: -1
      })
    }
  },

  onScanMaterial() {
    wx.scanCode({
      success: (res) => {
        const batchNo = res.result
        this.lookupOutboundMaterial(batchNo)
      }
    })
  },

  async lookupOutboundMaterial(batchNo) {

    let info = null
    try {
      const res = await lookupMaterialByBatch(batchNo)
      if (res && res.code === 0 && res.data) {
        info = res.data
      }
    } catch (_) {

    }

    if (!info) {
      wx.showToast({ title: '未找到该批次物料', icon: 'none' })
      return
    }

    this.setData({
      'outboundForm.materialCode': info.materialCode || batchNo,
      'outboundForm.materialName': info.materialName || '',
      'outboundForm.spec': info.spec || '',
      'outboundForm.batchNo': info.batchNo || batchNo
    })
  },

  onOutboundMaterialSearch(e) {
    this.setData({ 'outboundForm.materialCode': e.detail.value })
  },

  onOutboundMaterialSearchConfirm(e) {
    const value = e.detail.value
    if (value) {

      lookupMaterialByBatch(value).then(res => {
        if (res && res.code === 0 && res.data) {
          const info = res.data
          this.setData({
            'outboundForm.materialCode': info.materialCode || value,
            'outboundForm.materialName': info.materialName || '',
            'outboundForm.spec': info.spec || '',
            'outboundForm.batchNo': info.batchNo || value
          })
        } else {
          this.setData({
            'outboundForm.materialCode': value,
            'outboundForm.materialName': '',
            'outboundForm.spec': '',
            'outboundForm.batchNo': ''
          })
        }
      }).catch(() => {
        this.setData({
          'outboundForm.materialCode': value,
          'outboundForm.materialName': '',
          'outboundForm.spec': '',
          'outboundForm.batchNo': ''
        })
      })
    }
  },

  onOutboundQtyChange(e) {
    const delta = parseInt(e.currentTarget.dataset.delta)
    const current = parseInt(this.data.outboundForm.outboundQty) || 0
    const newVal = Math.max(0, current + delta)
    this.setData({ 'outboundForm.outboundQty': newVal })
  },

  onOutboundQtyInput(e) {
    const val = Math.max(0, parseInt(e.detail.value) || 0)
    this.setData({ 'outboundForm.outboundQty': val })
  },

  onDestinationChange(e) {
    const index = parseInt(e.detail.value)
    this.setData({
      destinationIndex: index,
      'outboundForm.destination': DESTINATION_OPTIONS[index]
    })
  },

  onRecipientInput(e) {
    this.setData({ 'outboundForm.recipient': e.detail.value })
  },

  onNotesInput(e) {
    this.setData({ 'outboundForm.notes': e.detail.value })
  },

  onSubmitOutbound() {
    const { materialCode, materialName, outboundQty, destination, recipient, notes } = this.data.outboundForm
    const { submittingOutbound } = this.data

    if (submittingOutbound) return

    if (!materialName) {
      wx.showToast({ title: '请选择物料', icon: 'none' })
      return
    }
    const qty = parseInt(outboundQty) || 0
    if (qty <= 0) {
      wx.showToast({ title: '请输入有效的出库数量', icon: 'none' })
      return
    }
    if (!destination) {
      wx.showToast({ title: '请选择去向', icon: 'none' })
      return
    }
    if (!recipient.trim()) {
      wx.showToast({ title: '请输入领用人', icon: 'none' })
      return
    }

    this.setData({ submittingOutbound: true })

    submitOutbound({
      materialCode: materialCode,
      materialName: materialName,
      spec: this.data.outboundForm.spec,
      batchNo: this.data.outboundForm.batchNo,
      qty: qty,
      destination: destination,
      recipient: recipient.trim(),
      notes: (notes || '').trim()
    }).then(res => {
      this.setData({ submittingOutbound: false })
      if (res.code === 0) {
        wx.showToast({ title: '出库单已创建', icon: 'success', duration: 2000 })

        this.setData({
          showOutboundForm: false,
          outboundForm: { materialCode: '', materialName: '', spec: '', batchNo: '', outboundQty: 0, destination: '', recipient: '', notes: '' },
          destinationIndex: -1
        })

        this.loadTasks()
      } else {
        wx.showToast({ title: res.message || '创建失败', icon: 'none' })
      }
    }).catch(() => {
      this.setData({ submittingOutbound: false })
      wx.showToast({ title: '提交失败', icon: 'none' })
    })
  }
})
